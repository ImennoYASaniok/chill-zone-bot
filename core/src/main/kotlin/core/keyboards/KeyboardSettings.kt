package core.keyboards

import core.Button
import core.ChooseButton
import core.BoolButton

import kotlin.collections.mutableListOf

fun getKeyboardSettings(): MutableList<MutableList<Button>> {
    val decorationButton = ChooseButton(
        name = "Оформление",
        list = mapOf(
            "тип 1" to null,
            "тип 2" to null
        )
    )

    val showImgButton = BoolButton(
        name = "Показывать картинки сообщений"
    )

    return mutableListOf(
        mutableListOf(decorationButton, showImgButton),
    )
}