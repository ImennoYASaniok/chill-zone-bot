package ChillZoneBot.core.src.main.kotlin.InlineClass

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import kotlin.String

open class InlineButton(var name: String, val callback: String, val func: (() -> String?)? = null) {
    open fun getInlineButton(): InlineKeyboardButton {
        return InlineKeyboardButton.CallbackData(getText(), callback)
    }

    open fun getText(): String {
        return name
    }

    open fun detect(): String? {
        var message: String? = null
        if (func != null) message = func.invoke()
        return message
    }
}

class BoolInlineButton(
    name: String,
    callback: String,
    var flag: Boolean = true,
    val funcTrue: (() -> String?)? = null,
    val funcFalse: (() -> String?)? = null
) : InlineButton(name,callback) {
    val nameTrue = "✅"
    val nameFalse = "❌"

    override fun getText(): String {
        return "[${if (flag) nameTrue else nameFalse}] $name"
    }

    override fun detect(): String? {
        var message: String? = null
        if (flag) {
            if (funcTrue != null) message = funcTrue.invoke()
        } else {
            if (funcFalse != null) message = funcFalse.invoke()
        }
        flag = !flag
        return message
    }
}

class ChooseInlineButton(
    name: String,
    callback: String,
    val list: Map<String, (() -> String?)?>,
    startIndex: Int = 0
) : InlineButton(name, callback) {
    var currentIndex: Int = startIndex

    override fun getText(): String {
        return "[${list.keys.toList()[currentIndex]}] $name"
    }

    override fun detect(): String? {
        var message: String? = null
        val currentFunc = list.values.toList()[currentIndex]
        if (currentFunc != null) message = currentFunc()
        currentIndex++
        currentIndex %= list.size
        return message
    }
}


class InlineClass(
    var keyboard: MutableList<MutableList<InlineButton>>,
    val startCommand: String,
    val textMessage: String
) {
    var inlineKeyboard = mutableListOf<List<InlineKeyboardButton>>()

    fun getKeyboardInlineMarkup(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.create(inlineKeyboard)
    }

    init {
        update()
    }

    fun update() {
        inlineKeyboard.clear()
        for (rowKeyboard in keyboard) {
            val rowInlineKeyboard = mutableListOf<InlineKeyboardButton>()
            for (buttonKeyboard in rowKeyboard) {
                rowInlineKeyboard.add(buttonKeyboard.getInlineButton())
                println(buttonKeyboard.getText())
            }
            inlineKeyboard.add(rowInlineKeyboard)
        }
        println()
    }

    fun detect(text: String): String? {
        for (indRow in keyboard.indices) {
            for (indCol in keyboard[indRow].indices) {
                if (keyboard[indRow][indCol].getText() == text) {
                    val message = keyboard[indRow][indCol].detect()
                    return message
                }
            }
        }
        return null
    }

    fun isNameInlineButton(text: String): Boolean {
        var checkVal = false
        for (indRow in keyboard.indices) {
            for (indCol in keyboard[indRow].indices) {
                if (keyboard[indRow][indCol].getText() == text) {
                    checkVal = true
                }
            }
        }
        return checkVal
    }

    fun main(text: String, bot: Bot, chatId: ChatId) {
        val condIsNameInlineButton: Boolean = isNameInlineButton(text)
        var message: String? = null
        if (condIsNameInlineButton) {
            message = detect(text)
        }
        if (text == textMessage || condIsNameInlineButton) {
            update()
        }
        if (text == startCommand|| text == textMessage || condIsNameInlineButton) {
            if (message == null) message = textMessage
            bot.sendMessage(chatId, text = message, replyMarkup = getKeyboardInlineMarkup())
        }

    }
}