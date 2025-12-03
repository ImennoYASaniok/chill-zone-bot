package ChillZoneBot.app.src.main.kotlin.App

import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineClass
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import keyboards.base.getKeyboardMenu
import keyboards.base.getInlineKeyboardMenu


import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId

import io.github.cdimascio.dotenv.dotenv

fun main() {
    val dotenv = dotenv()
    val bot = bot {
        token = dotenv["BOT_TOKEN"]

        dispatch {
            val replyMenu = ReplyClass(
                keyboard = getKeyboardMenu(),
                startCommand = "/start",
                textMessage = "replyMenu"
            )

            val inlineMenu = InlineClass(
                keyboard = getInlineKeyboardMenu(),
                startCommand = "/start1",
                textMessage = "inlineMenu"
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
//                "Message" -> {
//                val inlinemarkup = InlineKeyboardMarkup.create(
//                    listOf(
//                        listOf(
//                            InlineKeyboardButton.CallbackData("Bt1", "callback1")
//                        ),
//                        listOf(
//                            InlineKeyboardButton.CallbackData("Bt2", "callback2"),
//                            InlineKeyboardButton.CallbackData("Bt3", "callback3")),
//                    )
//                )
//                bot.sendMessage(chatId, text = "...", replyMarkup = inlinemarkup)
//            }
                val chatId = ChatId.fromId(message.chat.id)
                if (text=="/start") {
                    replyMenu.main(text, bot = bot, chatId = chatId)
                } else if (text == "/start1") {
                    inlineMenu.main(text, bot= bot, chatId = chatId)
                }
            }
        }
    }

    bot.startPolling()
}