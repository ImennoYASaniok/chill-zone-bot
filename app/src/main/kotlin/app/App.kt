package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import io.github.cdimascio.dotenv.dotenv
import core.currentChatId
import core.utils.Logger
import core.messageClasses.ReplyList

fun getUsername(user: User?): String {
    return user?.username ?: "Не указан"
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
                val chatIdLong = message.chat.id
                currentChatId = chatIdLong

                val username = getUsername(message.from)

                val baseKwargs = mapOf(
                    "chatId" to chatIdLong,
                    "text" to text,
                    "username" to username
                )

                val argsGeneralMenu = ReplyList.replyGeneralMenu.processing(text, baseKwargs)
                val argsSettings = ReplyList.replySettings.processing(text, baseKwargs)
                val argsAccount = ReplyList.replyAccount.processing(text, baseKwargs)
                val argsMiniGames = ReplyList.replyMiniGames.processing(text, baseKwargs)
                val argsTicTacToe = ReplyList.replyTicTacToe.processing(text, baseKwargs)

                ReplyList.replyGeneralMenu.callMain(text, bot = bot, chatId = chatId, pair = argsGeneralMenu)
                ReplyList.replySettings.callMain(text, bot = bot, chatId = chatId, pair = argsSettings)
                ReplyList.replyAccount.callMain(text, bot = bot, chatId = chatId, pair = argsAccount)
                ReplyList.replyMiniGames.callMain(text, bot = bot, chatId = chatId, pair = argsMiniGames)
                ReplyList.replyTicTacToe.callMain(text, bot = bot, chatId = chatId, pair = argsTicTacToe)
            }
        }
    }

    bot.startPolling()
}