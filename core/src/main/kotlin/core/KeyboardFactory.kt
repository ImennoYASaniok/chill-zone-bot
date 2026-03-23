package core

import com.github.kotlintelegrambot.entities.ReplyKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

private fun kb(rows: List<List<String>>): ReplyKeyboardMarkup {
    return ReplyKeyboardMarkup(
        keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )
}

object KeyboardFactory {
    fun mainMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("👤 Профиль", "🗂 Подборки"),
            listOf("😂 Мемы", "🔮 Предсказания"),
            listOf("📝 Тесты", "📅 События"),
            listOf("🎮 Мини-игры", "🖼 Пиксель-арт"),
            listOf("💬 Обратная связь", "⚙️ Настройки")
        )
    )

    fun profileMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Изменить имя", "Изменить био"),
            listOf("Скрыть/показать профиль", "Переключить картинки"),
            listOf("⬅️ Обратно")
        )
    )

    fun settingsMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Переключить картинки"),
            listOf("Сбросить сессию"),
            listOf("⬅️ Обратно")
        )
    )

    fun memesMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Следующий мем", "Добавить мем"),
            listOf("👍", "👎"),
            listOf("⭐ Избранное", "Мои избранные"),
            listOf("⬅️ Обратно")
        )
    )

    fun predictionsMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Получить предсказание", "Мои предсказания"),
            listOf("Добавить предсказание", "Поиск предсказаний"),
            listOf("⬅️ Обратно")
        )
    )

    fun predictionRarity(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Обычное", "Редкое"),
            listOf("Эпическое", "Легендарное"),
            listOf("⬅️ Обратно")
        )
    )

    fun testsMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Случайный тест", "Создать тест"),
            listOf("Мои тесты"),
            listOf("⬅️ Обратно")
        )
    )

    fun testKinds(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("SINGLE", "MULTI"),
            listOf("NUMBER", "MATCH"),
            listOf("⬅️ Обратно")
        )
    )

    fun testsContinue(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Да", "Нет"),
            listOf("⬅️ Обратно")
        )
    )

    fun eventsMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Ближайшие события", "Создать событие"),
            listOf("Мои события"),
            listOf("⬅️ Обратно")
        )
    )

    fun gamesMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("⚽ Гол", "🏀 В кольцо"),
            listOf("🎡 Колесо фортуны", "✂️ Камень-ножницы-бумага"),
            listOf("📊 Моя статистика", "🏆 Топ игроков"),
            listOf("⬅️ Обратно")
        )
    )

    fun collectionsMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("🎬 Фильм", "📺 Сериал"),
            listOf("📚 Книга", "🎮 Игра"),
            listOf("По запросу"),
            listOf("⬅️ Обратно")
        )
    )

    fun feedbackMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Оставить отзыв", "Посмотреть поддержку"),
            listOf("⬅️ Обратно")
        )
    )

    fun pixelMenu(): ReplyKeyboardMarkup = kb(
        listOf(
            listOf("Публичный холст"),
            listOf("⬅️ Обратно")
        )
    )
}