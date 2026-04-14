package data.models

enum class QuestionKind {
    SINGLE, MULTI, NUMBER, MATCH
}

data class TestQuestion(
    val id: Long,
    val testId: Long,
    val position: Int,
    val kind: QuestionKind,
    val prompt: String,
    val options: List<String>,
    val answer: String
)

data class TestSummary(
    val id: Long,
    val title: String,
    val authorId: Long?
)
