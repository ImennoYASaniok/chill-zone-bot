package core.messageClasses

import core.ReplyClass
import core.handlers.handlerMenu
import core.keyboards.getKeyboardGeneralMenu
import core.utils.Logger

fun getReplyGeneralMenu(): ReplyClass {
    val replyClass = ReplyClass(
        keyboard = getKeyboardGeneralMenu(),
        globalCommand = "/start",
        startFunc = ::handlerMenu,
        urls = mutableListOf("generalMenu/GeneralMenu.png")
    )
    Logger.info("replyClass", "Создан ReplyClass: $replyClass")

    return replyClass
}