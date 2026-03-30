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
                    val itemId = obj.get("id").asIntOrNull() ?: obj.hashCode()
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
                        id = itemId,
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
                        source = "Кинопоиск",
                        metadata = mapOf(
                            "url" to if (typeText == "tv-series") {
                                "https://www.kinopoisk.ru/series/$itemId/"
                            } else {
                                "https://www.kinopoisk.ru/film/$itemId/"
                            }
                        )
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
        val withKeyUrl = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=$limit" +
            (if (!token.isNullOrBlank()) "&key=$token" else "")
        val noKeyUrl = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=$limit"

        return try {
            fun parseItems(body: String): List<RecommendationItem> {
                val json = gson.fromJson(body, JsonObject::class.java)
                val items = json.getAsJsonArray("items") ?: run {
                    logApiInfo("BookApi", "Google Books API вернул 0 результатов для запроса '$query'.")
                    return emptyList()
                }
                return items.mapNotNull { item ->
                    val itemObj = item.asObjectOrNull() ?: return@mapNotNull null
                    val volumeInfo = itemObj.get("volumeInfo").asObjectOrNull() ?: return@mapNotNull null
                    val saleInfo = itemObj.get("saleInfo").asObjectOrNull()
                    val link = saleInfo?.get("buyLink").asStringOrNull()
                        ?: volumeInfo.get("infoLink").asStringOrNull()
                        ?: volumeInfo.get("previewLink").asStringOrNull()
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
                        source = "Google Books",
                        metadata = if (link.isNullOrBlank()) emptyMap() else mapOf("url" to link)
                    )
                }
            }

            val withKeyRequest = Request.Builder().url(withKeyUrl).build()
            client.newCall(withKeyRequest).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && !body.isNullOrBlank()) {
                    return parseItems(body)
                }
                if (response.code == 403 && !token.isNullOrBlank()) {
                    logApiWarn("BookApi", "HTTP 403 c ключом. Вероятно, Books API отключен в проекте ключа. Пробую без ключа.")
                    logApiWarn("BookApi", "Включите Books API: https://console.developers.google.com/apis/api/books.googleapis.com/overview")
                    val noKeyRequest = Request.Builder().url(noKeyUrl).build()
                    client.newCall(noKeyRequest).execute().use { retryResponse ->
                        val retryBody = retryResponse.body?.string()
                        if (!retryResponse.isSuccessful) {
                            logApiWarn("BookApi", "HTTP ${retryResponse.code} от Google Books API (без ключа). URL=${noKeyRequest.url}")
                            return emptyList()
                        }
                        if (retryBody.isNullOrBlank()) {
                            logApiWarn("BookApi", "Пустой ответ от Google Books API (без ключа). URL=${noKeyRequest.url}")
                            return emptyList()
                        }
                        return parseItems(retryBody)
                    }
                }
                if (!response.isSuccessful) {
                    logApiWarn("BookApi", "HTTP ${response.code} от Google Books API. URL=${withKeyRequest.url}")
                    return emptyList()
                }
                logApiWarn("BookApi", "Пустой ответ от Google Books API. URL=${withKeyRequest.url}")
                return emptyList()
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
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun search(query: String, limit: Int): List<RecommendationItem> {
        val normalizedQuery = query.trim().ifBlank { "популярные игры" }
        val encodedQuery = URLEncoder.encode(normalizedQuery, StandardCharsets.UTF_8.toString())
        val request = Request.Builder()
            .url("https://store.steampowered.com/api/storesearch/?term=$encodedQuery&l=russian&cc=ru")
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logApiWarn("GameApi", "HTTP ${response.code} от Steam Store API. URL=${request.url}")
                    return emptyList()
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    logApiWarn("GameApi", "Пустой ответ от Steam Store API. URL=${request.url}")
                    return emptyList()
                }

                val json = gson.fromJson(body, JsonObject::class.java)
                val array = json.getAsJsonArray("items") ?: return emptyList()
                val allGames = array.mapNotNull { game ->
                    val obj = game.asObjectOrNull() ?: return@mapNotNull null
                    val title = obj.get("name").asStringOrNull() ?: return@mapNotNull null
                    val tags = obj.get("tags")
                        ?.takeIf { it.isJsonArray }
                        ?.asJsonArray
                        ?.mapNotNull { it.asStringOrNull() }
                        ?: emptyList()
                    RecommendationItem(
                        id = obj.get("id").asIntOrNull() ?: obj.hashCode(),
                        type = RecommendationType.GAME,
                        title = title,
                        genres = tags,
                        moods = emptyList(),
                        year = 0,
                        description = "Игра из каталога Steam.",
                        posterUrl = obj.get("tiny_image").asStringOrNull(),
                        source = "Steam",
                        metadata = mapOf("url" to "https://store.steampowered.com/app/${obj.get("id").asIntOrNull() ?: obj.hashCode()}/")
                    )
                }

                val normalized = normalizedQuery.lowercase()
                val filtered = if (normalized.isBlank()) {
                    allGames
                } else {
                    val terms = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
                    allGames.filter { item ->
                        val haystack = (item.title + " " + item.genres.joinToString(" ")).lowercase()
                        terms.any { haystack.contains(it) }
                    }
                }

                val out = if (filtered.isEmpty()) {
                    allGames.take(limit)
                } else {
                    filtered.take(limit)
                }
                if (out.isEmpty()) {
                    logApiInfo("GameApi", "Не найдено игр по запросу '$query'.")
                }
                out.map { base ->
                    val details = fetchDetails(base.id)
                    if (details == null) {
                        base
                    } else {
                        base.copy(
                            genres = if (details.genres.isNotEmpty()) details.genres else base.genres,
                            year = details.year ?: base.year,
                            description = details.description.ifBlank { base.description },
                            rating = details.rating ?: base.rating,
                            posterUrl = details.posterUrl ?: base.posterUrl
                        )
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            logApiError("GameApi", "Таймаут при обращении к Steam Store API.", e)
            emptyList()
        } catch (e: Exception) {
            logApiError("GameApi", "Не удалось обратиться к Steam Store API.", e)
            emptyList()
        }
    }

    private fun fetchDetails(appId: Int): SteamDetails? {
        return try {
            val request = Request.Builder()
                .url("https://store.steampowered.com/api/appdetails?appids=$appId&l=russian&cc=ru")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = gson.fromJson(body, JsonObject::class.java)
                val appNode = root.get(appId.toString()).asObjectOrNull() ?: return null
                val success = appNode.get("success").asStringOrNull()?.toBooleanStrictOrNull() ?: false
                if (!success) return null
                val data = appNode.get("data").asObjectOrNull() ?: return null

                val description = data.get("short_description").asStringOrNull().orEmpty()
                val genres = data.get("genres")
                    ?.takeIf { it.isJsonArray }
                    ?.asJsonArray
                    ?.mapNotNull { it.asObjectOrNull()?.get("description").asStringOrNull() }
                    ?: emptyList()
                val poster = data.get("header_image").asStringOrNull()
                val rating = data.get("metacritic")
                    .asObjectOrNull()
                    ?.get("score")
                    .asDoubleOrNull()
                val releaseDate = data.get("release_date")
                    .asObjectOrNull()
                    ?.get("date")
                    .asStringOrNull()
                    .orEmpty()
                val year = Regex("(19|20)\\d{2}").find(releaseDate)?.value?.toIntOrNull()

                SteamDetails(
                    description = description,
                    genres = genres,
                    year = year,
                    rating = rating,
                    posterUrl = poster
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class SteamDetails(
        val description: String,
        val genres: List<String>,
        val year: Int?,
        val rating: Double?,
        val posterUrl: String?
    )
}
