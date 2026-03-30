package data

object RecommendationRepository {
    fun all(): List<RecommendationItem> = emptyList()

    fun searchMultiple(
        type: RecommendationType,
        query: String? = null,
        limit: Int = 5,
        offset: Int = 0
    ): SearchResult {
        val normalizedQuery = query?.trim().orEmpty()
        val requestQuery = normalizedQuery.ifBlank { defaultQuery(type) }
        val fetched = when (type) {
            RecommendationType.FILM, RecommendationType.SERIES -> KinoApi.search(requestQuery, limit + offset + 20)
            RecommendationType.BOOK -> BookApi.search(requestQuery, limit + offset + 20)
            RecommendationType.GAME -> GameApi.search(requestQuery, limit + offset + 20)
        }

        val filteredByType = when (type) {
            RecommendationType.FILM, RecommendationType.SERIES -> fetched.filter { it.type == type }
            else -> fetched
        }

        val ranked = rankByRelevance(filteredByType, normalizedQuery.ifBlank { requestQuery })
        val page = ranked.drop(offset).take(limit)
        val resultSource = ranked.firstOrNull()?.source ?: "external"

        return SearchResult(
            items = page,
            totalCount = ranked.size,
            hasMore = ranked.size > offset + limit,
            source = resultSource
        )
    }

    fun random(type: RecommendationType, query: String? = null): RecommendationItem? {
        return searchMultiple(type, query, limit = 20, offset = 0).items.randomOrNull()
    }

    fun summarize(item: RecommendationItem): String {
        val sourceUrl = item.metadata["url"] as? String
        return buildString {
            append(item.title).append(" (").append(item.year).append(")").append("\n")
            if (item.genres.isNotEmpty()) {
                append("Жанры: ").append(item.genres.joinToString(", ")).append("\n")
            }
            if (item.moods.isNotEmpty()) {
                append("Настроение: ").append(item.moods.joinToString(", ")).append("\n")
            }
            append("\n").append(item.description.ifBlank { "Описание отсутствует." })
            append("\nИсточник: ").append(item.source)
            if (!sourceUrl.isNullOrBlank()) {
                append("\nСсылка: ").append(sourceUrl)
            }
        }
    }

    private fun defaultQuery(type: RecommendationType): String {
        return when (type) {
            RecommendationType.FILM -> "популярные фильмы"
            RecommendationType.SERIES -> "популярные сериалы"
            RecommendationType.BOOK -> "бестселлеры"
            RecommendationType.GAME -> "popular games"
        }
    }

    private fun rankByRelevance(items: List<RecommendationItem>, query: String): List<RecommendationItem> {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return items
            .map { item ->
                val title = item.title.lowercase()
                val description = item.description.lowercase()
                val genres = item.genres.joinToString(" ").lowercase()
                val score = words.sumOf { word ->
                    var s = 0.0
                    if (title.contains(word)) s += 3.0
                    if (description.contains(word)) s += 1.5
                    if (genres.contains(word)) s += 1.0
                    s
                } + item.rating * 0.2
                item.copy(relevanceScore = score)
            }
            .sortedByDescending { it.relevanceScore }
            .distinctBy { "${it.type}_${it.id}_${it.title}_${it.year}" }
    }
}
