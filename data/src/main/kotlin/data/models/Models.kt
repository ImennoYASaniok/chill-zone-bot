package data.models

enum class AccountType(val dbValue: String, val label: String) {
    USER("user", "Пользователь"),
    MODERATOR("moderator", "Модератор"),
    ADMIN("admin", "Админ");

    companion object {
        fun fromDb(value: String?): AccountType {
            val normalized = value?.trim()?.lowercase()
            return values().firstOrNull { it.dbValue == normalized } ?: USER
        }
    }
}

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
    val accountType: AccountType = AccountType.USER,
    val isAdmin: Boolean = accountType == AccountType.ADMIN,
    val lastActivityAt: java.time.LocalDateTime? = null,
    val avatarFileId: String? = null
)

