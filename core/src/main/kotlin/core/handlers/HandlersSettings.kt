package core.handlers

import core.utils.Message

fun handlerSettings(kwargs: Map<String, Any>? = null): String {
    return Message.getMessage("settings/Settings.txt")
}