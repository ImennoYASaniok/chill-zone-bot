package core.predictions

import java.io.File
import kotlin.random.Random

data class Prediction(val id: Int, val text: String, val rarity: String)

object PredictionStorage {
    private val file = File("predictions.txt").apply { if (!exists()) createNewFile() }
    private val list: MutableList<Prediction> = mutableListOf()

    init {
        // id|rarity|text
        file.readLines().forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = line.split("|", limit = 3)
            if (parts.size < 3) return@forEach
            val id = parts[0].toIntOrNull() ?: return@forEach
            val rarity = parts[1].trim()
            val text = parts[2].trim()
            if (text.isBlank()) return@forEach
            list.add(Prediction(id, text, rarity))
        }
        // Если файла нет/пустой — добавим пару дефолтных, чтобы фича "сразу работала"
        if (list.isEmpty()) {
            add("Сегодня тебе повезёт в мелочах.", "Обычное")
            add("Неожиданная встреча окажется полезной.", "Редкое")
            add("Твои планы ускорятся быстрее, чем ты думаешь.", "Эпическое")
            add("Большой шанс на прорыв — не тормози.", "Легендарное")
        }
    }

    private fun persist(p: Prediction) {
        file.appendText("${p.id}|${p.rarity}|${p.text}\n")
    }

    fun all(): List<Prediction> = list.toList()

    fun add(text: String, rarity: String): Prediction {
        val newId = (list.maxOfOrNull { it.id } ?: 0) + 1
        val p = Prediction(newId, text.trim(), rarity)
        list.add(p)
        persist(p)
        return p
    }

    fun search(q: String): List<Prediction> =
        list.filter { it.text.contains(q, ignoreCase = true) || it.rarity.contains(q, ignoreCase = true) }

    fun randomByWeightedRarity(): Prediction? {
        if (list.isEmpty()) return null
        val rand = Random.nextInt(100)
        val rarity = when {
            rand < 70 -> "Обычное"
            rand < 94 -> "Редкое"
            rand < 99 -> "Эпическое"
            else -> "Легендарное"
        }
        val pool = list.filter { it.rarity == rarity }
        val actualPool = if (pool.isNotEmpty()) pool else list
        return actualPool[Random.nextInt(actualPool.size)]
    }
}

object UserPredictionHistory {
    private val file = File("predictions_history.txt").apply { if (!exists()) createNewFile() }
    private val byChat: MutableMap<Long, MutableSet<Int>> = mutableMapOf()

    init {
        // chatId|predictionId
        file.readLines().forEach { line ->
            if (line.isBlank()) return@forEach
            val p = line.split("|")
            if (p.size < 2) return@forEach
            val chatId = p[0].toLongOrNull() ?: return@forEach
            val predId = p[1].toIntOrNull() ?: return@forEach
            byChat.getOrPut(chatId) { mutableSetOf() }.add(predId)
        }
    }

    private fun saveAll() {
        val sb = StringBuilder()
        for ((chat, set) in byChat) {
            for (id in set) sb.append(chat).append('|').append(id).append('\n')
        }
        file.writeText(sb.toString())
    }

    fun list(chatId: Long): List<Prediction> {
        val set = byChat[chatId] ?: return emptyList()
        val all = PredictionStorage.all().associateBy { it.id }
        return set.mapNotNull { all[it] }
    }

    fun add(chatId: Long, p: Prediction): Boolean {
        val set = byChat.getOrPut(chatId) { mutableSetOf() }
        val before = set.size
        set.add(p.id)
        val changed = set.size != before
        if (changed) saveAll()
        return changed
    }
}

object PredictionsFlow {
    // на один чат — одно состояние ввода
    private val waitingRarity: MutableMap<Long, String?> = mutableMapOf()
    private val searchMode: MutableSet<Long> = mutableSetOf()

    val rarities = listOf("Обычное", "Редкое", "Эпическое", "Легендарное")

    fun startAdd(chatId: Long) { waitingRarity[chatId] = "" }
    fun setRarity(chatId: Long, rarity: String) { waitingRarity[chatId] = rarity }
    fun getRarity(chatId: Long): String? = waitingRarity[chatId]
    fun stopAdd(chatId: Long) { waitingRarity.remove(chatId) }

    fun startSearch(chatId: Long) { searchMode.add(chatId) }
    fun isSearching(chatId: Long): Boolean = searchMode.contains(chatId)
    fun stopSearch(chatId: Long) { searchMode.remove(chatId) }
}
