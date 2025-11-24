package ChillZoneBot.core.src.main.kotlin.Buttons

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

open class Button(var name: String) {
    var button = KeyboardButton(name)
    open fun updateButton() {
        button = KeyboardButton(name)
    }
    open fun get_text(): String {
        return name
    }
    open fun changeName() {

    }
}

class BoolButton(name: String, var flag: Boolean = true) : Button(name) {
    override fun get_text(): String {
        return name + " $flag"
    }

    override fun changeName() {
        flag = !flag
        name = get_text()
        updateButton()
    }


}

class ChooseButton(name: String, var list: List<String>, startIndex: Int = 0) : Button(name) {
    var currentIndex: Int = startIndex
    override fun get_text(): String {
        return list[currentIndex]
    }

    override fun changeName() {
        name = get_text()
        updateButton()
        currentIndex++
        currentIndex %= list.size
    }

    init {
        changeName()
        currentIndex--
    }
}


class replyMarkup(var old_keyboard: MutableList<MutableList<Button>>) {
    var keyboard = mutableListOf<List<KeyboardButton>>()
    var replyKeyboardMarkup = KeyboardReplyMarkup(
        keyboard = keyboard,
        resizeKeyboard = true,
        oneTimeKeyboard = true
    )

    fun update() {
        keyboard.clear()
        for (i in old_keyboard) {
            val currentRow = mutableListOf<KeyboardButton>()
            for (j in i) {
                currentRow.add(j.button)
            }
            keyboard.add(currentRow)
        }
        replyKeyboardMarkup = KeyboardReplyMarkup(
            keyboard = keyboard,
            resizeKeyboard = true,
            oneTimeKeyboard = true
        )
    }

    init {
        update()
    }


}