package core.keyboards

import core.Button
import core.States

fun getKeyboardMiniGames(): MutableList<MutableList<Button>> {
    val ttt = Button(
        name = "❌⭕️ Крестики-нолики",
        stateChange = States.TicTacToe
    )
    KeyboardNames.generalMenu["tictactoe"] = ttt.getText()

    return mutableListOf(
        mutableListOf(ttt)
    )
}