package core.messageClasses

import core.ReplyClass
import core.States
import core.handlers.handlerTicTacToe
import core.keyboards.KeyboardNames
import core.keyboards.getKeyboardTicTacToe
import core.utils.Logger

fun getReplyTicTacToe(): ReplyClass {
    val replyClass = ReplyClass(
        keyboard = getKeyboardTicTacToe(),
        globalCommand = "/tictactoe",
        command = KeyboardNames.generalMenu["tictactoe"],
        state = States.TicTacToe,
        stateBack = States.MiniGamesMenu,
        startFunc = ::handlerTicTacToe,
        urls = mutableListOf(),
        useFormat = false
    )
    Logger.info("replyClass", "Создан ReplyClass: $replyClass")
    return replyClass
}