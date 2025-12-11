package core

import java.io.File

fun get_message(path: String): String {
    val text = File(path).readText()
    return text
}