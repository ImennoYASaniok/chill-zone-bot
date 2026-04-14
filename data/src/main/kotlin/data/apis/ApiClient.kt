package data.apis

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import data.models.RecommendationItem
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
import okhttp3.Interceptor
import okhttp3.Response

// Утилитарные функции для логирования и парсинга JSON
fun logApiInfo(api: String, message: String) {
    println("INFO: [$api] $message")
}

fun logApiWarn(api: String, message: String) {
    println("WARN: [$api] $message")
}

fun logApiError(api: String, message: String, e: Exception? = null) {
    println("ERROR: [$api] $message")
    if (e != null) {
        println("ERROR: [$api] ${e::class.simpleName}: ${e.message}")
    }
}

fun JsonElement?.asObjectOrNull(): JsonObject? {
    if (this == null || !this.isJsonObject) return null
    return this.asJsonObject
}

fun JsonElement?.asStringOrNull(): String? {
    if (this == null || !this.isJsonPrimitive) return null
    return runCatching { this.asString }.getOrNull()
}

fun JsonElement?.asIntOrNull(): Int? {
    if (this == null || !this.isJsonPrimitive) return null
    return runCatching { this.asInt }.getOrNull()
}

fun JsonElement?.asDoubleOrNull(): Double? {
    if (this == null || !this.isJsonPrimitive) return null
    return runCatching { this.asDouble }.getOrNull()
}

interface ExternalApi {
    fun search(query: String, limit: Int): List<RecommendationItem>
}

object ApiClient {
    // Настраиваем OkHttpClient с улучшенными параметрами TLS для решения SSL проблем
    val client = run {
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
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "ChillZoneBot/1.0 (Telegram Bot)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    val gson = Gson()
}