import ChillZoneBot.core.src.main.kotlin.Buttons.BoolButton
import ChillZoneBot.core.src.main.kotlin.Buttons.Button
import ChillZoneBot.core.src.main.kotlin.Buttons.ChooseButton
import ChillZoneBot.core.src.main.kotlin.Buttons.replyMarkup
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import io.github.cdimascio.dotenv.dotenv


fun main() {
    val dotenv = dotenv()
    val bot = bot {

        token = dotenv["BOT_TOKEN"]

        dispatch {
            val b1 = Button("Option 1")
            val b2 = ChooseButton("name: ", listOf("name1", "name2", "name3"), 0)
            val b3 = BoolButton("Option 2")
            val replyKeyboardMarkup = replyMarkup(
                mutableListOf(
                    mutableListOf(b1, b2),
                    mutableListOf(b3)
                )
            )
            callbackQuery("callback1") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Callback1!")
            }
            callbackQuery("callback2") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Callback2!")
                //update.message.replyMarkup
            }
            callbackQuery("callback3") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Callback3!")
            }
            text {

                val chatId = message.chat.id
                when (text) {
                    // ReplyKeyboard
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

//                        bot.sendMessage(
//                            chatId = ChatId.fromId(chatId),
//                            text = text,
//                            replyMarkup = replyKeyboardMarkup.replyKeyboardMarkup
//                        )
                        if (text=="Option 1") bot.sendMessage(ChatId.fromId(message.chat.id), text = "You chose Option 1!")
                        if ((text=="false") or (text == "true")) {

                            replyKeyboardMarkup.old_keyboard[1][0].changeName()
                            replyKeyboardMarkup.update()
                            bot.sendMessage(ChatId.fromId(message.chat.id), text = "You chose Option 2!", replyMarkup = replyKeyboardMarkup.replyKeyboardMarkup)
                        }
                        if (text=="Option 3") {
                            bot.sendMessage(ChatId.fromId(message.chat.id), text = "You chose Option 3!")
                        }
                    }
                    // end
                }
            }
        }
    }

    bot.startPolling()
}
