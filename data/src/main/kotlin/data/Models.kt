package data

enum class RecommendationType {
    FILM, SERIES, BOOK, GAME
}

enum class QuestionKind {
    SINGLE, MULTI, NUMBER, MATCH
}

data class UserProfile(
    val userId: Long,
    val username: String,
    val displayName: String,
    val bio: String,
    val hidden: Boolean,
    val showMedia: Boolean,
    val rating: Int
)

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

data class PredictionItem(
    val id: Long,
    val text: String,
    val rarity: String,
    val authorId: Long?
)

data class GameStats(
    val userId: Long,
    val rating: Int,
    val wins: Int,
    val losses: Int,
    val streak: Int,
    val bestStreak: Int
)

data class RecommendationItem(
    val id: Int,
    val type: RecommendationType,
    val title: String,
    val genres: List<String>,
    val moods: List<String>,
    val year: Int,
    val description: String
)

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

data class EventItem(
    val id: Long,
    val ownerId: Long,
    val title: String,
    val description: String,
    val place: String,
    val startsAt: String,
    val maxPeople: Int,
    val kind: String
)

data class FeedbackItem(
    val id: Long,
    val userId: Long?,
    val username: String,
    val scope: String,
    val text: String,
    val createdAt: String
)
