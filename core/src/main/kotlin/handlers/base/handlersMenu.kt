package ChillZoneBot.core.src.main.kotlin.handlers.base

import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import io.github.cdimascio.dotenv.dotenv
import java.io.File
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

    init {
        try {
            val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
            val databaseUser = dotenv()["DATABASE_USER"]
            val databasePassword = dotenv()["DATABASE_PASSWORD"]

            val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

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
            val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
            val databaseUser = dotenv()["DATABASE_USER"]
            val databasePassword = dotenv()["DATABASE_PASSWORD"]
            val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)
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

    fun getNextRandomUnseen(chatId: Long): Meme? {
        val all = MemeStorage.getAll()
        if (all.isEmpty()) return null
        val seen = seenByChat.getOrPut(chatId) { mutableSetOf() }
        val unseen = all.filter { it.id !in seen }
        if (unseen.isEmpty()) return null
        val meme = unseen.random()
        seen.add(meme.id)
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
    private val file = File("favorites.txt").apply { if (!exists()) createNewFile() }

    init {
        for (line in file.readLines()) {
            if (line.isBlank()) continue
            val parts = line.split("|")
            if (parts.size < 2) continue
            val chatId = parts[0].toLongOrNull() ?: continue
            val memeId = parts[1].toIntOrNull() ?: continue
            favByChat.getOrPut(chatId) { mutableSetOf() }.add(memeId)
        }
    }

    private fun saveAll() {
        val sb = StringBuilder()
        for ((chatId, set) in favByChat) {
            for (memeId in set) sb.append(chatId).append("|").append(memeId).append("\n")
        }
        file.writeText(sb.toString())
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
    private val file = File("votes.txt").apply { if (!exists()) createNewFile() }

    init {
        for (line in file.readLines()) {
            if (line.isBlank()) continue
            val parts = line.split("|")
            if (parts.size < 3) continue
            val chatId = parts[0].toLongOrNull() ?: continue
            val memeId = parts[1].toIntOrNull() ?: continue
            val vote = parts[2].toIntOrNull() ?: continue
            if (vote != 1 && vote != -1) continue
            votes.getOrPut(chatId) { mutableMapOf() }[memeId] = vote
        }
    }

    private fun saveAll() {
        val sb = StringBuilder()
        for ((chatId, map) in votes) {
            for ((memeId, vote) in map) {
                sb.append(chatId).append("|").append(memeId).append("|").append(vote).append("\n")
            }
        }
        file.writeText(sb.toString())
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