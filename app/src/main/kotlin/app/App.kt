package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId

import io.github.cdimascio.dotenv.dotenv

import core.States
import core.currState
import core.utils.Logger
import core.messageClasses.ReplyList
import core.messageClasses.getReplyGeneralMenu
import core.messageClasses.getReplySettings

fun main() {
    val dotenv = dotenv()
    Logger.info("bot", "Запущен бот")
    val bot = bot {
        token = dotenv["BOT_TOKEN"]
        Logger.info("env", "Получен токен")

        dispatch {
            text {
                val chatId = ChatId.fromId(message.chat.id)

                val pairGeneralMenu = ReplyList.replyGeneralMenu.processing(text)
                val pairSettings = ReplyList.replySettings.processing(text)

                ReplyList.replyGeneralMenu.callMain(text, bot = bot, chatId = chatId, pair = pairGeneralMenu)
                ReplyList.replySettings.callMain(text, bot = bot, chatId = chatId, pair = pairSettings)
            }
        }
    }

    bot.startPolling()
}