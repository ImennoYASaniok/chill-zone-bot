package core.tests

import kotlin.random.Random

data class TestQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

data class Test(
    val id: Int,
    val title: String,
    val questions: List<TestQuestion>
)

object TestStorage {
    private val tests: List<Test> = listOf(
        Test(
            id = 1,
            title = "Мини-тест: логика",
            questions = listOf(
                TestQuestion(
                    text = "Сколько будет 7 + 5?",
                    options = listOf("10", "11", "12", "13"),
                    correctIndex = 2
                ),
                TestQuestion(
                    text = "Продолжи последовательность: 2, 4, 8, ?",
                    options = listOf("10", "12", "14", "16"),
                    correctIndex = 3
                ),
                TestQuestion(
                    text = "Какой вариант — НЕ цвет?",
                    options = listOf("Синий", "Квадрат", "Зелёный", "Красный"),
                    correctIndex = 1
                )
            )
        )
    )

    fun random(): Test = tests[Random.nextInt(tests.size)]
}

object TestsFlow {
    data class Session(
        val test: Test,
        var qIndex: Int = 0,
        var correct: Int = 0
    )

    private val sessions = mutableMapOf<Long, Session>()

    fun start(chatId: Long): Session {
        val s = Session(TestStorage.random())
        sessions[chatId] = s
        return s
    }

    fun get(chatId: Long): Session? = sessions[chatId]

    fun stop(chatId: Long) {
        sessions.remove(chatId)
    }

    fun answer(chatId: Long, optionIndex: Int): Pair<Boolean, Session?> {
        val s = sessions[chatId] ?: return false to null
        val q = s.test.questions[s.qIndex]
        val isCorrect = optionIndex == q.correctIndex
        if (isCorrect) s.correct++
        s.qIndex++
        return isCorrect to s
    }
}
