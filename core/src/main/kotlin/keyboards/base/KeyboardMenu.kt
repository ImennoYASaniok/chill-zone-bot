package ChillZoneBot.core.src.main.kotlin.keyboards.base.KeyboardMenu

import ChillZoneBot.core.src.main.kotlin.ReplyClass.BoolButton
import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ChooseButton

import ChillZoneBot.core.src.main.kotlin.handlers.base.handlersMenu.detectFalse

import kotlin.collections.mutableListOf

fun getKeyboardMenu(): MutableList<MutableList<Button>> {
    val button1 = Button(
        name = "Button"
    )
    val button2 = ChooseButton(
        name = "ChooseButton",
        list = mapOf(
            "name1" to null,
            "name2" to null,
            "name3" to null
        ),
        startIndex = 0
    )
    val button3 = BoolButton(
        name = "BoolButton",
        funcFalse = ::detectFalse,
        flag = false,
    )

    return mutableListOf(
        mutableListOf(button1, button2),
        mutableListOf(button3)
    )
}