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
        val neededGenres: ArrayList<String> = arrayListOf()
        var createTablesQuery =
            "INSERT INTO correctTextAnswers(questionId, ans) VALUES(1, 'да');"
        println("ВАЖНО")

        connection.prepareStatement(createTablesQuery).execute()
//        connection.createStatement().executeQuery(createTablesQuery).use { resultSet ->
//            while(resultSet.next()) {
//
//                println(resultSet.getInt(1))
//            }
//        }

//        var preparedStatement = connection.prepareStatement(createTablesQuery)
//        val sqlArray = connection.createArrayOf("VARCHAR", neededGenres.toArray())
//        preparedStatement.setArray(1, sqlArray)
//        preparedStatement.setInt(2, neededGenres.size)
//        preparedStatement.executeQuery().use { resultSet ->
//            while(resultSet.next()) {
//                println
//            }
//        }

    } catch (e: SQLException) {
        e.printStackTrace()
    }
}