package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import data.models.UserProfile
import data.services.AdminService

object KeyboardProfile {
    fun profileMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val isAdmin =
                userProfile?.let {
                    val adminStatus = AdminService.isAdmin(it.userId)
                    adminStatus
                }
                        ?: false

        val rows = mutableListOf<List<String>>()

        rows.addAll(listOf(listOf("✏️ Изменить профиль"), listOf("📊 Статистика аккаунта")))

        if (isAdmin) {
            rows.add(listOf("🛡️ Админ панель"))
        }

        rows.add(listOf("⬅️ Обратно"))

        return kb(rows)
    }

    fun editProfileMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val profileHidden = userProfile?.hidden ?: false
        val usernameHidden = userProfile?.hideUsername ?: false
        val hasAvatar = userProfile?.avatarFileId != null

        return kb(
                listOf(
                        listOf("Изменить имя", "Изменить био"),
                        listOf(if (hasAvatar) "🖼️ Изменить аватарку" else "🖼️ Добавить аватарку"),
                        listOf(
                                if (usernameHidden) "Показать username [👁️]"
                                else "Скрыть username [🙈]",
                                if (profileHidden) "Показать профиль [👁️]"
                                else "Скрыть профиль [🙈]"
                        ),
                        listOf("⬅️ Обратно")
                )
        )
    }

    fun settingsMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val mediaEnabled = userProfile?.showMedia ?: true

        return kb(
                listOf(
                        listOf(
                                if (mediaEnabled) "Выключить картинки [❌]"
                                else "Включить картинки [✅]"
                        ),
                        listOf("⬅️ Обратно")
                )
        )
    }

    fun statsMenu(): KeyboardReplyMarkup {
        return kb(listOf(listOf("⬅️ Обратно")))
    }

    fun avatarProcessingMenu(minSize: Int): KeyboardReplyMarkup {
        return kb(
                listOf(
                        listOf(
                                "✂️ Обрезать до ${minSize}x${minSize}",
                                "📦 Добавить границы до квадрата"
                        ),
                        listOf("⬅️ Отмена")
                )
        )
    }

    private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
        return KeyboardReplyMarkup(
                keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
                resizeKeyboard = true,
                oneTimeKeyboard = false
        )
    }
}
