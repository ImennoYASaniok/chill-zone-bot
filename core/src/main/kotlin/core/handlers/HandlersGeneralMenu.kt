package core.handlers

import core.utils.Message

fun handlerMenu(kwargs: Map<String, Any>? = null): String {
    return Message.getMessage("generalMenu/GeneralMenu.txt")
}