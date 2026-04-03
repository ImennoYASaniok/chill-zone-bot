package core.routers

import data.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.*
import core.keyboards.KeyboardFactory
import core.ImageManager
import core.SessionStore
import core.FSMContext
import core.PendingAction
import core.Session

class RouterCore(
    private val users: UserRepository,
    private val memes: MemeRepository,
    private val predictions: PredictionRepository,
    private val games: GameRepository,
    private val tests: TestRepository,
    private val events: EventRepository,
    private val feedback: FeedbackRepository
) {
    private fun chatId(message: Message): Long = message.chat.id
    private fun sender(message: Message): User? = message.from

    private fun displayName(user: User?): String = when {
        user?.username != null -> "@${user.username}"
        !user?.firstName.isNullOrBlank() -> user!!.firstName
        else -> "Пользователь"
    }

    fun handle(bot: Bot, message: Message) {
        val user = sender(message) ?: return
        val chat = ChatId.fromId(chatId(message))
        val uid = user.id
        val uname = user.username ?: ""
        val dname = displayName(user)

        users.ensure(uid, uname, dname)
        val session = SessionStore.get(uid)
        val text = message.text?.trim()

        // Обработка фото для добавления мема (системная функция)
        if (message.photo != null && session.action == PendingAction.ADD_MEME) {
            val photo = message.photo!!.lastOrNull() ?: return
            val caption = message.caption ?: session.data["caption"].orEmpty()
            memes.addMeme(photo.fileId, uid, caption)
            session.action = PendingAction.NONE
            session.data.clear()
            bot.sendMessage(chat, "Мем сохранён.", replyMarkup = KeyboardFactory.memesMenu())
            return
        }

        if (text.isNullOrBlank()) return

        // Сначала проверяем системные команды
        when (text) {
            "/start", "/start@" -> handleStart(bot, chat, uid)
            "/menu", "/menu@" -> handleMenu(bot, chat, uid)
            "/debug_profile" -> handleDebugProfile(bot, chat, uid)
            "/debug_update" -> handleDebugUpdate(bot, chat, uid)
            "/debug_compare" -> handleDebugCompare(bot, chat, uid)
            "⬅️ Обратно" -> {
                // Для контекста COLLECTIONS передаем обработку в RouterCollections
                if (session.context == FSMContext.COLLECTIONS) {
                    RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)
                } else {
                    handleBack(bot, chat, uid, session)
                }
            }
            else -> {
                // Перенаправляем в модули если не системная команда
                if (session.action != PendingAction.NONE && handlePending(bot, chat, uid, text, session)) {
                    return
                }
                handleModuleRoutes(bot, chat, uid, text, session)
            }
        }
    }

    // Системные команды
    private fun handleStart(bot: Bot, chat: ChatId, uid: Long) {
        SessionStore.clear(uid)
        val profile = users.profile(uid)
        val welcomeMessage = "Привет! Это Chill Zone Bot — всё для досуга в одном месте."
        ImageManager.sendMessageWithImage(bot, chat, welcomeMessage, profile) {
            val menuMessage = "Главное меню."
            bot.sendMessage(chat, menuMessage, replyMarkup = KeyboardFactory.mainMenu())
        }
    }

    private fun handleMenu(bot: Bot, chat: ChatId, uid: Long) {
        SessionStore.clear(uid)
        val profile = users.profile(uid)
        val menuMessage = "Главное меню."
        bot.sendMessage(chat, menuMessage, replyMarkup = KeyboardFactory.mainMenu())
    }

    private fun handleDebugProfile(bot: Bot, chat: ChatId, uid: Long) {
        val debugInfo = users.debugProfile(uid)
        bot.sendMessage(chat, debugInfo)
    }

    private fun handleDebugUpdate(bot: Bot, chat: ChatId, uid: Long) {
        val testResult = users.debugUpdateName(uid, "DEBUG_TEST_" + System.currentTimeMillis())
        bot.sendMessage(chat, testResult)
        val checkResult = users.debugProfile(uid)
        bot.sendMessage(chat, checkResult)
    }

    private fun handleDebugCompare(bot: Bot, chat: ChatId, uid: Long) {
        val testName = "COMPARE_" + System.currentTimeMillis()
        
        // Тестируем обычный метод
        users.updateName(uid, testName)
        val normalResult = users.profile(uid)
        
        // Тестируем прямой метод
        val directResult = users.debugProfile(uid)
        
        bot.sendMessage(chat, "СРАВНЕНИЕ:\nОбычный метод: ${normalResult?.displayName}\nПрямой метод: $directResult", parseMode = ParseMode.HTML)
    }

    private fun handleBack(bot: Bot, chat: ChatId, uid: Long, session: Session) {
        SessionStore.clear(uid)
        bot.sendMessage(chat, "Главное меню.", replyMarkup = KeyboardFactory.mainMenu())
    }

    // Перенаправление в модули
    private fun handleModuleRoutes(bot: Bot, chat: ChatId, uid: Long, text: String, session: Session) {
        when (text) {
            "👤 Профиль" -> RouterProfile.showProfile(bot, chat, uid, users, memes, predictions, tests, events, games)
            // Кнопки профиля
            "Изменить имя", "Показать username [👁️]", "Скрыть username [🙈]", "Изменить био", "Показать профиль [👁️]", "Скрыть профиль [🙈]" -> {
                RouterProfile.handleProfileAction(bot, chat, uid, text, users, memes, predictions, tests, events, games, session)
            }
            "⚙️ Настройки" -> RouterSettings.handleSettingsAction(bot, chat, uid, text, users)
            // Кнопки настроек
            "Включить картинки [✅]", "Выключить картинки [❌]" -> {
                RouterSettings.handleSettingsAction(bot, chat, uid, text, users)
            }
            "🗂 Подборки" -> RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)
            "🔍 Поиск" -> RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)
            "⭐ Избранное" -> RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)
            "😂 Мемы" -> RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            "Следующий мем", "Добавить мем", "💾 Избр. мем", "Мои избр. мемы" -> {
                RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            }
            "👍" -> RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            "👎" -> RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            "Мои избранные" -> RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            "🔮 Предсказания" -> RouterPredictions.handlePredictionAction(bot, chat, uid, text, predictions, users, session)
            "Получить предсказание", "Мои предсказания", "Поиск предсказаний", "Получить ещё", "Добавить в избранные" -> {
                RouterPredictions.handlePredictionAction(bot, chat, uid, text, predictions, users, session)
            }
            "📝 Тесты" -> RouterTests.handleTestAction(bot, chat, uid, text, tests, users, session)
            "📅 События" -> RouterEvents.handleEventAction(bot, chat, uid, text, events, users, session)
            "🎮 Мини-игры" -> RouterGames.handleGameAction(bot, chat, uid, text, games, users, session)
            "🖼 Пиксель-арт" -> RouterPixelArt.handlePixelAction(bot, chat, uid, text, users)
            "💬 Обратная связь" -> RouterFeedBack.handleFeedbackAction(bot, chat, uid, text, feedback, users, session)
            "💾 Сохранить" -> handleSaveAction(bot, chat, uid, session)
            "➡️ Ещё" -> handleMoreAction(bot, chat, uid, session)
            "🔍 Новый запрос" -> {
                // Передаем обработку в RouterCollections
                if (RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)) {
                    return
                } else {
                    bot.sendMessage(chat, "Не понял команду. Открой меню через /start.", replyMarkup = KeyboardFactory.mainMenu())
                }
            }
            else -> bot.sendMessage(chat, "Не понял команду. Открой меню через /start.", replyMarkup = KeyboardFactory.mainMenu())
        }
    }

    // Специальные обработчики для контекстных команд
    private fun handleSaveAction(bot: Bot, chat: ChatId, uid: Long, session: Session) {
        when (session.context) {
            core.FSMContext.MEMES -> {
                RouterMemes.handleMemeAction(bot, chat, uid, "💾 Сохранить", memes, users, session)
            }
            core.FSMContext.COLLECTIONS -> {
                RouterCollections.handleCollectionAction(bot, chat, uid, "💾 Сохранить", users, session)
            }
            else -> {
                RouterMemes.handleMemeAction(bot, chat, uid, "💾 Сохранить", memes, users, session)
            }
        }
    }

    private fun handleFavoriteAction(bot: Bot, chat: ChatId, uid: Long, session: Session) {
        when (session.context) {
            core.FSMContext.MEMES -> {
                RouterMemes.handleMemeAction(bot, chat, uid, "⭐ Избранное", memes, users, session)
            }
            core.FSMContext.COLLECTIONS -> {
                RouterCollections.handleCollectionAction(bot, chat, uid, "⭐ Избранное", users, session)
            }
            else -> {
                RouterMemes.handleMemeAction(bot, chat, uid, "⭐ Избранное", memes, users, session)
            }
        }
    }

    private fun handleMoreAction(bot: Bot, chat: ChatId, uid: Long, session: Session) {
        RouterCollections.handleCollectionAction(bot, chat, uid, "➡️ Ещё", users, session)
    }

    // Обработка pending actions - перенаправление в модули
    private fun handlePending(bot: Bot, chat: ChatId, uid: Long, text: String, session: Session): Boolean {
        return when (session.action) {
            PendingAction.EDIT_NAME, PendingAction.EDIT_BIO -> {
                RouterProfile.handleProfileAction(bot, chat, uid, text, users, memes, predictions, tests, events, games, session)
            }
            PendingAction.FEEDBACK_TEXT -> {
                RouterFeedBack.handleFeedbackAction(bot, chat, uid, text, feedback, users, session)
            }
            PendingAction.ADD_PREDICTION_TEXT, PendingAction.ADD_PREDICTION_RARITY, PendingAction.SEARCH_PREDICTIONS -> {
                RouterPredictions.handlePredictionAction(bot, chat, uid, text, predictions, users, session)
            }
            PendingAction.CREATE_TEST_TITLE, PendingAction.CREATE_TEST_KIND, PendingAction.CREATE_TEST_PROMPT, 
            PendingAction.CREATE_TEST_MORE, PendingAction.PLAY_TEST -> {
                RouterTests.handleTestAction(bot, chat, uid, text, tests, users, session)
            }
            PendingAction.CREATE_EVENT_TITLE, PendingAction.CREATE_EVENT_DESC, PendingAction.CREATE_EVENT_PLACE,
            PendingAction.CREATE_EVENT_TIME, PendingAction.CREATE_EVENT_MAX -> {
                RouterEvents.handleEventAction(bot, chat, uid, text, events, users, session)
            }
            PendingAction.SEARCH_TYPE_SELECT, PendingAction.COLLECTION_QUERY, PendingAction.COLLECTION_RESULTS -> {
                RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)
            }
            else -> false
        }
    }
    
    // Обработка callback запросов - перенаправление в соответствующие модули
    fun handleCallback(bot: Bot, callback: com.github.kotlintelegrambot.entities.CallbackQuery) {
        val chat = ChatId.fromId(callback.message!!.chat.id)
        val uid = callback.from.id
        val session = SessionStore.get(uid)
        val data = callback.data ?: return

        when {
            // Подборки - сохранение/удаление избранного
            data.startsWith("save_item_") || data.startsWith("delete_favorite_") || data.startsWith("nav_favorite_") -> {
                RouterCollections.handleCallback(bot, callback, users)
            }
            // Другие callback запросы можно добавить здесь по мере необходимости
            else -> {
                bot.answerCallbackQuery(callback.id, "Неизвестная команда.")
            }
        }
    }
}