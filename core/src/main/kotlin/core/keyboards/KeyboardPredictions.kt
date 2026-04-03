package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

object KeyboardPredictions {
    fun predictionsMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Получить предсказание", "Мои предсказания"),
            listOf("Поиск предсказаний"),
            listOf("⬅️ Обратно")
        )
    )

    fun predictionProcessMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Получить ещё", "Добавить в избранные"),
            listOf("⬅️ Обратно")
        )
    )

    fun predictionRarity(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Обычное", "Редкое"),
            listOf("Эпическое", "Легендарное"),
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