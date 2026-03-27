package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import io.github.cdimascio.dotenv.dotenv

fun main() {
    val env = dotenv()
    val tok = env["BOT_TOKEN"] ?: env["TELEGRAM_TOKEN"] ?: error("BOT_TOKEN is not set")

    val tgBot = bot {
        token = tok

        dispatch {
            command("start") {
                val cId = ChatId.fromId(message.chat.id)
                bot.sendMessage(
                    chatId = cId,
                    text = "Привет! Бот успешно запущен."
                )
            }
        }
    }

    tgBot.startPolling()
}