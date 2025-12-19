package ChillZoneBot.core.src.main.kotlin.handlers.base

import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import io.github.cdimascio.dotenv.dotenv
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

data class Meme(
    val id: Int,
    val fileId: String?
)
/*

CREATE TABLE memes(id SERIAL PRIMARY KEY, fileId TEXT); - запрос на создание таблицы мемов

 */
object MemeStorage {
    private val memes = mutableListOf<Meme>()
    // ----------- База Данных -----------
    private val databaseUrl: String = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    private val databaseUser: String = dotenv()["DATABASE_USER"]
    private val databasePassword: String = dotenv()["DATABASE_PASSWORD"]
    private val connection: Connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

    init {
        try {
            var query = "SELECT * FROM Memes"
            var statement = connection.createStatement()
            statement.executeQuery(query).use { resultSet ->
                while (resultSet.next()) {
                    memes.add(Meme(resultSet.getInt(1), resultSet.getString(2)))
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun size(): Int = memes.size
    fun getAll(): List<Meme> = memes.toList()
    fun getById(id: Int): Meme? = memes.find { it.id == id }

    fun addMeme(fileId: String): Meme {
        val newId = if (memes.isEmpty()) 1 else memes.maxOf { it.id } + 1
        val meme = Meme(newId, fileId)
        memes.add(meme)
        try {
            val insertMovieQuery =
                "INSERT INTO Memes (fileId) VALUES (?)"
            val preparedStatement = connection.prepareStatement(insertMovieQuery)
            preparedStatement.setString(1, fileId)
            val rowsAffected = preparedStatement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return meme
    }
}

object MemeAddState {
    var waitingForPhoto: Boolean = false
}

object UserMemeHistory {
    private val seenByChat = mutableMapOf<Long, MutableSet<Int>>()


    // ----------- База Данных -----------
    private val databaseUrl: String = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    private val databaseUser: String = dotenv()["DATABASE_USER"]
    private val databasePassword: String = dotenv()["DATABASE_PASSWORD"]
    private val connection: Connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

    init {
        val statement = connection.createStatement()
        val query = "SELECT * FROM History;"

        statement.executeQuery(query).use { resultSet ->
            while (resultSet.next()) {
                val chatId = resultSet.getLong("userId")
                val memeId = resultSet.getInt("memeId")
                if (seenByChat[chatId] == null) {
                    seenByChat[chatId] = mutableSetOf(memeId)
                } else {
                    seenByChat[chatId]!!.add(memeId)
                }
            }
        }
    }

    fun saveAll(chatId: Long, memeId: Int) {
        val query = "INSERT INTO History(userId, memeId) VALUES(?, ?);"
        val statement = connection.prepareStatement(query)

        statement.setLong(1, chatId)
        statement.setInt(2, memeId)

        statement.executeUpdate()
    }


    fun getNextRandomUnseen(chatId: Long): Meme? {
        val all = MemeStorage.getAll()
        if (all.isEmpty()) return null
        val seen = seenByChat.getOrPut(chatId) { mutableSetOf() }
        println(seen)
        val unseen = all.filter { it.id !in seen }
        if (unseen.isEmpty()) return null
        val meme = unseen.random()
        seen.add(meme.id)

        saveAll(chatId, meme.id)

        return meme
    }
}

object UserMemeSession {
    private val lastShownByChat = mutableMapOf<Long, Int?>()

    fun setLastShown(chatId: Long, memeId: Int?) {
        lastShownByChat[chatId] = memeId
    }

    fun getLastShown(chatId: Long): Int? = lastShownByChat[chatId]
}

object FavoritesStorage {
    private val favByChat = mutableMapOf<Long, MutableSet<Int>>()

    private val databaseUrl: String = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    private val databaseUser: String = dotenv()["DATABASE_USER"]
    private val databasePassword: String = dotenv()["DATABASE_PASSWORD"]
    private val connection: Connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)


    init {
        try {
            var query = "SELECT * FROM Favourites"
            var statement = connection.createStatement()
            statement.executeQuery(query).use { resultSet ->
                while (resultSet.next()) {
                    val chatId = resultSet.getLong("userId")
                    val memeId = resultSet.getInt("memeId")
                    favByChat.getOrPut(chatId) { mutableSetOf() }.add(memeId)
                }
            }
            println()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    private fun saveAll() {
        for ((chatId, set) in favByChat) {
            for (memeId in set) {
                try {
                    val query = "INSERT INTO Favourites(userId, memeId) VALUES(?, ?);"
                    val statement = connection.prepareStatement(query)

                    statement.setLong(1, chatId)
                    statement.setInt(2, memeId)

                    statement.executeUpdate()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun add(chatId: Long, memeId: Int): Boolean {
        val set = favByChat.getOrPut(chatId) { mutableSetOf() }
        val before = set.size
        set.add(memeId)
        val changed = set.size != before
        if (changed) saveAll()
        return changed
    }

    fun remove(chatId: Long, memeId: Int): Boolean {
        val set = favByChat.getOrPut(chatId) { mutableSetOf() }
        val removed = set.remove(memeId)
        if (removed) saveAll()
        return removed
    }

    fun list(chatId: Long): List<Meme> {
        val set = favByChat[chatId] ?: return emptyList()
        return set.mapNotNull { MemeStorage.getById(it) }
    }
}

data class MemeVotes(val likes: Int, val dislikes: Int)

object VotesStorage {
    private val votes = mutableMapOf<Long, MutableMap<Int, Int>>()
    private val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    private val databaseUser = dotenv()["DATABASE_USER"]
    private val databasePassword = dotenv()["DATABASE_PASSWORD"]

    private val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

    init {
        try {
            var query = "SELECT * FROM LikesDislikes"
            var statement = connection.createStatement()
            statement.executeQuery(query).use { resultSet ->
                while (resultSet.next()) {
                    val chatId = resultSet.getLong("userId")
                    val memeId = resultSet.getInt("memeId")
                    val vote = resultSet.getInt("vote")
                    votes.getOrPut(chatId) { mutableMapOf() }[memeId] = vote
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    private fun saveAll() {
        for ((chatId, map) in votes) {
            for ((memeId, vote) in map) {
                try {
                    val query = "INSERT INTO LikesDislikes(userId, memeId, vote) VALUES(?, ?, ?);"
                    val statement = connection.prepareStatement(query)

                    statement.setLong(1, chatId)
                    statement.setInt(2, memeId)
                    statement.setInt(3, vote)

                    statement.executeUpdate()

                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getCounts(memeId: Int): MemeVotes {
        var likes = 0
        var dislikes = 0
        for ((_, map) in votes) {
            val v = map[memeId] ?: continue
            if (v == 1) likes++
            if (v == -1) dislikes++
        }
        return MemeVotes(likes, dislikes)
    }

    fun like(chatId: Long, memeId: Int): MemeVotes {
        val map = votes.getOrPut(chatId) { mutableMapOf() }
        val current = map[memeId]
        if (current == 1) map.remove(memeId) else map[memeId] = 1
        saveAll()
        return getCounts(memeId)
    }

    fun dislike(chatId: Long, memeId: Int): MemeVotes {
        val map = votes.getOrPut(chatId) { mutableMapOf() }
        val current = map[memeId]
        if (current == -1) map.remove(memeId) else map[memeId] = -1
        saveAll()
        return getCounts(memeId)
    }
}

object ReplyMenuHolder {
    lateinit var replyMenu: ReplyClass
}