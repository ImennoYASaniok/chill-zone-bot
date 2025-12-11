package core.keyboards

import core.BoolButton
import core.Button
import core.ChooseButton

import kotlin.collections.mutableListOf

fun getKeyboardBaseMenu(): MutableList<MutableList<Button>> {
    val profileButton = Button(
        name = "👤 Профиль"
    )
    val settingsButton = Button(
        name = "⚙️ Настройки"
    )
    val collectionsButton = Button(
        name = "🗂️ Подборки"
    )
    val miniGamesButton = Button(
        name = "🎮 Мини-игры"
    )
    val predictionsButton = Button(
        name = "🔮 Предсказания"
    )
    val memeButton = Button(
        name = "😂 Мемы"
    )
    val testsButton = Button(
        name = "📝 Тесты"
    )
    val pixelArtsButton = Button(
        name = "🖼️ Пиксель-арты"
    )
    val feedbackButton = Button(
        name = "💬 Обратная связь"
    )

    return mutableListOf(
        mutableListOf(profileButton, settingsButton),
        mutableListOf(collectionsButton, miniGamesButton),
        mutableListOf(predictionsButton, memeButton),
        mutableListOf(testsButton, pixelArtsButton),
        mutableListOf(feedbackButton),
    )
}