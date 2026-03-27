package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.dispatcher.telegramError
import core.Entry
import data.Schema
import data.SeedData
import io.github.cdimascio.dotenv.dotenv

fun main() {
    println("Chill Zone Bot: initializing database schema")
    Schema.ensure()
    println("Chill Zone Bot: seeding default data")
    SeedData.ensure()
    println("Chill Zone Bot: database is ready")

    val env = dotenv()
    val token = env["BOT_TOKEN"] ?: env["TELEGRAM_TOKEN"] ?: error("BOT_TOKEN is not set")
    val router = Entry.buildRouter()

    lateinit var telegramBot: com.github.kotlintelegrambot.Bot

    telegramBot = bot {
        this.token = token

        dispatch {
            message {
                val preview = when {
                    !message.text.isNullOrBlank() -> message.text!!.trim().replace(Regex("\\s+"), " ").take(80)
                    message.photo != null -> "<photo>"
                    else -> "<non-text>"
                }

                println("Chill Zone Bot: incoming message chat=${message.chat.id} user=${message.from?.id} payload=$preview")

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

            telegramError {
                System.err.println(
                    "Chill Zone Bot: Telegram polling error [${this.error.getType()}] ${this.error.getErrorMessage()}"
                )
            }
        }
    }

    println("Chill Zone Bot: polling started")
    telegramBot.startPolling()
}
