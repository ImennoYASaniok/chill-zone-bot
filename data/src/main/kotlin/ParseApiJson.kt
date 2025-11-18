package org.example
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL


@Serializable
data class MovieListResponse(
    val docs: List<Movie>,
    val total: Int,
    val limit: Int,
    val page: Int,
    val pages: Int
)

// ----- Основная модель фильма -----
@Serializable
data class Movie(
    val id: Int,
    val externalId: ExternalId? = null,
    val name: String?,
    val alternativeName: String? = null,
    val enName: String? = null,
    val names: List<Name>? = null,
    val type: String,
    val typeNumber: Int,
    val year: Int? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val slogan: String? = null,
    val status: String? = null,
    val facts: List<Fact>? = null,
    val rating: Rating? = null,
    val votes: Votes? = null,
    val movieLength: Int? = null,
    val ratingMpaa: String? = null,
    val ageRating: Int? = null,
    val logo: Logo? = null,
    val poster: Poster? = null,
    val backdrop: Backdrop? = null,
    val videos: Videos? = null,
    val genres: List<Genre>? = null,
    val countries: List<Country>? = null,
    val persons: List<Person>? = null,
    val reviewInfo: ReviewInfo? = null,
    val seasonsInfo: List<SeasonsInfo>? = null,
    val budget: Budget? = null,
    val fees: Fees? = null,
    val premiere: Premiere? = null,
    val similarMovies: List<SimilarMovie>? = null,
    val sequelsAndPrequels: List<SequelsAndPrequels>? = null,
    val watchability: Watchability? = null,
    val releaseYears: List<ReleaseYear>? = null,
    val top10: Int? = null,
    val top250: Int? = null,
    val ticketsOnSale: Boolean? = null,
    val totalSeriesLength: Int? = null,
    val seriesLength: Int? = null,
    val isSeries: Boolean? = null,
    val audience: List<Audience>? = null,
    val lists: List<String>? = null,
    val networks: Networks? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null
)

// ----- Вспомогательные классы -----

@Serializable
data class ExternalId(
    val kpHD: String?,
    val imdb: String?,
    val tmdb: Int?
)

@Serializable
data class Name(
    val name: String?,
    val language: String? = null,
    val type: String? = null
)

@Serializable
data class Fact(
    val value: String,
    val type: String,
    val spoiler: Boolean?
)

@Serializable
data class Rating(
    val kp: Double?,
    val imdb: Double?,
    val tmdb: Double? = null,
    val filmCritics: Double?,
    val russianFilmCritics: Double?,
    val await: Double? = null
)

@Serializable
data class Votes(
    val kp: Double?, // Может быть строкой, как в примере
    val imdb: Int?,
    val tmdb: Int? = null,
    val filmCritics: Int?,
    val russianFilmCritics: Int?,
    val await: Int?
)

@Serializable
data class Logo(
    val url: String?
)

@Serializable
data class Poster(
    val url: String?,
    val previewUrl: String?
)

@Serializable
data class Backdrop(
    val url: String?,
    val previewUrl: String?
)

@Serializable
data class Video(
    val url: String,
    val name: String,
    val site: String,
    val size: Int?,
    val type: String
)

@Serializable
data class Videos(
    val trailers: List<Video>?
)

@Serializable
data class Genre(
    val name: String
)

@Serializable
data class Country(
    val name: String
)

@Serializable
data class Person(
    val id: Int,
    val photo: String?,
    val name: String,
    val enName: String?,
    val description: String?,
    val profession: String?,
    val enProfession: String?
)

@Serializable
data class ReviewInfo(
    val count: Int?,
    val positiveCount: Int?,
    val percentage: String?
)

@Serializable
data class SeasonsInfo(
    val number: Int?,
    val episodesCount: Int?
)

@Serializable
data class Budget(
    val value: Int?,
    val currency: String?
)

@Serializable
data class Fees(
    val world: FeeInfo?,
    val russia: FeeInfo?,
    val usa: FeeInfo?
)

@Serializable
data class FeeInfo(
    val value: Int?,
    val currency: String?
)

@Serializable
data class Premiere(
    val country: String?,
    val world: String?, // Можно преобразовать в Date, если нужно
    val russia: String?,
    val digital: String?,
    val cinema: String?,
    val bluray: String?,
    val dvd: String?
)

@Serializable
data class SimilarMovie(
    val id: Int,
    val name: String?,
    val enName: String?,
    val alternativeName: String?,
    val type: String?,
    val poster: Poster?,
    val rating: Rating?,
    val year: Int?
)

@Serializable
data class SequelsAndPrequels(
    val id: Int,
    val name: String?,
    val enName: String?,
    val alternativeName: String?,
    val type: String?,
    val poster: Poster?,
    val rating: Rating?,
    val year: Int?
)

@Serializable
data class WatchabilityItem(
    val name: String,
    val logo: Logo?,
    val url: String
)

@Serializable
data class Watchability(
    val items: List<WatchabilityItem>?
)

@Serializable
data class ReleaseYear(
    val start: Int?,
    val end: Int?
)

@Serializable
data class Audience(
    val count: Int?,
    val country: String?
)

@Serializable
data class NetworkItem(
    val name: String,
    val logo: Logo?
)

@Serializable
data class Networks(
    val items: List<NetworkItem>?
)


// ----- Функция парсинга -----

fun parseDetailedMovieJson(jjsonString: String) {
    try {
        val jsonString = jjsonString.trimIndent()
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true // Игнорировать неизвестные ключи
            coerceInputValues = true // Позволяет обрабатывать null значения для полей, которые могут быть null
        }

        val movieListResponse: MovieListResponse = json.decodeFromString(jsonString)

        println("Получено фильмов: ${movieListResponse.docs.size}")
        println("Общее количество: ${movieListResponse.total}")
        println("Текущая страница: ${movieListResponse.page}")

//        for (movie in movieListResponse.docs) {
//            println("--- Фильм ---")
//            println("ID: ${movie.id}")
//            println("Название (Основное): ${movie.name}")
//            println("Название (Альтернативное): ${movie.alternativeName}")
//            println("Год: ${movie.year ?: "Неизвестен"}")
//            println("Описание: ${movie.description ?: "Нет описания"}")
//            println("Рейтинг КиноПоиск: ${movie.rating?.kp ?: "N/A"}")
//            println("Страны: ${movie.countries.joinToString { it.name }}")
//            println("Жанры: ${movie.genres.joinToString { it.name }}")
//
//            // Пример доступа к более вложенным данным
//            movie.persons?.filter { it.enProfession == "actor" }?.take(3)?.forEach { actor ->
//                println("Актер: ${actor.name} (${actor.enName})")
//            }
//
//            movie.videos?.trailers?.firstOrNull()?.let { trailer ->
//                println("Ссылка на трейлер: ${trailer.url}")
//            }
//        }
//        println(movieListResponse.docs)
        try {
            val readedFromBD = File("bd.txt").readText()
            val bdJson = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
            //println(readedFromBD)
                val readedData = bdJson.decodeFromString<MutableList<Movie>>(readedFromBD)
                readedData += movieListResponse.docs

                File("bd.txt").writeText(bdJson.encodeToString<MutableList<Movie>>(readedData))
                println(movieListResponse.page)
            } catch (e: Exception) {
                val bdJson = Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
                println("Ошибка при чтении файла: ${e.message}")
                File("bd.txt").writeText(bdJson.encodeToString<MutableList<Movie>>(movieListResponse.docs.toMutableList()))
            }


    } catch (e: Exception) {
        println("Ошибка при парсинге JSON: ${e.message}")
        e.printStackTrace()
//        if (jjsonString.length > 500) {
//            println("Начало JSON строки: ${jjsonString.take(500)}")
//        } else {
//            println("JSON строка: $jjsonString")
//        }
    }
}


fun load(amount: Int = 1, page: Int = 1): String {
    val apiUrl = "https://api.poiskkino.dev/v1.4/movie?page=$page&limit=$amount" //API endpoint
    println("https://api.poiskkino.dev/v1.4/movie?page=$page&limit=$amount")
    val apiKey = "B9CRGQQ-RNZ4VQD-KH4V4YG-TA3CXEY"
    try {
        val url : URL = URI.create(apiUrl).toURL()
        val connection : HttpURLConnection = url.openConnection() as HttpURLConnection

        //Request method: GET
        connection.setRequestProperty("X-API-KEY", apiKey)
        connection.requestMethod = "GET"

        // Response code
        val responseCode: Int = connection.responseCode
        println("Response Code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Read and print the response data
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            var line: String?
            val response = StringBuilder()

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }

            reader.close()
            connection.disconnect()
            return response.toString()
        } else {
            println("Error: Unable to fetch data from the API")
            connection.disconnect()
            return ""
        }

        // Close the connection

    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}


fun main() {
    for (i in 1..13) {
        parseDetailedMovieJson(load(250,i))
        Thread.sleep(1000)
    }

}