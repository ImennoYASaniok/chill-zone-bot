package data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.cdimascio.dotenv.dotenv
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private fun logApiInfo(api: String, message: String) {
    println("INFO: [$api] $message")
}

private fun logApiWarn(api: String, message: String) {
    println("WARN: [$api] $message")
}

private fun logApiError(api: String, message: String, e: Exception? = null) {
    println("ERROR: [$api] $message")
    if (e != null) {
        println("ERROR: [$api] ${e::class.simpleName}: ${e.message}")
    }
}

private fun JsonElement?.asObjectOrNull(): JsonObject? {
    if (this == null || !this.isJsonObject) return null
    return this.asJsonObject
}

private fun JsonElement?.asStringOrNull(): String? {
    if (this == null || !this.isJsonPrimitive) return null
    return runCatching { this.asString }.getOrNull()
}

private fun JsonElement?.asIntOrNull(): Int? {
    if (this == null || !this.isJsonPrimitive) return null
    return runCatching { this.asInt }.getOrNull()
}

private fun JsonElement?.asDoubleOrNull(): Double? {
    if (this == null || !this.isJsonPrimitive) return null
    return runCatching { this.asDouble }.getOrNull()
}

interface ExternalApi {
    fun search(query: String, limit: Int): List<RecommendationItem>
}

object KinoApi : ExternalApi {
    private val env = dotenv()
    private val token = env["KINO_API_TOKEN"]
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun search(query: String, limit: Int): List<RecommendationItem> {
        if (token.isNullOrBlank()) {
            logApiWarn("KinoApi", "KINO_API_TOKEN не задан. Внешний поиск фильмов/сериалов отключён.")
            return emptyList()
        }
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())

        val request = Request.Builder()
            .url("https://api.kinopoisk.dev/v1.4/movie/search?query=$encodedQuery&limit=$limit")
            .addHeader("X-API-KEY", token)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logApiWarn("KinoApi", "HTTP ${response.code} от API Кинопоиска. URL=${request.url}")
                    return emptyList()
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    logApiWarn("KinoApi", "Пустой ответ от API Кинопоиска. URL=${request.url}")
                    return emptyList()
                }
                val json = gson.fromJson(body, JsonObject::class.java)
                val docs = json.getAsJsonArray("docs") ?: run {
                    logApiWarn("KinoApi", "Поле docs отсутствует в ответе API Кинопоиска.")
                    return emptyList()
                }

                docs.mapNotNull { doc ->
                    val obj = doc.asObjectOrNull() ?: return@mapNotNull null
                    val typeText = obj.get("type").asStringOrNull()
                    val genres = obj.get("genres")
                        ?.takeIf { it.isJsonArray }
                        ?.asJsonArray
                        ?.mapNotNull { genre ->
                            genre.asObjectOrNull()?.get("name").asStringOrNull()
                        }
                        ?: emptyList()
                    val rating = obj.get("rating")
                        .asObjectOrNull()
                        ?.get("kp")
                        .asDoubleOrNull()
                        ?: 0.0
                    val posterUrl = obj.get("poster")
                        .asObjectOrNull()
                        ?.get("url")
                        .asStringOrNull()

                    RecommendationItem(
                        id = obj.get("id").asIntOrNull() ?: obj.hashCode(),
                        type = if (typeText == "tv-series") RecommendationType.SERIES else RecommendationType.FILM,
                        title = obj.get("name").asStringOrNull()
                            ?: obj.get("alternativeName").asStringOrNull()
                            ?: "Unknown",
                        genres = genres,
                        moods = emptyList(),
                        year = obj.get("year").asIntOrNull() ?: 0,
                        description = obj.get("description").asStringOrNull() ?: "",
                        rating = rating,
                        posterUrl = posterUrl,
                        source = "Кинопоиск"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logApiError("KinoApi", "Таймаут при обращении к API Кинопоиска. Проверьте сеть/VPN/доступность API.", e)
            emptyList()
        } catch (e: Exception) {
            logApiError("KinoApi", "Не удалось обратиться к API Кинопоиска.", e)
            emptyList()
        }
    }
}

object BookApi : ExternalApi {
    private val env = dotenv()
    private val token = env["BOOKS_API_TOKEN"]
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun search(query: String, limit: Int): List<RecommendationItem> {
        if (token.isNullOrBlank()) {
            logApiInfo("BookApi", "BOOKS_API_TOKEN не задан. Используется Google Books API без ключа (ограниченная квота).")
        }
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=$limit" +
            (if (!token.isNullOrBlank()) "&key=$token" else "")

        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logApiWarn("BookApi", "HTTP ${response.code} от Google Books API. URL=${request.url}")
                    return emptyList()
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    logApiWarn("BookApi", "Пустой ответ от Google Books API. URL=${request.url}")
                    return emptyList()
                }
                val json = gson.fromJson(body, JsonObject::class.java)
                val items = json.getAsJsonArray("items") ?: run {
                    logApiInfo("BookApi", "Google Books API вернул 0 результатов для запроса '$query'.")
                    return emptyList()
                }

                items.mapNotNull { item ->
                    val itemObj = item.asObjectOrNull() ?: return@mapNotNull null
                    val volumeInfo = itemObj.get("volumeInfo").asObjectOrNull() ?: return@mapNotNull null
                    RecommendationItem(
                        id = itemObj.get("id").asStringOrNull()?.hashCode() ?: itemObj.hashCode(),
                        type = RecommendationType.BOOK,
                        title = volumeInfo.get("title").asStringOrNull() ?: "Unknown",
                        genres = volumeInfo.get("categories")
                            ?.takeIf { it.isJsonArray }
                            ?.asJsonArray
                            ?.mapNotNull { it.asStringOrNull() }
                            ?: emptyList(),
                        moods = emptyList(),
                        year = volumeInfo.get("publishedDate").asStringOrNull()?.take(4)?.toIntOrNull() ?: 0,
                        description = volumeInfo.get("description").asStringOrNull() ?: "",
                        rating = volumeInfo.get("averageRating").asDoubleOrNull() ?: 0.0,
                        posterUrl = volumeInfo.get("imageLinks")
                            .asObjectOrNull()
                            ?.get("thumbnail")
                            .asStringOrNull(),
                        source = "Google Books"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logApiError("BookApi", "Таймаут при обращении к Google Books API. Проверьте сеть.", e)
            emptyList()
        } catch (e: Exception) {
            logApiError("BookApi", "Не удалось обратиться к Google Books API.", e)
            emptyList()
        }
    }
}

object GameApi : ExternalApi {
    // Для IGDB нужен Client ID и Access Token (OAuth2). 
    // В данном примере реализуем упрощенный поиск через Free-to-play API или аналоги, 
    // либо оставим заглушку, так как IGDB требует сложной авторизации.
    override fun search(query: String, limit: Int): List<RecommendationItem> {
        // Заглушка для демонстрации, так как IGDB API требует OAuth
        return emptyList()
    }
}
