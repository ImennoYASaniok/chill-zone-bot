package KinoPart

import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

import io.github.cdimascio.dotenv.dotenv
import java.sql.DriverManager
import java.sql.SQLException

import Model.*

fun updateDatabase(movieListResponse: MovieListResponse?) {
    movieListResponse?.let {
        val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
        val databaseUser = dotenv()["DATABASE_USER"]
        val databasePassword = dotenv()["DATABASE_PASSWORD"]
        try {
            for (i in movieListResponse.docs) {
                if (i.isSeries ?: false) {
                    val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

                    // insert into movies table

                    var insertMovieQuery =
                        "INSERT INTO Movies (kId, name , year, description, movieLength, kpRating, imdbRating) VALUES (?, ?, ?, ?, ?, ?, ?)"
                    var preparedStatement = connection.prepareStatement(insertMovieQuery)

                    preparedStatement.setInt(1, i.id)
                    preparedStatement.setString(2, i.name)
                    i.year?.let { preparedStatement.setInt(3, i.year) }
                    preparedStatement.setString(4, i.description)
                    i.movieLength?.let { preparedStatement.setInt(5, i.movieLength) }
                    i.rating?.let {
                        i.rating.kp?.let {
                            preparedStatement.setDouble(6, i.rating.kp)
                        }
                        i.rating.imdb?.let {
                            preparedStatement.setDouble(7, i.rating.imdb)
                        }
                    }

                    var rowsAffected = preparedStatement.executeUpdate()
                    println("Rows affected: $rowsAffected")

                    //insert into genres table

                    i.genres?.let {
                        for (genre in i.genres) {
                            insertMovieQuery = "INSERT INTO Genres (idMovie, name) VALUES (?, ?)"
                            preparedStatement = connection.prepareStatement(insertMovieQuery)
                            preparedStatement.setInt(1, i.id)
                            preparedStatement.setString(2, genre.name)
                            rowsAffected = preparedStatement.executeUpdate()
                            println("Rows affected: $rowsAffected")
                        }
                    }

                    // insert into similarMovies table

                    i.similarMovies?.let {
                        for (similarMovie in i.similarMovies) {
                            insertMovieQuery = "INSERT INTO Movies (idMovie, idSimilarMovie) VALUES (?, ?)"
                            preparedStatement = connection.prepareStatement(insertMovieQuery)
                            preparedStatement.setInt(1, i.id)
                            preparedStatement.setInt(2, similarMovie.id)
                            rowsAffected = preparedStatement.executeUpdate()
                            println("Rows affected: $rowsAffected")
                        }
                    }
                } else {
                    val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

                    // insert into series table

                    var insertMovieQuery =
                        "INSERT INTO Series (kId, name , year, description, seriesAmount, kpRating, imdbRating) VALUES (?, ?, ?, ?, ?, ?, ?)"
                    var preparedStatement = connection.prepareStatement(insertMovieQuery)

                    preparedStatement.setInt(1, i.id)
                    preparedStatement.setString(2, i.name)
                    i.year?.let { preparedStatement.setInt(3, i.year) }
                    preparedStatement.setString(4, i.description)
                    i.totalSeriesLength?.let { preparedStatement.setInt(5, i.totalSeriesLength) }
                    i.rating?.let {
                        i.rating.kp?.let {
                            preparedStatement.setDouble(6, i.rating.kp)
                        }
                        i.rating.imdb?.let {
                            preparedStatement.setDouble(7, i.rating.imdb)
                        }
                    }

                    var rowsAffected = preparedStatement.executeUpdate()
                    println("Rows affected: $rowsAffected")

                    //insert into genres table

                    i.genres?.let {
                        for (genre in i.genres) {
                            insertMovieQuery = "INSERT INTO Genres (idMovie, name) VALUES (?, ?)"
                            preparedStatement = connection.prepareStatement(insertMovieQuery)
                            preparedStatement.setInt(1, i.id)
                            preparedStatement.setString(2, genre.name)
                            rowsAffected = preparedStatement.executeUpdate()
                            println("Rows affected: $rowsAffected")
                        }
                    }
                }
            }

        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}



fun parseJson(jjsonString: String) : MovieListResponse? {
    try {
        val jsonString = jjsonString.trimIndent()
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true // Игнорировать неизвестные ключи
            coerceInputValues = true // Позволяет обрабатывать null значения для полей, которые могут быть null
        }

        val movieListResponse: MovieListResponse = json.decodeFromString(jsonString)

        return movieListResponse

    } catch (e: Exception) {

        println("Ошибка при парсинге JSON: ${e.message}")
        e.printStackTrace()

        return null
    }
}


fun getFromApi(amount: Int = 1, page: Int = 1): String {
    val apiUrl = "https://api.poiskkino.dev/v1.4/movie?page=$page&limit=$amount&sortField=rating.kp&sortType=-1" //API endpoint
    val apiKey = dotenv()["KINO_API_TOKEN"]
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