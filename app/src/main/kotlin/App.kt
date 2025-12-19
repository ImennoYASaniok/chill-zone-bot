import ChillZoneBot.core.src.main.kotlin.ReplyClass.BoolButton
import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ChooseButton
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import ChillZoneBot.core.src.main.kotlin.handlers.base.FavoritesStorage
import ChillZoneBot.core.src.main.kotlin.handlers.base.MemeAddState
import ChillZoneBot.core.src.main.kotlin.handlers.base.MemeStorage
import ChillZoneBot.core.src.main.kotlin.handlers.base.ReplyMenuHolder
import ChillZoneBot.core.src.main.kotlin.handlers.base.UserMemeHistory
import ChillZoneBot.core.src.main.kotlin.handlers.base.UserMemeSession
import ChillZoneBot.core.src.main.kotlin.handlers.base.VotesStorage
import ChillZoneBot.core.src.main.kotlin.handlers.memes.getMemeKeyboard
import ChillZoneBot.core.src.main.kotlin.keyboards.base.getKeyboardMenu

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
                val chatIdLong = msg.chat.id
                val chatId = ChatId.fromId(chatIdLong)

                val photo = msg.photo?.lastOrNull()
                if (photo != null) {
                    val fileId = photo.fileId

                    if (MemeAddState.waitingForPhoto) {
                        MemeAddState.waitingForPhoto = false
                        val meme = MemeStorage.addMeme(fileId)
                        bot.sendMessage(chatId, "Мем сохранён под номером ${meme.id}")
                    } else {
                        bot.sendMessage(chatId, "Чтобы добавить мем, нажми «Мемы» → «Добавить мем», а потом отправь фото.")
                    }
                    return@message
                }

                val t = msg.text ?: return@message

                fun sendMeme(memeId: Int, fileId: String?) {
                    if (fileId != null) bot.sendPhoto(chatId, fileId)
                    val counts = VotesStorage.getCounts(memeId)
                    bot.sendMessage(chatId, "👍 ${counts.likes} | 👎 ${counts.dislikes}")
                }

                if (t == "Мемы") {
                    replyMenu.keyboard = getMemeKeyboard()
                    replyMenu.update()

                    val meme = UserMemeHistory.getNextRandomUnseen(chatIdLong)
                    if (meme == null) {
                        println("!!!!!!!!!!!!!")
                        UserMemeSession.setLastShown(chatIdLong, null)
                        if (MemeStorage.size() == 0) {
                            bot.sendMessage(chatId, "Здесь пока нет мемов. Нажми «Добавить мем», чтобы загрузить первый.", replyMarkup = replyMenu.getKeyboardReplyMarkup())
                        } else {
                            bot.sendMessage(chatId, "Ты уже посмотрел все мемы. Добавь новый мем или подожди, пока его добавит кто-то другой.", replyMarkup = replyMenu.getKeyboardReplyMarkup())
                        }
                    } else {
                        UserMemeSession.setLastShown(chatIdLong, meme.id)
                        sendMeme(meme.id, meme.fileId)
                        bot.sendMessage(chatId, "Меню мемов", replyMarkup = replyMenu.getKeyboardReplyMarkup())
                    }
                    return@message
                }

                if (t == "Следующий мем") {
                    val meme = UserMemeHistory.getNextRandomUnseen(chatIdLong)
                    if (meme == null) {
                        UserMemeSession.setLastShown(chatIdLong, null)
                        if (MemeStorage.size() == 0) bot.sendMessage(chatId, "Здесь пока нет мемов. Нажми «Добавить мем», чтобы загрузить первый.")
                        else bot.sendMessage(chatId, "Ты уже посмотрел все мемы. Добавь новый мем или подожди, пока его добавит кто-то другой.")
                        return@message
                    }

                    UserMemeSession.setLastShown(chatIdLong, meme.id)
                    sendMeme(meme.id, meme.fileId)
                    return@message
                }

                if (t == "Добавить мем") {
                    MemeAddState.waitingForPhoto = true
                    bot.sendMessage(chatId, "Отправь картинку с мемом одним сообщением.")
                    return@message
                }

                if (t == "👍") {
                    val lastId = UserMemeSession.getLastShown(chatIdLong)
                    if (lastId == null) {
                        bot.sendMessage(chatId, "Сначала открой мем.")
                        return@message
                    }
                    val counts = VotesStorage.like(chatIdLong, lastId)
                    bot.sendMessage(chatId, "👍 ${counts.likes} | 👎 ${counts.dislikes}")
                    return@message
                }

                if (t == "👎") {
                    val lastId = UserMemeSession.getLastShown(chatIdLong)
                    if (lastId == null) {
                        bot.sendMessage(chatId, "Сначала открой мем.")
                        return@message
                    }
                    val counts = VotesStorage.dislike(chatIdLong, lastId)
                    bot.sendMessage(chatId, "👍 ${counts.likes} | 👎 ${counts.dislikes}")
                    return@message
                }

                if (t == "В избранное") {
                    val lastId = UserMemeSession.getLastShown(chatIdLong)
                    if (lastId == null) {
                        bot.sendMessage(chatId, "Сначала открой мем, потом добавляй в избранное.")
                        return@message
                    }
                    val added = FavoritesStorage.add(chatIdLong, lastId)
                    if (added) bot.sendMessage(chatId, "Добавлено в избранное.") else bot.sendMessage(chatId, "Этот мем уже в избранном.")
                    return@message
                }

                if (t == "Удалить из избранного") {
                    val lastId = UserMemeSession.getLastShown(chatIdLong)
                    if (lastId == null) {
                        bot.sendMessage(chatId, "Сначала открой мем, потом удаляй из избранного.")
                        return@message
                    }
                    val removed = FavoritesStorage.remove(chatIdLong, lastId)
                    if (removed) bot.sendMessage(chatId, "Удалено из избранного.") else bot.sendMessage(chatId, "Этого мема нет в избранном.")
                    return@message
                }

                if (t == "Избранное") {
                    val favs = FavoritesStorage.list(chatIdLong)
                    if (favs.isEmpty()) {
                        bot.sendMessage(chatId, "В избранном пока пусто.")
                        return@message
                    }
                    for (m in favs) sendMeme(m.id, m.fileId)
                    return@message
                }

                if (t == "Назад") {
                    replyMenu.keyboard = getKeyboardMenu()
                    replyMenu.update()
                    bot.sendMessage(chatId, "Главное меню", replyMarkup = replyMenu.getKeyboardReplyMarkup())
                    return@message
                }

                replyMenu.main(t, bot = bot, chatId = chatId)
            }
        }
    }

    bot.startPolling()
}