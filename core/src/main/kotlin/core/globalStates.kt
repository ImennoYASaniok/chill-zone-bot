package core

import BooksPart.BooksStates
import KinoPart.KinoStates
import core.handlers.Account
import core.messageClasses.ReplyList

object globalStates {
    var globalAccounts = mutableMapOf<Long, Account>()
    var globalBooks = mutableMapOf<Long, BooksStates>()
    var globalKino = mutableMapOf<Long, KinoStates>()
    var globalStates = mutableMapOf<Long, States>()
    var globalRepliesList = mutableMapOf<Long, ReplyList>()
    var globalKeyboardIndex = mutableMapOf<Long, Int>()
}