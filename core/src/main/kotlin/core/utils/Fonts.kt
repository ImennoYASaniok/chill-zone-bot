package core.utils

object Fonts {
    val fontBrackets = setPatternFont("[", "]")
    val fontBracketsSeparator = " "


    fun setPatternFont(firstExtraSym: String, lastExtraSym: String): MutableMap<Char, String> {
        val fontMap = mutableMapOf<Char, String>()

        for (indSym in 'А'.code..'Я'.code) {
            fontMap[indSym.toChar()] = "$firstExtraSym${indSym.toChar()}$lastExtraSym"
        }

        return fontMap
    }

    fun formatText(text: String): String {
        return text.map { sym ->
            fontBrackets[sym]
        }.joinToString(fontBracketsSeparator)
    }
}