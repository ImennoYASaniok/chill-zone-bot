package ChillZoneBot.app.src.main.kotlin.App

import io.github.cdimascio.dotenv.dotenv
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

fun getTestQuestions(testId: Int): List<QuestionWithAnswers> {
    val databaseUrl = dotenv()["DATABASE_URL"]
    val databaseUser = dotenv()["DATABASE_USER"]
    val databasePassword = dotenv()["DATABASE_PASSWORD"]

    val questionsList = mutableListOf<QuestionWithAnswers>()

    try {
        val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

        // 1. Получаем все вопросы теста
        val questionsQuery = """
            SELECT id, title, answersAmount 
            FROM questions 
            WHERE testId = ?
            ORDER BY id
        """.trimIndent()

        val questionsStmt = connection.prepareStatement(questionsQuery)
        questionsStmt.setInt(1, testId)

        questionsStmt.executeQuery().use { questionsResult ->
            while (questionsResult.next()) {
                val questionId = questionsResult.getInt("id")
                val questionTitle = questionsResult.getString("title")
                val answersAmount = questionsResult.getInt("answersAmount")

                // Определяем тип вопроса
                val questionType = determineQuestionType(connection, questionId)

                // Получаем ответы
                val answers = when (questionType) {
                    "CHOOSE" -> getChooseAnswers(connection, questionId)
                    "INT" -> getIntAnswers(connection, questionId)
                    "TEXT" -> getTextAnswers(connection, questionId)
                    else -> emptyList()
                }

                var question: Any

                if (questionType == "CHOOSE") {

                    question = QuestionWithAnswers(
                        id = questionId,
                        title = questionTitle,
                        answersAmount = answersAmount,
                        type = questionType,
                        answers = answers,
                        chooseAnswers = getChooseAnswers(connection, questionId)
                    )
                } else {

                    question = QuestionWithAnswers(
                        id = questionId,
                        title = questionTitle,
                        answersAmount = answersAmount,
                        type = questionType,
                        answers = answers,
                        chooseAnswers = emptyList()
                    )
                }

                questionsList.add(question)
            }
        }

        questionsStmt.close()
        connection.close()

    } catch (e: SQLException) {
        e.printStackTrace()
    }

    return questionsList
}

// Исправленная функция для получения вариантов выбора
fun getChooseAnswers(connection: Connection, questionId: Int): List<ChooseAnswer> {
    val answers = mutableListOf<ChooseAnswer>()

    // Для вопросов типа CHOOSE должна существовать таблица chooseAnswers
    // Проверим сначала, существует ли она
    val checkExists = "SELECT 1 FROM chooseAnswers WHERE questionId = ? LIMIT 1"
    val checkStmt = connection.prepareStatement(checkExists)
    checkStmt.setInt(1, questionId)
    val exists = checkStmt.executeQuery().next()
    checkStmt.close()

    if (!exists) {
        println("Warning: No chooseAnswers found for question ID: $questionId")
        return emptyList()  // Возвращаем пустой список, если нет записей
    }

    // Если запись существует, получаем данные
    val query = """
        SELECT 
            ca.ans1, ca.ans2, ca.ans3, ca.ans4, ca.ans5, ca.ans6,
            COALESCE(cca.ans1, FALSE) as is_correct1, 
            COALESCE(cca.ans2, FALSE) as is_correct2, 
            COALESCE(cca.ans3, FALSE) as is_correct3,
            COALESCE(cca.ans4, FALSE) as is_correct4, 
            COALESCE(cca.ans5, FALSE) as is_correct5, 
            COALESCE(cca.ans6, FALSE) as is_correct6
        FROM chooseAnswers ca
        LEFT JOIN correctChooseAnswers cca ON ca.questionId = cca.questionId
        WHERE ca.questionId = ?
    """.trimIndent()

    val stmt = connection.prepareStatement(query)
    stmt.setInt(1, questionId)

    stmt.executeQuery().use { result ->
        if (result.next()) {
            // Проверяем каждый из 6 возможных вариантов
            for (i in 1..6) {
                val answerText = result.getString("ans$i")

                // Если вариант ответа существует (не NULL)
                if (answerText != null) {
                    // Получаем флаг правильности
                    val isCorrect = result.getBoolean("is_correct$i")

                    answers.add(ChooseAnswer(
                        text = answerText,
                        isCorrect = isCorrect
                    ))
                }
            }
        }
    }

    stmt.close()
    return answers
}

// Функции для других типов вопросов остаются такими же
fun getIntAnswers(connection: Connection, questionId: Int): List<IntAnswer> {
    val answers = mutableListOf<IntAnswer>()

    val query = "SELECT ans FROM correctIntAnswers WHERE questionId = ?"
    val stmt = connection.prepareStatement(query)
    stmt.setInt(1, questionId)

    stmt.executeQuery().use { result ->
        while (result.next()) {
            val ans = result.getInt("ans")
            if (!result.wasNull()) {
                answers.add(IntAnswer(ans))
            }
        }
    }

    stmt.close()
    return answers
}

fun getTextAnswers(connection: Connection, questionId: Int): List<TextAnswer> {
    val answers = mutableListOf<TextAnswer>()

    val query = "SELECT ans FROM correctTextAnswers WHERE questionId = ?"
    val stmt = connection.prepareStatement(query)
    stmt.setInt(1, questionId)

    stmt.executeQuery().use { result ->
        while (result.next()) {
            val ans = result.getString("ans")
            if (ans != null) {
                answers.add(TextAnswer(ans))
            }
        }
    }

    stmt.close()
    return answers
}

// Data classes для разных типов ответов
data class ChooseAnswer(
    val text: String,
    val isCorrect: Boolean
)

data class IntAnswer(val value: Int)
data class TextAnswer(val value: String)

data class QuestionWithAnswers(
    val id: Int,
    val title: String,
    val answersAmount: Int,
    val type: String, // "CHOOSE", "INT", "TEXT"
    val answers: List<Any>,
    val chooseAnswers: List<ChooseAnswer>
)


fun determineQuestionType(connection: Connection, questionId: Int): String {
    // Порядок проверки ВАЖЕН! Сначала проверяем "CHOOSE", потом остальные

    // 1. Проверяем chooseAnswers - если есть, то это CHOOSE вопрос
    // (для вопросов с выбором варианта всегда есть эта таблица)
    val checkChoose = "SELECT 1 FROM chooseAnswers WHERE questionId = ? LIMIT 1"
    val stmt1 = connection.prepareStatement(checkChoose)
    stmt1.setInt(1, questionId)
    val hasChoose = stmt1.executeQuery().next()
    stmt1.close()

    if (hasChoose) {
        println("Found CHOOSE question for ID: $questionId")
        return "CHOOSE"
    }

    // 2. Проверяем correctIntAnswers
    val checkInt = "SELECT 1 FROM correctIntAnswers WHERE questionId = ? LIMIT 1"
    val stmt2 = connection.prepareStatement(checkInt)
    stmt2.setInt(1, questionId)
    val hasInt = stmt2.executeQuery().next()
    stmt2.close()

    if (hasInt) {
        println("Found INT question for ID: $questionId")
        return "INT"
    }

    // 3. Проверяем correctTextAnswers
    val checkText = "SELECT 1 FROM correctTextAnswers WHERE questionId = ? LIMIT 1"
    val stmt3 = connection.prepareStatement(checkText)
    stmt3.setInt(1, questionId)
    val hasText = stmt3.executeQuery().next()
    stmt3.close()

    if (hasText) {
        println("Found TEXT question for ID: $questionId")
        return "TEXT"
    }

    println("Warning: No question type found for ID: $questionId")
    return "UNKNOWN"
}