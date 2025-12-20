package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

fun getKeyboardMiniGamesMain(): KeyboardReplyMarkup {
    val keyboard: List<List<KeyboardButton>> = listOf(
        listOf(KeyboardButton("🎡 Прокрутить колесо")),
        listOf(KeyboardButton("📊 Моя статистика"), KeyboardButton("🏆 Топ 10 игроков")),
        listOf(KeyboardButton("⬅️ Обратно"))
    )

    return KeyboardReplyMarkup(
        keyboard = keyboard,
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )
}

fun getKeyboardMiniGamesChoose(): KeyboardReplyMarkup {
    val keyboard: List<List<KeyboardButton>> = listOf(
        listOf(KeyboardButton("⚽ Гол"), KeyboardButton("🏀 В кольцо")),
        listOf(KeyboardButton("🎰 Колесо фортуны")),
        listOf(KeyboardButton("⬅️ Назад"))
    )

    return KeyboardReplyMarkup(
        keyboard = keyboard,
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )
}
