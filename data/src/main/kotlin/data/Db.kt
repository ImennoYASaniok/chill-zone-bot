package data

import io.github.cdimascio.dotenv.dotenv
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

object Db {
    private val env = dotenv()
    private val url = env["DATABASE_URL"] ?: error("DATABASE_URL is not set")
    private val user = env["DATABASE_USER"] ?: ""
    private val pass = env["DATABASE_PASSWORD"] ?: ""

    val connection: Connection by lazy {
        DriverManager.getConnection(url, user, pass)
    }

    fun raw(sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }

    fun execute(sql: String, bind: (PreparedStatement) -> Unit = {}): Int {
        connection.prepareStatement(sql).use { stmt ->
            bind(stmt)
            return stmt.executeUpdate()
        }
    }

    fun insertId(sql: String, bind: (PreparedStatement) -> Unit = {}): Long {
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            bind(stmt)
            stmt.executeUpdate()
            stmt.generatedKeys.use { keys ->
                return if (keys.next()) keys.getLong(1) else 0L
            }
        }
    }

    fun <T> query(sql: String, bind: (PreparedStatement) -> Unit = {}, map: (ResultSet) -> T): List<T> {
        connection.prepareStatement(sql).use { stmt ->
            bind(stmt)
            stmt.executeQuery().use { rs ->
                val out = mutableListOf<T>()
                while (rs.next()) out += map(rs)
                return out
            }
        }
    }

    fun <T> single(sql: String, bind: (PreparedStatement) -> Unit = {}, map: (ResultSet) -> T): T? {
        return query(sql, bind, map).firstOrNull()
    }
}
