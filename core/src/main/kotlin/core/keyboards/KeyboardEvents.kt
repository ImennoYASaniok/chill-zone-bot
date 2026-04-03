package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

object KeyboardEvents {
    fun eventsMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Ближайшие события", "Создать событие"),
            listOf("Мои события"),
            listOf("⬅️ Обратно")
        )
    )

    private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
        return KeyboardReplyMarkup(
            keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
            resizeKeyboard = true,
            oneTimeKeyboard = false
        )
    }
}