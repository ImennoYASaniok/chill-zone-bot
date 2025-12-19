package core.handlers

import core.States
import core.currState
import java.io.File
import java.time.LocalDateTime

object FeedbackHandlers {
    private val feedbackFile = File("feedback.txt").apply { if (!exists()) createNewFile() }

    // Текст при входе в меню
    fun startFeedbackHandler(kwargs: Map<String, Any>? = null): String {
        return "Напишите ваше сообщение или предложение ниже. Я обязательно его прочитаю! \n\nЧтобы отменить, нажмите кнопку «Назад»."
    }

    // Сохранение текста в файл
    fun saveFeedback(chatId: Long, username: String, text: String): String {
        val timestamp = LocalDateTime.now().toString()
        feedbackFile.appendText("[$timestamp] ID: $chatId | User: @$username | Text: $text\n")

        // После сохранения возвращаем пользователя в главное меню
        currState = States.GeneralMenu
        return "✅ Спасибо за ваш отзыв! Сообщение успешно отправлено."
    }
}
