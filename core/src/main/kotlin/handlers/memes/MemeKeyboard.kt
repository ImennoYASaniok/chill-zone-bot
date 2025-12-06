package ChillZoneBot.core.src.main.kotlin.handlers.memes

import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button

fun getMemeKeyboard(): MutableList<MutableList<Button>> {
    val nextButton = Button("Следующий мем")
    val addButton = Button("Добавить мем")
    val backButton = Button("Назад")

    return mutableListOf(
        mutableListOf(nextButton),
        mutableListOf(addButton),
        mutableListOf(backButton)
    )
}