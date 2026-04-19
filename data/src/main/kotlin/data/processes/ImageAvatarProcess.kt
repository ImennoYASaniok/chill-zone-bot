package data.processes

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import java.awt.Graphics2D
import java.awt.Color

/**
 * Обработчик аватарок: обрезка или добавление черных границ
 */
object ImageAvatarProcess {
    
    /**
     * Режимы обработки изображения
     */
    enum class ProcessMode {
        CROP,      // Обрезать по меньшей стороне
        LETTERBOX  // Добавить черные границы
    }
    
    /**
     * Обработать изображение аватарки
     * 
     * @param inputPath Путь к исходному файлу
     * @param outputPath Путь для сохранения результата
     * @param mode Режим обработки (crop или letterbox)
     * @return true если успешно, false если ошибка
     */
    fun processImage(inputPath: String, outputPath: String, mode: ProcessMode): Boolean {
        return try {
            val inputFile = File(inputPath)
            if (!inputFile.exists()) {
                println("❌ Исходный файл не найден: $inputPath")
                return false
            }
            
            val img = ImageIO.read(inputFile) 
                ?: throw Exception("Не удалось прочитать изображение")
            
            val width = img.width
            val height = img.height
            
            val processedImg = when (mode) {
                ProcessMode.CROP -> cropImage(img, width, height)
                ProcessMode.LETTERBOX -> letterboxImage(img, width, height)
            }
            
            // Сохраняем в JPEG с высоким качеством
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            ImageIO.write(processedImg, "JPEG", outputFile)
            
            val resultSize = if (mode == ProcessMode.CROP) 
                minOf(width, height) 
            else 
                maxOf(width, height)
            
            println("✅ Изображение обработано: ${mode.name} до ${resultSize}x${resultSize}")
            true
        } catch (e: Exception) {
            println("❌ Ошибка при обработке изображения: ${e.message}")
            false
        }
    }
    
    /**
     * Обрезать изображение по меньшей стороне (центрировано)
     */
    private fun cropImage(img: BufferedImage, width: Int, height: Int): BufferedImage {
        val size = minOf(width, height)
        val left = (width - size) / 2
        val top = (height - size) / 2
        
        return img.getSubimage(left, top, size, size)
    }
    
    /**
     * Добавить черные границы для получения квадрата
     */
    private fun letterboxImage(img: BufferedImage, width: Int, height: Int): BufferedImage {
        val size = maxOf(width, height)
        
        // Создаем новое изображение черного цвета
        val newImg = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val graphics = newImg.createGraphics()
        
        // Заполняем черным цветом
        graphics.color = Color.BLACK
        graphics.fillRect(0, 0, size, size)
        
        // Размещаем оригинальное изображение в центре
        val left = (size - width) / 2
        val top = (size - height) / 2
        graphics.drawImage(img, left, top, null)
        graphics.dispose()
        
        return newImg
    }
    
    /**
     * Получить информацию о размерах изображения
     * 
     * @return Пара (width, height) или null если ошибка
     */
    fun getImageSize(filePath: String): Pair<Int, Int>? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            
            val img = ImageIO.read(file) ?: return null
            Pair(img.width, img.height)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Проверить, является ли изображение квадратным
     */
    fun isSquareImage(filePath: String): Boolean {
        val size = getImageSize(filePath) ?: return false
        return size.first == size.second
    }
    
    /**
     * Конвертировать изображение в JPEG (используется для seed data)
     * 
     * @param inputPath Путь к исходному файлу
     * @param outputPath Путь для сохранения результата
     * @return true если успешно
     */
    fun convertToJpeg(inputPath: String, outputPath: String): Boolean {
        return try {
            val inputFile = File(inputPath)
            if (!inputFile.exists()) return false
            
            val img = ImageIO.read(inputFile) 
                ?: throw Exception("Не удалось прочитать изображение")
            
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            ImageIO.write(img, "JPEG", outputFile)
            
            println("✅ Изображение конвертировано в JPEG: $outputPath")
            true
        } catch (e: Exception) {
            println("❌ Ошибка при конвертировании: ${e.message}")
            false
        }
    }
    
    /**
     * Обработать все изображения в папке (для seed data)
     * Обрезает неквадратные изображения и конвертирует в JPEG
     */
    fun processAvatarDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                println("❌ Директория не найдена: $dirPath")
                return false
            }
            
            dir.listFiles()?.forEach { file ->
                if (file.isFile && isImageFile(file.name)) {
                    val (width, height) = getImageSize(file.absolutePath) ?: return@forEach
                    println("📄 ${file.name}: ${width}x${height}")
                    
                    // Обрезаем если не квадратное
                    if (width != height) {
                        val tempFile = File(file.absolutePath + ".tmp")
                        if (processImage(file.absolutePath, tempFile.absolutePath, ProcessMode.CROP)) {
                            file.delete()
                            tempFile.renameTo(file)
                        }
                    }
                    
                    // Конвертируем в JPEG если другой формат
                    if (!file.name.endsWith(".jpg", ignoreCase = true)) {
                        val jpegFile = File(file.parentFile, file.nameWithoutExtension + ".jpg")
                        if (convertToJpeg(file.absolutePath, jpegFile.absolutePath)) {
                            if (file.name != jpegFile.name) {
                                file.delete()
                            }
                        }
                    }
                }
            }
            
            println("✅ Обработка директории завершена")
            true
        } catch (e: Exception) {
            println("❌ Ошибка при обработке директории: ${e.message}")
            false
        }
    }
    
    /**
     * Проверить, является ли файл изображением
     */
    private fun isImageFile(filename: String): Boolean {
        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "avif")
        return imageExtensions.any { filename.endsWith(it, ignoreCase = true) }
    }
}
