package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

object KeyboardTests {
    fun testsMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Случайный тест", "Создать тест"),
            listOf("Мои тесты"),
            listOf("⬅️ Обратно")
        )
    )

    fun testKinds(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("SINGLE", "MULTI"),
            listOf("NUMBER", "MATCH"),
            listOf("⬅️ Обратно")
        )
    )

    fun testsContinue(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Да", "Нет"),
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