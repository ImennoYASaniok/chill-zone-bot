package ChillZoneBot.core.src.main.kotlin.handlers.predictions

import java.io.File

data class Prediction(
    val id: Int,
    val text: String,
    val rarity: String
)

object PredictionStorage {
    private val predictions = mutableListOf<Prediction>()
    private val file = File("predictions.txt").apply { if (!exists()) createNewFile() }

    init {
        file.readLines().forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = line.split("|")
            if (parts.size < 3) return@forEach
            val id = parts[0].toIntOrNull() ?: return@forEach
            val text = parts[1]
            val rarity = parts[2]
            predictions.add(Prediction(id, text, rarity))
        }
    }

    fun addPrediction(text: String, rarity: String): Prediction {
        val newId = if (predictions.isEmpty()) 1 else predictions.maxOf { it.id } + 1
        val pred = Prediction(newId, text, rarity)
        predictions.add(pred)
        file.appendText("${pred.id}|${pred.text}|${pred.rarity}\n")
        return pred
    }

    fun getAll(): List<Prediction> = predictions.toList()
    fun size(): Int = predictions.size
    fun getByIndex(index: Int): Prediction? {
        if (predictions.isEmpty()) return null
        val i = index % predictions.size
        return predictions[i]
    }
}

object UserPredictionHistory {
    private val userPredictions = mutableMapOf<Long, MutableList<Prediction>>()

    fun addToUser(chatId: Long, pred: Prediction) {
        val list = userPredictions.getOrPut(chatId) { mutableListOf() }
        list.add(pred)
    }

    fun getUserPredictions(chatId: Long): List<Prediction> {
        return userPredictions[chatId]?.toList() ?: emptyList()
    }
}