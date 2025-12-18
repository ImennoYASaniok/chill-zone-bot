package core.utils

import core.utils.Message.setTag
import java.io.File

val TAG_TITLE_1 = setTag("#")
val TAG_TITLE_2 = setTag("##")
val TAG_WARNING_1 = setTag(">!")
val TAG_WARNING_2 = setTag(">!!")
val TAG_REFERENCE = setTag(">i")

object TagFormator {
    fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    fun setTag(string: String, typeTag: String, nameArg: String? = null, arg: String? = null):String {
        if (nameArg == null) {
            return "<$typeTag>${escapeHtml(string)}</$typeTag>"
        }
        else {
            return "<$typeTag $nameArg=$arg>${escapeHtml(string)}</$typeTag>"
        }
    }

    fun setBold(string: String): String {
        return setTag(string, "b")
        // b или strong
    }

    fun setItalic(string: String): String {
        return setTag(string, "i")
        // i или em
    }

    fun setBoldItalic(string: String): String {
        return setBold(setItalic(string))
    }

    fun setHidden(string: String): String {
        return setTag(string, "tg-spoiler")
    }

    fun setUnderlined(string: String): String {
        return setTag(string, "u")
        // u или ins
    }

    fun setStrike(string: String): String {
        return setTag(string, "s")
        // s, strike или del
    }

    fun setMonospaced(string: String): String {
        return setTag(string, "code")
    }

    fun setLink(string: String, link: String): String {
        return setTag(string, typeTag = "a", nameArg = "href", arg = link)
    }

    fun setBlockquote(string: String): String {
        return setTag(string, "blockquote")
    }

    fun setTitle1(string: String, typeTag: Int): String {
        var resString = string
        resString = resString.replace(TAG_TITLE_1, "")
        if (typeTag == 1) {
            resString = setBold(resString.uppercase())
        }
        else if (typeTag == 2) {
            TODO()
        }
        else if (typeTag == 3) {
            resString = Fonts.formatText(resString)

        }
        return resString
    }

    fun setTitle2(string: String): String {
        var resString = string
        resString = resString.replace(TAG_TITLE_2, "")
        resString = setUnderlined(resString.capitalize())
        return resString
    }

    fun setWarning1(string: String): String {
        var resString = "⚠️ $string"
        resString = resString.replace(TAG_WARNING_1, "")
        resString = setTag(resString, "blockquote")
        return resString
    }

    fun setWarning2(string: String): String {
        var resString = "❗ $string"
        resString = resString.replace(TAG_WARNING_2, "")
        resString = setTag(resString, "blockquote")
        return resString
    }

    fun setReference(string: String): String {
        var resString = "ℹ️ $string"
        resString = resString.replace(TAG_REFERENCE, "")
        resString = setTag(resString, "blockquote")
        return resString
    }
}

object Message {
    fun formatTextMessage(text: String): String {
        val formatedText = text.split("\n").joinToString("\n") { line ->
            val resLine = formatLine(line)
            resLine
        }
        return formatedText
    }

    fun formatLine(line: String): String {
        var resLine = line
        if (lineIsEmpty(line)) {
            return resLine
        }
        else if (searchTag(TAG_TITLE_1, resLine)) {
            resLine = TagFormator.setTitle1(resLine, 1)
        }
        else if (searchTag(TAG_TITLE_2, resLine)) {
            resLine = TagFormator.setTitle2(resLine)
        }
        else if (searchTag(TAG_WARNING_1, resLine)) {
            resLine = TagFormator.setWarning1(resLine)
        }
        else if (searchTag(TAG_WARNING_2, resLine)) {
            resLine = TagFormator.setWarning2(resLine)
        }
        else if (searchTag(TAG_REFERENCE, resLine)) {
            resLine = TagFormator.setReference(resLine)
        }

        return resLine
    }

    fun lineIsEmpty(line: String): Boolean {
        return line.isEmpty() || line.isBlank()
    }

    fun searchTag(tag: String, string: String): Boolean {
        if (tag.isEmpty()) return false
        return tag == string.lowercase().slice((0..<tag.length))
    }

    fun setTag(tag: String): String {
        return "$tag "
    }

    fun setEscapedString(string: String): String {
        return string.replace("\n", "\\n")
            .replace("\t", "\\t")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
            .replace("\\", "\\\\")
    }

    fun setPlaceHolders(text: String, placeholder: Map<String, Any>? = null): String {
        var resText = text
        if (placeholder != null) {
            placeholder.forEach { (key, value) ->
                if (value is String) {
                    resText = resText.replace("$$key", value)
                }
            }
        }

        return resText
    }

    fun getMessage(path: String, kwargs: Map<String, Any>? = null): String {
        var text = File("core/messageAssets/$path").readText()

        text = formatTextMessage(text)
        text = setPlaceHolders(text, kwargs)

        return text
    }
}