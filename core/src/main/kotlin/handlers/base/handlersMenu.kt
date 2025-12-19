package ChillZoneBot.core.src.main.kotlin.handlers.predictions


import ChillZoneBot.core.src.main.kotlin.handlers.predictions.PredictionStorage.logger
import ChillZoneBot.core.src.main.kotlin.handlers.predictions.PredictionStorage.predictions
import io.github.cdimascio.dotenv.dotenv
import java.sql.DriverManager
import java.util.logging.Logger

// Класс предсказания


data class Prediction(
    val id: Int,
    val text: String,
    val rarity: String
)

object PredictionStorage {
    private val logger = Logger.getLogger("XZ-PREDICTIONS")
    val predictions = mutableMapOf<Int, Prediction>()
    // -------- База Данных --------
    private val databaseUrl: String = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    private val databaseUser: String = dotenv()["DATABASE_USER"]
    private val databasePassword: String = dotenv()["DATABASE_PASSWORD"]
    private val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

    init {

        val query = "SELECT * FROM Predictions"

        connection.createStatement().executeQuery(query).use { resultSet ->
            while(resultSet.next()) {
                val id = resultSet.getInt(1)
                val text = resultSet.getString(2)
                val rarity = resultSet.getString(3)

                predictions[id] = Prediction(id, text, rarity)

            }
        }
    }

    fun addPrediction(text: String, rarity: String): Prediction {
        val newId = if (predictions.isEmpty()) 1 else predictions.maxOf { it.key } + 1
        val pred = Prediction(newId, text, rarity)

        predictions[newId] = pred

        val query = "INSERT INTO Predictions(content, rarity) VALUES(?, ?);"

        val prepStmt = connection.prepareStatement(query)

        prepStmt.setString(1, text)
        prepStmt.setString(2, rarity)

        val insertionResult = prepStmt.executeUpdate()

        logger.info("Prediction insertions status: $insertionResult")


        return pred
    }

    fun getAll(): Map<Int, Prediction> = predictions.toMap()

    fun size(): Int = predictions.size
}
// История предсказаний пользователя
object UserPredictionHistory {
    private val history: MutableMap<Long, MutableList<Prediction>> = mutableMapOf()
    private val logger = Logger.getLogger("XZ-PREDICTIONS-HISTORY")
    // -------- База Данных --------
    private val databaseUrl: String = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
    private val databaseUser: String = dotenv()["DATABASE_USER"]
    private val databasePassword: String = dotenv()["DATABASE_PASSWORD"]
    private val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

    init {

        val query = "SELECT * FROM PredictionsHistory"
        val prepStatement = connection.createStatement()

        prepStatement.executeQuery(query).use { resultSet ->
            while(resultSet.next()) {
                val userId = resultSet.getLong(2)
                val predictionId = resultSet.getInt(3)

                if (history[userId] == null) {
                    history[userId] = mutableListOf(predictions[predictionId]!!)
                } else {
                    history[userId]!!.add(predictions[predictionId]!!)
                }
            }
        }



    }

    fun addToUser(userId: Long, prediction: Prediction) {


        val list = history.getOrPut(userId) { mutableListOf() }
        list.add(prediction)


        val query = "INSERT INTO PredictionsHistory(userId, predictionId) VALUES(?, ?);"

        val prepStmt = connection.prepareStatement(query)

        prepStmt.setLong(1, userId)
        prepStmt.setInt(2, prediction.id)

        val insertionResult = prepStmt.executeUpdate()

        logger.info("User history INSERT status: $insertionResult")



    }

    fun getUserPredictions(userId: Long): List<Prediction> {

        return history[userId]?.toList() ?: emptyList()

    }
}

