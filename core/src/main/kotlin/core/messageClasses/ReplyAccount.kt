package core.messageClasses

import core.utils.Logger
import core.ReplyClass
import core.States
import core.handlers.handlerAccount
import core.keyboards.KeyboardNames
import core.keyboards.getKeyboardEmpty

fun getReplyAccount(): ReplyClass {
    val replyClass = ReplyClass(
        keyboard = getKeyboardEmpty(),
        globalCommand = "/account",
        command = KeyboardNames.generalMenu["account"],
        state = States.Account,
        stateBack = States.GeneralMenu,
        startFunc = ::handlerAccount,
        urls = mutableListOf("account/Account.png")
    )
    Logger.info("replyClass", "Создан ReplyClass: $replyClass")

    return replyClass
}