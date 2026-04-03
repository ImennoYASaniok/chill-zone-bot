package data

data class FavoriteItem(
    val userId: Long,
    val itemId: Int,
    val itemType: RecommendationType,
    val title: String,
    val year: Int,
    val posterUrl: String?,
    val source: String,
    val url: String?,
    val description: String?
)

object FavoriteRepository {
    fun add(userId: Long, item: RecommendationItem) {
        val url = item.metadata["url"] as? String
        val cleanDescription = item.description
            .replace(Regex("<[^>]*>"), "") // Удаляем HTML теги
            .replace(Regex("&[^;]*;"), "") // Удаляем HTML сущности
            .replace(Regex("\\s+"), " ") // Заменяем множественные пробелы
            .trim()
            .take(500) // Ограничиваем до 500 символов
        
        Db.execute(
            """
            insert into user_favorites(user_id, item_id, item_type, title, year, poster_url, source, url, description)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (user_id, item_id, item_type) do nothing
            """
        ) { stmt ->
            stmt.setLong(1, userId)
            stmt.setInt(2, item.id)
            stmt.setString(3, item.type.name)
            stmt.setString(4, item.title)
            stmt.setInt(5, item.year)
            stmt.setString(6, item.posterUrl)
            stmt.setString(7, item.source)
            stmt.setString(8, url)
            stmt.setString(9, cleanDescription)
        }
    }

    fun remove(userId: Long, itemId: Int, type: RecommendationType) {
        Db.execute("delete from user_favorites where user_id = ? and item_id = ? and item_type = ?") { stmt ->
            stmt.setLong(1, userId)
            stmt.setInt(2, itemId)
            stmt.setString(3, type.name)
        }
    }

    fun list(userId: Long): List<FavoriteItem> {
        return Db.query(
            "select item_id, item_type, title, year, poster_url, source, url, description from user_favorites where user_id = ? order by created_at desc",
            bind = { stmt -> stmt.setLong(1, userId) },
            map = { rs ->
                FavoriteItem(
                    userId = userId,
                    itemId = rs.getInt("item_id"),
                    itemType = RecommendationType.valueOf(rs.getString("item_type")),
                    title = rs.getString("title"),
                    year = rs.getInt("year"),
                    posterUrl = rs.getString("poster_url"),
                    source = rs.getString("source"),
                    url = rs.getString("url"),
                    description = rs.getString("description")
                )
            }
        )
    }

    fun isFavorite(userId: Long, itemId: Int, type: RecommendationType): Boolean {
        return Db.single(
            "select 1 from user_favorites where user_id = ? and item_id = ? and item_type = ?",
            bind = { stmt ->
                stmt.setLong(1, userId)
                stmt.setInt(2, itemId)
                stmt.setString(3, type.name)
            },
            map = { true }
        ) ?: false
    }
}
