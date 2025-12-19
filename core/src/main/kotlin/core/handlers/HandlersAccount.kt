package core.handlers

import core.utils.Message

import com.github.kotlintelegrambot.bot
import io.github.cdimascio.dotenv.dotenv
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

fun handlerAccount(kwargs: Map<String, Any>? = null): String {
    // username в kwargs["username"], запихни её в бд
    val databaseUrl: String = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    val databaseUser: String = dotenv()["DATABASE_USER"]
    val databasePassword: String = dotenv()["DATABASE_PASSWORD"]
    val connection: Connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

    try {
        val query = "INSERT INTO Accounts(userId, username) VALUES(?, ?);"
        val statement = connection.prepareStatement(query)
        statement.setLong(1, kwargs!!["userId"].toString().toLong())
        statement.setString(2, kwargs["username"].toString())
    } catch (e: SQLException) {
        e.printStackTrace()
    }

    return Message.getMessage("account/Account.txt", kwargs)
}