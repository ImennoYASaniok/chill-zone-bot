package com.github.kotlintelegrambot.entities

import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

data class ReplyKeyboardMarkup(
    val keyboard: List<List<KeyboardButton>>,
    val resizeKeyboard: Boolean? = null,
    val oneTimeKeyboard: Boolean? = null,
    val selective: Boolean? = null,
    val inputFieldPlaceholder: String? = null,
    val isPersistent: Boolean? = null
) : ReplyMarkup