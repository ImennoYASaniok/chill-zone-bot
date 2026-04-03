package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

object KeyboardFeedBack {
    fun feedbackMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Оставить отзыв", "Посмотреть поддержку"),
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