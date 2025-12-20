package ChillZoneBot.core.src.main.kotlin.ReplyClass

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import kotlin.String

open class Button(var name: String, val func: (() -> String?)? = null) {
    open fun getButton(): KeyboardButton {
        return KeyboardButton(getText())
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

class BoolButton(
    name: String,
    var flag: Boolean = true,
    val funcTrue: (() -> String?)? = null,
    val funcFalse: (() -> String?)? = null
) : Button(name) {
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

class ChooseButton(
    name: String,
    val list: Map<String, (() -> String?)?>,
    startIndex: Int = 0
) : Button(name) {
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


class ReplyClass1(
    var keyboard: MutableList<MutableList<Button>>,
    val startCommand: String,
    val textMessage: String
) {
    var replyKeyboard = mutableListOf<List<KeyboardButton>>()

    fun getKeyboardReplyMarkup(): KeyboardReplyMarkup {
        return KeyboardReplyMarkup(
            keyboard = replyKeyboard,
            resizeKeyboard = true,
            oneTimeKeyboard = true
        )
    }

    init {
        update()
    }

    fun update() {
        replyKeyboard.clear()
        for (rowKeyboard in keyboard) {
            val rowReplyKeyboard = mutableListOf<KeyboardButton>()
            for (buttonKeyboard in rowKeyboard) {
                rowReplyKeyboard.add(buttonKeyboard.getButton())
                println(buttonKeyboard.getText())
            }
            replyKeyboard.add(rowReplyKeyboard)
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

    fun isNameButton(text: String): Boolean {
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
        val condIsNameButton: Boolean = isNameButton(text)
        var message: String? = null
        if (condIsNameButton) {
            message = detect(text)
        }
        if (text == textMessage || condIsNameButton) {
            update()
        }
        if (text == startCommand|| text == textMessage || condIsNameButton) {
            if (message == null) message = textMessage
            bot.sendMessage(chatId, text = message, replyMarkup = getKeyboardReplyMarkup())
        }

    }
}