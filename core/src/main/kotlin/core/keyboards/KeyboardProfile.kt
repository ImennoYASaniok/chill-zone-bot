package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import data.UserProfile

object KeyboardProfile {
    fun profileMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val profileHidden = userProfile?.hidden ?: false
        val usernameHidden = userProfile?.hideUsername ?: false
        
        return kb(
            listOf(
                listOf("Изменить имя", if (usernameHidden) "Показать username [👁️]" else "Скрыть username [🙈]"),
                listOf("Изменить био"),
                listOf(
                    if (profileHidden) "Показать профиль [👁️]" else "Скрыть профиль [🙈]"
                ),
                listOf("⬅️ Обратно")
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

    private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
        return KeyboardReplyMarkup(
            keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
            resizeKeyboard = true,
            oneTimeKeyboard = false
        )
    }
}