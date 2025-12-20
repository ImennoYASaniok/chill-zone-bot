package core.messageClasses

import core.ReplyClass
import core.States
import core.handlers.handlerMenu
import core.keyboards.getKeyboardGeneralMenu
import core.utils.Logger

fun getReplyGeneralMenu(chatId: Long, currPage: Int): ReplyClass {
    val replyClass = ReplyClass(
        keyboard = getKeyboardGeneralMenu(chatId),
        globalCommand = "/start",
        state = States.GeneralMenu,
        startFunc = ::handlerMenu,
        urls = mutableListOf("generalMenu/GeneralMenu.png"),
        chatId = chatId,
        currPage = currPage
    )
    Logger.info("replyClass", "Создан ReplyClass: $replyClass")

    return replyClass
}