package ChillZoneBot.core.src.main.kotlin.handlers.base

import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import java.io.File

data class Meme(
    val id: Int,
    val fileId: String?
)

object MemeStorage {
    private val memes = mutableListOf<Meme>()
    private val file = File("memes.txt").apply {
        if (!exists()) createNewFile()
    }

    init {
        val lines = file.readLines()
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = line.split("|")
            if (parts.size < 2) continue
            val id = parts[0].toIntOrNull() ?: continue
            val fileId = parts[1]
            memes.add(Meme(id, fileId))
        }
    }

    fun size(): Int = memes.size

    fun getByIndex(index: Int): Meme? {
        if (memes.isEmpty()) return null
        val i = index % memes.size
        return memes[i]
    }

    fun addMeme(fileId: String): Meme {
        val newId = if (memes.isEmpty()) 1 else memes.maxOf { it.id } + 1
        val meme = Meme(newId, fileId)
        memes.add(meme)
        file.appendText("${meme.id}|${meme.fileId}\n")
        return meme
    }

    fun getById(id: Int): Meme? = memes.find { it.id == id }

    fun getAll(): List<Meme> = memes.toList()
}

object UserMemeHistory {
    private val seenByChat = mutableMapOf<Long, MutableSet<Int>>()

    fun getNextRandomUnseen(chatId: Long): Meme? {
        val allMemes = MemeStorage.getAll()
        if (allMemes.isEmpty()) return null
        val seenSet = seenByChat.getOrPut(chatId) { mutableSetOf() }
        val unseen = allMemes.filter { it.id !in seenSet }
        if (unseen.isEmpty()) return null
        val meme = unseen.random()
        seenSet.add(meme.id)
        return meme
    }

    fun reset(chatId: Long) {
        seenByChat.remove(chatId)
    }
}

object MemeState {
    var currentIndex: Int = 0

    fun reset() {
        currentIndex = 0
    }

    fun getCurrentMeme(): Meme? = MemeStorage.getByIndex(currentIndex)

    fun nextMeme(): Meme? {
        if (MemeStorage.size() == 0) return null
        currentIndex++
        if (currentIndex >= MemeStorage.size()) currentIndex = 0
        return MemeStorage.getByIndex(currentIndex)
    }
}

object MemeAddState {
    var waitingForPhoto: Boolean = false
}

object ReplyMenuHolder {
    lateinit var replyMenu: ReplyClass
}