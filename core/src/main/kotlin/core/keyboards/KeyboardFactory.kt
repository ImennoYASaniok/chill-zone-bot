package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import data.UserProfile
import data.FavoriteItem

// Вспомогательная функция для создания клавиатур
private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
    return KeyboardReplyMarkup(
        keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )
}

object KeyboardFactory {
    // Системные клавиатуры (основные меню)
    fun mainMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("👤 Профиль", "🗂 Подборки"),
            listOf("😂 Мемы", "🔮 Предсказания"),
            listOf("📝 Тесты", "📅 События"),
            listOf("🎮 Мини-игры", "🖼 Пиксель-арт"),
            listOf("💬 Обратная связь", "⚙️ Настройки")
        )
    )

    // Перенаправления в модульные клавиатуры
    fun profileMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        return KeyboardProfile.profileMenu(userProfile)
    }

    fun settingsMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        return KeyboardProfile.settingsMenu(userProfile)
    }

    fun memesMenu(): KeyboardReplyMarkup {
        return KeyboardMemes.memesMenu()
    }

    fun predictionsMenu(): KeyboardReplyMarkup {
        return KeyboardPredictions.predictionsMenu()
    }

    fun predictionProcessMenu(): KeyboardReplyMarkup {
        return KeyboardPredictions.predictionProcessMenu()
    }

    fun predictionRarity(): KeyboardReplyMarkup {
        return KeyboardPredictions.predictionRarity()
    }

    fun testsMenu(): KeyboardReplyMarkup {
        return KeyboardTests.testsMenu()
    }

    fun testKinds(): KeyboardReplyMarkup {
        return KeyboardTests.testKinds()
    }

    fun testsContinue(): KeyboardReplyMarkup {
        return KeyboardTests.testsContinue()
    }

    fun eventsMenu(): KeyboardReplyMarkup {
        return KeyboardEvents.eventsMenu()
    }

    fun gamesMenu(): KeyboardReplyMarkup {
        return KeyboardGames.gamesMenu()
    }

    fun collectionsMenu(): KeyboardReplyMarkup {
        return KeyboardCollections.collectionsMenu()
    }

    fun searchTypeMenu(): KeyboardReplyMarkup {
        return KeyboardCollections.searchTypeMenu()
    }

    fun searchResultsMenu(hasMore: Boolean = false): KeyboardReplyMarkup {
        return KeyboardCollections.searchResultsMenu(hasMore)
    }

    fun feedbackMenu(): KeyboardReplyMarkup {
        return KeyboardFeedBack.feedbackMenu()
    }

    fun pixelMenu(): KeyboardReplyMarkup {
        return KeyboardPixelArt.pixelMenu()
    }

    // Системные inline клавиатуры (общие для всех модулей)
    fun searchResultsInlineKeyboard(itemsCount: Int): InlineKeyboardMarkup {
        return KeyboardCollections.searchResultsInlineKeyboard(itemsCount)
    }

    fun searchResultsMenuWithInline(itemsCount: Int, hasMore: Boolean = false): KeyboardReplyMarkup {
        return KeyboardCollections.searchResultsMenuWithInline(itemsCount, hasMore)
    }

    fun favoritesInlineKeyboard(favorites: List<FavoriteItem>): InlineKeyboardMarkup {
        return KeyboardCollections.favoritesInlineKeyboard(favorites)
    }

    fun singleFavoriteDeleteKeyboard(favorite: FavoriteItem, currentIndex: Int, totalCount: Int): InlineKeyboardMarkup {
        return KeyboardCollections.singleFavoriteDeleteKeyboard(favorite, currentIndex, totalCount)
    }

    fun favoritesNavKeyboard(currentIndex: Int, totalCount: Int): KeyboardReplyMarkup {
        return KeyboardCollections.favoritesNavKeyboard(currentIndex, totalCount)
    }

    fun singleFavoriteNav(): KeyboardReplyMarkup {
        return KeyboardCollections.singleFavoriteNav()
    }
}