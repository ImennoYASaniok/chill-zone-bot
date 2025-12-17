package core.keyboards

import core.Button
import core.States

fun getKeyboardGeneralMenu(): MutableList<MutableList<Button>> {
    val profileButton = Button(
        name = "👤 Профиль"
    )
    KeyboardNames.generalMenu["profile"] = profileButton.getText()

    val settingsButton = Button(
        name = "⚙️ Настройки",
        stateChange = States.Settings,
    )
    KeyboardNames.generalMenu["settings"] = settingsButton.getText()

    val collectionsButton = Button(
        name = "🗂️ Подборки"
    )
    KeyboardNames.generalMenu["collections"] = collectionsButton.getText()

    val miniGamesButton = Button(
        name = "🎮 Мини-игры"
    )
    KeyboardNames.generalMenu["miniGames"] = miniGamesButton.getText()

    val predictionsButton = Button(
        name = "🔮 Предсказания"
    )
    KeyboardNames.generalMenu["predictions"] = profileButton.getText()

    val memesButton = Button(
        name = "😂 Мемы"
    )
    KeyboardNames.generalMenu["memes"] = memesButton.getText()

    val testsButton = Button(
        name = "📝 Тесты"
    )
    KeyboardNames.generalMenu["tests"] = testsButton.getText()

    val pixelArtsButton = Button(
        name = "🖼️ Пиксель-арты"
    )
    KeyboardNames.generalMenu["pixelArts"] = pixelArtsButton.getText()

    val feedbackButton = Button(
        name = "💬 Обратная связь"
    )
    KeyboardNames.generalMenu["feedback"] = feedbackButton.getText()

    val aboutButton = Button(
        name = "ℹ️ О нас"
    )
    KeyboardNames.generalMenu["about"] = aboutButton.getText()

    return mutableListOf(
        mutableListOf(profileButton, settingsButton),
        mutableListOf(collectionsButton, miniGamesButton),
        mutableListOf(predictionsButton, memesButton),
        mutableListOf(testsButton, pixelArtsButton),
        mutableListOf(feedbackButton, aboutButton),
    )
}