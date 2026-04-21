package data.models

data class UserProfile(
    val userId: Long,
    val username: String,
    val displayName: String,
    val bio: String,
    val hidden: Boolean,
    val showMedia: Boolean,
    val rating: Int,
    val hideUsername: Boolean = false,
    val isBanned: Boolean = false,
    val createdAt: java.time.LocalDateTime? = null,
    val bannedAt: String? = null,
    val reason: String? = null,
    val banExpiresAt: String? = null,
    val isAdmin: Boolean = false,
    val lastActivityAt: java.time.LocalDateTime? = null,
    val avatarFileId: String? = null
)

