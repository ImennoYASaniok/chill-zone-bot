package data.apis

import data.*
import data.models.*
import io.github.cdimascio.dotenv.dotenv
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object KinoApi : ExternalApi {
    private val env = dotenv()
    private val token = env["KINO_API_TOKEN"]
    private val gson = ApiClient.gson
    private val client = ApiClient.client

    override fun search(query: String, limit: Int): List<RecommendationItem> {
        if (token.isNullOrBlank()) {
            logApiWarn("KinoApi", "KINO_API_TOKEN не задан. Внешний поиск фильмов/сериалов отключён.")
            return emptyList()
        }
        
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        
        // Пробуем основной URL, если не сработает - альтернативный
        val urls = listOf(
            "https://api.kinopoisk.dev/v1.4/movie/search?query=$encodedQuery&limit=$limit",
            "https://kinopoiskapiunofficial.tech/api/v2.2/films/search-by-keyword?keyword=$encodedQuery&page=1"
        )
        
        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("X-API-KEY", token)
                    .addHeader("User-Agent", "ChillZoneBot/1.0")
                    .build()

                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    logApiWarn("KinoApi", "HTTP ${response.code} от API Кинопоиска. URL=$url")
                    if (url == urls.first()) {
                        continue // Пробуем следующий URL
                    } else {
                        return emptyList()
                    }
                }
                
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    logApiWarn("KinoApi", "Пустой ответ от API Кинопоиска. URL=$url")
                    if (url == urls.first()) {
                        continue
                    } else {
                        return emptyList()
                    }
                }

                try {
                    val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                    val docs = when {
                        url.contains("kinopoiskapiunofficial") -> {
                            json.getAsJsonArray("films") ?: run {
                                logApiWarn("KinoApi", "Поле films отсутствует в ответе API Кинопоиска.")
                                return emptyList()
                            }
                        }
                        else -> {
                            json.getAsJsonArray("docs") ?: run {
                                logApiWarn("KinoApi", "Поле docs отсутствует в ответе API Кинопоиска.")
                                return emptyList()
                            }
                        }
                    }
                    
                    // Если успешно обработали, возвращаем результат
                    return docs.mapNotNull { doc ->
                        val obj = doc.asObjectOrNull() ?: return@mapNotNull null
                        val typeText = obj.get("type")?.asStringOrNull()
                        val itemId = when {
                            url.contains("kinopoiskapiunofficial") -> {
                                obj.get("filmId")?.asIntOrNull() ?: obj.hashCode()
                            }
                            else -> {
                                obj.get("id")?.asIntOrNull() ?: obj.hashCode()
                            }
                        }
                        val genres = obj.get("genres")
                            ?.takeIf { it.isJsonArray }
                            ?.asJsonArray
                            ?.mapNotNull { genre ->
                                genre.asObjectOrNull()?.get("name")?.asStringOrNull()
                            }
                            ?: emptyList()
                        val rating = obj.get("rating")
                            ?.asObjectOrNull()
                            ?.get("kp")
                            ?.asDoubleOrNull()
                            ?: 0.0
                        val posterUrl = obj.get("poster")
                            ?.asObjectOrNull()
                            ?.get("url")
                            ?.asStringOrNull()

                        RecommendationItem(
                            id = itemId,
                            type = if (typeText == "tv-series") RecommendationType.SERIES else RecommendationType.FILM,
                            title = obj.get("name")?.asStringOrNull()
                                ?: obj.get("alternativeName")?.asStringOrNull()
                                ?: "Unknown",
                            genres = genres,
                            moods = emptyList(),
                            year = obj.get("year")?.asIntOrNull() ?: 0,
                            description = obj.get("description")?.asStringOrNull() ?: "",
                            rating = rating,
                            posterUrl = posterUrl,
                            url = if (typeText == "tv-series") {
                                "https://www.kinopoisk.ru/series/$itemId/"
                            } else {
                                "https://www.kinopoisk.ru/film/$itemId/"
                            },
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
                } catch (e: Exception) {
                    logApiError("KinoApi", "Ошибка при обработке ответа от API Кинопоиска. URL=$url", e)
                    if (url == urls.first()) {
                        continue // Пробуем следующий URL
                    } else {
                        return emptyList()
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                logApiError("KinoApi", "Таймаут при обращении к API Кинопоиска. URL=$url", e)
                if (url == urls.first()) {
                    continue // Пробуем следующий URL
                } else {
                    return emptyList()
                }
            } catch (e: Exception) {
                logApiError("KinoApi", "Не удалось обратиться к API Кинопоиска. URL=$url", e)
                if (url == urls.first()) {
                    continue // Пробуем следующий URL
                } else {
                    return emptyList()
                }
            }
        }
        
        // Если все URL не сработали
        return emptyList()
    }
}