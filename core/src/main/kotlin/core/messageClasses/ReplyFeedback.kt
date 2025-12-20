package core.messageClasses

import core.ReplyClass
import core.States
import core.handlers.FeedbackHandlers
import core.keyboards.getKeyboardFeedback

fun getReplyFeedback(): ReplyClass {
    return ReplyClass(
        keyboard = getKeyboardFeedback(),
        globalCommand = "/feedback",
        command = "💬 Обратная связь",
        state = States.FeedbackMenu,
        stateBack = States.GeneralMenu,
        startFunc = FeedbackHandlers::startFeedbackHandler
    )
}
