package org.example
import io.github.cdimascio.dotenv.dotenv





//fun loadFetch(path: String) : AnyFrame {
//    val rows = mutableListOf<AnyRow>()
//    var pagePath = path
//
//    do {
//        val row = load(pagePath)
//        rows.add(row)
//
//        val next = row.getValueOrNull<String>("nextPageToken")
//
//        pagePath = path+"&pageToken="
//    } while (next != )
//}
fun main() {
    val dotenv = dotenv()
    val apiKey = dotenv["KINO_API_TOKEN"]
    println(load())
}