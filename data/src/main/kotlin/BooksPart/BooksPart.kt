package BooksPart

import Models.BooksModel.*
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL


fun parseJson(jjsonString: String) : List<BookResponse>? {
    try {
        val jsonString = jjsonString.trimIndent()
        val json = Json {
            ignoreUnknownKeys = true // Игнорировать неизвестные ключи
                //coerceInputValues = true // Позволяет обрабатывать null значения для полей, которые могут быть null
        }

        val response = json.decodeFromString<OuterResponse>(jsonString)

        return response.items

    } catch (e: Exception) {

        println("Ошибка при парсинге JSON: ${e.message}")
        e.printStackTrace()

        return null
    }
}


fun getFromApi(baseQuery: String): String { //amount: Int = 1, page: Int = 1
    val apiKey = dotenv()["BOOKS_API_TOKEN"]
    val query = baseQuery.replace(' ', '+')
    val apiUrl = "https://www.googleapis.com/books/v1/volumes?q=$query&key=$apiKey" //API endpoint
    try {
        val url : URL = URI.create(apiUrl).toURL()
        val connection : HttpURLConnection = url.openConnection() as HttpURLConnection

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

fun collectResponse(query: String): List<BookResponse>? {
    println(getFromApi(query))
    return parseJson(getFromApi(query))
}

fun getInfo(query: String): MutableList<String> {
    val responses = mutableListOf<String>()
    val collected = collectResponse(query)
    if (collected == null) {
        return mutableListOf()
    } else {
        for (i in collected) {
            val book = i.volumeInfo
            val response =
                "Название: ${book.title}\n\nАвтор(ы): ${book.authors}\nКоличество страниц: ${book.pageCount}\n\nОписание: ${book.description}"

            responses.add(response)

        }
        return responses
    }
}