package core.messageClasses

import com.github.kotlintelegrambot.entities.TelegramFile
import java.io.File


fun getPodbImg(): TelegramFile {
    return TelegramFile.ByFile(File("./collections/collections.png"))
}
fun getFilmImg(): TelegramFile {
    return TelegramFile.ByFile(File("./collections/films/Films.png"))
}
fun getSerialImg(): TelegramFile {
    return TelegramFile.ByFile(File("./collections/serials/Serials.png"))
}
fun getBooksImg(): TelegramFile {
    return TelegramFile.ByFile(File("./collections/books/Books.png"))
}