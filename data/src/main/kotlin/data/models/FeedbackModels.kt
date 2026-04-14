package data.models

data class FeedbackItem(
    val id: Long,
    val userId: Long?,
    val username: String,
    val scope: String,
    val text: String,
    val createdAt: String
)
