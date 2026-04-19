package core

import com.github.kotlintelegrambot.entities.Message

/**
 * Утилита для валидации загруженных файлов
 */
object FileValidator {
    
    private val VALID_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    
    /**
     * Получает информацию о размерах фото
     * @return Пара (width, height) или null если нет фото
     */
    fun getPhotoSize(message: Message): Pair<Int, Int>? {
        val photo = message.photo?.lastOrNull() ?: return null
        return Pair(photo.width, photo.height)
    }
    
    /**
     * Проверяет, является ли изображение квадратным
     */
    fun isSquareImage(width: Int, height: Int): Boolean {
        return width == height
    }
    
    /**
     * Получает информацию о необходимой обработке для неквадратного изображения
     */
    fun getImageProcessingInfo(width: Int, height: Int): Pair<Int, String> {
        val minSize = minOf(width, height)
        
        return Pair(minSize, if (width > height) "ширина" else "высота")
    }
    
    /**
     * Проверяет, является ли сообщение корректным изображением для аватарки
     * @return Пара (isValid, errorMessage)
     */
    fun validateAvatarImage(message: Message): Pair<Boolean, String?> {
        // Проверяем, отправил ли пользователь документ вместо фото
        if (message.document != null) {
            val fileName = message.document?.fileName ?: "файл"
            val fileExtension = fileName.substringAfterLast(".", "")
            
            return if (fileExtension.isEmpty()) {
                Pair(false, "❌ Невозможно установить эту картинку на аватарку.\nФайл должен иметь расширение изображения (JPG, PNG, GIF, WEBP, BMP).")
            } else {
                Pair(false, "❌ Невозможно установить эту картинку на аватарку.\nТип файла .$fileExtension не поддерживается.\nПожалуйста, отправьте изображение в формате JPG, PNG, GIF, WEBP или BMP.")
            }
        }
        
        // Проверяем, отправил ли пользователь другой тип файла (видео, аудио и т.д.)
        if (message.video != null || message.audio != null || message.voice != null || 
            message.animation != null || message.sticker != null) {
            return Pair(false, "❌ Невозможно установить эту картинку на аватарку.\nПожалуйста, отправьте обычное изображение (JPG, PNG, GIF, WEBP или BMP).")
        }
        
        // Если это фото - валидно
        if (message.photo != null) {
            return Pair(true, null)
        }
        
        // Если ничего из вышеперечисленного - неверный тип
        return Pair(false, "❌ Невозможно установить эту картинку на аватарку.\nПожалуйста, отправьте изображение.")
    }
    
    /**
     * Проверяет, является ли сообщение корректным изображением для мема
     * @return Пара (isValid, errorMessage)
     */
    fun validateMemeImage(message: Message): Pair<Boolean, String?> {
        // Проверяем, отправил ли пользователь документ вместо фото
        if (message.document != null) {
            val fileName = message.document?.fileName ?: "файл"
            val fileExtension = fileName.substringAfterLast(".", "")
            
            return if (fileExtension.isEmpty()) {
                Pair(false, "❌ Невозможно добавить этот файл как мем.\nФайл должен быть изображением (JPG, PNG, GIF, WEBP, BMP).")
            } else {
                Pair(false, "❌ Невозможно добавить этот файл как мем.\nТип файла .$fileExtension не поддерживается.\nПожалуйста, отправьте изображение в формате JPG, PNG, GIF, WEBP или BMP.")
            }
        }
        
        // Проверяем, отправил ли пользователь другой тип файла
        if (message.video != null || message.audio != null || message.voice != null || 
            message.animation != null || message.sticker != null) {
            return Pair(false, "❌ Невозможно добавить этот файл как мем.\nПожалуйста, отправьте обычное изображение (JPG, PNG, GIF, WEBP или BMP).")
        }
        
        // Если это фото - валидно
        if (message.photo != null) {
            return Pair(true, null)
        }
        
        // Если ничего из вышеперечисленного - неверный тип
        return Pair(false, "❌ Невозможно добавить этот файл как мем.\nПожалуйста, отправьте изображение.")
    }
}
