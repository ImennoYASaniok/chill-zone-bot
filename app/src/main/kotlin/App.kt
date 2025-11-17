import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId

import io.github.cdimascio.dotenv.dotenv

fun main() {
    val dotenv = dotenv()

    val bot = bot {

        token = dotenv["BOT_TOKEN"]

        dispatch {
            text {
                val chatId = message?.chat?.id
                val incomingText = text

                if (chatId != null && incomingText != null) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = incomingText
                    )
                }
            }
        }
    }

    bot.startPolling()
}
