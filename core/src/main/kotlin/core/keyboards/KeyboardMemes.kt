package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

fun getKeyboardMemes(): KeyboardReplyMarkup {
    val keyboard: List<List<KeyboardButton>> = listOf(
        listOf(KeyboardButton("Следующий мем")),
        listOf(KeyboardButton("Добавить мем")),
        listOf(KeyboardButton("👍"), KeyboardButton("👎")),
        listOf(KeyboardButton("В избранное"), KeyboardButton("Удалить из избранного")),
        listOf(KeyboardButton("Избранное")),
        listOf(KeyboardButton("⬅️ Обратно"))
    )

    return KeyboardReplyMarkup(
        keyboard = keyboard,
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )
}
