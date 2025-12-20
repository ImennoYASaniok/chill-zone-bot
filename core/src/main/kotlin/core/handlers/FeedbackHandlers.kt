package core.handlers

import core.States
import core.currState
import java.io.File
import java.time.LocalDateTime

object FeedbackHandlers {
    private val feedbackFile = File("feedback.txt").apply { if (!exists()) createNewFile() }

    fun startFeedbackHandler(kwargs: Map<String, Any>? = null): String {
        return "Напиши сообщение/предложение одним текстом — я сохраню его в файл.\n\nЧтобы отменить, нажми «⬅️ Обратно»."
    }

    fun saveFeedback(chatId: Long, username: String, text: String): String {
        val timestamp = LocalDateTime.now().toString()
        feedbackFile.appendText("[$timestamp] ID: $chatId | User: @$username | Text: $text\n")
        currState = States.GeneralMenu
        return "✅ Спасибо! Отзыв сохранён."
    }
}
