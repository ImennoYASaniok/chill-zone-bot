package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import data.models.UserProfile
import data.AdminService

object KeyboardProfile {
    fun profileMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val isAdmin = userProfile?.let { 
            val adminStatus = AdminService.isAdmin(it.userId)
            println("DEBUG: Проверка админских прав для пользователя ${it.userId}: $adminStatus")
            adminStatus
        } ?: false
        
        println("DEBUG: profileMenu - userProfile: $userProfile, isAdmin: $isAdmin")
        
        val rows = mutableListOf<List<String>>()

        rows.addAll(
            listOf(
                listOf("✏️ Изменить профиль"),
                listOf("📊 Статистика аккаунта")
            )
        )

        if (isAdmin) {
            rows.add(listOf("🛡️ Админ панель"))
            println("DEBUG: Админская кнопка добавлена")
        }

        rows.add(listOf("⬅️ Обратно"))

        return kb(rows)
    }

    fun editProfileMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val profileHidden = userProfile?.hidden ?: false
        val usernameHidden = userProfile?.hideUsername ?: false

        return kb(
            listOf(
                listOf("Изменить имя", "Изменить био"),
                listOf(if (usernameHidden) "Показать username [👁️]" else "Скрыть username [🙈]", if (profileHidden) "Показать профиль [👁️]" else "Скрыть профиль [🙈]"),
                listOf("⬅️ Назад")
            )
        )
    }

    fun settingsMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val mediaEnabled = userProfile?.showMedia ?: true
        
        return kb(
            listOf(
                listOf(if (mediaEnabled) "Выключить картинки [❌]" else "Включить картинки [✅]"),
                listOf("⬅️ Обратно")
            )
        )
    }

    fun statsMenu(): KeyboardReplyMarkup {
        return kb(
            listOf(
                listOf("⬅️ Назад")
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