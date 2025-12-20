package ChillZoneBot.app.src.main.kotlin.App

import io.github.cdimascio.dotenv.dotenv
import java.sql.DriverManager
import java.sql.SQLException

fun main() {
    val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    val databaseUser = dotenv()["DATABASE_USER"]
    val databasePassword = dotenv()["DATABASE_PASSWORD"]
    try {
        val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)
        var createTablesQuery =
            "TRUNCATE Memes CASCADE;TRUNCATE History CASCADE; TRUNCATE Favourites CASCADE; TRUNCATE LikesDislikes CASCADE;"

        connection.prepareStatement(createTablesQuery).execute()

    } catch (e: SQLException) {
        e.printStackTrace()
    }
}