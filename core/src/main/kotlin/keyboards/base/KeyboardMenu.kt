package keyboards.base

import ChillZoneBot.core.src.main.kotlin.InlineClass.BoolInlineButton
import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineButton
import ChillZoneBot.core.src.main.kotlin.InlineClass.ChooseInlineButton
import ChillZoneBot.core.src.main.kotlin.ReplyClass.BoolButton
import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ChooseButton
fun getPredictionKeyboard(): MutableList<MutableList<Button>> {
    val nextButton = Button("Следующее предсказание")
    val addButton = Button("Добавить предсказание")
    val myButton = Button("Мои предсказания")
    val backButton = Button("Назад")

    return mutableListOf(
        mutableListOf(nextButton),
        mutableListOf(addButton),
        mutableListOf(myButton),
        mutableListOf(backButton)
    )
}

fun getRarityKeyboard(): MutableList<MutableList<Button>> {
    val commonButton = Button("Обычное")
    val rareButton = Button("Редкое")
    val epicButton = Button("Эпическое")
    val legendaryButton = Button("Легендарное")
    val backButton = Button("Назад")

    return mutableListOf(
        mutableListOf(commonButton, rareButton),
        mutableListOf(epicButton, legendaryButton),
        mutableListOf(backButton)
    )
}