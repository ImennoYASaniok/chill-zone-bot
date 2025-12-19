package core.handlers

import core.utils.Message

import com.github.kotlintelegrambot.bot

fun handlerAccount(kwargs: Map<String, Any>? = null): String {
    return Message.getMessage("account/Account.txt", kwargs)
    // username в kwargs["username"], запихни её в бд
}