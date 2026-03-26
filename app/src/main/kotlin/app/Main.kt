package app

import core.Entry
import data.Schema
import data.SeedData
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.dispatcher.text
import io.github.cdimascio.dotenv.dotenv

fun main() {
    Schema.ensure()
    SeedData.ensure()

    val env = dotenv()
    val token = env["BOT_TOKEN"] ?: env["TELEGRAM_TOKEN"] ?: error("BOT_TOKEN is not set")
    val router = Entry.buildRouter()

    lateinit var telegramBot: com.github.kotlintelegrambot.Bot

    telegramBot = bot {
        this.token = token

        dispatch {
            text {
                runCatching {
                    router.handle(telegramBot, message)
                }.onFailure { e ->
                    e.printStackTrace()
                    telegramBot.sendMessage(
                        chatId = com.github.kotlintelegrambot.entities.ChatId.fromId(message.chat.id),
                        text = "Внутренняя ошибка при обработке сообщения."
                    )
                }
            }

//            telegramError {
//                it.printStackTrace()
//            }
        }
    }

    telegramBot.startPolling()
}