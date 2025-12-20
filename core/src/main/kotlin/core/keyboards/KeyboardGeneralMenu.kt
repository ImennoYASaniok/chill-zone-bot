package core.keyboards

import core.Button
import core.States

fun getKeyboardGeneralMenu(chatId: Long): MutableList<MutableList<Button>> {
    val accountButton = Button(
        name = "👤 Профиль",
        stateChange = States.Account,
        chatId = chatId
    )
    KeyboardNames.generalMenu["account"] = accountButton.getText()

    val settingsButton = Button(
        name = "⚙️ Настройки",
        stateChange = States.Settings,
        chatId = chatId
    )
    KeyboardNames.generalMenu["settings"] = settingsButton.getText()

    val collectionsButton = Button(
        name = "🗂️ Подборки",
        chatId = chatId
    )
    KeyboardNames.generalMenu["collections"] = collectionsButton.getText()

    val miniGamesButton = Button(
        name = "🎮 Мини-игры",
        chatId = chatId
    )
    KeyboardNames.generalMenu["miniGames"] = miniGamesButton.getText()

    val predictionsButton = Button(
        name = "🔮 Предсказания",
        chatId = chatId
    )
    KeyboardNames.generalMenu["predictions"] = predictionsButton.getText()

    val memesButton = Button(
        name = "😂 Мемы",
        stateChange = States.MemeMenu,
        chatId = chatId
    )
    KeyboardNames.generalMenu["memes"] = memesButton.getText()

    val testsButton = Button(
        name = "📝 Тесты",
        chatId = chatId
    )
    KeyboardNames.generalMenu["tests"] = testsButton.getText()

    val pixelArtsButton = Button(
        name = "🖼️ Пиксель-арты",
        chatId = chatId
    )
    KeyboardNames.generalMenu["pixelArts"] = pixelArtsButton.getText()

    val feedbackButton = Button(
        name = "💬 Обратная связь",
        chatId = chatId
    )
    KeyboardNames.generalMenu["feedback"] = feedbackButton.getText()

    val aboutButton = Button(
        name = "ℹ️ О нас",
        chatId = chatId
    )
    KeyboardNames.generalMenu["about"] = aboutButton.getText()

    return mutableListOf(
        mutableListOf(accountButton, settingsButton),
        mutableListOf(collectionsButton, miniGamesButton),
        mutableListOf(predictionsButton, memesButton),
        mutableListOf(testsButton, pixelArtsButton),
        mutableListOf(feedbackButton, aboutButton),
    )
}