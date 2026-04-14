package data

import io.github.cdimascio.dotenv.dotenv
import data.models.*
import java.sql.DriverManager

object AdminService {
    private val env = dotenv()
    private val url = env["DATABASE_URL"] ?: error("DATABASE_URL is not set")
    private val user = env["DATABASE_USER"] ?: ""
    private val pass = env["DATABASE_PASSWORD"] ?: ""
    
    // Загрузка админских ID из .env
    val adminIds = env["ADMINS_ID"] 
        ?.removeSurrounding("[", "]") 
        ?.split(",")
        ?.map { it.trim().toLongOrNull() }
        ?.filterNotNull() 
        ?: emptyList()
    
    init {
        println("DEBUG: AdminService инициализирован")
        println("DEBUG: ADMINS_ID из .env: ${env["ADMINS_ID"]}")
        println("DEBUG: Загруженные админские ID: $adminIds")
    }
    
    fun isAdmin(userId: Long): Boolean {
        val result = userId in adminIds
        println("DEBUG: isAdmin($userId) = $result, adminIds = $adminIds")
        return result
    }
    
    // Функция для перезагрузки админов (для отладки)
    fun reloadAdmins(): List<Long> {
        val envValue = env["ADMINS_ID"]
        println("DEBUG: Перезагрузка админов, сырое значение: '$envValue'")
        
        val newAdminIds = envValue 
            ?.removeSurrounding("[", "]") 
            ?.split(",")
            ?.map { it.trim().toLongOrNull() }
            ?.filterNotNull() 
            ?: emptyList()
            
        println("DEBUG: Новые админские ID: $newAdminIds")
        return newAdminIds
    }
    
    fun banUser(userId: Long, reason: String? = null): Boolean {
        return try {
            Db.execute(
                """
                insert into banned_users (user_id, reason)
                values (?, ?)
                on conflict (user_id) do update set reason = excluded.reason, banned_at = now()
                """
            ) { stmt ->
                stmt.setLong(1, userId)
                stmt.setString(2, reason)
            }
            true
        } catch (e: Exception) {
            println("Error banning user $userId: ${e.message}")
            false
        }
    }
    
    fun unbanUser(userId: Long): Boolean {
        return try {
            Db.execute(
                """
                delete from banned_users where user_id = ?
                """
            ) { stmt ->
                stmt.setLong(1, userId)
            }
            true
        } catch (e: Exception) {
            println("Error unbanning user $userId: ${e.message}")
            false
        }
    }
    
    fun isUserBanned(userId: Long): Boolean {
        return try {
            Db.single(
                """
                select count(*) as count from banned_users where user_id = ?
                """,
                bind = { stmt -> stmt.setLong(1, userId) },
                map = { rs -> rs.getInt("count") > 0 }
            ) ?: false
        } catch (e: Exception) {
            println("Error checking ban status for user $userId: ${e.message}")
            false
        }
    }
    
    fun searchUsers(query: String, limit: Int = 10): List<UserProfile> {
        return try {
            val searchPattern = "%${query.lowercase()}%"
            Db.query(
                """
                select u.user_id, u.username, u.display_name, u.bio, u.hidden, u.show_media, u.rating, u.hide_username,
                       case when bu.user_id is not null then true else false end as is_banned
                from users u
                left join banned_users bu on u.user_id = bu.user_id
                where cast(u.user_id as text) = ? 
                   or lower(u.username) like ? 
                   or lower(u.display_name) like ?
                order by u.updated_at desc
                limit ?
                """,
                bind = { stmt ->
                    stmt.setString(1, query) // Для точного совпадения ID
                    stmt.setString(2, searchPattern) // Для частичного совпадения username
                    stmt.setString(3, searchPattern) // Для частичного совпадения display_name
                    stmt.setInt(4, limit)
                },
                map = { rs ->
                    UserProfile(
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
                }
            )
        } catch (e: Exception) {
            println("Error searching users with query '$query': ${e.message}")
            emptyList()
        }
    }
    
    fun getBannedUsers(limit: Int = 20): List<BannedUser> {
        return try {
            Db.query(
                """
                select bu.user_id, u.username, u.display_name, bu.banned_at, bu.reason
                from banned_users bu
                join users u on bu.user_id = u.user_id
                order by bu.banned_at desc
                limit ?
                """,
                bind = { stmt -> stmt.setInt(1, limit) },
                map = { rs ->
                    BannedUser(
                        userId = rs.getLong("user_id"),
                        username = rs.getString("username") ?: "",
                        displayName = rs.getString("display_name") ?: "",
                        bannedAt = rs.getTimestamp("banned_at").toString(),
                        reason = rs.getString("reason")
                    )
                }
            )
        } catch (e: Exception) {
            println("Error getting banned users: ${e.message}")
            emptyList()
        }
    }
    
    fun getAllUsers(limit: Int = 20, offset: Int = 0): List<UserProfile> {
        return try {
            Db.query(
                """
                select u.user_id, u.username, u.display_name, u.bio, u.hidden, u.show_media, u.rating, u.hide_username,
                       case when bu.user_id is not null then true else false end as is_banned
                from users u
                left join banned_users bu on u.user_id = bu.user_id
                order by u.updated_at desc
                limit ? offset ?
                """,
                bind = { stmt ->
                    stmt.setInt(1, limit)
                    stmt.setInt(2, offset)
                },
                map = { rs ->
                    UserProfile(
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
                }
            )
        } catch (e: Exception) {
            println("Error getting all users: ${e.message}")
            emptyList()
        }
    }
    
    fun getUsersCount(): Int {
        return try {
            Db.single(
                """
                select count(*) as count from users
                """,
                bind = { },
                map = { rs -> rs.getInt("count") }
            ) ?: 0
        } catch (e: Exception) {
            println("Error getting users count: ${e.message}")
            0
        }
    }
    
    fun getBannedUsersCount(): Int {
        return try {
            Db.single(
                """
                select count(*) as count from banned_users
                """,
                bind = { },
                map = { rs -> rs.getInt("count") }
            ) ?: 0
        } catch (e: Exception) {
            println("Error getting banned users count: ${e.message}")
            0
        }
    }
}
