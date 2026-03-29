package core

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import data.UserProfile

/**
 * Управление изображениями для сообщений бота
 */
object ImageManager {
    
    // Соответствия сообщений и путей к изображениям
    // В реальном проекте здесь будут file_id изображений, загруженных в Telegram
    private val messageImages = mapOf(
        "Привет! Это Chill Zone Bot — всё для досуга в одном месте." to "welcome_image",
        "Главное меню." to "main_menu_image", 
        "Настройки." to "settings_image",
        "Раздел мемов." to "memes_image",
        "Раздел предсказаний." to "predictions_image",
        "Раздел тестов." to "tests_image",
        "Раздел событий." to "events_image",
        "Мини-игры." to "games_image",
        "Пиксель-арт пока доступен как отдельный веб-модуль, но раздел уже предусмотрен в меню." to "pixel_art_image",
        "Раздел поддержки." to "feedback_image",
        "Выбери тип подборки." to "collections_image"
    )
    
    // Временные заглушки для file_id - в реальном проекте нужно загрузить изображения в Telegram
    private val imageFileIds = mapOf(
        "welcome_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample1", // заглушка
        "main_menu_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample2", // заглушка
        "settings_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample3", // заглушка
        "memes_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample4", // заглушка
        "predictions_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample5", // заглушка
        "tests_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample6", // заглушка
        "events_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample7", // заглушка
        "games_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample8", // заглушка
        "pixel_art_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample9", // заглушка
        "feedback_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample10", // заглушка
        "collections_image" to "AgACAgQAAxkBAAMWZq5BvZ7X8Yexample11" // заглушка
    )
    
    /**
     * Отправляет сообщение с изображением, если у пользователя включены картинки
     * @param bot экземпляр бота
     * @param chat ID чата
     * @param message текст сообщения
     * @param userProfile профиль пользователя для проверки настроек
     * @param onMessageSent callback для отправки текстового сообщения
     */
    fun sendMessageWithImage(
        bot: Bot,
        chat: ChatId,
        message: String,
        userProfile: UserProfile?,
        onMessageSent: () -> Unit = {}
    ) {
        val shouldSendImage = userProfile?.showMedia ?: true
        
        if (shouldSendImage) {
            val imageKey = messageImages[message]
            val fileId = imageKey?.let { imageFileIds[it] }
            
            if (fileId != null) {
                // Отправляем изображение
                bot.sendPhoto(chat, fileId, caption = message)
            } else {
                // Если нет соответствующего изображения, отправляем только текст
                bot.sendMessage(chat, message)
            }
        } else {
            // Если у пользователя отключены картинки, отправляем только текст
            bot.sendMessage(chat, message)
        }
        
        onMessageSent()
    }
    
    /**
     * Проверяет, есть ли изображение для данного сообщения
     */
    fun hasImage(message: String): Boolean {
        return messageImages.containsKey(message)
    }
    
    /**
     * Добавляет новое соответствие сообщения и изображения
     */
    fun addMessageImage(message: String, imageKey: String, fileId: String) {
        // Для расширения системы в будущем
    }
}
