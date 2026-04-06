package data.apis

import data.*
import io.github.cdimascio.dotenv.dotenv
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object BookApi : ExternalApi {
    private val env = dotenv()
    private val token = env["BOOKS_API_TOKEN"]
    private val gson = ApiClient.gson
    private val client = ApiClient.client

    override fun search(query: String, limit: Int): List<RecommendationItem> {
        if (token.isNullOrBlank()) {
            logApiInfo("BookApi", "Используется Google Books API без ключа (ограниченная квота)")
        }
        
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val withKeyUrl = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=$limit" +
            (if (!token.isNullOrBlank()) "&key=$token" else "")
        val noKeyUrl = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=$limit"

        return try {
            fun parseItems(body: String): List<RecommendationItem> {
                val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                val items = json.getAsJsonArray("items") ?: run {
                    logApiInfo("BookApi", "Google Books API: 0 результатов для '$query'")
                    return emptyList()
                }
                
                val result = items.mapNotNull { item ->
                    val itemObj = item.asObjectOrNull() ?: return@mapNotNull null
                    val volumeInfo = itemObj.get("volumeInfo").asObjectOrNull() ?: return@mapNotNull null
                    val saleInfo = itemObj.get("saleInfo").asObjectOrNull()
                    val link = saleInfo?.get("buyLink")?.asStringOrNull()
                        ?: volumeInfo.get("infoLink")?.asStringOrNull()
                        ?: volumeInfo.get("previewLink")?.asStringOrNull()
                    RecommendationItem(
                        id = itemObj.get("id")?.asStringOrNull()?.hashCode() ?: itemObj.hashCode(),
                        type = RecommendationType.BOOK,
                        title = volumeInfo.get("title")?.asStringOrNull() ?: "Unknown",
                        genres = volumeInfo.get("categories")
                            ?.takeIf { it.isJsonArray }
                            ?.asJsonArray
                            ?.mapNotNull { it.asStringOrNull() }
                            ?: emptyList(),
                        moods = emptyList(),
                        year = volumeInfo.get("publishedDate")?.asStringOrNull()?.take(4)?.toIntOrNull() ?: 0,
                        description = volumeInfo.get("description")?.asStringOrNull() ?: "",
                        rating = volumeInfo.get("averageRating")?.asDoubleOrNull() ?: 0.0,
                        posterUrl = volumeInfo.get("imageLinks")
                            .asObjectOrNull()
                            ?.get("thumbnail")
                            ?.asStringOrNull(),
                        url = link ?: "",
                        source = "Google Books",
                        metadata = mapOf("url" to (link ?: ""))
                    )
                }
                return result
            }

            val url = if (!token.isNullOrBlank()) withKeyUrl else noKeyUrl
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logApiWarn("BookApi", "HTTP ${response.code} от Google Books API для '$query'")
                    return emptyList()
                }
                
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    logApiWarn("BookApi", "Пустой ответ от Google Books API для '$query'")
                    return emptyList()
                }
                
                val items = parseItems(body)
                logApiInfo("BookApi", "Найдено ${items.size} книг для '$query'")
                items
            }
        } catch (e: java.net.SocketTimeoutException) {
            logApiError("BookApi", "Таймаут при обращении к Google Books API для '$query'", e)
            emptyList()
        } catch (e: Exception) {
            logApiError("BookApi", "Ошибка при поиске книг для '$query'", e)
            emptyList()
        }
    }
}