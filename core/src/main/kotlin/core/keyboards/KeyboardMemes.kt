package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

object KeyboardMemes {
    fun memesMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Следующий мем", "Добавить мем"),
            listOf("👍", "👎"),
            listOf("💾 Избр. мем", "Мои избр. мемы"),
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