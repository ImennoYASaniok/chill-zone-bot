package ChillZoneBot.app.src.main.kotlin.App
import io.github.cdimascio.dotenv.dotenv
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.dispatcher.message
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import ChillZoneBot.core.src.main.kotlin.handlers.predictions.*
import ChillZoneBot.core.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandlerEnvironment
import com.github.kotlintelegrambot.dispatch
object ReplyMenuHolderPrediction {
    lateinit var replyMenu: ReplyClass
}
fun main() {
    val dotenv = dotenv()
    val bot = Bot.Builder()
        .token(dotenv["BOT_TOKEN"])
        .build()

    // Создаём главное меню
    val replyMenu = ReplyClass(
        keyboard = getMainKeyboard(),
        startCommand = "/start",
        textMessage = "Главное меню"
    )
    ReplyMenuHolder.replyMenu = replyMenu

    // Обработка всех сообщений
    bot.dispatch {
        message(Filter.Text) { env: MessageHandlerEnvironment ->
            handleMessage(env, bot, replyMenu)
        }
    }

    bot.startPolling()
}

// Функция обработки сообщений
fun handleMessage(env: MessageHandlerEnvironment, bot: Bot, replyMenu: ReplyClass) {
    val chatId = ChatId.fromId(env.message.chat.id)
    val chatLongId = env.message.chat.id
    val text = env.message.text ?: return

    // Первое приветствие
    if (text == "/start") {
        bot.sendMessage(
            chatId,
            "Вас приветствует сервис с предсказаниями.\n\n" +
                    "Вам могут выпасть разные типы предсказаний:\n" +
                    "• Обычное (70%)\n" +
                    "• Редкое (24%)\n" +
                    "• Эпическое (5%)\n" +
                    "• Легендарное (1%)\n\n" +
                    "Выберите действие ниже.",
            replyMarkup = replyMenu.getKeyboardReplyMarkup()
        )
        return
    }

    // Получить предсказание
    if (text == "Получить предсказание") {
        if (PredictionStorage.size() == 0) {
            bot.sendMessage(chatId, "Пока нет предсказаний. Добавьте первым своё.")
            return
        }

        val rand = (0..99).random()
        val rarity = when {
            rand < 70 -> "Обычное"
            rand < 94 -> "Редкое"
            rand < 99 -> "Эпическое"
            else -> "Легендарное"
        }

        val available = PredictionStorage.getAll().filter { it.rarity == rarity }
        if (available.isEmpty()) {
            bot.sendMessage(chatId, "Предсказаний этой редкости пока нет.")
            return
        }

        val prediction = available.random()
        val userList = UserPredictionHistory.getUserPredictions(chatLongId)
        val messageText = if (userList.any { it.id == prediction.id }) {
            "Вам выпало повторное предсказание:\n\n${prediction.text}"
        } else {
            UserPredictionHistory.addToUser(chatLongId, prediction)
            prediction.text
        }

        bot.sendMessage(chatId, messageText)
        return
    }

    // Мои предсказания
    if (text == "Мои предсказания") {
        val list = UserPredictionHistory.getUserPredictions(chatLongId)
        if (list.isEmpty()) bot.sendMessage(chatId, "У вас пока нет предсказаний.")
        else {
            val textList = list.joinToString("\n") { "• ${it.text} (${it.rarity})" }
            bot.sendMessage(chatId, "Ваши предсказания:\n$textList")
        }
        return
    }

    // Добавить предсказание
    if (text == "Добавить предсказание") {
        bot.sendMessage(chatId, "Выберите редкость для нового предсказания:")
        replyMenu.keyboard = getRarityKeyboard()
        replyMenu.update()
        return
    }

    // Проверка выбранной редкости
    val rarities = listOf("Обычное", "Редкое", "Эпическое", "Легендарное")
    if (rarities.contains(text)) {
        ReplyMenuHolder.selectedRarity = text
        bot.sendMessage(chatId, "Напишите текст вашего предсказания:")
        return
    }
    // Получение текста предсказания
    if (ReplyMenuHolder.selectedRarity != null) {
        val rarity = ReplyMenuHolder.selectedRarity!!
        PredictionStorage.addPrediction(text, rarity)
        bot.sendMessage(chatId, "Предсказание добавлено в общие предсказания!")
        ReplyMenuHolder.selectedRarity = null
        replyMenu.keyboard = getMainKeyboard()
        replyMenu.update()
        bot.sendMessage(chatId, "Главное меню", replyMarkup = replyMenu.getKeyboardReplyMarkup())
        return
    }

    // Поиск предсказаний
    if (text == "Поиск предсказаний") {
        bot.sendMessage(chatId, "Введите текст для поиска предсказаний:")
        ReplyMenuHolder.searchMode = true
        return
    }

    if (ReplyMenuHolder.searchMode) {
        val results = PredictionStorage.getAll().filter { it.text.contains(text, ignoreCase = true) }
        if (results.isEmpty()) bot.sendMessage(chatId, "Ничего не найдено.")
        else {
            val foundText = results.joinToString("\n") { "• ${it.text} (${it.rarity})" }
            bot.sendMessage(chatId, "Результаты поиска:\n$foundText")
        }
        ReplyMenuHolder.searchMode = false
        return
    }

    // Главное меню по умолчанию
    replyMenu.main(text, bot, chatId)
}

// Глобальный объект для меню и состояния добавления/поиска
object ReplyMenuHolder {
    lateinit var replyMenu: ChillZoneBot.core.ReplyClass.ReplyClass
    var selectedRarity: String? = null
    var searchMode: Boolean = false
}