package core.keyboards

import core.Button
import core.ChooseButton

import kotlin.collections.mutableListOf

fun getKeyboardSettings(): MutableList<MutableList<Button>> {
    val decorationButton = ChooseButton(
        name = "Оформление",
        list = mapOf(
            "тип 1" to null,
            "тип 2" to null,
            "тип 3" to null
        )
    )

    val languageButton = ChooseButton(
        name = "Язык",
        list = mapOf(
            "русс" to null,
            "англ" to null
        )
    )

    return mutableListOf(
        mutableListOf(decorationButton, languageButton),
    )
}