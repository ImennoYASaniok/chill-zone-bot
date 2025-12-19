package core.messageClasses

import core.ReplyClass
import core.States
import core.handlers.handlerMiniGames
import core.keyboards.KeyboardNames
import core.keyboards.getKeyboardMiniGames
import core.utils.Logger

fun getReplyMiniGames(): ReplyClass {
    val replyClass = ReplyClass(
        keyboard = getKeyboardMiniGames(),
        globalCommand = "/minigames",
        command = KeyboardNames.generalMenu["miniGames"],
        state = States.MiniGamesMenu,
        stateBack = States.GeneralMenu,
        startFunc = ::handlerMiniGames,
        urls = mutableListOf("miniGames/MiniGames.png")
    )
    Logger.info("replyClass", "Создан ReplyClass: $replyClass")
    return replyClass
}