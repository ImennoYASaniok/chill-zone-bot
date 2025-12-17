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

import Models.FilmsModel.*
import java.sql.ResultSet


object KinoStates {
    var chosenGenres: ArrayList<String> = arrayListOf()
    var foundFilms: MutableList<String> = mutableListOf()
    var filmIndex: Int = 0
}


fun searchMoviesDB() {
    val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    val databaseUser = dotenv()["DATABASE_USER"]
    val databasePassword = dotenv()["DATABASE_PASSWORD"]
    val neededGenres = KinoStates.chosenGenres
    if (neededGenres.isNotEmpty()) {
        try {
            val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

            var createTablesQuery =
                "SELECT Movies.* FROM Movies WHERE Movies.id IN (SELECT idMovie FROM Genres WHERE name = ANY(?::VARCHAR[]) GROUP BY idMovie HAVING COUNT(DISTINCT name) = ?);"
            var query = """
            SELECT Movies.*, 
                   STRING_AGG(DISTINCT Genres.name, ', ') as genres_string,
                   ARRAY_AGG(DISTINCT Genres.name) as genres_array
            FROM Movies
            JOIN Genres ON Movies.id = Genres.idMovie
            WHERE Movies.id IN (
                SELECT Genres.idMovie 
                FROM Genres 
                WHERE Genres.name = ANY(?::VARCHAR[])
                GROUP BY Genres.idMovie 
                HAVING COUNT(DISTINCT Genres.name) = ?
            )
            GROUP BY Movies.id
            ORDER BY Movies.id
        """.trimIndent()
            var preparedStatement = connection.prepareStatement(query)
            val sqlArray = connection.createArrayOf("VARCHAR", neededGenres.toTypedArray())
            preparedStatement.setArray(1, sqlArray)
            preparedStatement.setInt(2, neededGenres.size)
            preparedStatement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    KinoStates.foundFilms.add(
                        "Название: ${resultSet.getString("name")}\n\nГод выхода: ${
                            resultSet.getInt(
                                "year"
                            )
                        }\nДлительность: ${resultSet.getInt("movieLength")} минут\nЖанры: ${
                            resultSet.getString(
                                "genres_string"
                            ) ?: "нет данных"
                        }\n\nОписание:\n${
                            resultSet.getString(
                                "description"
                            ) ?: "отсутствует"
                        }\n\nРейтинг на кинопоиске: ${resultSet.getFloat("kpRating")}\nРейтинг на IMDb: ${
                            resultSet.getFloat(
                                "imdbRating"
                            )
                        }"
                    )
                }
                println("Got resultSet!")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    } else {
        try {
            val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

            var createTablesQuery =
                "SELECT Movies.* FROM Movies WHERE Movies.id IN (SELECT idMovie FROM Genres WHERE name = ANY(?::VARCHAR[]) GROUP BY idMovie HAVING COUNT(DISTINCT name) = ?);"
            var query = """
            SELECT Movies.*, 
                       STRING_AGG(DISTINCT Genres.name, ', ') as genres_string
                FROM Movies
                LEFT JOIN Genres ON Movies.id = Genres.idMovie
                GROUP BY Movies.id
                ORDER BY Movies.id
        """.trimIndent()
            var preparedStatement = connection.prepareStatement(query)
            preparedStatement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    println(1)
                    KinoStates.foundFilms.add(
                        "Название: ${resultSet.getString("name")}\n\nГод выхода: ${
                            resultSet.getInt(
                                "year"
                            )
                        }\nДлительность: ${resultSet.getInt("movieLength")} минут\nЖанры: ${
                            resultSet.getString(
                                "genres_string"
                            ) ?: "нет данных"
                        }\n\nОписание:\n${
                            resultSet.getString(
                                "description"
                            ) ?: "отсутствует"
                        }\n\nРейтинг на кинопоиске: ${resultSet.getFloat("kpRating")}\nРейтинг на IMDb: ${
                            resultSet.getFloat(
                                "imdbRating"
                            )
                        }"
                    )
                }
                println("Got resultSet!")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}

fun searchSeriesDB() {
    val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    val databaseUser = dotenv()["DATABASE_USER"]
    val databasePassword = dotenv()["DATABASE_PASSWORD"]
    val neededGenres = KinoStates.chosenGenres
    if (neededGenres.isNotEmpty()) {
        try {
            val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

            var createTablesQuery =
                "SELECT Movies.* FROM Movies WHERE Movies.id IN (SELECT idMovie FROM Genres WHERE name = ANY(?::VARCHAR[]) GROUP BY idMovie HAVING COUNT(DISTINCT name) = ?);"
            var query = """
            SELECT Series.*, 
                   STRING_AGG(DISTINCT Genres.name, ', ') as genres_string,
                   ARRAY_AGG(DISTINCT Genres.name) as genres_array
            FROM Series
            JOIN Genres ON Series.id = Genres.idMovie
            WHERE Series.id IN (
                SELECT Genres.idMovie 
                FROM Genres 
                WHERE Genres.name = ANY(?::VARCHAR[])
                GROUP BY Genres.idMovie 
                HAVING COUNT(DISTINCT Genres.name) = ?
            )
            GROUP BY Series.id
            ORDER BY Series.id
        """.trimIndent()
            var preparedStatement = connection.prepareStatement(query)
            val sqlArray = connection.createArrayOf("VARCHAR", neededGenres.toTypedArray())
            preparedStatement.setArray(1, sqlArray)
            preparedStatement.setInt(2, neededGenres.size)
            preparedStatement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    KinoStates.foundFilms.add(
                        "Название: ${resultSet.getString("name")}\n\nГод выхода: ${
                            resultSet.getInt(
                                "year"
                            )
                        }\nСуммарная длительность: ${resultSet.getInt("seriesAmount")} минут\nЖанры: ${
                            resultSet.getString(
                                "genres_string"
                            )
                        }\n\nОписание:\n${
                            resultSet.getString(
                                "description"
                            ) ?: "отсутствует"
                        }\n\nРейтинг на кинопоиске: ${resultSet.getFloat("kpRating")}\nРейтинг на IMDb: ${
                            resultSet.getFloat(
                                "imdbRating"
                            )
                        }"
                    )
                }
                println("Got resultSet!")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    } else {
        try {
            val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

            var createTablesQuery =
                "SELECT Movies.* FROM Movies WHERE Movies.id IN (SELECT idMovie FROM Genres WHERE name = ANY(?::VARCHAR[]) GROUP BY idMovie HAVING COUNT(DISTINCT name) = ?);"
            var query = """
            SELECT Series.*, 
                       STRING_AGG(DISTINCT Genres.name, ', ') as genres_string
                FROM Series
                LEFT JOIN Genres ON Series.id = Genres.idMovie
                GROUP BY Series.id
                ORDER BY Series.id
        """.trimIndent()
            var preparedStatement = connection.prepareStatement(query)
            preparedStatement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    KinoStates.foundFilms.add(
                        "Название: ${resultSet.getString("name")}\n\nГод выхода: ${
                            resultSet.getInt(
                                "year"
                            )
                        }\nСуммарная длительность: ${resultSet.getInt("seriesAmount")} минут\nЖанры: ${
                            resultSet.getString(
                                "genres_string"
                            )
                        }\n\nОписание:\n${
                            resultSet.getString(
                                "description"
                            ) ?: "отсутствует"
                        }\n\nРейтинг на кинопоиске: ${resultSet.getFloat("kpRating")}\nРейтинг на IMDb: ${
                            resultSet.getFloat(
                                "imdbRating"
                            )
                        }"
                    )
                }
                println("Got resultSet!")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}

fun updateDatabase(movieListResponse: MovieListResponse?) {
    movieListResponse?.let {
        val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
        val databaseUser = dotenv()["DATABASE_USER"]
        val databasePassword = dotenv()["DATABASE_PASSWORD"]
        try {
            println("List size: ${movieListResponse.docs.size}")
            val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)
            for (i in movieListResponse.docs) {
                if (!(i.isSeries ?: false)) {


                    // insert into movies table

                    var insertMovieQuery =
                        "INSERT INTO Movies (kId, name , year, description, movieLength, kpRating, imdbRating) VALUES (?, ?, ?, ?, ?, ?, ?)"
                    var preparedStatement = connection.prepareStatement(insertMovieQuery)
                    if (i.name == null) continue
                    preparedStatement.setInt(1, i.id)
                    preparedStatement.setString(2, i.name)
                    i.year?.let { preparedStatement.setInt(3, i.year) }
                    preparedStatement.setString(4, i.description)
                    if (i.movieLength == null ) {
                        preparedStatement.setInt(5, 0)
                    } else {
                        preparedStatement.setInt(5, i.movieLength)
                    }



                    i.rating?.let {
                        i.rating.kp?.let {
                            preparedStatement.setDouble(6, i.rating.kp)
                        }
                        i.rating.imdb?.let {
                            preparedStatement.setDouble(7, i.rating.imdb)
                        }
                    }

                    var rowsAffected = preparedStatement.executeUpdate()
                    //println("Rows affected: $rowsAffected")

                    //insert into genres table

                    i.genres?.let {
                        for (genre in i.genres) {
                            insertMovieQuery = "INSERT INTO Genres (idMovie, name) VALUES (?, ?)"
                            preparedStatement = connection.prepareStatement(insertMovieQuery)
                            preparedStatement.setInt(1, i.id)
                            preparedStatement.setString(2, genre.name)
                            rowsAffected = preparedStatement.executeUpdate()
                            //println("Rows affected: $rowsAffected")
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
                            //println("Rows affected: $rowsAffected")
                        }
                    }
                } else {

                    // insert into series table

                    var insertMovieQuery =
                        "INSERT INTO Series (kId, name , year, description, seriesAmount, kpRating, imdbRating) VALUES (?, ?, ?, ?, ?, ?, ?)"
                    var preparedStatement = connection.prepareStatement(insertMovieQuery)

                    preparedStatement.setInt(1, i.id)
                    preparedStatement.setString(2, i.name)
                    i.year?.let { preparedStatement.setInt(3, i.year) }

                    if (i.description == null) continue

                    preparedStatement.setString(4, i.description)

                    if (i.totalSeriesLength == null ) {
                        preparedStatement.setInt(5, 0)
                    } else {
                        preparedStatement.setInt(5, i.totalSeriesLength)
                    }
                    i.rating?.let {
                        i.rating.kp?.let {
                            preparedStatement.setDouble(6, i.rating.kp)
                        }
                        i.rating.imdb?.let {
                            preparedStatement.setDouble(7, i.rating.imdb)
                        }
                    }

                    var rowsAffected = preparedStatement.executeUpdate()
                    //println("Rows affected: $rowsAffected")

                    //insert into genres table

                    i.genres?.let {
                        for (genre in i.genres) {
                            insertMovieQuery = "INSERT INTO Genres (idMovie, name) VALUES (?, ?)"
                            preparedStatement = connection.prepareStatement(insertMovieQuery)
                            preparedStatement.setInt(1, i.id)
                            preparedStatement.setString(2, genre.name)
                            rowsAffected = preparedStatement.executeUpdate()
                            //println("Rows affected: $rowsAffected")
                        }
                    }
                }
            }
            connection.close()
            Thread.sleep(500)

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