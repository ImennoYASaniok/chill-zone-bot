package ChillZoneBot.app.src.main.kotlin.App

import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.TestCore.*

object TestsStates {
    var creatingTest: Boolean = false
    var waitingForName: Boolean = false
    var waitingForQuestionsAmount: Boolean = false
    var waitingForQuestionText: Boolean = false
    var waitingForQuestionType: Boolean = false
    var waitingForAnswersAmount: Boolean = false
    var waitingForAnswers: Boolean = false
    var waitingForCorrectAnswers: Boolean = false
    var currentTest: Test = Test()
    var waitingForAuthor: Boolean = false
    var questionNomer: Int = 1

    var currentQuestionText: String = ""
    var currentAnswerType: AnswerType = AnswerType.DIGITAL
    var currentAnswersAmount: Int = 1
    var answerNomer: Int = 1
    var currentAnswers: MutableList<MutableList<Button>> = mutableListOf()
    var currentCorrectAnswer: Any = Any()
    var currentMaxChooseOptions: Int = 1
}



class Test(
    var name: String = "",
    var author: String = "",
    var questions: MutableList<Question> = mutableListOf(),
    var questionsAmount: Int = 0
    ) {

    fun construct() {

        for (questionNumber in 1..questionsAmount) {
            val question = readln() // текст вопроса
            val currentType = AnswerType.entries.random() // тип ответа
            val amountOfOptions = readln().toInt() // количество ответов на вопрос
            val answers = mutableListOf<Any>() // массив с ответами


            when (currentType) {

                AnswerType.DIGITAL -> { // если тип ответа числовой

                    for (optionIndex in 0..amountOfOptions) {
                        val currentAnswer = readln().toInt()
                        answers.add(currentAnswer)
                    }
                    val answer = readln().toInt()
                    questions.add(Question(question, currentType, answer, answers))

                }

                AnswerType.MULTIPLE_OPTIONS -> { // если тип ответа несколько вариантов

                    for (optionIndex in 0..amountOfOptions) {
                        val currentAnswer = readln()
                        answers.add(currentAnswer)
                    }


                    val answer = mutableListOf<String>()
                    questions.add(Question(question, currentType, answer, answers))

                }

                AnswerType.ONE_OPTION -> { // если тип ответа один вариант

                    for (optionIndex in 0..amountOfOptions) {
                        val currentAnswer = readln()
                        answers.add(currentAnswer)
                    }

                    val answer = readln()
                    questions.add(Question(question, currentType, answer, answers))

                }

                AnswerType.TEXT -> { // если тип ответа текстовый

                    for (optionIndex in 0..amountOfOptions) {
                        val currentAnswer = readln()
                        answers.add(currentAnswer)
                    }

                    val answer = readln()
                    questions.add(Question(question, currentType, answer, answers))

                }

                AnswerType.COLOR -> { // если тип ответа цветовой

                    for (optionIndex in 0..amountOfOptions) {
                        val currentAnswer = Colors.entries.random()
                        answers.add(currentAnswer)
                    }

                    val answer = Pair(Colors.BLACK, Colors.WHITE)
                    questions.add(Question(question, currentType, answer, answers))

                }

            }

        }
    }
}