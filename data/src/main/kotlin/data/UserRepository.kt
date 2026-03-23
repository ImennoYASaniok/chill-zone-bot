package data

class UserRepository {
    fun ensure(userId: Long, username: String, displayName: String) {
        Db.execute(
            """
            insert into users(user_id, username, display_name)
            values (?, ?, ?)
            on conflict (user_id)
            do update set username = excluded.username,
                          display_name = excluded.display_name,
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

    fun profile(userId: Long): UserProfile? {
        return Db.single(
            """
            select user_id, username, display_name, bio, hidden, show_media, rating
            from users
            where user_id = ?
            """,
            bind = { stmt -> stmt.setLong(1, userId) },
            map = { rs ->
                UserProfile(
                    userId = rs.getLong("user_id"),
                    username = rs.getString("username") ?: "",
                    displayName = rs.getString("display_name") ?: "",
                    bio = rs.getString("bio") ?: "",
                    hidden = rs.getBoolean("hidden"),
                    showMedia = rs.getBoolean("show_media"),
                    rating = rs.getInt("rating")
                )
            }
        )
    }

    fun updateName(userId: Long, name: String) {
        Db.execute("update users set display_name = ?, updated_at = now() where user_id = ?") { stmt ->
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

    fun allCount(): Int {
        return Db.single("select count(*) as c from users", map = { it.getInt("c") }) ?: 0
    }
}
