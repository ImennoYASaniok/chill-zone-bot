package core.keyboards

import core.Button
import core.States

fun getKeyboardFeedback(): MutableList<MutableList<Button>> {
    val backButton = Button(
        name = "⬅️ Назад в меню",
        stateChange = States.GeneralMenu
    )

    return mutableListOf(
        mutableListOf(backButton)
    )
}
