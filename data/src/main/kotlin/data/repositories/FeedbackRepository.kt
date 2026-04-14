package data

import data.models.*

class FeedbackRepository {
    fun save(userId: Long?, username: String, scope: String, text: String): Long {
        return Db.insertId(
            "insert into feedback_items(user_id, username, scope, text) values (?, ?, ?, ?)"
        ) { stmt ->
            if (userId == null) stmt.setObject(1, null) else stmt.setLong(1, userId)
            stmt.setString(2, username)
            stmt.setString(3, scope)
            stmt.setString(4, text)
        }
    }

    fun recent(limit: Int = 20): List<FeedbackItem> {
        return Db.query(
            "select id, user_id, username, scope, text, created_at from feedback_items order by id desc limit ?",
            bind = { stmt -> stmt.setInt(1, limit) },
            map = { rs ->
                FeedbackItem(
                    id = rs.getLong("id"),
                    userId = rs.getObject("user_id") as? Long,
                    username = rs.getString("username") ?: "",
                    scope = rs.getString("scope") ?: "",
                    text = rs.getString("text") ?: "",
                    createdAt = rs.getString("created_at")
                )
            }
        )
    }
}
