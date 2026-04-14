package data.models

data class BannedUser(
    val userId: Long,
    val username: String,
    val displayName: String,
    val bannedAt: String,
    val reason: String?
)

data class AdminSearchResult(
    val users: List<UserProfile>,
    val totalCount: Int,
    val hasMore: Boolean
)
