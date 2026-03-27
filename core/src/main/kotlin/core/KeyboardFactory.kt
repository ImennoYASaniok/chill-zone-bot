package core

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

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

    fun profileMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Изменить имя", "Изменить био"),
            listOf("Скрыть/показать профиль", "Переключить картинки"),
            listOf("⬅️ Обратно")
        )
    )

    fun settingsMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Переключить картинки"),
            listOf("Сбросить сессию"),
            listOf("⬅️ Обратно")
        )
    )

    fun memesMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("Следующий мем", "Добавить мем"),
            listOf("👍", "👎"),
            listOf("⭐ Избранное", "Мои избранные"),
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
            listOf("🎬 Фильм", "📺 Сериал"),
            listOf("📚 Книга", "🎮 Игра"),
            listOf("По запросу"),
            listOf("⬅️ Обратно")
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