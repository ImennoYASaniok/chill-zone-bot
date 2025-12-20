package core.messageClasses

import core.globalStates

object cI {
    var chatId: Long = 0L
}

object ReplyList {

    var replyGeneralMenu = getReplyGeneralMenu(cI.chatId, globalStates.globalKeyboardIndex[cI.chatId] ?: 0)
    var replySettings = getReplySettings(cI.chatId, globalStates.globalKeyboardIndex[cI.chatId] ?: 0)
    var replyAccount = getReplyAccount(cI.chatId, globalStates.globalKeyboardIndex[cI.chatId] ?: 0)
    var replyFeedback = getReplyFeedback(cI.chatId, globalStates.globalKeyboardIndex[cI.chatId] ?: 0)
}