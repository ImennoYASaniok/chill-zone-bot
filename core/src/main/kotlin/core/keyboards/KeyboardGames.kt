package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

object KeyboardGames {
    fun gamesMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("⚽ Гол", "🏀 В кольцо"),
            listOf("🎡 Колесо фортуны", "✂️ Камень-ножницы-бумага"),
            listOf("📊 Моя статистика", "🏆 Топ игроков"),
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