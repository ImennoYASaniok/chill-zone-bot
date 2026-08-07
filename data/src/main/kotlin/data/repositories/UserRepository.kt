package data.repositories

import data.Db
import data.models.AccountType
import data.models.UserProfile
import data.services.AdminService
import java.time.ZoneOffset

class UserRepository {
    fun ensure(userId: Long, username: String, displayName: String) {
        // Если имя не указано, берем username без символа @
        val finalDisplayName = displayName.ifBlank { username.removePrefix("@") }

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
            stmt.setString(3, finalDisplayName)
        }

        Db.execute(
                """
            insert into game_stats(user_id)
            values (?)
            on conflict (user_id) do nothing
            """
        ) { stmt -> stmt.setLong(1, userId) }
    }

    fun profile(userId: Long): UserProfile? {
        return try {
            Db.single(
                    """
                select u.user_id, u.username, u.display_name, u.bio, u.hidden, u.show_media, u.rating,
                       coalesce(u.hide_username, false) as hide_username,
                      coalesce(u.account_type, 'user') as account_type,
                       case when bu.user_id is not null and (bu.ban_expires_at is null or bu.ban_expires_at > now()) then true else false end as is_banned,
                       bu.banned_at,
                       bu.reason,
                       bu.ban_expires_at,
                       coalesce(u.created_at, now()) as created_at,
                       coalesce(u.last_activity_at, now()) as last_activity_at,
                       u.avatar_file_id
                from users u
                left join banned_users bu on u.user_id = bu.user_id
                where u.user_id = ?
                """,
                    bind = { stmt -> stmt.setLong(1, userId) },
                    map = { rs ->
                        val displayName = rs.getString("display_name") ?: ""
                        val username = rs.getString("username") ?: ""
                        // Если имя не указано, используем username без @
                        val finalDisplayName = displayName.ifBlank { username.removePrefix("@") }

                        UserProfile(
                                userId = rs.getLong("user_id"),
                                username = username,
                                displayName = finalDisplayName,
                                bio = rs.getString("bio") ?: "",
                                hidden = rs.getBoolean("hidden"),
                                showMedia = rs.getBoolean("show_media"),
                                rating = rs.getInt("rating"),
                                hideUsername = rs.getBoolean("hide_username"),
                                isBanned = rs.getBoolean("is_banned"),
                                createdAt = rs.getTimestamp("created_at")?.toLocalDateTime(),
                                bannedAt = rs.getString("banned_at"),
                                reason = rs.getString("reason"),
                                banExpiresAt =
                                        rs.getTimestamp("ban_expires_at")
                                                ?.toInstant()
                                                ?.atOffset(ZoneOffset.UTC)
                                                ?.toString(),
                                accountType = AdminService.resolveAccountType(
                                    rs.getLong("user_id"),
                                    AccountType.fromDb(rs.getString("account_type"))
                                ),
                                lastActivityAt =
                                        rs.getTimestamp("last_activity_at")?.toLocalDateTime(),
                                avatarFileId = rs.getString("avatar_file_id")
                        )
                    }
            )
        } catch (e: Exception) {
            println("ERROR: Ошибка при получении профиля пользователя $userId: ${e.message}")
            null
        }
    }

    fun updateName(userId: Long, name: String) {
        Db.execute("update users set display_name = ?, updated_at = now() where user_id = ?") { stmt
            ->
            stmt.setString(1, name)
            stmt.setLong(2, userId)
        }
    }

    fun updateBio(userId: Long, bio: String) {
        Db.execute("update users set bio = ?, updated_at = now() where user_id = ?") { stmt ->
            stmt.setString(1, bio)
            stmt.setLong(2, userId)
        }
    }

    fun updateUsername(userId: Long, username: String) {
        Db.execute("update users set username = ?, updated_at = now() where user_id = ?") { stmt ->
            stmt.setString(1, username)
            stmt.setLong(2, userId)
        }
    }

    fun toggleHidden(userId: Long): Boolean {
        return Db.single(
                "update users set hidden = not hidden, updated_at = now() where user_id = ? returning hidden",
                bind = { stmt -> stmt.setLong(1, userId) },
                map = { rs -> rs.getBoolean("hidden") }
        )
                ?: false
    }

    fun addRating(userId: Long, delta: Int) {
        Db.execute("update users set rating = rating + ?, updated_at = now() where user_id = ?") {
                stmt ->
            stmt.setInt(1, delta)
            stmt.setLong(2, userId)
        }
    }

    fun toggleMedia(userId: Long): Boolean {
        return Db.single(
                "update users set show_media = not coalesce(show_media, false), updated_at = now() where user_id = ? returning coalesce(show_media, false)",
                bind = { stmt -> stmt.setLong(1, userId) },
                map = { rs -> rs.getBoolean(1) }
        )
                ?: false
    }

    fun toggleHideUsername(userId: Long): Boolean {
        return Db.single(
                "update users set hide_username = not coalesce(hide_username, false), updated_at = now() where user_id = ? returning coalesce(hide_username, false)",
                bind = { stmt -> stmt.setLong(1, userId) },
                map = { rs -> rs.getBoolean(1) }
        )
                ?: false
    }

    fun updateLastActivity(userId: Long) {
        try {
            Db.execute("update users set last_activity_at = now() where user_id = ?") { stmt ->
                stmt.setLong(1, userId)
            }
        } catch (e: Exception) {
            println(
                    "ERROR: Ошибка при обновлении last_activity_at для пользователя $userId: ${e.message}"
            )
        }
    }

    fun getActiveUsersCount(minutesThreshold: Int = 3): Int {
        return try {
            Db.single(
                    """
                select count(*) as count from users 
                where last_activity_at > now() - interval '${minutesThreshold} minutes'
                """,
                    bind = {},
                    map = { rs -> rs.getInt("count") }
            )
                    ?: 0
        } catch (e: Exception) {
            println("ERROR: Ошибка при получении количества активных пользователей: ${e.message}")
            0
        }
    }

    fun setAvatar(userId: Long, avatarFileId: String) {
        try {
            Db.execute(
                    "update users set avatar_file_id = ?, updated_at = now() where user_id = ?"
            ) { stmt ->
                stmt.setString(1, avatarFileId)
                stmt.setLong(2, userId)
            }
        } catch (e: Exception) {
            println("ERROR: Ошибка при сохранении аватарки для пользователя $userId: ${e.message}")
        }
    }

    fun deleteAvatar(userId: Long) {
        try {
            Db.execute(
                    "update users set avatar_file_id = null, updated_at = now() where user_id = ?"
            ) { stmt -> stmt.setLong(1, userId) }
        } catch (e: Exception) {
            println("ERROR: Ошибка при удалении аватарки для пользователя $userId: ${e.message}")
        }
    }

    fun setAccountType(userId: Long, accountType: AccountType) {
        try {
            Db.execute(
                    "update users set account_type = ?, updated_at = now() where user_id = ?"
            ) { stmt ->
                stmt.setString(1, accountType.dbValue)
                stmt.setLong(2, userId)
            }
        } catch (e: Exception) {
            println("ERROR: Ошибка при обновлении типа аккаунта для пользователя $userId: ${e.message}")
        }
    }
}
