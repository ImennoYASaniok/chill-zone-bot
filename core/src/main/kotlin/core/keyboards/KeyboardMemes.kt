package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

object KeyboardMemes {
    fun memesMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Следующий мем", "Мои избр. мемы"),
            listOf("Добавить мем"),
            listOf("⬅️ Обратно")
        )
    )

    fun memesViewerMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Следующий мем", "💾 Избр. мем"),
            listOf("Мои избр. мемы", "Добавить мем"),
            listOf("⬅️ Обратно")
        )
    )

    fun memesAddMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("⬅️ Обратно")
        )
    )

    fun memesFavoritesMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Следующий мем"),
            listOf("⬅️ Обратно")
        )
    )

    fun memeActionsInline(memeId: Long): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.create(
            listOf(
                listOf(
                    InlineKeyboardButton.CallbackData(text = "👍", callbackData = "meme_vote_${memeId}_1"),
                    InlineKeyboardButton.CallbackData(text = "👎", callbackData = "meme_vote_${memeId}_-1")
                ),
                listOf(
                    InlineKeyboardButton.CallbackData(text = "💾 Избр. мем", callbackData = "meme_fav_$memeId"),
                    InlineKeyboardButton.CallbackData(text = "Следующий мем", callbackData = "meme_next")
                )
            )
        )
    }

    private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
        return KeyboardReplyMarkup(
            keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
            resizeKeyboard = true,
            oneTimeKeyboard = false
        )
    }
}
