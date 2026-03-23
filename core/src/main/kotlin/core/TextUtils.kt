package core

fun normalizeList(text: String): List<String> {
    return text.split(";", ",", " ", "|")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

fun joinLines(parts: List<String>): String = parts.joinToString("\n")

fun formatBool(value: Boolean): String = if (value) "включено" else "выключено"
