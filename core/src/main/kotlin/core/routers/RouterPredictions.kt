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

object RouterPredictions {
    fun givePrediction(bot: Bot, chat: ChatId, uid: Long, predictions: PredictionRepository, users: UserRepository) {
        val p = predictions.randomWeighted()
        if (p == null) {
            bot.sendMessage(chat, "Пока нет предсказаний.", replyMarkup = KeyboardFactory.predictionsMenu())
            return
        }
        predictions.collect(uid, p.id)
        users.addRating(uid, 1)
        bot.sendMessage(chat, " ${p.text}\n\nРедкость: ${p.rarity}", parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.predictionProcessMenu())
    }

    fun showCollectedPredictions(bot: Bot, chat: ChatId, uid: Long, predictions: PredictionRepository) {
        val list = predictions.listCollected(uid)
        if (list.isEmpty()) {
            bot.sendMessage(chat, "Коллекция предсказаний пуста.", replyMarkup = KeyboardFactory.predictionsMenu())
            return
        }
        bot.sendMessage(chat, list.joinToString("\n\n") { "• ${it.rarity}: ${it.text}" }, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.predictionsMenu())
    }

    fun showPredictionMenu(bot: Bot, chat: ChatId, uid: Long, users: UserRepository) {
        val profile = users.profile(uid)
        val menuMessage = """🔮 <b>Меню предсказаний</b>

Здесь вы можете:
• <b>Получить предсказание</b> - получить случайное предсказание с редкостью
• <b>Мои предсказания</b> - просмотреть собранную коллекцию предсказаний
• <b>Поиск предсказаний</b> - найти предсказания по тексту или редкости

Выберите действие:"""
        ImageManager.sendMessageWithImage(bot, chat, menuMessage, profile) {
            bot.sendMessage(chat, "Доступные опции:", replyMarkup = KeyboardFactory.predictionsMenu())
        }
    }

    fun handlePredictionAction(bot: Bot, chat: ChatId, uid: Long, text: String, predictions: PredictionRepository, users: UserRepository, session: Session): Boolean {
        when (text) {
            "🔮 Предсказания" -> {
                showPredictionMenu(bot, chat, uid, users)
                return true
            }
            "Получить предсказание" -> {
                givePrediction(bot, chat, uid, predictions, users)
                return true
            }
            "Мои предсказания" -> {
                showCollectedPredictions(bot, chat, uid, predictions)
                return true
            }
            "Поиск предсказаний" -> {
                session.action = PendingAction.SEARCH_PREDICTIONS
                val profile = users.profile(uid)
                ImageManager.sendMessageWithImage(bot, chat, "Напиши часть текста или редкость.", profile) {
                    bot.sendMessage(chat, "Ожидаю запрос:", replyMarkup = KeyboardFactory.predictionsMenu())
                }
                return true
            }
            "Получить ещё" -> {
                givePrediction(bot, chat, uid, predictions, users)
                return true
            }
            "Добавить в избранные" -> {
                // TODO: Добавить логику добавления в избранные
                bot.sendMessage(chat, "Функция избранных предсказаний в разработке.", replyMarkup = KeyboardFactory.predictionProcessMenu())
                return true
            }
            else -> return false
        }
    }
}