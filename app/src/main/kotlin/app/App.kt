package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User

import io.github.cdimascio.dotenv.dotenv

import core.States
import core.currState
import core.messageClasses.ReplyList
import core.utils.Logger
import core.keyboards.getKeyboardMemes
import core.memes.FavoritesStorage
import core.memes.MemeAddState
import core.memes.MemeStorage
import core.memes.UserMemeHistory
import core.memes.UserMemeSession
import core.memes.VotesStorage

fun getUsername(user: User?): String {
    return user?.username ?: "Не указан"
}
fun getUserId(user: User?): Long {
    return user?.id!!
}

private fun openGeneralMenu(text: String, bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, username: String, userId: Long) {
    val argsGeneralMenu = ReplyList.replyGeneralMenu.processing(text)
    val argsSettings = ReplyList.replySettings.processing(text)
    val argsAccount = ReplyList.replyAccount.processing(text, mapOf("username" to username, "userId" to userId))

    ReplyList.replyGeneralMenu.callMain(text, bot = bot, chatId = chatId, pair = argsGeneralMenu)
    ReplyList.replySettings.callMain(text, bot = bot, chatId = chatId, pair = argsSettings)
    ReplyList.replyAccount.callMain(text, bot = bot, chatId = chatId, pair = argsAccount)
}

private fun sendMemeWithCounts(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, memeId: Int, fileId: String?) {
    if (fileId != null) {
        bot.sendPhoto(chatId, fileId)
    }
    val counts = VotesStorage.getCounts(memeId)
    bot.sendMessage(chatId, "👍 ${counts.likes} | 👎 ${counts.dislikes}")
}

private fun showMemeMenu(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, chatIdLong: Long) {
    val meme = UserMemeHistory.getNextRandomUnseen(chatIdLong)

    if (meme == null) {
        UserMemeSession.setLastShown(chatIdLong, null)
        if (MemeStorage.size() == 0) {
            bot.sendMessage(
                chatId,
                "Здесь пока нет мемов. Нажми «Добавить мем», чтобы загрузить первый.",
                replyMarkup = getKeyboardMemes()
            )
        } else {
            bot.sendMessage(
                chatId,
                "Ты уже посмотрел все мемы. Нажми «Следующий мем» после того, как добавят новые, или сбрось просмотр командой /memes_reset.",
                replyMarkup = getKeyboardMemes()
            )
        }
        return
    }

    UserMemeSession.setLastShown(chatIdLong, meme.id)
    sendMemeWithCounts(bot, chatId, meme.id, meme.fileId)
    bot.sendMessage(chatId, "Меню мемов", replyMarkup = getKeyboardMemes())
}

private fun handleMemes(text: String, bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, chatIdLong: Long): Boolean {
    // Returns true if the text was handled here
    when (text) {
        "/memes_reset" -> {
            UserMemeHistory.reset(chatIdLong)
            bot.sendMessage(chatId, "Ок, сбросил историю просмотренных мемов.", replyMarkup = getKeyboardMemes())
            return true
        }
        "Следующий мем" -> {
            val meme = UserMemeHistory.getNextRandomUnseen(chatIdLong)
            if (meme == null) {
                UserMemeSession.setLastShown(chatIdLong, null)
                if (MemeStorage.size() == 0) bot.sendMessage(chatId, "Здесь пока нет мемов. Нажми «Добавить мем», чтобы загрузить первый.")
                else bot.sendMessage(chatId, "Ты уже посмотрел все мемы. Добавь новый мем или подожди, пока его добавит кто-то другой.")
                return true
            }
            UserMemeSession.setLastShown(chatIdLong, meme.id)
            sendMemeWithCounts(bot, chatId, meme.id, meme.fileId)
            return true
        }
        "Добавить мем" -> {
            MemeAddState.waitingForPhoto = true
            bot.sendMessage(chatId, "Отправь картинку с мемом одним сообщением.")
            return true
        }
        "👍" -> {
            val lastId = UserMemeSession.getLastShown(chatIdLong)
            if (lastId == null) {
                bot.sendMessage(chatId, "Сначала открой мем.")
                return true
            }
            val counts = VotesStorage.like(chatIdLong, lastId)
            bot.sendMessage(chatId, "👍 ${counts.likes} | 👎 ${counts.dislikes}")
            return true
        }
        "👎" -> {
            val lastId = UserMemeSession.getLastShown(chatIdLong)
            if (lastId == null) {
                bot.sendMessage(chatId, "Сначала открой мем.")
                return true
            }
            val counts = VotesStorage.dislike(chatIdLong, lastId)
            bot.sendMessage(chatId, "👍 ${counts.likes} | 👎 ${counts.dislikes}")
            return true
        }
        "В избранное" -> {
            val lastId = UserMemeSession.getLastShown(chatIdLong)
            if (lastId == null) {
                bot.sendMessage(chatId, "Сначала открой мем, потом добавляй в избранное.")
                return true
            }
            val added = FavoritesStorage.add(chatIdLong, lastId)
            if (added) bot.sendMessage(chatId, "Добавлено в избранное.") else bot.sendMessage(chatId, "Этот мем уже в избранном.")
            return true
        }
        "Удалить из избранного" -> {
            val lastId = UserMemeSession.getLastShown(chatIdLong)
            if (lastId == null) {
                bot.sendMessage(chatId, "Сначала открой мем, потом удаляй из избранного.")
                return true
            }
            val removed = FavoritesStorage.remove(chatIdLong, lastId)
            if (removed) bot.sendMessage(chatId, "Удалено из избранного.") else bot.sendMessage(chatId, "Этого мема нет в избранном.")
            return true
        }
        "Избранное" -> {
            val favs = FavoritesStorage.list(chatIdLong)
            if (favs.isEmpty()) {
                bot.sendMessage(chatId, "В избранном пока пусто.")
                return true
            }
            for (m in favs) sendMemeWithCounts(bot, chatId, m.id, m.fileId)
            return true
        }
        "⬅️ Обратно" -> {
            currState = States.GeneralMenu
            return true
        }
    }

    return false
}

fun main() {
    val dotenv = dotenv()
    Logger.info("bot", "Запущен бот")

    val bot = bot {
        token = dotenv["BOT_TOKEN"]
        Logger.info("env", "Получен токен")

        dispatch {
            message {
                val chatIdLong = message.chat.id
                val chatId = ChatId.fromId(chatIdLong)
                val username = getUsername(message.from)
                val userId = getUserId(message.from)

                // 1) Photo handling (used for meme upload)
                val photo = message.photo?.lastOrNull()
                if (photo != null) {
                    val fileId = photo.fileId
                    if (currState == States.MemeMenu && MemeAddState.waitingForPhoto) {
                        MemeAddState.waitingForPhoto = false
                        val meme = MemeStorage.addMeme(fileId)
                        bot.sendMessage(chatId, "Мем сохранён под номером ${meme.id}", replyMarkup = getKeyboardMemes())
                    } else {
                        bot.sendMessage(chatId, "Чтобы добавить мем, нажми «😂 Мемы» → «Добавить мем», а потом отправь фото.")
                    }
                    return@message
                }

                val t = message.text ?: return@message
                // 2) Enter memes from General Menu
                if (t == "😂 Мемы") {
                    currState = States.MemeMenu
                    showMemeMenu(bot, chatId, chatIdLong)
                    return@message
                }

                // 3) Meme menu flow
                if (currState == States.MemeMenu) {
                    val handled = handleMemes(t, bot, chatId, chatIdLong)
                    if (handled) {
                        if (t == "⬅️ Обратно") {
                            openGeneralMenu("/start", bot, chatId, username, userId)
                        }
                        return@message
                    }
                    // If unknown text in meme menu, just show menu again
                    bot.sendMessage(chatId, "Меню мемов", replyMarkup = getKeyboardMemes())
                    return@message
                }

                // 4) Default flow (menus / settings / account)
                openGeneralMenu(t, bot, chatId, username, userId)
            }
        }
    }

    bot.startPolling()
}