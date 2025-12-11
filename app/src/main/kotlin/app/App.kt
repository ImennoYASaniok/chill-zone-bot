package app

import core.ReplyClass
import core.keyboards.getKeyboardBaseMenu
import core.handlers.handlerMenu


import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import core.States

import io.github.cdimascio.dotenv.dotenv

fun main() {
    val dotenv = dotenv()
    val bot = bot {
        token = dotenv["BOT_TOKEN"]

        dispatch {
            val replyBaseMenu = ReplyClass(
                keyboard = getKeyboardBaseMenu(),
                startCommand = "/start",
                startFunc = ::handlerMenu
            )

            text {
                val chatId = ChatId.fromId(message.chat.id)

                replyBaseMenu.main(text, bot = bot, chatId = chatId, state = States.BaseMenu)
            }
        }
    }

    bot.startPolling()
}