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
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import okhttp3.ConnectionSpec
import java.util.Arrays
import okhttp3.TlsVersion
import okhttp3.CipherSuite

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
    
    // Настраиваем OkHttpClient с улучшенными параметрами TLS для решения SSL проблем
    private val client = run {
        // Создаем trust all certificates trust manager для решения проблем с SSL
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        
        // Настраиваем ConnectionSpec для поддержки современных TLS версий
        val connectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
            .cipherSuites(
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,
                CipherSuite.TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256
            )
            .build()
    
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .connectionSpecs(listOf(connectionSpec, ConnectionSpec.CLEARTEXT))
            .build()
    }
    private val gson = Gson()

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
                    val json = gson.fromJson(body, JsonObject::class.java)
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
            } catch (e: SocketTimeoutException) {
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
                        source = "Google Books",
                        metadata = mapOf("url" to (link ?: ""))
                    )
                }
            }

            val url = if (!token.isNullOrBlank()) withKeyUrl else noKeyUrl
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logApiWarn("BookApi", "HTTP ${response.code} от Google Books API.")
                    return emptyList()
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    logApiWarn("BookApi", "Пустой ответ от Google Books API.")
                    return emptyList()
                }
                parseItems(body)
            }
        } catch (e: SocketTimeoutException) {
            logApiError("BookApi", "Таймаут при обращении к Google Books API.", e)
            return emptyList()
        } catch (e: Exception) {
            logApiError("BookApi", "Не удалось обратиться к Google Books API.", e)
            return emptyList()
        }
    }
}

object GameApi : ExternalApi {
    private val env = dotenv()
    private val token = env["RAWG_API_TOKEN"]
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun search(query: String, limit: Int): List<RecommendationItem> {
        if (token.isNullOrBlank()) {
            logApiWarn("GameApi", "RAWG_API_TOKEN не задан. Внешний поиск игр отключён.")
            return emptyList()
        }
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://api.rawg.io/games?search=$encodedQuery&page_size=$limit&key=$token"

        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logApiWarn("GameApi", "HTTP ${response.code} от RAWG API.")
                    return emptyList()
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    logApiWarn("GameApi", "Пустой ответ от RAWG API.")
                    return emptyList()
                }
                val json = gson.fromJson(body, JsonObject::class.java)
                val results = json.getAsJsonArray("results") ?: run {
                    logApiInfo("GameApi", "RAWG API вернул 0 результатов для запроса '$query'.")
                    return emptyList()
                }
                
                results.mapNotNull { item ->
                    val itemObj = item.asObjectOrNull() ?: return@mapNotNull null
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
                        description = itemObj.get("description")?.asStringOrNull() ?: "",
                        rating = itemObj.get("rating")?.asDoubleOrNull() ?: 0.0,
                        posterUrl = itemObj.get("background_image")?.asStringOrNull(),
                        source = "RAWG",
                        metadata = mapOf("url" to (itemObj.get("website")?.asStringOrNull() ?: ""))
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logApiError("GameApi", "Таймаут при обращении к RAWG API.", e)
            return emptyList()
        } catch (e: Exception) {
            logApiError("GameApi", "Не удалось обратиться к RAWG API.", e)
            return emptyList()
        }
    }
}
