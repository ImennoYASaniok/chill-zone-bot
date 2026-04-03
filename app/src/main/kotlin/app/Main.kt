package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ChatId
import core.Entry
import data.schemas.Schema
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
    var lastPollingErrorKey = ""
    var lastPollingErrorAt = 0L
    var suppressedPollingErrors = 0

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

                try {
                    router.handle(telegramBot, message)
                } catch (e: java.lang.Exception) {
                    println("Chill Zone Bot: message handling error")
                    telegramBot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Внутренняя ошибка при обработке сообщения."
                    )
                }
            }
            
            callbackQuery {
                val callbackQuery = this.callbackQuery
                println("Chill Zone Bot: incoming callback chat=${callbackQuery.message?.chat?.id} user=${callbackQuery.from.id} data=${callbackQuery.data}")
                
                try {
                    router.handleCallback(telegramBot, callbackQuery)
                } catch (e: java.lang.Exception) {
                    println("Chill Zone Bot: callback handling error: ${e.message}")
                    telegramBot.answerCallbackQuery(callbackQuery.id, "Ошибка при обработке запроса.")
                }
            }

            telegramError {
                val type = this.error.getType().name
                val message = this.error.getErrorMessage()
                val key = "$type|$message"
                val now = System.currentTimeMillis()
                val sameAsLast = key == lastPollingErrorKey
                val tooFrequent = now - lastPollingErrorAt < 15000
                if (sameAsLast && tooFrequent) {
                    suppressedPollingErrors += 1
                    return@telegramError
                }
                if (suppressedPollingErrors > 0) {
                    println("Chill Zone Bot: suppressed polling errors x$suppressedPollingErrors")
                    suppressedPollingErrors = 0
                }
                lastPollingErrorKey = key
                lastPollingErrorAt = now
                println("Chill Zone Bot: Telegram polling error [$type] $message")
                if (type == "RETRIEVE_UPDATES") {
                    println("Chill Zone Bot: проверьте доступ контейнера к api.telegram.org, корректность BOT_TOKEN и отсутствие блокировок сети/VPN/DNS.")
                }
            }
        }
    }

    println("Chill Zone Bot: polling started")
    telegramBot.startPolling()
}
