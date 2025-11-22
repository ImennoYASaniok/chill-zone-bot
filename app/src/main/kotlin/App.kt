import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import io.github.cdimascio.dotenv.dotenv
import ChillZoneBot.core.src.main.kotlin.Buttons.*


fun main() {
    val dotenv = dotenv()
    val bot = bot {

        token = dotenv["BOT_TOKEN"]

        dispatch {
            callbackQuery("callback1") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Callback1!")
            }
            callbackQuery("callback2") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Callback2!")
            }
            callbackQuery("callback3") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Callback3!")
            }
            text {

                val chatId = message?.chat?.id
                if (chatId != null) {

                    when (text) {
                        // ReplyKeyboard
                        "Option 1" -> bot.sendMessage(ChatId.fromId(message.chat.id), text = "You chose Option 1!")
                        "Option 2" -> bot.sendMessage(ChatId.fromId(message.chat.id), text = "You chose Option 2!")
                        "Option 3" -> bot.sendMessage(ChatId.fromId(message.chat.id), text = "You chose Option 3!")
                        "Message" -> {
                            val inlinemarkup = InlineKeyboardMarkup.create(
                                listOf(
                                    listOf(
                                        InlineKeyboardButton.CallbackData("Bt1", "callback1")
                                    ),
                                    listOf(
                                        InlineKeyboardButton.CallbackData("Bt2", "callback2"),
                                        InlineKeyboardButton.CallbackData("Bt3", "callback3")),
                                )
                            )
                            bot.sendMessage(ChatId.fromId(message.chat.id), text = "...", replyMarkup = inlinemarkup)
                        }
                        else -> {
                            val keyboard = listOf(
                                listOf(KeyboardButton("Option 1"), KeyboardButton("Option 2")),
                                listOf(KeyboardButton("Option 3"))
                            )
                            val replyKeyboardMarkup = replyMarkup(
                                mutableListOf(
                                    mutableListOf(Button("Option 1"), ChooseButton("name: ", listOf("name1", "name2", "name3"), 2)),
                                    mutableListOf(BoolButton("Option 2"))
                                )
                            )
                            bot.sendMessage(
                                chatId = ChatId.fromId(chatId),
                                text = text,
                                replyMarkup = replyKeyboardMarkup.replyKeyboardMarkup
                            )
                        }
                        // end
                    }
                }
            }
        }
    }

    bot.startPolling()
}
