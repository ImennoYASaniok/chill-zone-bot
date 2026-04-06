package data

import io.github.cdimascio.dotenv.dotenv
import java.sql.DriverManager

class UserRepository {
    private val env = dotenv()
    private val url = env["DATABASE_URL"] ?: error("DATABASE_URL is not set")
    private val user = env["DATABASE_USER"] ?: ""
    private val pass = env["DATABASE_PASSWORD"] ?: ""
    
    fun ensure(userId: Long, username: String, displayName: String) {
        Db.execute(
            """
            insert into users(user_id, username, display_name)
            values (?, ?, ?)
            on conflict (user_id)
            do update set username = excluded.username,
                          updated_at = now()
            """
        ) { stmt ->
            stmt.setLong(1, userId)
            stmt.setString(2, username)
            stmt.setString(3, displayName)
        }

        Db.execute(
            """
            insert into game_stats(user_id)
            values (?)
            on conflict (user_id) do nothing
            """
        ) { stmt ->
            stmt.setLong(1, userId)
        }
    }

    // Временный метод для отладки - прямая проверка БД
    fun debugProfile(userId: Long): String {
        return try {
            val conn = DriverManager.getConnection(url, user, pass)
            val stmt = conn.prepareStatement("SELECT user_id, display_name, username, updated_at FROM users WHERE user_id = ?")
            stmt.setLong(1, userId)
            val rs = stmt.executeQuery()
            
            val result = if (rs.next()) {
                "DB_DIRECT: user_id=${rs.getLong("user_id")}, display_name='${rs.getString("display_name")}', username='${rs.getString("username")}', updated_at=${rs.getTimestamp("updated_at")}"
            } else {
                "DB_DIRECT: Пользователь $userId не найден"
            }
            
            rs.close()
            stmt.close()
            conn.close()
            result
        } catch (e: Exception) {
            "DB_DIRECT ERROR: ${e.message}"
        }
    }

    fun profile(userId: Long): UserProfile? {
        println("DEBUG: Запрос профиля для пользователя $userId")
        return try {
            Db.single(
                """
                select u.user_id, u.username, u.display_name, u.bio, u.hidden, u.show_media, u.rating, 
                       coalesce(u.hide_username, false) as hide_username,
                       case when bu.user_id is not null then true else false end as is_banned
                from users u
                left join banned_users bu on u.user_id = bu.user_id
                where u.user_id = ?
                """,
                bind = { stmt -> stmt.setLong(1, userId) },
                map = { rs ->
                    val profile = UserProfile(
                        userId = rs.getLong("user_id"),
                        username = rs.getString("username") ?: "",
                        displayName = rs.getString("display_name") ?: "",
                        bio = rs.getString("bio") ?: "",
                        hidden = rs.getBoolean("hidden"),
                        showMedia = rs.getBoolean("show_media"),
                        rating = rs.getInt("rating"),
                        hideUsername = rs.getBoolean("hide_username"),
                        isBanned = rs.getBoolean("is_banned")
                    )
                    println("DEBUG: Получен профиль для пользователя $userId: displayName='${profile.displayName}', username='${profile.username}', isBanned=${profile.isBanned}")
                    profile
                }
            )
        } catch (e: Exception) {
            println("ERROR: Ошибка при получении профиля пользователя $userId: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Временный метод для отладки - прямое обновление БД
    fun debugUpdateName(userId: Long, name: String): String {
        return try {
            val conn = DriverManager.getConnection(url, user, pass)
            conn.autoCommit = true // Принудительный коммит
            val stmt = conn.prepareStatement("UPDATE users SET display_name = ?, updated_at = now() WHERE user_id = ?")
            stmt.setString(1, name)
            stmt.setLong(2, userId)
            val rowsAffected = stmt.executeUpdate()
            
            val result = "DB_UPDATE_DIRECT: rowsAffected=$rowsAffected, newName='$name'"
            
            stmt.close()
            conn.close()
            result
        } catch (e: Exception) {
            "DB_UPDATE_DIRECT ERROR: ${e.message}"
        }
    }

    fun updateName(userId: Long, name: String) {
        println("DEBUG: Обновление имени для пользователя $userId на '$name'")
        
        // Сначала проверим текущее состояние
        val beforeProfile = debugProfile(userId)
        println("DEBUG: Состояние ДО обновления: $beforeProfile")
        
        try {
            Db.execute("update users set display_name = ?, updated_at = now() where user_id = ?") { stmt ->
                stmt.setString(1, name)
                stmt.setLong(2, userId)
            }
            println("DEBUG: SQL запрос на обновление имени выполнен успешно")
            
            // Проверяем сразу после обновления через прямой метод
            val afterDirect = debugProfile(userId)
            println("DEBUG: Состояние ПОСЛЕ обновления (прямой метод): $afterDirect")
            
            // Проверяем через обычный метод
            val updatedProfile = profile(userId)
            println("DEBUG: Состояние ПОСЛЕ обновления (обычный метод): displayName='${updatedProfile?.displayName}'")
            
            // Дополнительная проверка - ждем немного и проверяем еще раз
            Thread.sleep(100)
            val delayedCheck = debugProfile(userId)
            println("DEBUG: Состояние через 100мс: $delayedCheck")
            
        } catch (e: Exception) {
            println("ERROR: Ошибка при обновлении имени: ${e.message}")
            e.printStackTrace()
        }
    }

    fun updateBio(userId: Long, bio: String) {
        Db.execute("update users set bio = ?, updated_at = now() where user_id = ?") { stmt ->
            stmt.setString(1, bio)
            stmt.setLong(2, userId)
        }
    }

    fun toggleHidden(userId: Long): Boolean {
        return Db.single(
            "update users set hidden = not hidden, updated_at = now() where user_id = ? returning hidden",
            bind = { stmt -> stmt.setLong(1, userId) },
            map = { rs -> rs.getBoolean("hidden") }
        ) ?: false
    }

    fun toggleMedia(userId: Long): Boolean {
        return Db.single(
            "update users set show_media = not show_media, updated_at = now() where user_id = ? returning show_media",
            bind = { stmt -> stmt.setLong(1, userId) },
            map = { rs -> rs.getBoolean("show_media") }
        ) ?: true
    }

    fun addRating(userId: Long, delta: Int) {
        Db.execute("update users set rating = rating + ?, updated_at = now() where user_id = ?") { stmt ->
            stmt.setInt(1, delta)
            stmt.setLong(2, userId)
        }
    }

    fun toggleHideUsername(userId: Long): Boolean {
        return Db.single(
            "update users set hide_username = not coalesce(hide_username, false), updated_at = now() where user_id = ? returning coalesce(hide_username, false)",
            bind = { stmt -> stmt.setLong(1, userId) },
            map = { rs -> rs.getBoolean(1) }
        ) ?: false
    }
}
