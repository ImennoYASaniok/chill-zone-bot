package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

fun getKeyboardPredictionsMain(): KeyboardReplyMarkup {
    val keyboard: List<List<KeyboardButton>> = listOf(
        listOf(KeyboardButton("Получить предсказание")),
        listOf(KeyboardButton("Мои предсказания")),
        listOf(KeyboardButton("Добавить предсказание"), KeyboardButton("Поиск предсказаний")),
        listOf(KeyboardButton("⬅️ Обратно"))
    )

    return KeyboardReplyMarkup(
        keyboard = keyboard,
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )
}

fun getKeyboardPredictionsRarity(): KeyboardReplyMarkup {
    val keyboard: List<List<KeyboardButton>> = listOf(
        listOf(KeyboardButton("Обычное"), KeyboardButton("Редкое")),
        listOf(KeyboardButton("Эпическое"), KeyboardButton("Легендарное")),
        listOf(KeyboardButton("⬅️ Обратно"))
    )

    return KeyboardReplyMarkup(
        keyboard = keyboard,
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )
}
