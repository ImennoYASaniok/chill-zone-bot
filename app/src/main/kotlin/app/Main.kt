package app

import core.Entry
import data.Schema
import data.SeedData
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
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
            message {
                router.handle(telegramBot, message)
            }
        }
    }

    telegramBot.startPolling()
}
