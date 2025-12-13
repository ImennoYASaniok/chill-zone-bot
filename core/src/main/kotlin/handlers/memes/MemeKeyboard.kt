package ChillZoneBot.core.src.main.kotlin.handlers.memes

import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button

fun getMemeKeyboard(): MutableList<MutableList<Button>> {
    val nextButton = Button("Следующий мем")
    val addButton = Button("Добавить мем")
    val likeButton = Button("👍")
    val dislikeButton = Button("👎")
    val favAddButton = Button("В избранное")
    val favListButton = Button("Избранное")
    val favRemoveButton = Button("Удалить из избранного")
    val backButton = Button("Назад")

    return mutableListOf(
        mutableListOf(nextButton),
        mutableListOf(addButton),
        mutableListOf(likeButton, dislikeButton),
        mutableListOf(favAddButton, favRemoveButton),
        mutableListOf(favListButton),
        mutableListOf(backButton)
    )
}