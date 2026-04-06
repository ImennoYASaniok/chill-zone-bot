package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import data.UserProfile
import data.AdminService

object KeyboardProfile {
    fun profileMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val profileHidden = userProfile?.hidden ?: false
        val usernameHidden = userProfile?.hideUsername ?: false
        val isAdmin = userProfile?.let { 
            val adminStatus = AdminService.isAdmin(it.userId)
            println("DEBUG: Проверка админских прав для пользователя ${it.userId}: $adminStatus")
            adminStatus
        } ?: false
        
        println("DEBUG: profileMenu - userProfile: $userProfile, isAdmin: $isAdmin")
        
        val rows = mutableListOf<List<String>>()
        
        // Добавляем админскую кнопку в начало для администраторов
        if (isAdmin) {
            rows.add(listOf("🛡️ Админ панель"))
            println("DEBUG: Админская кнопка добавлена")
        }
        
        rows.addAll(listOf(
            listOf("Изменить имя", if (usernameHidden) "Показать username [👁️]" else "Скрыть username [🙈]"),
            listOf("Изменить био"),
            listOf(
                if (profileHidden) "Показать профиль [👁️]" else "Скрыть профиль [🙈]"
            ),
            listOf("⬅️ Обратно")
        ))
        
        return kb(rows)
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

    private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
        return KeyboardReplyMarkup(
            keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
            resizeKeyboard = true,
            oneTimeKeyboard = false
        )
    }
}