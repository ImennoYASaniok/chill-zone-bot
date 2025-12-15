package core

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import core.utils.Logger
import core.utils.Url

import java.io.File
import java.io.FileNotFoundException

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
    var urls: MutableList<String> = mutableListOf(),
    useFormat: Boolean = true,
    thresholdCountButtons: Int = 6
) {
    val groupKeyboards = mutableListOf<MutableList<MutableList<Button>>>()
    var replyKeyboard = mutableListOf<List<KeyboardButton>>()

    val nameNextButton = "➡️ Дальше"
    val nameBackButton = "⬅️ Обратно"

    var currIndKeyboard = 0
    var maxCountKeyboard = 0

    var typeImgUrl = ""

    val PARSE_MODE = ParseMode.HTML

    init {
        if (keyboard == mutableListOf<MutableList<Button>>() || keyboard == mutableListOf<MutableList<Button>>(mutableListOf<Button>())) {
            throw IndexOutOfBoundsException("У $this reply клавиатура без кнопок ")
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

        var tempTypeImgUrl = ""
        for (url in urls) {
            if (tempTypeImgUrl == Url.determineTypeUrl(url) || tempTypeImgUrl == "") {
                tempTypeImgUrl = Url.determineTypeUrl(url)
            } else {
                throw FileNotFoundException("У $this В списке картинок есть разные типы url (одновременно и путь, и ссылка), либо есть некорректные url")
            }
        }
        typeImgUrl = tempTypeImgUrl
        
        if (typeImgUrl == "path") {
            urls = urls.map { url ->
                Url.formatUrl(url)
            }.toMutableList()
        }
    }

    override fun toString(): String {
        return "ReplyClass(startCommand='$startCommand')"
    }

    fun formatKeyboard() {
        var ind = 0
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
                // println(buttonKeyboard.getText())
            }
            replyKeyboard.add(rowReplyKeyboard)
        }
        // println()
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

    fun sendDefaultMessage(message: String, bot: Bot, chatId: ChatId) {
        bot.sendMessage(
            chatId = chatId,
            text = message,
            parseMode = PARSE_MODE,
            replyMarkup = getKeyboardReplyMarkup()
        )
    }

    fun sendImgMessage(message: String, bot: Bot, chatId: ChatId, url: String) {
        bot.sendPhoto(
            chatId = chatId,
            photo = (if (typeImgUrl == "path") TelegramFile.ByFile(File(url)) else TelegramFile.ByUrl(url)),
            caption = message,
            parseMode = PARSE_MODE,
            replyMarkup = getKeyboardReplyMarkup()
        )
    }

    fun sendImgsMessage(message: String, bot: Bot, chatId: ChatId, urls: MutableList<String>) {
        val media = mutableListOf<InputMediaPhoto>()

        for (ind in 0 until urls.size) {
            if (ind == 0) {
                media.add(InputMediaPhoto(
                    media = (if (typeImgUrl == "path") TelegramFile.ByFile(File(urls[ind])) else TelegramFile.ByUrl(urls[ind])),
                    caption = message,
                    parseMode = "Markdown"
                ))
            }
            else {
                media.add(InputMediaPhoto(
                    media = (if (typeImgUrl == "path") TelegramFile.ByFile(File(urls[ind])) else TelegramFile.ByUrl(urls[ind])),
                ))
            }

        }

        bot.sendMediaGroup(
            chatId = chatId,
            mediaGroup = MediaGroup.from(*media.toTypedArray()),
        )
        sendDefaultMessage(message, bot, chatId)
    }

    fun sendAnimationMessage(message: String, bot: Bot, chatId: ChatId, url: String) {
        if (typeImgUrl == "path") {
            val file = File(url)
            println("File exists: ${file.exists()}")
            println("File path: ${file.absolutePath}")
            println("File size: ${file.length()} bytes")
            if (!file.exists()) {
                Logger.error("Miss file", "Файла по пути ${file.absolutePath} не существует")
                return
            }

            val result = bot.sendAnimation(
                chatId = chatId,
                animation = (if (typeImgUrl == "path") TelegramFile.ByFile(File(url)) else TelegramFile.ByUrl(url)),
                caption = message,
                parseMode = PARSE_MODE,
                replyMarkup = getKeyboardReplyMarkup()
            )
        }
    }

    fun sendFileMessage(message: String, bot: Bot, chatId: ChatId, url: String) {
        bot.sendDocument(
            chatId = chatId,
            document = (if (typeImgUrl == "path") TelegramFile.ByFile(File(url)) else TelegramFile.ByUrl(url)),
            caption = message,
            parseMode = PARSE_MODE,
            replyMarkup = getKeyboardReplyMarkup()
        )
    }

    fun sendVideoMessage(message: String, bot: Bot, chatId: ChatId, url: String) {
        bot.sendVideo(
            chatId = chatId,
            video = (if (typeImgUrl == "path") TelegramFile.ByFile(File(url)) else TelegramFile.ByUrl(url)),
            caption = message,
            parseMode = PARSE_MODE,
            replyMarkup = getKeyboardReplyMarkup()
        )
    }

    fun sendAudioMessage(message: String, bot: Bot, chatId: ChatId, url: String) {
        bot.sendAudio(
            chatId = chatId,
            audio = (if (typeImgUrl == "path")
                TelegramFile.ByFile(File(url))
            else
                TelegramFile.ByUrl(url)),
            title = "Title",
            performer = "Performer",
            duration = 180,
            replyMarkup = getKeyboardReplyMarkup()
        )
        sendDefaultMessage(message, bot, chatId)
    }

    fun main(text: String, bot: Bot, chatId: ChatId, state: States) {
        if (currState == state) {
            val conditionIsNameButton: Boolean = isNameButton(text)
            var message: String? = null

            if (conditionIsNameButton) {
                message = detect(text)
            }
            if (text == message || conditionIsNameButton) {
                update()
            }

            if (text == startCommand || text == message || conditionIsNameButton) {
                if (message == null) {
                    message = startFunc()
                    if (urls.size == 1) {
                        if (Url.determineTypeFile(urls[0]) in Url.TYPE_IMGS) {
                            sendImgMessage(message, bot, chatId, urls[0])
                        }
                        else if (Url.determineTypeFile(urls[0]) == Url.TYPE_ANIMATION) {
                            sendAnimationMessage(message, bot, chatId, urls[0])
                        }
                        else if (Url.determineTypeFile(urls[0]) in Url.TYPE_VIDEOS) {
                            sendVideoMessage(message, bot, chatId, urls[0])
                        }
                        else if (Url.determineTypeFile(urls[0]) in Url.TYPE_AUDIO) {
                            sendAudioMessage(message, bot, chatId, urls[0])
                        }
                        else {
                            sendFileMessage(message, bot, chatId, urls[0])
                        }
                    }
                    else if (urls.size > 1) {
                        if (Url.determineTypeFile(urls[0]) in Url.TYPE_IMGS) {
                            sendImgsMessage(message, bot, chatId, urls)
                        }
                        else {
                            for (imgUrl in urls) {
                                if (Url.determineTypeFile(imgUrl) == Url.TYPE_ANIMATION) {
                                    sendAnimationMessage(message, bot, chatId, imgUrl)
                                }
                                else if (Url.determineTypeFile(imgUrl) in Url.TYPE_VIDEOS) {
                                    sendVideoMessage(message, bot, chatId, imgUrl)
                                }
                                else if (Url.determineTypeFile(imgUrl) in Url.TYPE_AUDIO) {
                                    sendAudioMessage(message, bot, chatId, imgUrl)
                                }
                                else {
                                    sendFileMessage(message, bot, chatId, imgUrl)
                                }
                            }
                        }
                    }
                    else {
                        sendDefaultMessage(message, bot, chatId)
                        Logger.info("bot", "Отправлено дефолт сообщение по команде '$text'")
                    }
                }
                else {
                    sendDefaultMessage(message, bot, chatId)
                    Logger.info("bot", "Отправлено дефолт сообщение по команде $text")
                }
            }
        }
    }
}