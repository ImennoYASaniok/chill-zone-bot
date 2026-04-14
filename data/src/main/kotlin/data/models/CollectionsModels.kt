package data.models

enum class RecommendationType {
    FILM, SERIES, BOOK, GAME
}

data class RecommendationItem(
    val id: Int,
    val type: RecommendationType,
    val title: String,
    val genres: List<String>,
    val moods: List<String>,
    val year: Int,
    val description: String,
    val relevanceScore: Double = 1.0,
    val popularity: Int = 0,
    val rating: Double = 0.0,
    val posterUrl: String? = null,
    val url: String? = null,
    val source: String = "local",
    val metadata: Map<String, Any> = emptyMap()
)

data class SearchResult(
    val items: List<RecommendationItem>,
    val totalCount: Int,
    val hasMore: Boolean,
    val source: String // "local" или API название
)

data class SearchConfig(
    val maxResults: Int = 5,
    val enableApi: Boolean = true,
    val cacheTimeout: Long = 300_000, // 5 минут
    val fuzzyThreshold: Double = 0.7
)
