package core.memes

import io.github.cdimascio.dotenv.dotenv
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.random.Random

// -------------------- Models --------------------

data class Meme(val id: Int, val fileId: String)

data class MemeVotes(val likes: Int, val dislikes: Int)

// -------------------- Meme storage --------------------

object MemeStorage {
    private val databaseUrl: String = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    private val databaseUser: String = dotenv()["DATABASE_USER"]
    private val databasePassword: String = dotenv()["DATABASE_PASSWORD"]
    private val connection: Connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)
    private val memes = mutableListOf<Meme>()

    init {
        // Format: id|fileId
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
        val newId = (memes.maxOfOrNull { it.id } ?: 0) + 1
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

    fun reset(chatId: Long) {
        seenByChat.remove(chatId)
    }
    fun saveAll(chatId: Long, memeId: Int) {
        val query = "INSERT INTO History(userId, memeId) VALUES(?, ?);"
        val statement = connection.prepareStatement(query)

        statement.setLong(1, chatId)
        statement.setInt(2, memeId)

        statement.executeUpdate()
    }
}

object UserMemeSession {
    private val lastShownByChat = mutableMapOf<Long, Int?>()

    fun setLastShown(chatId: Long, memeId: Int?) {
        lastShownByChat[chatId] = memeId
    }

    fun getLastShown(chatId: Long): Int? = lastShownByChat[chatId]
}

// -------------------- Favorites --------------------

object FavoritesStorage {
    private val databaseUrl: String = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    private val databaseUser: String = dotenv()["DATABASE_USER"]
    private val databasePassword: String = dotenv()["DATABASE_PASSWORD"]
    private val connection: Connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)
    private val favByChat = mutableMapOf<Long, MutableSet<Int>>()

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

    fun saveAll() {
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

// -------------------- Votes --------------------

object VotesStorage {
    private val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    private val databaseUser = dotenv()["DATABASE_USER"]
    private val databasePassword = dotenv()["DATABASE_PASSWORD"]
    private val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)
    // chatId -> (memeId -> vote)
    private val votes = mutableMapOf<Long, MutableMap<Int, Int>>()

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
        val sb = StringBuilder()
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
            when (map[memeId]) {
                1 -> likes++
                -1 -> dislikes++
            }
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
