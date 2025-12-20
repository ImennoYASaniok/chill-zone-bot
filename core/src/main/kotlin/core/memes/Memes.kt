package core.memes

import java.io.File
import kotlin.random.Random

// -------------------- Models --------------------

data class Meme(val id: Int, val fileId: String)

data class MemeVotes(val likes: Int, val dislikes: Int)

// -------------------- Meme storage --------------------

object MemeStorage {
    private val file = File("memes.txt").apply { if (!exists()) createNewFile() }
    private val memes = mutableListOf<Meme>()

    init {
        // Format: id|fileId
        for (line in file.readLines()) {
            if (line.isBlank()) continue
            val parts = line.split("|", limit = 2)
            if (parts.size < 2) continue
            val id = parts[0].toIntOrNull() ?: continue
            val fileId = parts[1].trim()
            if (fileId.isBlank()) continue
            memes.add(Meme(id, fileId))
        }
    }

    fun size(): Int = memes.size

    fun getAll(): List<Meme> = memes.toList()

    fun getById(id: Int): Meme? = memes.find { it.id == id }

    fun addMeme(fileId: String): Meme {
        val newId = (memes.maxOfOrNull { it.id } ?: 0) + 1
        val meme = Meme(newId, fileId)
        memes.add(meme)
        file.appendText("${meme.id}|${meme.fileId}\n")
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
        val meme = unseen[Random.nextInt(unseen.size)]
        seen.add(meme.id)
        return meme
    }

    fun reset(chatId: Long) {
        seenByChat.remove(chatId)
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
    private val file = File("favorites.txt").apply { if (!exists()) createNewFile() }
    private val favByChat = mutableMapOf<Long, MutableSet<Int>>()

    init {
        // Format: chatId|memeId
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

// -------------------- Votes --------------------

object VotesStorage {
    private val file = File("votes.txt").apply { if (!exists()) createNewFile() }
    // chatId -> (memeId -> vote)
    private val votes = mutableMapOf<Long, MutableMap<Int, Int>>()

    init {
        // Format: chatId|memeId|vote(1/-1)
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
