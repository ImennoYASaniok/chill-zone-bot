package core.routers

import data.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import core.keyboards.KeyboardFactory
import core.keyboards.KeyboardCollections
import core.ImageManager
import core.SessionStore
import core.FSMContext
import core.PendingAction
import core.Session
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.github.kotlintelegrambot.entities.CallbackQuery
import com.github.kotlintelegrambot.entities.User
import data.RecommendationRepository
import data.RecommendationType
import data.FavoriteItem
import data.FavoriteRepository
import data.UserRepository

object RouterCollections {
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
                    bot.sendMessage(chat, "✅ Сохранено в избранное!", replyMarkup = KeyboardCollections.searchResultNavKeyboard())
                } else {
                    bot.sendMessage(chat, "Нет элементов для сохранения.", replyMarkup = KeyboardCollections.searchResultNavKeyboard())
                }
            } catch (e: Exception) {
                println("Error saving to favorites: ${e.message}")
                bot.sendMessage(chat, "Ошибка при сохранении.", replyMarkup = KeyboardCollections.searchResultNavKeyboard())
            }
        }
    }

    fun handleCollectionAction(bot: Bot, chat: ChatId, uid: Long, text: String, users: UserRepository, session: Session): Boolean {
        when (text) {
            "🗂 Подборки" -> {
                session.action = PendingAction.NONE
                session.context = FSMContext.COLLECTIONS
                session.data.clear()
                val profile = users.profile(uid)
                val collectionsMessage = """🔍 <b>Меню подборок</b>

Здесь вы можете:
• 🔍 <b>Поиск</b> - находить фильмы, сериалы, книги и игры по запросу
• ⭐️ <b>Избранное</b> - просматривать сохраненные элементы

Выберите действие:"""
                ImageManager.sendMessageWithImage(bot, chat, collectionsMessage, profile, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.collectionsMenu())
                return true
            }
            "🔍 Поиск" -> {
                session.action = PendingAction.SEARCH_TYPE_SELECT
                session.context = FSMContext.COLLECTIONS
                session.data.clear()
                val profile = users.profile(uid)
                val searchTypeMessage = """🔍 <b>Выберите тип поиска</b>

Выберите категорию для поиска:"""
                bot.sendMessage(chat, searchTypeMessage, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.searchTypeMenu())
                return true
            }
            "⭐ Избранное" -> {
                session.action = PendingAction.VIEW_FAVORITES
                session.context = FSMContext.COLLECTIONS
                showCollectionFavorites(bot, chat, uid, users)
                return true
            }
            "🎬 Фильм", "📺 Сериал", "📚 Книга", "🎮 Игра" -> {
                // Определяем тип контента из текста кнопки
                val type = when (text) {
                    "🎬 Фильм" -> RecommendationType.FILM
                    "📺 Сериал" -> RecommendationType.SERIES
                    "📚 Книга" -> RecommendationType.BOOK
                    "🎮 Игра" -> RecommendationType.GAME
                    else -> RecommendationType.FILM
                }
                
                // Устанавливаем тип и переходим к вводу запроса
                session.data["collection_type"] = type.name
                session.action = PendingAction.COLLECTION_QUERY
                
                val profile = users.profile(uid)
                val queryMessage = """🔍 <b>Поиск ${getTypeDisplayName(type).lowercase()}</b>

Введите название ${getTypeDisplayName(type).lowercase()} для поиска:"""
                ImageManager.sendMessageWithImage(bot, chat, queryMessage, profile, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.singleFavoriteNav())
                return true
            }
            "➡️ Ещё" -> {
                // Загружаем следующие результаты и добавляем к текущим
                val type = session.data["collection_type"]?.let { RecommendationType.valueOf(it) } ?: RecommendationType.FILM
                val query = session.data["collection_query"]
                val offset = (session.data["collection_offset"]?.toIntOrNull() ?: 0) + 3
                
                // Устанавливаем состояние для навигации по результатам
                session.action = PendingAction.COLLECTION_RESULTS
                
                // Вызываем suspend функцию через coroutine scope
                GlobalScope.launch {
                    val result = RecommendationRepository.searchMultiple(type, query, 3, offset = offset)
                    
                    if (result.items.isEmpty()) {
                        bot.sendMessage(chat, "Других вариантов не нашёл.", replyMarkup = KeyboardCollections.searchResultNavKeyboard())
                    } else {
                        // Получаем текущие результаты из сессии
                        val currentResultsData = session.data["search_results"] ?: ""
                        val currentResults = if (currentResultsData.isNotEmpty()) {
                            currentResultsData.split("|").mapNotNull { itemData ->
                                val parts = itemData.split(":")
                                if (parts.size == 2) {
                                    // В реальном приложении здесь нужно будет восстанавливать объекты
                                    // Пока просто загружаем заново все результаты
                                    null
                                } else null
                            }
                        } else emptyList()
                        
                        // Загружаем все результаты заново для простоты
                        val allResults = RecommendationRepository.searchMultiple(type, query, offset + 3, offset = 0).items
                        
                        val currentCount = allResults.size
                        val totalCount = result.totalCount
                        val resultHeader = if (totalCount > currentCount) {
                            "Ещё результаты: ${currentCount} из ${totalCount}"
                        } else {
                            "Все результаты: ${currentCount} из ${totalCount}"
                        }
                        
                        bot.sendMessage(chat, "$resultHeader\n\nИспользуйте стрелочки для навигации:", parseMode = ParseMode.HTML)
                        
                        // Обновляем данные в сессии
                        session.data["search_results"] = allResults.joinToString("|") { "${it.id}:${it.type.name}" }
                        session.data["search_total_count"] = allResults.size.toString()
                        session.data["collection_offset"] = offset.toString()
                        
                        // Показываем первый из новых результатов
                        showSingleSearchResult(bot, chat, uid, allResults, offset)
                    }
                }
                return true
            }
            "🔍 Новый запрос" -> {
                // Начинаем новый поиск в той же категории
                val type = session.data["collection_type"]?.let { RecommendationType.valueOf(it) }
                
                if (type != null) {
                    // Сбрасываем данные предыдущего поиска, но сохраняем тип
                    session.data["collection_query"] = ""
                    session.data["collection_offset"] = "0"
                    session.action = PendingAction.COLLECTION_QUERY
                    
                    val profile = users.profile(uid)
                    val queryMessage = """🔍 <b>Поиск ${getTypeDisplayName(type).lowercase()}</b>

Введите название ${getTypeDisplayName(type).lowercase()} для поиска:"""
                    ImageManager.sendMessageWithImage(bot, chat, queryMessage, profile, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.singleFavoriteNav())
                    return true
                } else {
                    // Если тип не определен, возвращаем к выбору типа
                    session.action = PendingAction.SEARCH_TYPE_SELECT
                    val profile = users.profile(uid)
                    val searchTypeMessage = """🔍 <b>Выберите тип поиска</b>

Выберите категорию для поиска:"""
                    bot.sendMessage(chat, searchTypeMessage, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.searchTypeMenu())
                    return true
                }
            }
            "💾 Сохранить" -> {
                when (session.context) {
                    FSMContext.MEMES -> {
                        // Для мемов - существующая логика
                        return false
                    }
                    FSMContext.COLLECTIONS -> {
                        // Для подборок - новая логика сохранения
                        saveToFavorites(bot, chat, uid, session)
                        return true
                    }
                }
            }
            "Мои избранные" -> {
                when (session.context) {
                    FSMContext.MEMES -> {
                        // Показать избранные мемы
                        return false
                    }
                    FSMContext.COLLECTIONS -> {
                        // Показать избранные подборки
                        showCollectionFavorites(bot, chat, uid, users)
                        return true
                    }
                }
            }
            "⬅️ Обратно" -> {
                when (session.action) {
                    PendingAction.VIEW_FAVORITES -> {
                        // Возврат из избранного в меню подборок
                        session.action = PendingAction.NONE
                        session.context = FSMContext.COLLECTIONS
                        val profile = users.profile(uid)
                        val collectionsMessage = """🔍 <b>Меню подборок</b>

Здесь вы можете:
• 🔍 <b>Поиск</b> - находить фильмы, сериалы, книги и игры по запросу
• ⭐️ <b>Избранное</b> - просматривать сохраненные элементы

Выберите действие:"""
                        ImageManager.sendMessageWithImage(bot, chat, collectionsMessage, profile, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.collectionsMenu())
                        return true
                    }
                    PendingAction.COLLECTION_QUERY -> {
                        // Возврат из ввода запроса в меню выбора типа поиска
                        session.action = PendingAction.SEARCH_TYPE_SELECT
                        val profile = users.profile(uid)
                        val searchTypeMessage = """🔍 <b>Выберите тип поиска</b>

Выберите категорию для поиска:"""
                        bot.sendMessage(chat, searchTypeMessage, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.searchTypeMenu())
                        return true
                    }
                    PendingAction.COLLECTION_RESULTS -> {
                        // Возврат из результатов поиска в меню выбора типа поиска
                        session.action = PendingAction.SEARCH_TYPE_SELECT
                        val profile = users.profile(uid)
                        val searchTypeMessage = """🔍 <b>Выберите тип поиска</b>

Выберите категорию для поиска:"""
                        bot.sendMessage(chat, searchTypeMessage, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.searchTypeMenu())
                        return true
                    }
                    PendingAction.SEARCH_TYPE_SELECT -> {
                        // Возврат из меню выбора типа поиска в меню подборок
                        session.action = PendingAction.NONE
                        session.context = FSMContext.COLLECTIONS
                        val profile = users.profile(uid)
                        val collectionsMessage = """🔍 <b>Меню подборок</b>

Здесь вы можете:
• 🔍 <b>Поиск</b> - находить фильмы, сериалы, книги и игры по запросу
• ⭐️ <b>Избранное</b> - просматривать сохраненные элементы

Выберите действие:"""
                        ImageManager.sendMessageWithImage(bot, chat, collectionsMessage, profile, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.collectionsMenu())
                        return true
                    }
                    else -> {
                        // Для всех остальных случаев - главное меню подборок
                        session.action = PendingAction.NONE
                        val profile = users.profile(uid)
                        val collectionsMessage = """🔍 <b>Меню подборок</b>

Здесь вы можете:
• 🔍 <b>Поиск</b> - находить фильмы, сериалы, книги и игры по запросу
• ⭐️ <b>Избранное</b> - просматривать сохраненные элементы

Выберите действие:"""
                        ImageManager.sendMessageWithImage(bot, chat, collectionsMessage, profile, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.collectionsMenu())
                        return true
                    }
                }
            }
            else -> {
                // Обработка текстовых сообщений (поисковые запросы)
                if (session.action == PendingAction.COLLECTION_QUERY) {
                    // Убедимся, что контекст установлен
                    session.context = FSMContext.COLLECTIONS
                    
                    val type = session.data["collection_type"]?.let { RecommendationType.valueOf(it) }
                    val query = text.trim()
                    
                    if (type != null && query.isNotEmpty()) {
                        session.data["collection_query"] = query
                        session.data["collection_offset"] = "0"
                        session.action = PendingAction.COLLECTION_RESULTS
                        
                        GlobalScope.launch {
                            try {
                                val result = RecommendationRepository.searchMultiple(type, query, 3, offset = 0)
                                
                                if (result.items.isEmpty()) {
                                    session.action = PendingAction.SEARCH_TYPE_SELECT
                                    bot.sendMessage(chat, "Ничего не найдено по запросу \"$query\".", replyMarkup = KeyboardFactory.searchTypeMenu())
                                } else {
                                    // Сохраняем все результаты в сессию для навигации
                                    session.data["search_results"] = result.items.joinToString("|") { "${it.id}:${it.type.name}" }
                                    session.data["search_current_index"] = "0"
                                    session.data["search_total_count"] = result.items.size.toString()
                                    session.data["search_query"] = query
                                    session.data["search_type"] = type.name
                                    
                                    val currentCount = result.items.size
                                    val totalCount = result.totalCount
                                    val resultHeader = if (totalCount > currentCount) {
                                        "Найдено ${currentCount} из ${totalCount} результатов по запросу \"$query\":"
                                    } else {
                                        "Найдено ${currentCount} результатов по запросу \"$query\":"
                                    }
                                    
                                    bot.sendMessage(chat, "$resultHeader\n\nИспользуйте стрелочки для навигации:", parseMode = ParseMode.HTML)
                                    
                                    // Показываем первый результат
                                    showSingleSearchResult(bot, chat, uid, result.items, 0)
                                }
                            } catch (e: Exception) {
                                println("Error searching: ${e.message}")
                                bot.sendMessage(chat, "Ошибка при поиске.", replyMarkup = KeyboardFactory.collectionsMenu())
                            }
                        }
                        return true
                    }
                }
                return false
            }
        }
    }

    private fun showSingleSearchResult(bot: Bot, chat: ChatId, uid: Long, items: List<RecommendationItem>, index: Int) {
        if (index < 0 || index >= items.size) return
        
        val item = items[index]
        val isSaved = FavoriteRepository.isFavorite(uid, item.id, item.type)
        
        val message = buildString {
            append("🔍 Результат поиска (${index + 1} из ${items.size})\n\n")
            append("${item.title} (${item.year})\n")
            append("Тип: ${getTypeDisplayName(item.type)}\n")
            
            if (item.description.isNotEmpty()) {
                val cleanDescription = item.description
                    .replace(Regex("<[^>]*>"), "") // Удаляем HTML теги
                    .replace(Regex("&[^;]*;"), "") // Удаляем HTML сущности
                    .replace(Regex("\\s+"), " ") // Заменяем множественные пробелы
                    .trim()
                    .take(300) // Ограничиваем до 300 символов
                
                append("Описание: ${cleanDescription}\n")
            }
            
            if (item.posterUrl != null) {
                append("Ссылка: ${item.posterUrl}\n")
            }
        }
        
        val inlineKeyboard = KeyboardCollections.singleSearchResultKeyboard(item, index, items.size, isSaved)
        val replyKeyboard = KeyboardCollections.searchResultNavKeyboard()
        
        bot.sendMessage(chat, message, parseMode = ParseMode.HTML, replyMarkup = replyKeyboard)
        bot.sendMessage(chat, "Управление:", replyMarkup = inlineKeyboard)
    }

    private fun showFavoriteItem(bot: Bot, chat: ChatId, uid: Long, favorites: List<FavoriteItem>, index: Int) {
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

    fun handleCallback(bot: Bot, callback: CallbackQuery, users: UserRepository) {
        val chat = ChatId.fromId(callback.message!!.chat.id)
        val uid = callback.from.id
        val session = SessionStore.get(uid)
        val data = callback.data ?: return

        when {
            data.startsWith("save_item_") -> {
                val itemNumber = data.substringAfter("save_item_").toIntOrNull() ?: return
                val type = session.data["collection_type"]?.let { RecommendationType.valueOf(it) }
                val query = session.data["collection_query"]
                val offset = session.data["collection_offset"]?.toIntOrNull() ?: 0
                
                if (type == null) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: не определен тип контента.")
                    return
                }
                
                GlobalScope.launch {
                    try {
                        val results = RecommendationRepository.searchMultiple(type, query, 3, offset)
                        val itemIndex = itemNumber - 1
                        if (itemIndex in results.items.indices) {
                            val item = results.items[itemIndex]
                            
                            // Проверяем, уже ли в избранном
                            val isAlreadyFavorite = FavoriteRepository.isFavorite(uid, item.id, item.type)
                            
                            if (isAlreadyFavorite) {
                                bot.answerCallbackQuery(callback.id, "Этот ${getTypeDisplayName(item.type).lowercase()} уже есть в избранных")
                                bot.sendMessage(chat, "⚠️ Этот ${getTypeDisplayName(item.type).lowercase()} уже есть в избранных")
                            } else {
                                FavoriteRepository.add(uid, item)
                                bot.answerCallbackQuery(callback.id, "В избранные добавлен №$itemNumber")
                                bot.sendMessage(chat, "✅ В избранные добавлен №$itemNumber")
                            }
                        } else {
                            bot.answerCallbackQuery(callback.id, "Ошибка: элемент не найден.")
                        }
                    } catch (e: Exception) {
                        println("Error saving item to favorites: ${e.message}")
                        bot.answerCallbackQuery(callback.id, "Ошибка при сохранении.")
                    }
                }
            }
            
            data.startsWith("nav_search_") -> {
                val newIndex = data.substringAfter("nav_search_").toIntOrNull()
                if (newIndex == null) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: неверный индекс.")
                    return
                }
                
                val type = session.data["search_type"]?.let { RecommendationType.valueOf(it) }
                val query = session.data["search_query"]
                
                if (type != null && query != null) {
                    GlobalScope.launch {
                        try {
                            val result = RecommendationRepository.searchMultiple(type, query, 20, offset = 0)
                            if (newIndex < result.items.size) {
                                session.data["search_current_index"] = newIndex.toString()
                                showSingleSearchResult(bot, chat, uid, result.items, newIndex)
                            }
                        } catch (e: Exception) {
                            println("Error navigating search results: ${e.message}")
                            bot.answerCallbackQuery(callback.id, "Ошибка при навигации.")
                        }
                    }
                }
                
                bot.answerCallbackQuery(callback.id)
            }
            
            data.startsWith("save_single_") -> {
                val parts = data.substringAfter("save_single_").split("_")
                if (parts.size != 2) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: неверный формат данных.")
                    return
                }
                
                val itemId = parts[0].toIntOrNull()
                val itemTypeName = parts[1]
                
                if (itemId == null) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: неверный ID элемента.")
                    return
                }
                
                try {
                    val itemType = RecommendationType.valueOf(itemTypeName)
                    val type = session.data["search_type"]?.let { RecommendationType.valueOf(it) }
                    val query = session.data["search_query"]
                    
                    if (type != null && query != null) {
                        GlobalScope.launch {
                            try {
                                val result = RecommendationRepository.searchMultiple(type, query, 20, offset = 0)
                                val item = result.items.find { it.id == itemId && it.type == itemType }
                                
                                if (item != null) {
                                    val isAlreadyFavorite = FavoriteRepository.isFavorite(uid, item.id, item.type)
                                    
                                    if (isAlreadyFavorite) {
                                        bot.answerCallbackQuery(callback.id, "Этот ${getTypeDisplayName(item.type).lowercase()} уже есть в избранных")
                                    } else {
                                        FavoriteRepository.add(uid, item)
                                        bot.answerCallbackQuery(callback.id, "✅ Сохранено в избранное!")
                                    }
                                } else {
                                    bot.answerCallbackQuery(callback.id, "Ошибка: элемент не найден.")
                                }
                            } catch (e: Exception) {
                                println("Error saving single item to favorites: ${e.message}")
                                bot.answerCallbackQuery(callback.id, "Ошибка при сохранении.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing item type: ${e.message}")
                    bot.answerCallbackQuery(callback.id, "Ошибка: неверный тип элемента.")
                }
            }
            
            data.startsWith("remove_item_") -> {
                val parts = data.substringAfter("remove_item_").split("_")
                if (parts.size != 2) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: неверный формат данных.")
                    return
                }
                
                val itemId = parts[0].toIntOrNull()
                val itemTypeName = parts[1]
                
                if (itemId == null) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: неверный ID элемента.")
                    return
                }
                
                try {
                    val itemType = RecommendationType.valueOf(itemTypeName)
                    FavoriteRepository.remove(uid, itemId, itemType)
                    bot.answerCallbackQuery(callback.id, "✅ Удалено из избранного!")
                    
                    // Обновляем сообщение с новым состоянием кнопки
                    val type = session.data["search_type"]?.let { RecommendationType.valueOf(it) }
                    val query = session.data["search_query"]
                    val currentIndex = session.data["search_current_index"]?.toIntOrNull() ?: 0
                    
                    if (type != null && query != null) {
                        GlobalScope.launch {
                            try {
                                val result = RecommendationRepository.searchMultiple(type, query, 20, offset = 0)
                                if (currentIndex < result.items.size) {
                                    showSingleSearchResult(bot, chat, uid, result.items, currentIndex)
                                }
                            } catch (e: Exception) {
                                println("Error updating search result after removal: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error removing from favorites: ${e.message}")
                    bot.answerCallbackQuery(callback.id, "Ошибка при удалении.")
                }
            }
            
            data.startsWith("delete_favorite_") -> {
                val parts = data.substringAfter("delete_favorite_").split("_")
                if (parts.size != 2) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: неверный формат данных.")
                    return
                }
                
                val itemId = parts[0].toIntOrNull()
                val itemTypeName = parts[1]
                
                if (itemId == null) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: неверный ID элемента.")
                    return
                }
                
                try {
                    val itemType = RecommendationType.valueOf(itemTypeName)
                    FavoriteRepository.remove(uid, itemId, itemType)
                    bot.answerCallbackQuery(callback.id, "✅ Удалено из избранного!")
                    
                    // Обновляем список избранных и показываем обновленный интерфейс
                    GlobalScope.launch {
                        val updatedFavorites = FavoriteRepository.list(uid)
                        if (updatedFavorites.isEmpty()) {
                            // Если избранных больше нет, остаемся в меню подборок
                            val session = SessionStore.get(uid)
                            session.action = PendingAction.NONE
                            val profile = users.profile(uid)
                            val collectionsMessage = """🔍 <b>Меню подборок</b>

Здесь вы можете:
• 🔍 <b>Поиск</b> - находить фильмы, сериалы, книги и игры по запросу
• ⭐️ <b>Избранное</b> - просматривать сохраненные элементы

Выберите действие:"""
                            bot.sendMessage(chat, "У вас больше нет избранных элементов.")
                            ImageManager.sendMessageWithImage(
                                bot, chat, collectionsMessage, profile,
                                replyMarkup = KeyboardFactory.collectionsMenu(),
                                parseMode = ParseMode.HTML
                            )
                        } else {
                            // Показываем обновленный список
                            val session = SessionStore.get(uid)
                            val currentIndex = session.data["favorites_index"]?.toIntOrNull() ?: 0
                            
                            // Если удалили последний элемент и он был текущим, показываем предыдущий
                            val newIndex = if (currentIndex >= updatedFavorites.size) {
                                updatedFavorites.size - 1
                            } else {
                                currentIndex
                            }
                            
                            session.data["favorites_index"] = newIndex.toString()
                            session.data["favorites_total"] = updatedFavorites.size.toString()
                            
                            showFavoriteItem(bot, chat, uid, updatedFavorites, newIndex)
                        }
                    }
                } catch (e: Exception) {
                    println("Error deleting from favorites: ${e.message}")
                    bot.answerCallbackQuery(callback.id, "Ошибка при удалении.")
                }
            }
            
            data.startsWith("nav_favorite_") -> {
                val newIndex = data.substringAfter("nav_favorite_").toIntOrNull()
                if (newIndex == null) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: неверный индекс.")
                    return
                }
                
                val favorites = FavoriteRepository.list(uid)
                if (newIndex < 0 || newIndex >= favorites.size) {
                    bot.answerCallbackQuery(callback.id, "Ошибка: индекс вне диапазона.")
                    return
                }
                
                // Обновляем индекс в сессии
                val session = SessionStore.get(uid)
                session.data["favorites_index"] = newIndex.toString()
                
                // Показываем новый элемент
                GlobalScope.launch {
                    showFavoriteItem(bot, chat, uid, favorites, newIndex)
                }
                
                bot.answerCallbackQuery(callback.id)
            }
        }
    }
}
