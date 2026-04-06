package data

import data.apis.KinoApi
import data.apis.BookApi
import data.apis.GameApi

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
        
        // Фильтруем результаты с низкой релевантностью
        val filteredByRelevance = if (normalizedQuery.isNotBlank()) {
            ranked.filter { it.relevanceScore >= 10.0 }
        } else {
            ranked
        }
        
        val page = filteredByRelevance.drop(offset).take(limit)
        val resultSource = filteredByRelevance.firstOrNull()?.source ?: "external"

        return SearchResult(
            items = page,
            totalCount = filteredByRelevance.size,
            hasMore = filteredByRelevance.size > offset + limit,
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
        if (query.isBlank()) return items.sortedByDescending { it.rating }
        
        val queryWords = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val queryLower = query.lowercase()
        
        return items
            .map { item ->
                val title = item.title.lowercase()
                val description = item.description.lowercase()
                val genres = item.genres.joinToString(" ").lowercase()
                
                var score = 0.0
                
                // 1. Точное совпадение всего запроса в названии (максимальный приоритет)
                if (title.contains(queryLower)) {
                    score += 100.0
                }
                
                // 2. Совпадение всех слов запроса в названии
                val titleWords = title.split(Regex("[^a-zA-Zа-яА-Я0-9]+")).filter { it.isNotBlank() }
                val matchedWords = queryWords.count { word ->
                    titleWords.any { titleWord -> 
                        titleWord.contains(word) || word.contains(titleWord)
                    }
                }
                if (matchedWords == queryWords.size && queryWords.isNotEmpty()) {
                    score += 50.0 * matchedWords
                }
                
                // 3. Частичное совпадение слов в названии
                queryWords.forEach { word ->
                    if (word.length >= 3) {
                        // Точное совпадение отдельного слова
                        if (titleWords.contains(word)) {
                            score += 20.0
                        }
                        // Часть слова
                        else if (title.contains(word)) {
                            score += 10.0
                        }
                    } else {
                        // Короткие слова (1-2 символа)
                        if (title.contains(word)) {
                            score += 5.0
                        }
                    }
                }
                
                // 4. Совпадение в описании (меньший приоритет)
                queryWords.forEach { word ->
                    if (description.contains(word)) {
                        score += 2.0
                    }
                }
                
                // 5. Совпадение в жанрах
                queryWords.forEach { word ->
                    if (genres.contains(word)) {
                        score += 1.0
                    }
                }
                
                // 6. Бонус за рейтинг (но не так много)
                score += item.rating * 0.1
                
                // 7. Штраф за слишком короткие совпадения
                if (score > 0 && score < 15) {
                    score *= 0.1 // Сильно снижаем релевантность плохих совпадений
                }
                
                item.copy(relevanceScore = score)
            }
            .sortedByDescending { it.relevanceScore }
            .distinctBy { "${it.type}_${it.id}_${it.title}_${it.year}" }
    }
}
