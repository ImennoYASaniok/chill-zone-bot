package core.handlers

import core.utils.Message

fun handlerMiniGames(kwargs: Map<String, Any>? = null): String {
    return Message.getMessage("miniGames/MiniGames.txt")
}