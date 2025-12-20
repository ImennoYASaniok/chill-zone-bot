package core.keyboards

import core.Button
import core.ChooseButton
import core.BoolButton

import kotlin.collections.mutableListOf

fun getKeyboardSettings(chatId: Long): MutableList<MutableList<Button>> {
    val decorationButton = ChooseButton(
        name = "Оформление",
        list = mapOf(
            "тип 1" to null,
            "тип 2" to null
        ),
        chatId = chatId
    )

    val showImgButton = BoolButton(
        name = "Показывать картинки сообщений",
        chatId = chatId
    )

    return mutableListOf(
        mutableListOf(decorationButton, showImgButton),
    )
}