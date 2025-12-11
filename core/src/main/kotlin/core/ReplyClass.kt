package core

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import kotlin.collections.mutableListOf

open class Button(var name: String, val func: (() -> String?)? = null) {
    override fun toString(): String {
        return "Button(name='$name')"
    }

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

    override fun toString(): String {
        return "BoolButton(name='$name', flag=$flag)"
    }

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

    override fun toString(): String {
        return "BoolButton(name='$name', currentIndex=$currentIndex, list=$list)"
    }

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


class ReplyClass(
    var keyboard: MutableList<MutableList<Button>>,
    val startCommand: String,
    val startFunc: (() -> String),
    useFormat: Boolean = true,
    thresholdCountButtons: Int = 6
) {
    val groupKeyboards = mutableListOf<MutableList<MutableList<Button>>>()
    var replyKeyboard = mutableListOf<List<KeyboardButton>>()

    val nameNextButton = "➡️ Дальше"
    val nameBackButton = "⬅️ Обратно"

    var currIndKeyboard = 0
    var maxCountKeyboard = 0

    init {
        if (keyboard == mutableListOf<MutableList<Button>>() || keyboard == mutableListOf<MutableList<Button>>(mutableListOf<Button>())) {
            throw IndexOutOfBoundsException("Клавиатура без кнопок")
        }

        if (useFormat) {
            if (keyboard.sumOf { buttons -> buttons.size } > thresholdCountButtons) {
                formatKeyboard()
            }
            else {
                groupKeyboards.add(keyboard)
            }
        }
        maxCountKeyboard = groupKeyboards.size
    }

    fun formatKeyboard() {
        var ind: Int = 0
        var indCol = 0
        val maxCountCol = 2
        var indRow = 0
        val maxCountRow = 3
        var indButton = 0

        val countAllButtons = keyboard.sumOf { buttons -> buttons.size }
        val preformattedButtons = mutableListOf<Button>()
        for (buttons in keyboard) {
            for (button in buttons) {
                preformattedButtons.add(button)

                if (ind >= countAllButtons - 1 && indButton <= maxCountCol * maxCountRow - 1 - 1) {
                    preformattedButtons.add(getBackButton())
                }
                else if (indButton == maxCountCol * maxCountRow - 1 - 1) {
                    preformattedButtons.add(getNextButton())
                }

                indButton++
                ind++

                indButton %= maxCountCol * maxCountRow
            }
        }
        ind = 0
        indButton = 0

        val tempKeyboard = mutableListOf<MutableList<Button>>()
        val tempRow = mutableListOf<Button>()
        for (button in preformattedButtons) {
            println("$button, $indRow $indCol")

            tempRow.add(button)

            if (indCol == maxCountCol - 1 || ind >= preformattedButtons.size - 1) {
                tempKeyboard.add(tempRow.toMutableList())
                tempRow.clear()
                if (indRow == maxCountRow - 1 || ind >= preformattedButtons.size - 1) {
                    groupKeyboards.add(tempKeyboard.toMutableList())
                    tempKeyboard.clear()
                }
                indRow++
            }
            indCol++
            ind++

            indRow %= maxCountRow
            indCol %= maxCountCol
        }
        println("groupKeyboards:")
        for (currKeyboard in groupKeyboards) {
            println(currKeyboard)
        }

        println()

        setCurrKeyboard()
    }

    fun getNextButton(): Button {
        return Button(name = nameNextButton, func = ::incrementCurrIndKeyboard)
    }

    fun getBackButton(): Button {
        return Button(name = nameBackButton, func = ::incrementCurrIndKeyboard)
    }

    fun incrementCurrIndKeyboard(): Nothing? {
        currIndKeyboard++
        currIndKeyboard %= maxCountKeyboard
        setCurrKeyboard()
        return null
    }

    fun setCurrKeyboard() {
        keyboard = groupKeyboards[currIndKeyboard]
    }

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
        for (buttons in keyboard) {
            for (button in buttons) {
                if (button.getText() == text) {
                    checkVal = true
                }
            }
        }
        return checkVal
    }

    fun main(text: String, bot: Bot, chatId: ChatId, state: States) {
        if (currState == state) {
            val condIsNameButton: Boolean = isNameButton(text)
            var message: String? = null

            if (condIsNameButton) {
                message = detect(text)
            }
            if (text == message || condIsNameButton) {
                update()
            }
            if (text == startCommand || text == message || condIsNameButton) {
                if (message == null) message = startFunc()
                bot.sendMessage(chatId, text = message, replyMarkup = getKeyboardReplyMarkup())
            }
        }
    }
}