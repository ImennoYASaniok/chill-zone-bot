package ChillZoneBot.app.src.main.kotlin.App

import ChillZoneBot.core.src.main.kotlin.TestCore.*

class Test(
    var name: String,
    var author: String,
    var questions: MutableList<Question>
    ) {

    fun construct() {
        val questionsAmount = readln().toInt() // количество вопросов

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