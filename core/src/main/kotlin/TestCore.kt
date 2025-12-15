package ChillZoneBot.core.src.main.kotlin.TestCore

enum class AnswerType {
    TEXT,
    DIGITAL,
    ONE_OPTION,
    MULTIPLE_OPTIONS,
    COLOR
}

enum class Colors {
    RED,
    YELLOW,
    GREEN,
    ORANGE,
    BLUE,
    WHITE,
    BLACK,
    PURPLE,
    GRAY,
    BROWN,
    PINK
}

data class Question (
    var content: String,
    var typeAnswer: AnswerType,
    var correctAnswer: Any,
    var answers: MutableList<Any>
)