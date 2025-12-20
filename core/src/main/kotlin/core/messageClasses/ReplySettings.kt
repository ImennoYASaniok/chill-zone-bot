package core.messageClasses

import core.utils.Logger
import core.ReplyClass
import core.States
import core.handlers.handlerSettings
import core.keyboards.KeyboardNames
import core.keyboards.getKeyboardSettings

fun getReplySettings(chatId: Long, currPage: Int): ReplyClass {
    val replyClass = ReplyClass(
        keyboard = getKeyboardSettings(chatId),
        globalCommand = "/settings",
        command = KeyboardNames.generalMenu["settings"],
        state = States.Settings,
        stateBack = States.GeneralMenu,
        startFunc = ::handlerSettings,
        urls = mutableListOf("settings/Settings.png"),
        chatId = chatId,
        currPage = currPage
    )
    Logger.info("replyClass", "Создан ReplyClass: $replyClass")

    return replyClass
}