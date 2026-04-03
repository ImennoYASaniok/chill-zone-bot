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
import kotlin.random.Random

object RouterGames {
    fun playSimpleGame(bot: Bot, chat: ChatId, uid: Long, mode: String, games: GameRepository, users: UserRepository) {
        val points = when (mode) {
            "football" -> listOf(0, 5, 10, 15).random()
            "basketball" -> listOf(5, 10, 15, 20).random()
            else -> Random.nextInt(1, 10)
        }
        if (points > 0) {
            games.win(uid, points)
            users.addRating(uid, maxOf(1, points / 5))
            bot.sendMessage(chat, "Игра завершена. Ты получил $points очков!", replyMarkup = KeyboardFactory.gamesMenu())
        } else {
            games.lose(uid)
            bot.sendMessage(chat, "Не повезло, в этот раз без очков.", replyMarkup = KeyboardFactory.gamesMenu())
        }
    }

    fun playWheel(bot: Bot, chat: ChatId, uid: Long, games: GameRepository, users: UserRepository) {
        val prize = listOf(0, 5, 10, 15, 20, 30).random()
        if (prize > 0) {
            games.win(uid, prize)
            users.addRating(uid, maxOf(1, prize / 10))
            bot.sendMessage(chat, "Колесо фортуны: +$prize рейтинга!", replyMarkup = KeyboardFactory.gamesMenu())
        } else {
            games.lose(uid)
            bot.sendMessage(chat, "Колесо фортуны: пусто.", replyMarkup = KeyboardFactory.gamesMenu())
        }
    }

    fun showGameStats(bot: Bot, chat: ChatId, uid: Long, games: GameRepository) {
        val s = games.stats(uid)
        bot.sendMessage(
            chat,
            "Статистика:\nРейтинг: ${s.rating}\nПобеды: ${s.wins}\nПоражения: ${s.losses}\nStreak: ${s.streak}\nЛучший streak: ${s.bestStreak}",
            replyMarkup = KeyboardFactory.gamesMenu()
        )
    }

    fun showTopGames(bot: Bot, chat: ChatId, games: GameRepository) {
        val top = games.top()
        if (top.isEmpty()) {
            bot.sendMessage(chat, "Пока нет игроков в таблице.", replyMarkup = KeyboardFactory.gamesMenu())
            return
        }
        bot.sendMessage(
            chat,
            top.withIndex().joinToString("\n") { (idx, s) -> "${idx + 1}. #${s.userId} — ${s.rating}" },
            replyMarkup = KeyboardFactory.gamesMenu()
        )
    }

    fun handleGameAction(bot: Bot, chat: ChatId, uid: Long, text: String, games: GameRepository, users: UserRepository, session: Session): Boolean {
        when (text) {
            "🎮 Мини-игры" -> {
                showGameStats(bot, chat, uid, games)
                return true
            }
            "⚽ Гол" -> {
                playSimpleGame(bot, chat, uid, "football", games, users)
                return true
            }
            "🏀 В кольцо" -> {
                playSimpleGame(bot, chat, uid, "basketball", games, users)
                return true
            }
            "🎡 Колесо фортуны" -> {
                playWheel(bot, chat, uid, games, users)
                return true
            }
            "✂️ Камень-ножницы-бумага" -> {
                session.action = PendingAction.RPS_CHOICE
                val profile = users.profile(uid)
                ImageManager.sendMessageWithImage(bot, chat, "Выбери ход: камень, ножницы или бумага.", profile) {
                    bot.sendMessage(chat, "Ваш ход:", replyMarkup = KeyboardFactory.gamesMenu())
                }
                return true
            }
            "📊 Моя статистика" -> {
                showGameStats(bot, chat, uid, games)
                return true
            }
            "🏆 Топ игроков" -> {
                showTopGames(bot, chat, games)
                return true
            }
            else -> return false
        }
    }

    fun handleRpsChoice(bot: Bot, chat: ChatId, uid: Long, text: String, games: GameRepository, users: UserRepository, session: Session): Boolean {
        val userChoice = text.lowercase()
        val botChoice = listOf("камень", "ножницы", "бумага").random()
        val result = when {
            userChoice == botChoice -> "Ничья"
            userChoice == "камень" && botChoice == "ножницы" -> "Победа"
            userChoice == "ножницы" && botChoice == "бумага" -> "Победа"
            userChoice == "бумага" && botChoice == "камень" -> "Победа"
            else -> "Поражение"
        }
        if (result == "Победа") {
            games.win(uid, 5)
            users.addRating(uid, 1)
        } else if (result == "Поражение") {
            games.lose(uid)
        }
        session.action = PendingAction.NONE
        bot.sendMessage(chat, "Я выбрал: $botChoice. Результат: $result.", replyMarkup = KeyboardFactory.gamesMenu())
        return true
    }
}