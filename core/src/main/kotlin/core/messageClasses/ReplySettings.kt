package core.messageClasses

import core.utils.Logger
import core.ReplyClass
import core.handlers.handlerSettings
import core.keyboards.KeyboardNames
import core.keyboards.getKeyboardSettings

fun getReplySettings(): ReplyClass {
    val replyClass = ReplyClass(
        keyboard = getKeyboardSettings(),
        globalCommand = "/settings",
        command = KeyboardNames.generalMenu["settings"],
        startFunc = ::handlerSettings,
        urls = mutableListOf("settings/Settings.png")
    )
    Logger.info("replyClass", "Создан ReplyClass: $replyClass")

    return replyClass
}