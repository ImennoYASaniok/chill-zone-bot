package core.routers

import data.models.*
import data.repositories.FeedbackRepository
import data.repositories.UserRepository
import data.services.ModerationService
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
            else -> {
                if (session.action == PendingAction.FEEDBACK_TEXT) {
                    val moderationResult = ModerationService.checkText(uid, text)
                    if (!moderationResult.isAllowed) {
                        if (moderationResult.shouldBan) {
                            ModerationService.banForViolation(uid, "Использование ненормативной лексики")
                            ModerationService.clearWarnings(uid)
                            bot.sendMessage(chat, "🚫 Вы были заблокированы за использование ненормативной лексики.")
                        } else {
                            ModerationService.addWarning(uid)
                            bot.sendMessage(chat, "⚠️ <b>Предупреждение!</b>\n\nВаше сообщение содержит недопустимый контент.\n\nПовторное нарушение приведёт к автоматическому бану на 1 сутки.", parseMode = ParseMode.HTML)
                        }
                        session.action = PendingAction.NONE
                        return true
                    }
                    feedback.save(uid, "", scope = "general", text = text)
                    session.action = PendingAction.NONE
                    bot.sendMessage(chat, "✅ Спасибо за отзыв!", replyMarkup = KeyboardFactory.feedbackMenu())
                    return true
                }
                return false
            }
        }
    }
}