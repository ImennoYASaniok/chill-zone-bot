// package core

// import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
// import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
// import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
// import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
// import data.UserProfile
// import data.UserRepository
// import data.FavoriteItem

// private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
//     return KeyboardReplyMarkup(
//         keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
//         resizeKeyboard = true,
//         oneTimeKeyboard = false
//     )
// }

// object KeyboardFactory {
//     fun mainMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("👤 Профиль", "🗂 Подборки"),
//             listOf("😂 Мемы", "🔮 Предсказания"),
//             listOf("📝 Тесты", "📅 События"),
//             listOf("🎮 Мини-игры", "🖼 Пиксель-арт"),
//             listOf("💬 Обратная связь", "⚙️ Настройки")
//         )
//     )

//     fun profileMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
//         val profileHidden = userProfile?.hidden ?: false
//         val usernameHidden = userProfile?.hideUsername ?: false
        
//         return kb(
//             listOf(
//                 listOf("Изменить имя", if (usernameHidden) "Показать username [👁️]" else "Скрыть username [🙈]"),
//                 listOf("Изменить био"),
//                 listOf(
//                     if (profileHidden) "Показать профиль [👁️]" else "Скрыть профиль [🙈]"
//                 ),
//                 listOf("⬅️ Обратно")
//             )
//         )
//     }

//     fun settingsMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
//         val mediaEnabled = userProfile?.showMedia ?: true
        
//         return kb(
//             listOf(
//                 listOf(if (mediaEnabled) "Выключить картинки [❌]" else "Включить картинки [✅]"),
//                 listOf("⬅️ Обратно")
//             )
//         )
//     }

//     fun memesMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("Следующий мем", "Добавить мем"),
//             listOf("👍", "👎"),
//             listOf("💾 Избр. мем", "Мои избр. мемы"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun predictionsMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("Получить предсказание", "Мои предсказания"),
//             listOf("Добавить предсказание", "Поиск предсказаний"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun predictionRarity(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("Обычное", "Редкое"),
//             listOf("Эпическое", "Легендарное"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun testsMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("Случайный тест", "Создать тест"),
//             listOf("Мои тесты"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun testKinds(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("SINGLE", "MULTI"),
//             listOf("NUMBER", "MATCH"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun testsContinue(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("Да", "Нет"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun eventsMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("Ближайшие события", "Создать событие"),
//             listOf("Мои события"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun gamesMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("⚽ Гол", "🏀 В кольцо"),
//             listOf("🎡 Колесо фортуны", "✂️ Камень-ножницы-бумага"),
//             listOf("📊 Моя статистика", "🏆 Топ игроков"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun collectionsMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("🔍 Поиск", "⭐ Избранное"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun searchTypeMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("🎬 Фильм", "📺 Сериал", "📚 Книга", "🎮 Игра"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun searchResultsMenu(hasMore: Boolean = false): KeyboardReplyMarkup = kb(
//         listOf(
//             if (hasMore) listOf("➡️ Ещё", "⬅️ Обратно") else listOf("⬅️ Обратно")
//         )
//     )

//     fun feedbackMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("Оставить отзыв", "Посмотреть поддержку"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun pixelMenu(): KeyboardReplyMarkup = kb(
//         listOf(
//             listOf("Публичный холст"),
//             listOf("⬅️ Обратно")
//         )
//     )

//     fun searchResultsInlineKeyboard(itemsCount: Int): InlineKeyboardMarkup {
//         val rows = mutableListOf<List<InlineKeyboardButton>>()
        
//         // Добавляем только кнопки сохранения
//         val saveButtons = (1..itemsCount).map { index ->
//             InlineKeyboardButton.CallbackData(text = "Сохранить №$index", callbackData = "save_item_$index")
//         }
//         rows.add(saveButtons)
        
//         return InlineKeyboardMarkup.create(rows)
//     }

//     fun searchResultsMenuWithInline(itemsCount: Int, hasMore: Boolean = false): KeyboardReplyMarkup {
//         val rows = mutableListOf<List<String>>()
        
//         // Добавляем навигационные кнопки в reply клавиатуру
//         if (hasMore) {
//             rows.add(listOf("➡️ Ещё", "⬅️ Обратно"))
//         } else {
//             rows.add(listOf("⬅️ Обратно"))
//         }
        
//         return kb(rows)
//     }

//     fun favoritesInlineKeyboard(favorites: List<FavoriteItem>): InlineKeyboardMarkup {
//         val rows = mutableListOf<List<InlineKeyboardButton>>()
        
//         favorites.forEach { favorite ->
//             val deleteButton = InlineKeyboardButton.CallbackData(
//                 text = "🗑️ Удалить из избранного",
//                 callbackData = "delete_favorite_${favorite.itemId}_${favorite.itemType.name}"
//             )
//             rows.add(listOf(deleteButton))
//         }
        
//         return InlineKeyboardMarkup.create(rows)
//     }

//     fun singleFavoriteDeleteKeyboard(favorite: FavoriteItem, currentIndex: Int, totalCount: Int): InlineKeyboardMarkup {
//         val rows = mutableListOf<List<InlineKeyboardButton>>()
        
//         // Кнопка удаления
//         val deleteButton = InlineKeyboardButton.CallbackData(
//             text = "🗑️ Удалить из избранного",
//             callbackData = "delete_favorite_${favorite.itemId}_${favorite.itemType.name}"
//         )
//         rows.add(listOf(deleteButton))
        
//         // Стрелочки навигации - только если больше 1 элемента
//         if (totalCount > 1) {
//             val navButtons = mutableListOf<InlineKeyboardButton>()
            
//             // Левая стрелочка (к предыдущему или к последнему если первый)
//             navButtons.add(
//                 InlineKeyboardButton.CallbackData(
//                         text = "⬅️",
//                         callbackData = "nav_favorite_${if (currentIndex == 0) totalCount - 1 else currentIndex - 1}"
//                 )
//             )
            
//             // Правая стрелочка (к следующему или к первому если последний)
//             navButtons.add(
//                 InlineKeyboardButton.CallbackData(
//                         text = "➡️",
//                         callbackData = "nav_favorite_${if (currentIndex == totalCount - 1) 0 else currentIndex + 1}"
//                 )
//             )
            
//             rows.add(navButtons)
//         }
        
//         return InlineKeyboardMarkup.create(rows)
//     }

//     fun favoritesNavKeyboard(currentIndex: Int, totalCount: Int): KeyboardReplyMarkup {
//         val rows = mutableListOf<List<String>>()
        
//         // Стрелочки навигации
//         val navRow = mutableListOf<String>()
//         if (currentIndex > 0) {
//             navRow.add("⬅️")
//         }
//         if (currentIndex < totalCount - 1) {
//             navRow.add("➡️")
//         }
//         if (navRow.isNotEmpty()) {
//             rows.add(navRow)
//         }
        
//         // Кнопка "Обратно"
//         rows.add(listOf("⬅️ Обратно"))
        
//         return kb(rows)
//     }

//     fun singleFavoriteNav(): KeyboardReplyMarkup {
//         return kb(listOf(listOf("⬅️ Обратно")))
//     }
// }