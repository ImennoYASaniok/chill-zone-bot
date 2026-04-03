package data.apis

import data.*
import io.github.cdimascio.dotenv.dotenv
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GameApi : ExternalApi {
    private val env = dotenv()
    private val token = env["RAWG_API_TOKEN"]
    private val gson = ApiClient.gson
    private val client = ApiClient.client

    override fun search(query: String, limit: Int): List<RecommendationItem> {
        if (token.isNullOrBlank()) {
            logApiWarn("GameApi", "RAWG_API_TOKEN не задан. Внешний поиск игр отключён.")
            return emptyList()
        }
        
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://api.rawg.io/api/games?search=$encodedQuery&page_size=$limit&key=$token"

        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logApiWarn("GameApi", "HTTP ${response.code} от RAWG API для запроса '$query'")
                    return emptyList()
                }
                
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    logApiWarn("GameApi", "Пустой ответ от RAWG API для запроса '$query'")
                    return emptyList()
                }
                
                val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                val results = json.getAsJsonArray("results") ?: run {
                    logApiInfo("GameApi", "RAWG API: 0 результатов для '$query'")
                    return emptyList()
                }
                
                val items = results.mapNotNull { item ->
                    val itemObj = item.asObjectOrNull() ?: return@mapNotNull null
                    val description = itemObj.get("description")?.asStringOrNull()
                    
                    RecommendationItem(
                        id = itemObj.get("id")?.asIntOrNull() ?: itemObj.hashCode(),
                        type = RecommendationType.GAME,
                        title = itemObj.get("name")?.asStringOrNull() ?: "Unknown",
                        genres = itemObj.get("genres")
                            ?.takeIf { it.isJsonArray }
                            ?.asJsonArray
                            ?.mapNotNull { it.asObjectOrNull()?.get("name")?.asStringOrNull() }
                            ?: emptyList(),
                        moods = emptyList(),
                        year = itemObj.get("released")?.asStringOrNull()?.take(4)?.toIntOrNull() ?: 0,
                        description = description ?: "",
                        rating = itemObj.get("rating")?.asDoubleOrNull() ?: 0.0,
                        posterUrl = itemObj.get("background_image")?.asStringOrNull(),
                        source = "RAWG",
                        metadata = mapOf(
                            "url" to (itemObj.get("website")?.asStringOrNull() ?: itemObj.get("background_image")?.asStringOrNull() ?: "")
                        )
                    )
                }
                
                // Логируем только итоговый результат
                logApiInfo("GameApi", "Найдено ${items.size} игр для '$query'")
                items
            }
        } catch (e: java.net.SocketTimeoutException) {
            logApiError("GameApi", "Таймаут при обращении к RAWG API для '$query'", e)
            emptyList()
        } catch (e: Exception) {
            logApiError("GameApi", "Ошибка при поиске игр для '$query'", e)
            emptyList()
        }
    }
}