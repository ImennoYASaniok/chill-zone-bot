package data.models

data class PredictionItem(
    val id: Long,
    val text: String,
    val rarity: String,
    val authorId: Long?
)

