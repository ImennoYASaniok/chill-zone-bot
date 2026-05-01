package data.services

import data.processes.ImageAvatarProcess
import java.io.File
import java.net.URL

/**
 * Сервис для работы с аватарками пользователей
 * Обрабатывает загрузку, обработку и переупрузку аватарок в Telegram
 */
object AvatarService {
    
    private val AVATARS_TEMP_DIR = File(
        "data${File.separator}src${File.separator}main${File.separator}resources${File.separator}temp${File.separator}avatars"
    )
    
    // Получаем значение переменной окружения для Telegram Bot API
    private val botToken = io.github.cdimascio.dotenv.dotenv()["BOT_TOKEN"] 
        ?: io.github.cdimascio.dotenv.dotenv()["TELEGRAM_TOKEN"] 
        ?: ""
    
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
     * @param fileId ID файла аватарки (Telegram file ID)
     * @param mode Режим обработки: "crop" или "letterbox"
     * @return Путь к обработанному файлу или null если ошибка
     */
    fun processAvatarImage(fileId: String, mode: String): String? {
        return try {
            println("🖼️ Обработка аватарки: mode=$mode, fileId=$fileId")
            
            val processMode = when (mode) {
                "crop" -> ImageAvatarProcess.ProcessMode.CROP
                "letterbox" -> ImageAvatarProcess.ProcessMode.LETTERBOX
                else -> {
                    println("❌ Неизвестный режим обработки: $mode")
                    return null
                }
            }
            
            // Скачиваем аватарку из Telegram
            val downloadedPath = downloadAvatarFromTelegram(fileId) ?: return null
            
            // Финальный путь для обработанного файла
            val finalPath = downloadedPath.replace(".jpg", "_final.jpg")
            
            // Обрабатываем изображение и сохраняем в финальное место
            val success = ImageAvatarProcess.processImage(downloadedPath, finalPath, processMode)
            
            if (success) {
                val downloadedFile = File(downloadedPath)
                val finalFile = File(finalPath)
                
                // Удаляем исходный загруженный файл
                try {
                    downloadedFile.delete()
                } catch (e: Exception) {
                    println("⚠️ Не удалось удалить временный файл: ${e.message}")
                }
                
                if (finalFile.exists()) {
                    println("✅ Аватарка обработана в режиме: $processMode -> ${finalFile.absolutePath}")
                    return finalFile.absolutePath
                }
                null
            } else {
                println("❌ Ошибка при обработке изображения")
                // Удаляем загруженный файл при ошибке обработки
                try {
                    File(downloadedPath).delete()
                } catch (e: Exception) {
                    println("⚠️ Не удалось удалить временный файл: ${e.message}")
                }
                null
            }
        } catch (e: Exception) {
            println("❌ Ошибка при обработке аватарки: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Скачать аватарку из Telegram
     * 
     * @param fileId ID файла в Telegram
     * @return Путь к скачанному файлу или null если ошибка
     */
    fun downloadAvatarFromTelegram(fileId: String): String? {
        return try {
            if (botToken.isBlank()) {
                println("❌ BOT_TOKEN не установлен, не могу скачать файл из Telegram")
                return null
            }
            
            println("⏳ Скачивание аватарки из Telegram: $fileId")
            
            // Получаем информацию о файле через Telegram Bot API
            val getFileUrl = "https://api.telegram.org/bot$botToken/getFile?file_id=$fileId"
            val getFileResponse = URL(getFileUrl).readText()
            
            // Парсим JSON ответ (простое парсирование)
            val filePath = getFileResponse.let {
                val regex = """"file_path":"([^"]+)"""".toRegex()
                regex.find(it)?.groupValues?.get(1)
            } ?: return null
            
            // Скачиваем файл
            val downloadUrl = "https://api.telegram.org/file/bot$botToken/$filePath"
            val tempFile = File(AVATARS_TEMP_DIR, "avatar_${System.currentTimeMillis()}.jpg")
            
            URL(downloadUrl).openStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            println("✅ Аватарка скачана: ${tempFile.absolutePath}")
            tempFile.absolutePath
        } catch (e: Exception) {
            println("❌ Ошибка при скачивании аватарки: ${e.message}")
            e.printStackTrace()
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
