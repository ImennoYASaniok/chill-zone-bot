package core.routers.routerCollections

import data.*
import data.models.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import core.keyboards.KeyboardFactory
import core.keyboards.KeyboardCollections
import core.ImageManager
import core.SessionStore
import core.PendingAction
import core.Session
import core.FSMContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object RouterCollectionsFavorites {
    fun showCollectionFavorites(bot: Bot, chat: ChatId, uid: Long, users: UserRepository) {
        val favorites = FavoriteRepository.list(uid)
        if (favorites.isEmpty()) {
            // Сбрасываем контекст при пустых избранных
            val session = SessionStore.get(uid)
            session.action = PendingAction.NONE
            
            val profile = users.profile(uid)
            val collectionsMessage = """🔍 <b>Меню подборок</b>

Здесь вы можете:
• 🔍 <b>Поиск</b> - находить фильмы, сериалы, книги и игры по запросу
• ⭐️ <b>Избранное</b> - просматривать сохраненные элементы

Выберите действие:"""
            bot.sendMessage(chat, "У вас пока нет избранных элементов.")
            ImageManager.sendMessageWithImage(
                bot, chat, collectionsMessage, profile,
                parseMode = ParseMode.HTML,
                replyMarkup = KeyboardFactory.collectionsMenu()
            )
        } else {
            // Устанавливаем контекст для навигации
            val session = SessionStore.get(uid)
            session.action = PendingAction.VIEW_FAVORITES
            session.data["favorites_index"] = "0"
            session.data["favorites_total"] = favorites.size.toString()
            
            // Показываем первый элемент
            showFavoriteItem(bot, chat, uid, favorites, 0)
        }
    }

    fun saveToFavorites(bot: Bot, chat: ChatId, uid: Long, session: Session) {
        val type = session.data["collection_type"]?.let { RecommendationType.valueOf(it) }
        val query = session.data["collection_query"]
        val offset = session.data["collection_offset"]?.toIntOrNull() ?: 0
        
        if (type == null) {
            bot.sendMessage(chat, "Ошибка: не определен тип контента.", replyMarkup = KeyboardFactory.mainMenu())
            return
        }
        
        // Вызываем suspend функцию через coroutine scope
        GlobalScope.launch {
            try {
                val results = RecommendationRepository.searchMultiple(type, query, 5, offset)
                if (results.items.isNotEmpty()) {
                    val item = results.items.first()
                    FavoriteRepository.add(uid, item)
                    bot.sendMessage(chat, "✅ Сохранено в избранное!", replyMarkup = KeyboardCollections.searchResultNavKeyboard(false))
                } else {
                    bot.sendMessage(chat, "Нет элементов для сохранения.", replyMarkup = KeyboardCollections.searchResultNavKeyboard(false))
                }
            } catch (e: Exception) {
                println("Error saving to favorites: ${e.message}")
                bot.sendMessage(chat, "Ошибка при сохранении.", replyMarkup = KeyboardCollections.searchResultNavKeyboard(false))
            }
        }
    }

    fun showFavoriteItem(bot: Bot, chat: ChatId, uid: Long, favorites: List<FavoriteItem>, index: Int) {
        if (index < 0 || index >= favorites.size) return
        
        val favorite = favorites[index]
        val message = buildString {
            append("⭐ Избранное (${index + 1} из ${favorites.size})\n\n")
            append("${favorite.title} (${favorite.year})\n")
            append("Тип: ${getTypeDisplayName(favorite.itemType)}\n")
            if (!favorite.description.isNullOrBlank()) {
                val cleanDescription = favorite.description!!
                    .replace(Regex("\\s+"), " ") // Заменяем множественные пробелы
                    .trim()
                    .take(300) // Ограничиваем до 300 символов для избранного
                
                append("Описание: ${cleanDescription}\n")
            }
            if (favorite.url != null) {
                append("Ссылка: ${favorite.url}\n")
            }
        }
        
        // Inline клавиатура с кнопками удаления и навигации
        val inlineKeyboard = KeyboardFactory.singleFavoriteDeleteKeyboard(favorite, index, favorites.size)
        
        // Reply клавиатура с кнопками навигации для избранного
        val replyKeyboard = KeyboardFactory.favoritesNavKeyboard(index, favorites.size)
        
        bot.sendMessage(chat, message, replyMarkup = replyKeyboard)
        bot.sendMessage(chat, "Управление:", replyMarkup = inlineKeyboard)
    }

    private fun getTypeDisplayName(type: RecommendationType): String {
        return when (type) {
            RecommendationType.FILM -> "Фильм"
            RecommendationType.SERIES -> "Сериал"
            RecommendationType.BOOK -> "Книга"
            RecommendationType.GAME -> "Игра"
        }
    }
}
