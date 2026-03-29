package core

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import data.UserProfile
import data.UserRepository

private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
    return KeyboardReplyMarkup(
        keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )
}

object KeyboardFactory {
    fun mainMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("👤 Профиль", "🗂 Подборки"),
            listOf("😂 Мемы", "🔮 Предсказания"),
            listOf("📝 Тесты", "📅 События"),
            listOf("🎮 Мини-игры", "🖼 Пиксель-арт"),
            listOf("💬 Обратная связь", "⚙️ Настройки")
        )
    )

    fun profileMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val profileHidden = userProfile?.hidden ?: false
        val usernameHidden = userProfile?.hideUsername ?: false
        
        return kb(
            listOf(
                listOf("Изменить имя", if (usernameHidden) "Показать username [👁️]" else "Скрыть username [🙈]"),
                listOf("Изменить био"),
                listOf(
                    if (profileHidden) "Показать профиль [👁️]" else "Скрыть профиль [🙈]"
                ),
                listOf("⬅️ Обратно")
            )
        )
    }

    fun settingsMenu(userProfile: UserProfile? = null): KeyboardReplyMarkup {
        val mediaEnabled = userProfile?.showMedia ?: true
        
        return kb(
            listOf(
                listOf(if (mediaEnabled) "Выключить картинки [❌]" else "Включить картинки [✅]"),
                listOf("⬅️ Обратно")
            )
        )
    }

    fun memesMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Следующий мем", "Добавить мем"),
            listOf("👍", "👎"),
            listOf("💾 Избр. мем", "Мои избр. мемы"),
            listOf("⬅️ Обратно")
        )
    )

    fun predictionsMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Получить предсказание", "Мои предсказания"),
            listOf("Добавить предсказание", "Поиск предсказаний"),
            listOf("⬅️ Обратно")
        )
    )

    fun predictionRarity(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Обычное", "Редкое"),
            listOf("Эпическое", "Легендарное"),
            listOf("⬅️ Обратно")
        )
    )

    fun testsMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Случайный тест", "Создать тест"),
            listOf("Мои тесты"),
            listOf("⬅️ Обратно")
        )
    )

    fun testKinds(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("SINGLE", "MULTI"),
            listOf("NUMBER", "MATCH"),
            listOf("⬅️ Обратно")
        )
    )

    fun testsContinue(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Да", "Нет"),
            listOf("⬅️ Обратно")
        )
    )

    fun eventsMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Ближайшие события", "Создать событие"),
            listOf("Мои события"),
            listOf("⬅️ Обратно")
        )
    )

    fun gamesMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("⚽ Гол", "🏀 В кольцо"),
            listOf("🎡 Колесо фортуны", "✂️ Камень-ножницы-бумага"),
            listOf("📊 Моя статистика", "🏆 Топ игроков"),
            listOf("⬅️ Обратно")
        )
    )

    fun collectionsMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("🎬 Фильм", "📺 Сериал", "📚 Книга", "🎮 Игра"),
            listOf("По запросу", "⭐ Избранное"),
            listOf("⬅️ Обратно")
        )
    )

    fun searchResultsMenu(hasMore: Boolean = false): KeyboardReplyMarkup = kb(
        listOf(
            listOf("🔄 Другие варианты", "💾 Сохранить"),
            if (hasMore) listOf("➡️ Ещё", "⬅️ Обратно") else listOf("⬅️ Обратно")
        )
    )

    fun feedbackMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Оставить отзыв", "Посмотреть поддержку"),
            listOf("⬅️ Обратно")
        )
    )

    fun pixelMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Публичный холст"),
            listOf("⬅️ Обратно")
        )
    )
}