package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId

import io.github.cdimascio.dotenv.dotenv

import core.messageClasses.getReplyGeneralMenu
import core.States
import core.utils.Logger

fun main() {
    val dotenv = dotenv()
    Logger.info("bot", "Запущен бот")
    val bot = bot {
        token = dotenv["BOT_TOKEN"]
        Logger.info("env", "Получен токен")


        dispatch {
            val replyGeneralMenu = getReplyGeneralMenu()

            text {
                val chatId = ChatId.fromId(message.chat.id)

                replyGeneralMenu.main(text, bot = bot, chatId = chatId, state = States.GeneralMenu)
            }
        }
    }

    bot.startPolling()
}