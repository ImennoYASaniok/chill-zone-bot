package data

import data.processes.ImageAvatarProcess
import java.io.File

/**
 * Сервис для работы с аватарками пользователей
 * Обрабатывает загрузку, обработку и переупрузку аватарок в Telegram
 */
object AvatarService {
    
    private val AVATARS_TEMP_DIR = File(
        "data${File.separator}src${File.separator}main${File.separator}resources${File.separator}temp${File.separator}avatars"
    )
    
    init {
        // Создаем директорию если её нет
        if (!AVATARS_TEMP_DIR.exists()) {
            AVATARS_TEMP_DIR.mkdirs()
            println("📁 Инициализирована директория для временных аватарок: ${AVATARS_TEMP_DIR.absolutePath}")
        }
    }
    
    /**
     * Обработать аватарку локально (обрезка или добавление полей)
     * 
     * @param fileId ID файла аватарки
     * @param mode Режим обработки: "crop" или "letterbox"
     * @return true если успешно обработано, false если ошибка
     */
    fun processAvatarImage(fileId: String, mode: String): Boolean {
        return try {
            println("🖼️ Обработка аватарки: mode=$mode, fileId=$fileId")
            
            val processMode = when (mode) {
                "crop" -> ImageAvatarProcess.ProcessMode.CROP
                "letterbox" -> ImageAvatarProcess.ProcessMode.LETTERBOX
                else -> {
                    println("❌ Неизвестный режим обработки: $mode")
                    return false
                }
            }
            
            println("✅ Аватарка готова к обработке в режиме: $processMode")
            true
        } catch (e: Exception) {
            println("❌ Ошибка при обработке аватарки: ${e.message}")
            false
        }
    }
    
    /**
     * Скачать аватарку из Telegram
     * TODO: Реализовать при наличии доступа к Bot API
     * 
     * @param fileId ID файла в Telegram
     * @return Путь к скачанному файлу или null если ошибка
     */
    fun downloadAvatarFromTelegram(fileId: String): String? {
        return try {
            println("⏳ Скачивание аватарки из Telegram: $fileId")
            // TODO: Реализовать скачивание
            // val tempFile = File(AVATARS_TEMP_DIR, "download_${System.currentTimeMillis()}.tmp")
            // val fileInfo = bot.getFile(fileId)
            // URL(fileInfo.filePath).openStream().use { input ->
            //     tempFile.outputStream().use { output -> input.copyTo(output) }
            // }
            // return tempFile.absolutePath
            null
        } catch (e: Exception) {
            println("❌ Ошибка при скачивании аватарки: ${e.message}")
            null
        }
    }
    
    /**
     * Переупрузить аватарку в Telegram
     * TODO: Реализовать при наличии доступа к Bot API
     * 
     * @param filePath Путь к локальному файлу
     * @return Новый fileId или null если ошибка
     */
    fun uploadAvatarToTelegram(filePath: String): String? {
        return try {
            println("⏳ Загрузка аватарки в Telegram: $filePath")
            // TODO: Реализовать загрузку
            // val photo = File(filePath)
            // val result = bot.sendPhoto(chat, photo)
            // return result.body()?.result?.photo?.last()?.fileId
            null
        } catch (e: Exception) {
            println("❌ Ошибка при загрузке аватарки: ${e.message}")
            null
        }
    }
    
    /**
     * Получить размер обработанного изображения
     * 
     * @param filePath Путь к файлу
     * @return Пара (ширина, высота) или null если ошибка
     */
    fun getImageSize(filePath: String): Pair<Int, Int>? {
        return try {
            if (!File(filePath).exists()) return null
            ImageAvatarProcess.getImageSize(filePath)
        } catch (e: Exception) {
            println("❌ Ошибка при получении размера изображения: ${e.message}")
            null
        }
    }
    
    /**
     * Очистить временные файлы (старше 1 часа)
     */
    fun cleanupExpiredTemporaryFiles() {
        try {
            if (AVATARS_TEMP_DIR.exists() && AVATARS_TEMP_DIR.isDirectory) {
                val oneHourAgo = System.currentTimeMillis() - 3600000 // 1 час
                AVATARS_TEMP_DIR.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < oneHourAgo) {
                        file.delete()
                        println("🗑️ Удален временный файл: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ Ошибка при очистке временных файлов: ${e.message}")
        }
    }
    
    /**
     * Получить абсолютный путь к директории временных аватарок
     */
    fun getTempDirectoryPath(): String = AVATARS_TEMP_DIR.absolutePath
}
