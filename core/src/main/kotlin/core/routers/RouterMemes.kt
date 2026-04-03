package core.routers

import data.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import core.keyboards.KeyboardFactory
import core.ImageManager
import core.SessionStore
import core.Session
import core.PendingAction
import core.FSMContext

object RouterMemes {
    fun showMeme(bot: Bot, chat: ChatId, uid: Long, memes: MemeRepository) {
        val meme = memes.randomUnseen(uid)
        if (meme == null) {
            bot.sendMessage(chat, "Пока мемов нет или ты уже просмотрел все.", replyMarkup = KeyboardFactory.memesMenu())
            return
        }
        memes.markSeen(uid, meme.id)
        val counts = memes.counts(meme.id)
        SessionStore.get(uid).data["last_meme"] = meme.id.toString()
        bot.sendPhoto(chat, meme.fileId)
        bot.sendMessage(chat, "👍 ${counts.likes} | 👎 ${counts.dislikes}\n${meme.caption}", parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.memesMenu())
    }

    fun rateLastMeme(bot: Bot, chat: ChatId, uid: Long, vote: Int, memes: MemeRepository) {
        val meme = lastMeme(uid, memes) ?: run {
            bot.sendMessage(chat, "Сначала открой мем через «Следующий мем».", replyMarkup = KeyboardFactory.memesMenu())
            return
        }
        val counts = memes.vote(uid, meme.id, vote)
        bot.sendMessage(chat, "👍 ${counts.likes} | 👎 ${counts.dislikes}", replyMarkup = KeyboardFactory.memesMenu())
    }

    fun favoriteLastMeme(bot: Bot, chat: ChatId, uid: Long, memes: MemeRepository) {
        val meme = lastMeme(uid, memes) ?: run {
            bot.sendMessage(chat, "Сначала открой мем.", replyMarkup = KeyboardFactory.memesMenu())
            return
        }
        val added = memes.toggleFavorite(uid, meme.id)
        bot.sendMessage(chat, if (added) "Добавлено в избранное." else "Убрано из избранного.", replyMarkup = KeyboardFactory.memesMenu())
    }

    fun showFavorites(bot: Bot, chat: ChatId, uid: Long, memes: MemeRepository) {
        val favs = memes.favorites(uid)
        if (favs.isEmpty()) {
            bot.sendMessage(chat, "Пока избранное пусто.", replyMarkup = KeyboardFactory.memesMenu())
            return
        }
        favs.forEach {
            bot.sendPhoto(chat, it.fileId)
            bot.sendMessage(chat, it.caption.ifBlank { "Избранный мем #${it.id}" })
        }
        bot.sendMessage(chat, "Готово.", replyMarkup = KeyboardFactory.memesMenu())
    }

    fun handleMemeAction(bot: Bot, chat: ChatId, uid: Long, text: String, memes: MemeRepository, users: UserRepository, session: Session): Boolean {
        when (text) {
            "😂 Мемы" -> {
                showMeme(bot, chat, uid, memes)
                return true
            }
            "Следующий мем" -> {
                showMeme(bot, chat, uid, memes)
                return true
            }
            "👍" -> {
                rateLastMeme(bot, chat, uid, 1, memes)
                return true
            }
            "👎" -> {
                rateLastMeme(bot, chat, uid, -1, memes)
                return true
            }
            "Добавить мем" -> {
                session.action = PendingAction.ADD_MEME
                val profile = users.profile(uid)
                ImageManager.sendMessageWithImage(
                    bot, chat, "Отправь фото мема одним сообщением.", profile,
                    replyMarkup = KeyboardFactory.memesMenu()
                )
                return true
            }
            "💾 Избр. мем" -> {
                favoriteLastMeme(bot, chat, uid, memes)
                return true
            }
            "Мои избр. мемы" -> {
                showFavorites(bot, chat, uid, memes)
                return true
            }
            else -> return false
        }
    }

    private fun lastMeme(uid: Long, memes: MemeRepository): MemeItem? {
        val id = SessionStore.get(uid).data["last_meme"]?.toLongOrNull() ?: return null
        return memes.getById(id)
    }
}