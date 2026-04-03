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

object RouterEvents {
    fun showEvents(bot: Bot, chat: ChatId, events: EventRepository) {
        val list = events.upcoming()
        if (list.isEmpty()) {
            bot.sendMessage(chat, "Пока событий нет.", replyMarkup = KeyboardFactory.eventsMenu())
            return
        }
        bot.sendMessage(chat, list.joinToString("\n\n") { "• #${it.id} ${it.title}\n${it.place}\n${it.startsAt}\n${it.kind}" }, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.eventsMenu())
    }

    fun showMineEvents(bot: Bot, chat: ChatId, uid: Long, events: EventRepository) {
        val list = events.mine(uid)
        if (list.isEmpty()) {
            bot.sendMessage(chat, "У тебя пока нет своих событий.", replyMarkup = KeyboardFactory.eventsMenu())
            return
        }
        bot.sendMessage(chat, list.joinToString("\n\n") { "• #${it.id} ${it.title}\n${it.place}\n${it.startsAt}" }, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.eventsMenu())
    }

    fun handleEventAction(bot: Bot, chat: ChatId, uid: Long, text: String, events: EventRepository, users: UserRepository, session: Session): Boolean {
        when (text) {
            "📅 События" -> {
                showEvents(bot, chat, events)
                return true
            }
            "Ближайшие события" -> {
                showEvents(bot, chat, events)
                return true
            }
            "Мои события" -> {
                showMineEvents(bot, chat, uid, events)
                return true
            }
            "Создать событие" -> {
                session.action = PendingAction.CREATE_EVENT_TITLE
                session.data.clear()
                val profile = users.profile(uid)
                ImageManager.sendMessageWithImage(bot, chat, "Напиши название события.", profile) {
                    bot.sendMessage(chat, "Ожидаю название:", replyMarkup = KeyboardFactory.eventsMenu())
                }
                return true
            }
            else -> return false
        }
    }
}