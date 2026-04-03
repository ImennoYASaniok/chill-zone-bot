package core.routers

import data.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import core.keyboards.KeyboardFactory
import core.ImageManager
import core.Session
import core.PendingAction

object RouterPixelArt {
    fun handlePixelAction(bot: Bot, chat: ChatId, uid: Long, text: String, users: UserRepository): Boolean {
        when (text) {
            "🖼 Пиксель-арт" -> {
                val profile = users.profile(uid)
                val pixelMessage = "Пиксель-арт пока доступен как отдельный веб-модуль, но раздел уже предусмотрен в меню."
                ImageManager.sendMessageWithImage(bot, chat, pixelMessage, profile) {
                    bot.sendMessage(chat, "Доступные опции:", replyMarkup = KeyboardFactory.pixelMenu())
                }
                return true
            }
            "Публичный холст" -> {
                val profile = users.profile(uid)
                ImageManager.sendMessageWithImage(bot, chat, "Публичный холст можно подключить как Web App поверх этого меню.", profile) {
                    bot.sendMessage(chat, "Опции холста:", replyMarkup = KeyboardFactory.pixelMenu())
                }
                return true
            }
            else -> return false
        }
    }
}