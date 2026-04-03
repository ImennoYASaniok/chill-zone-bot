package core.routers

import data.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import core.keyboards.KeyboardFactory
import core.ImageManager
import core.SessionStore
import core.Session
import core.PendingAction

object RouterFeedBack {
    fun handleFeedbackAction(bot: Bot, chat: ChatId, uid: Long, text: String, feedback: FeedbackRepository, users: UserRepository, session: Session): Boolean {
        when (text) {
            "💬 Обратная связь" -> {
                val profile = users.profile(uid)
                ImageManager.sendMessageWithImage(bot, chat, "Если что-то сломалось, просто напиши сюда отзыв — он сохранится в PostgreSQL.", profile) {
                    bot.sendMessage(chat, "Поддержка:", replyMarkup = KeyboardFactory.feedbackMenu())
                }
                return true
            }
            "Оставить отзыв" -> {
                session.action = PendingAction.FEEDBACK_TEXT
                val profile = users.profile(uid)
                ImageManager.sendMessageWithImage(bot, chat, "Напиши отзыв одним сообщением.", profile) {
                    bot.sendMessage(chat, "Ожидаю отзыв:", replyMarkup = KeyboardFactory.feedbackMenu())
                }
                return true
            }
            "Посмотреть поддержку" -> {
                val profile = users.profile(uid)
                ImageManager.sendMessageWithImage(bot, chat, "Если что-то сломалось, просто напиши сюда отзыв — он сохранится в PostgreSQL.", profile) {
                    bot.sendMessage(chat, "Поддержка:", replyMarkup = KeyboardFactory.feedbackMenu())
                }
                return true
            }
            else -> return false
        }
    }
}