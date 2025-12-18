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

object GoingThroughTests {
    var testsId: MutableList<Int> = mutableListOf()
    var testsAuthor: MutableList<String> = mutableListOf()
    var testsQAmount: MutableList<Int> = mutableListOf()
    var testsTitles: MutableList<String> = mutableListOf()
    var currentIndex: Int = 0
    var currentQIndex: Int = 0
    var currentQaA: List<QuestionWithAnswers> = listOf()
    var waitingForAnswer: Boolean = false
    var choosingAns: Int = 1
    var selectedAnswers: MutableList<Int> = mutableListOf()
    var userAnswers: MutableList<UserAnswer> = mutableListOf()
    var correctAnswersCount: Int = 0

    fun reset() {
        currentQIndex = 0
        currentQaA = listOf()
        waitingForAnswer = false
        choosingAns = 1
        selectedAnswers.clear()
        userAnswers.clear()
        correctAnswersCount = 0
    }
}


class Test(
    var name: String = "",
    var author: String = "",
    var questions: MutableList<Question> = mutableListOf(),
    var questionsAmount: Int = 0
    ) {
}