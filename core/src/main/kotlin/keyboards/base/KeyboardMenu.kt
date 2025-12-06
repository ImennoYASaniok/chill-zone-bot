package ChillZoneBot.core.src.main.kotlin.keyboards.base

import ChillZoneBot.core.src.main.kotlin.ReplyClass.BoolButton
import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ChooseButton

fun getKeyboardMenu(): MutableList<MutableList<Button>> {
    val button1 = Button("Button")
    val button2 = ChooseButton(
        name = "ChooseButton",
        list = mapOf("name1" to null, "name2" to null, "name3" to null),
        startIndex = 0
    )
    val button3 = BoolButton("BoolButton", false)

    val memesButton = Button("Мемы")

    return mutableListOf(
        mutableListOf(button1, button2),
        mutableListOf(button3),
        mutableListOf(memesButton)
    )
}