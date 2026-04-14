package data.models

data class MemeItem(
    val id: Long,
    val fileId: String,
    val uploaderId: Long?,
    val caption: String
)

data class MemeVotes(
    val likes: Int,
    val dislikes: Int
)
