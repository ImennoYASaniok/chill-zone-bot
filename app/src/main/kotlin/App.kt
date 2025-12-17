package ChillZoneBot.app.src.main.kotlin.App
import io.github.cdimascio.dotenv.dotenv
import com.github.kotlintelegrambot.entities.ChatId
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import ChillZoneBot.core.src.main.kotlin.handlers.predictions.*
import ChillZoneBot.core.src.main.kotlin.handlers.predictions.getMainKeyboard
import ChillZoneBot.core.src.main.kotlin.handlers.predictions.getRarityKeyboard
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
object ReplyMenuHolder {
    lateinit var replyMenu: ReplyClass
    var selectedRarity: String? = null
    var searchMode: Boolean = false
}

fun main() {

    val dotenv = dotenv()

    // Инициализация главного меню
    ReplyMenuHolder.replyMenu = ReplyClass(
        keyboard = getMainKeyboard(),
        startCommand = "/start",
        textMessage = "Главное меню"
    )

    val bot = bot {
        token = dotenv["BOT_TOKEN"]

        dispatch {
            message {

                val chatIdLong = message.chat.id
                val chatId = ChatId.fromId(chatIdLong)
                val text = message.text ?: return@message

                // ===== /start =====
                if (text == "/start") {
                    ReplyMenuHolder.replyMenu.keyboard = getMainKeyboard()
                    ReplyMenuHolder.replyMenu.update()

                    bot.sendMessage(
                        chatId,
                        """
Вас приветствует сервис с предсказаниями. Предсказание — это фраза или сообщение о том, что может произойти в будущем, и мы предоставляем возможность получить такое сообщение.

Вам могут выпасть разные типы предсказаний, всего их 4: обычное, редкое, эпическое, легендарное.

• Обычное (70%)
• Редкое (24%)
• Эпическое (5%)
• Легендарное (1%)

Выберите действие ниже.
                        """.trimIndent(),
                        replyMarkup = ReplyMenuHolder.replyMenu.getKeyboardReplyMarkup()
                    )
                    return@message
                }

                // ===== Назад =====
                if (text == "Назад") {
                    // Сбрасываем состояние добавления/поиска
                    ReplyMenuHolder.selectedRarity = null
                    ReplyMenuHolder.searchMode = false

                    // Возврат к главному меню
                    ReplyMenuHolder.replyMenu.keyboard = getMainKeyboard()
                    ReplyMenuHolder.replyMenu.update()

                    bot.sendMessage(
                        chatId,
                        "Главное меню",
                        replyMarkup = ReplyMenuHolder.replyMenu.getKeyboardReplyMarkup()
                    )
                    return@message
                }

                // ===== Получить предсказание =====
                if (text == "Получить предсказание") {
                    if (PredictionStorage.getAll().isEmpty()) {
                        bot.sendMessage(chatId, "Пока нет предсказаний.")
                        return@message
                    }

                    val rand = (0..99).random()
                    val rarity = when {
                        rand < 70 -> "Обычное"
                        rand < 94 -> "Редкое"
                        rand < 99 -> "Эпическое"
                        else -> "Легендарное"
                    }

                    val pool = PredictionStorage.getAll().filter { it.rarity == rarity }
                    val prediction = pool.random()
                    val userList = UserPredictionHistory.getUserPredictions(chatIdLong)
                    // добавить добавление в бд
                    val out =
                        if (userList.any { it.id == prediction.id }) {
                            "Вам выпало повторное предсказание:\n\n${prediction.text} (${prediction.rarity})"
                        } else {
                            UserPredictionHistory.addToUser(chatIdLong, prediction)
                            "${prediction.text} (${prediction.rarity})"
                        }
                    // также сохранение повторок в дб
                    bot.sendMessage(chatId, out)
                    return@message
                }

                // ===== Мои предсказания =====
                if (text == "Мои предсказания") {
                    val list = UserPredictionHistory.getUserPredictions(chatIdLong)
                    if (list.isEmpty()) {
                        bot.sendMessage(chatId, "У вас пока нет предсказаний.")
                    } else {
                        val out = list.joinToString("\n") { "• ${it.text} (${it.rarity})" }
                        bot.sendMessage(chatId, "Ваши предсказания:\n$out")
                    }
                    return@message
                }

                // ===== Добавить предсказание =====
                if (text == "Добавить предсказание") {
                    ReplyMenuHolder.replyMenu.keyboard = getRarityKeyboard()
                    ReplyMenuHolder.replyMenu.update()

                    bot.sendMessage(
                        chatId,
                        "Выберите редкость для нового предсказания:",
                        replyMarkup = ReplyMenuHolder.replyMenu.getKeyboardReplyMarkup()
                    )
                    return@message
                }

                // ===== Выбор редкости =====
                val rarities = listOf("Обычное", "Редкое", "Эпическое", "Легендарное")
                if (text in rarities) {
                    ReplyMenuHolder.selectedRarity = text
                    bot.sendMessage(chatId, "Напишите текст предсказания:")
                    return@message
                }
                if (ReplyMenuHolder.selectedRarity != null) {
                    PredictionStorage.addPrediction(text, ReplyMenuHolder.selectedRarity!!)
                    ReplyMenuHolder.selectedRarity = null

                    ReplyMenuHolder.replyMenu.keyboard = getMainKeyboard()
                    ReplyMenuHolder.replyMenu.update()

                    bot.sendMessage(
                        chatId,
                        "Предсказание добавлено!",
                        replyMarkup = ReplyMenuHolder.replyMenu.getKeyboardReplyMarkup()
                    )
                    return@message
                }
                // тут сохранение в файл переделать в бд
                // ===== Ввод текста предсказания =====
                if (ReplyMenuHolder.selectedRarity != null) {
                    PredictionStorage.addPrediction(text, ReplyMenuHolder.selectedRarity!!)

                    // Сброс состояния и возврат к главному меню
                    ReplyMenuHolder.selectedRarity = null
                    ReplyMenuHolder.replyMenu.keyboard = getMainKeyboard()
                    ReplyMenuHolder.replyMenu.update()

                    bot.sendMessage(
                        chatId,
                        "Предсказание добавлено!",
                        replyMarkup = ReplyMenuHolder.replyMenu.getKeyboardReplyMarkup()
                    )
                    return@message
                }

                // ===== Поиск =====
                if (text == "Поиск предсказаний") {
                    ReplyMenuHolder.searchMode = true
                    bot.sendMessage(chatId, "Введите текст для поиска:")
                    return@message
                }

                if (ReplyMenuHolder.searchMode) {
                    val results = PredictionStorage.getAll().filter { it.text.contains(text, true) }

                    if (results.isEmpty()) {
                        bot.sendMessage(chatId, "Ничего не найдено.")
                    } else {
                        val out = results.joinToString("\n") { "• ${it.text} (${it.rarity})" }
                        bot.sendMessage(chatId, "Результаты поиска:\n$out")
                    }

                    ReplyMenuHolder.searchMode = false
                    return@message
                }

                // ===== Главное меню по умолчанию =====
                ReplyMenuHolder.replyMenu.main(text, bot, chatId)
            }
        }
    }

    bot.startPolling()
}










