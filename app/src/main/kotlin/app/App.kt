package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User

import io.github.cdimascio.dotenv.dotenv

import core.States
import core.currState
import core.utils.Logger
import core.messageClasses.ReplyList
import core.messageClasses.getReplyGeneralMenu
import core.messageClasses.getReplySettings

fun getUsername(user: User?): String {
    return user?.username ?: "Не указан"
}
fun getUserId(user: User?): Long {
    return user?.id!!
}

fun main() {
    val dotenv = dotenv()
    Logger.info("bot", "Запущен бот")
    val bot = bot {
        token = dotenv["BOT_TOKEN"]
        Logger.info("env", "Получен токен")

        dispatch {
            text {
                val chatId = ChatId.fromId(message.chat.id)

                val argsGeneralMenu = ReplyList.replyGeneralMenu.processing(text)
                val argsSettings = ReplyList.replySettings.processing(text)
                val argsAccount = ReplyList.replyAccount.processing(text, mapOf("username" to getUsername(message.from), "userId" to getUserId(message.from)))

                ReplyList.replyGeneralMenu.callMain(text, bot = bot, chatId = chatId, pair = argsGeneralMenu)
                ReplyList.replySettings.callMain(text, bot = bot, chatId = chatId, pair = argsSettings)
                ReplyList.replyAccount.callMain(text, bot = bot, chatId = chatId, pair = argsAccount)
            }
        }
    }

    bot.startPolling()
}