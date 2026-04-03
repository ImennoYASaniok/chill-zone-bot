package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import data.FavoriteItem
import data.RecommendationItem

object KeyboardCollections {
    fun collectionsMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("🔍 Поиск", "⭐ Избранное"),
            listOf("⬅️ Обратно")
        )
    )

    fun searchTypeMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("🎬 Фильм", "📺 Сериал", "📚 Книга", "🎮 Игра"),
            listOf("⬅️ Обратно")
        )
    )

    fun searchResultsMenu(hasMore: Boolean = false): KeyboardReplyMarkup = kb(
        listOf(
            if (hasMore) listOf("➡️ Ещё", "⬅️ Обратно") else listOf("⬅️ Обратно")
        )
    )

    fun searchResultsMenuWithInline(itemsCount: Int, hasMore: Boolean = false): KeyboardReplyMarkup {
        val rows = mutableListOf<List<String>>()
        
        // Добавляем навигационные кнопки в reply клавиатуру
        if (hasMore) {
            rows.add(listOf("🔍 Новый запрос", "➡️ Ещё", "⬅️ Обратно"))
        } else {
            rows.add(listOf("🔍 Новый запрос", "⬅️ Обратно"))
        }
        
        return kb(rows)
    }

    fun searchResultsInlineKeyboard(itemsCount: Int): InlineKeyboardMarkup {
        val rows = mutableListOf<List<InlineKeyboardButton>>()
        
        // Добавляем только кнопки сохранения
        val saveButtons = (1..itemsCount).map { index ->
            InlineKeyboardButton.CallbackData(text = "Сохранить №$index", callbackData = "save_item_$index")
        }
        rows.add(saveButtons)
        
        return InlineKeyboardMarkup.create(rows)
    }

    fun singleSearchResultKeyboard(item: RecommendationItem, currentIndex: Int, totalCount: Int, isSaved: Boolean): InlineKeyboardMarkup {
        val rows = mutableListOf<List<InlineKeyboardButton>>()
        
        // Кнопка сохранения/удаления
        val saveButton = if (isSaved) {
            InlineKeyboardButton.CallbackData(
                text = "✅ В избранном",
                callbackData = "remove_item_${item.id}_${item.type.name}"
            )
        } else {
            InlineKeyboardButton.CallbackData(
                text = "💾 Сохранить в избранное",
                callbackData = "save_single_${item.id}_${item.type.name}"
            )
        }
        rows.add(listOf(saveButton))
        
        // Стрелочки навигации - только если больше 1 элемента
        if (totalCount > 1) {
            val navButtons = mutableListOf<InlineKeyboardButton>()
            
            // Левая стрелочка (к предыдущему или к последнему если первый)
            navButtons.add(
                InlineKeyboardButton.CallbackData(
                        text = "⬅️",
                        callbackData = "nav_search_${if (currentIndex == 0) totalCount - 1 else currentIndex - 1}"
                )
            )
            
            // Правая стрелочка (к следующему или к первому если последний)
            navButtons.add(
                InlineKeyboardButton.CallbackData(
                        text = "➡️",
                        callbackData = "nav_search_${if (currentIndex == totalCount - 1) 0 else currentIndex + 1}"
                )
            )
            
            rows.add(navButtons)
        }
        
        return InlineKeyboardMarkup.create(rows)
    }

    fun searchResultNavKeyboard(): KeyboardReplyMarkup {
        // Только кнопка "Обратно" в reply клавиатуре
        return kb(listOf(listOf("⬅️ Обратно")))
    }

    fun favoritesInlineKeyboard(favorites: List<FavoriteItem>): InlineKeyboardMarkup {
        val rows = mutableListOf<List<InlineKeyboardButton>>()
        
        favorites.forEach { favorite ->
            val deleteButton = InlineKeyboardButton.CallbackData(
                text = "🗑️ Удалить из избранного",
                callbackData = "delete_favorite_${favorite.itemId}_${favorite.itemType.name}"
            )
            rows.add(listOf(deleteButton))
        }
        
        return InlineKeyboardMarkup.create(rows)
    }

    fun singleFavoriteDeleteKeyboard(favorite: FavoriteItem, currentIndex: Int, totalCount: Int): InlineKeyboardMarkup {
        val rows = mutableListOf<List<InlineKeyboardButton>>()
        
        // Кнопка удаления
        val deleteButton = InlineKeyboardButton.CallbackData(
            text = "🗑️ Удалить из избранного",
            callbackData = "delete_favorite_${favorite.itemId}_${favorite.itemType.name}"
        )
        rows.add(listOf(deleteButton))
        
        // Стрелочки навигации - только если больше 1 элемента
        if (totalCount > 1) {
            val navButtons = mutableListOf<InlineKeyboardButton>()
            
            // Левая стрелочка (к предыдущему или к последнему если первый)
            navButtons.add(
                InlineKeyboardButton.CallbackData(
                        text = "⬅️",
                        callbackData = "nav_favorite_${if (currentIndex == 0) totalCount - 1 else currentIndex - 1}"
                )
            )
            
            // Правая стрелочка (к следующему или к первому если последний)
            navButtons.add(
                InlineKeyboardButton.CallbackData(
                        text = "➡️",
                        callbackData = "nav_favorite_${if (currentIndex == totalCount - 1) 0 else currentIndex + 1}"
                )
            )
            
            rows.add(navButtons)
        }
        
        return InlineKeyboardMarkup.create(rows)
    }

    fun favoritesNavKeyboard(currentIndex: Int, totalCount: Int): KeyboardReplyMarkup {
        // Только кнопка "Обратно", стрелки уже есть в inline клавиатуре
        return kb(listOf(listOf("⬅️ Обратно")))
    }

    fun singleFavoriteNav(): KeyboardReplyMarkup {
        return kb(listOf(listOf("⬅️ Обратно")))
    }

    private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
        return KeyboardReplyMarkup(
            keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
            resizeKeyboard = true,
            oneTimeKeyboard = false
        )
    }
}