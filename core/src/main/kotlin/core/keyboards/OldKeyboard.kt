package keyboards.base

import ChillZoneBot.core.src.main.kotlin.InlineClass.BoolInlineButton
import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineButton
import ChillZoneBot.core.src.main.kotlin.InlineClass.ChooseInlineButton
import ChillZoneBot.core.src.main.kotlin.ReplyClass.BoolButton
import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ChooseButton

import kotlin.collections.mutableListOf

fun detectFalse(): String {
    return "выбрано false в BoolButton"
}

fun openMemes(): String {
    return "Открываю мемы..."
}

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

    val memeButton = Button(
        name = "Мемы"
    )

    return mutableListOf(
        mutableListOf(button1, button2),
        mutableListOf(button3),
        mutableListOf(memeButton)
    )
}

fun getInlineKeyboardMenu(): MutableList<MutableList<InlineButton>> {
    val button1 = InlineButton(
        name = "Button",
        callback = "callback1"
    )
    val button2 = ChooseInlineButton(
        name = "ChooseButton",
        list = mapOf(
            "name1" to null,
            "name2" to null,
            "name3" to null
        ),
        startIndex = 0,
        callback = "callback2"
    )
    val button3 = BoolInlineButton(
        name = "BoolButton",
        funcFalse = ::detectFalse,
        flag = false,
        callback = "callback3"
    )

    val memeButton = InlineButton(
        name = "Мемы",
        callback = "callback4"
    )

    return mutableListOf(
        mutableListOf(button1, button2),
        mutableListOf(button3),
        mutableListOf(memeButton)
    )
}