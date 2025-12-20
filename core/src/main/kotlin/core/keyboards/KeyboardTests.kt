package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

fun getKeyboardTestsMain(): KeyboardReplyMarkup {
    val keyboard: List<List<KeyboardButton>> = listOf(
        listOf(KeyboardButton("Начать тест")),
        listOf(KeyboardButton("⬅️ Обратно"))
    )

    return KeyboardReplyMarkup(keyboard = keyboard, resizeKeyboard = true, oneTimeKeyboard = false)
}

fun getKeyboardTestsOptions(options: List<String>): KeyboardReplyMarkup {
    val rows = mutableListOf<List<KeyboardButton>>()
    options.forEachIndexed { idx, opt ->
        rows.add(listOf(KeyboardButton("${idx + 1}) $opt")))
    }
    rows.add(listOf(KeyboardButton("⬅️ Обратно")))

    return KeyboardReplyMarkup(keyboard = rows, resizeKeyboard = true, oneTimeKeyboard = false)
}
