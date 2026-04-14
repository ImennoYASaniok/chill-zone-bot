package data.models

data class GameStats(
    val userId: Long,
    val rating: Int,
    val wins: Int,
    val losses: Int,
    val streak: Int,
    val bestStreak: Int
)
