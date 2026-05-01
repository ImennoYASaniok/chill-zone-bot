package core.routers

import data.models.*
import data.services.ModerationService
import data.repositories.EventRepository
import data.repositories.UserRepository
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
            else -> {
                when (session.action) {
                    PendingAction.CREATE_EVENT_TITLE, PendingAction.CREATE_EVENT_DESC, 
                    PendingAction.CREATE_EVENT_PLACE, PendingAction.CREATE_EVENT_TIME,
                    PendingAction.CREATE_EVENT_MAX, PendingAction.CREATE_EVENT_KIND -> {
                        val moderationResult = ModerationService.checkText(uid, text)
                        if (!moderationResult.isAllowed) {
                            if (moderationResult.shouldBan) {
                                ModerationService.banForViolation(uid, "Использование ненормативной лексики")
                                ModerationService.clearWarnings(uid)
                                bot.sendMessage(chat, "🚫 Вы были заблокированы за использование ненормативной лексики.")
                                session.action = PendingAction.NONE
                                session.data.clear()
                            } else {
                                ModerationService.addWarning(uid)
                                bot.sendMessage(chat, "⚠️ <b>Предупреждение!</b>\n\nВаше сообщение содержит недопустимый контент.\n\nПовторное нарушение приведёт к автоматическому бану на 1 сутки.", parseMode = ParseMode.HTML)
                            }
                            return true
                        }
                    }
                    else -> {}
                }

                when (session.action) {
                    PendingAction.CREATE_EVENT_TITLE -> {
                        session.data["event_title"] = text
                        session.action = PendingAction.CREATE_EVENT_DESC
                        bot.sendMessage(chat, "Теперь напиши описание:")
                        return true
                    }
                    PendingAction.CREATE_EVENT_DESC -> {
                        session.data["event_desc"] = text
                        session.action = PendingAction.CREATE_EVENT_PLACE
                        bot.sendMessage(chat, "Где будет событие?")
                        return true
                    }
                    PendingAction.CREATE_EVENT_PLACE -> {
                        session.data["event_place"] = text
                        session.action = PendingAction.CREATE_EVENT_TIME
                        bot.sendMessage(chat, "Когда начнётся? (YYYY-MM-DD HH:MM)")
                        return true
                    }
                    PendingAction.CREATE_EVENT_TIME -> {
                        session.data["event_time"] = text
                        session.action = PendingAction.CREATE_EVENT_MAX
                        bot.sendMessage(chat, "Сколько максимум участников? (0 = без лимита)")
                        return true
                    }
                    PendingAction.CREATE_EVENT_MAX -> {
                        val max = text.toIntOrNull() ?: 0
                        session.data["event_max"] = max.toString()
                        session.action = PendingAction.CREATE_EVENT_KIND
                        bot.sendMessage(chat, "Какой тип события? (Концерт, Митап, Вечеринка, Другое)")
                        return true
                    }
                    PendingAction.CREATE_EVENT_KIND -> {
                        val title = session.data["event_title"] ?: ""
                        val desc = session.data["event_desc"] ?: ""
                        val place = session.data["event_place"] ?: ""
                        val time = session.data["event_time"] ?: ""
                        val max = session.data["event_max"]?.toIntOrNull() ?: 0
                        val kind = text

                        events.add(uid, title, desc, place, time, max, kind)
                        session.action = PendingAction.NONE
                        session.data.clear()
                        bot.sendMessage(chat, "✅ Событие создано!\n\n📌 $title\n📍 $place\n🕒 $time\n👥 до $max участников\n🏷️ $kind", replyMarkup = KeyboardFactory.eventsMenu())
                        return true
                    }
                    else -> return false
                }
            }
        }
    }
}