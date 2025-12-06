import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import ChillZoneBot.core.src.main.kotlin.handlers.base.MemeAddState
import ChillZoneBot.core.src.main.kotlin.handlers.base.MemeState
import ChillZoneBot.core.src.main.kotlin.handlers.base.MemeStorage
import ChillZoneBot.core.src.main.kotlin.handlers.base.ReplyMenuHolder
import ChillZoneBot.core.src.main.kotlin.keyboards.base.getKeyboardMenu
import ChillZoneBot.core.src.main.kotlin.handlers.memes.getMemeKeyboard

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
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

            ReplyMenuHolder.replyMenu = replyMenu

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

            message {
                val msg = message
                val chatId = ChatId.fromId(msg.chat.id)

                val photo = msg.photo?.lastOrNull()
                if (photo != null) {
                    val fileId = photo.fileId

                    if (MemeAddState.waitingForPhoto) {
                        MemeAddState.waitingForPhoto = false
                        val meme = MemeStorage.addMeme(fileId)
                        bot.sendMessage(chatId, "Мем сохранён под номером ${meme.id}")
                    } else {
                        bot.sendMessage(
                            chatId,
                            "Чтобы добавить мем, нажми «Мемы» → «Добавить мем», а потом отправь фото."
                        )
                    }
                    return@message
                }

                val t = msg.text ?: return@message

                if (t == "Мемы") {
                    replyMenu.keyboard = getMemeKeyboard()
                    replyMenu.update()

                    if (MemeStorage.size() == 0) {
                        bot.sendMessage(
                            chatId,
                            "Здесь пока нет мемов. Нажми «Добавить мем», чтобы загрузить первый.",
                            replyMarkup = replyMenu.getKeyboardReplyMarkup()
                        )
                    } else {
                        MemeState.reset()
                        val meme = MemeState.getCurrentMeme()
                        val fileId = meme?.fileId
                        if (fileId != null) {
                            bot.sendPhoto(chatId, fileId)
                        }
                        bot.sendMessage(
                            chatId,
                            "Меню мемов",
                            replyMarkup = replyMenu.getKeyboardReplyMarkup()
                        )
                    }
                    return@message
                }

                if (t == "Следующий мем") {
                    if (MemeStorage.size() == 0) {
                        bot.sendMessage(chatId, "Здесь пока нет мемов. Нажми «Добавить мем», чтобы загрузить первый.")
                        return@message
                    }

                    val meme = MemeState.nextMeme()
                    val fileId = meme?.fileId
                    if (fileId != null) {
                        bot.sendPhoto(chatId, fileId)
                    } else {
                        bot.sendMessage(chatId, "Не удалось показать мем.")
                    }
                    return@message
                }

                if (t == "Добавить мем") {
                    MemeAddState.waitingForPhoto = true
                    bot.sendMessage(chatId, "Отправь картинку с мемом одним сообщением.")
                    return@message
                }

                if (t == "Назад") {
                    replyMenu.keyboard = getKeyboardMenu()
                    replyMenu.update()
                    bot.sendMessage(
                        chatId,
                        "Главное меню",
                        replyMarkup = replyMenu.getKeyboardReplyMarkup()
                    )
                    return@message
                }

                replyMenu.main(t, bot = bot, chatId = chatId)
            }
        }
    }

    bot.startPolling()
}