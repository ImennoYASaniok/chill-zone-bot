package core.routers

import data.*
import data.models.*
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
import core.routers.routerAdmin.RouterAdmin
import core.routers.routerCollections.RouterCollections

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

    // Проверка бана пользователя
    private fun checkUserBan(bot: Bot, chat: ChatId, uid: Long, serviceName: String): Boolean {
        return if (AdminService.isUserBanned(uid)) {
            val message = """🚫 <b>Доступ ограничен</b>

Вы были забанены и не можете использовать сервис "$serviceName".

Доступные функции:
👤 Профиль
⚙️ Настройки  
💬 Обратная связь

Для разбана обратитесь в службу поддержки."""
            bot.sendMessage(chat, message, parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.mainMenu())
            true
        } else {
            false
        }
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
            "/myid" -> {
                val profile = users.profile(uid)
                val isAdmin = AdminService.isAdmin(uid)
                val myidMessage = """🔍 <b>Информация о пользователе</b>

🆔 Ваш ID: <code>$uid</code>
👤 Имя: ${profile?.displayName ?: "неизвестно"}
🔹 Админ: ${if (isAdmin) "✅ Да" else "❌ Нет"}

📋 Админские ID в системе: ${AdminService.adminIds}"""
                bot.sendMessage(chat, myidMessage, parseMode = ParseMode.HTML)
            }
            "/reload_admins" -> {
                val newAdmins = AdminService.reloadAdmins()
                val reloadMessage = """🔄 <b>Админские ID перезагружены</b>

📋 Новый список: $newAdmins

🆔 Ваш ID: <code>$uid</code>
🔹 Вы админ: ${if (uid in newAdmins) "✅ Да" else "❌ Нет"}"""
                bot.sendMessage(chat, reloadMessage, parseMode = ParseMode.HTML)
            }
            "⬅️ Обратно" -> {
                // Проверяем, находимся ли мы в админском контексте
                if (session.action in listOf(
                    PendingAction.ADMIN_USER_MANAGEMENT,
                    PendingAction.ADMIN_USER_LIST, 
                    PendingAction.ADMIN_BANNED_LIST, 
                    PendingAction.ADMIN_SEARCH, 
                    PendingAction.ADMIN_SEARCH_RESULTS
                )) {
                    // Если в админском контексте - передаем в RouterAdmin
                    RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
                } else if (session.context == FSMContext.COLLECTIONS) {
                    // Для контекста COLLECTIONS проверяем текущее действие
                    when (session.action) {
                        PendingAction.NONE -> {
                            // В основном меню подборок - возвращаем в главное меню
                            handleBack(bot, chat, uid)
                        }
                        else -> {
                            // В поиске, избранном, результатах и т.д. - передаем в RouterCollections для правильной обработки
                            RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)
                        }
                    }
                } else {
                    // Для остальных контекстов - обычная логика
                    handleBack(bot, chat, uid)
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
        val menuMessage = "Главное меню."
        bot.sendMessage(chat, menuMessage, replyMarkup = KeyboardFactory.mainMenu())
    }

    private fun handleBack(bot: Bot, chat: ChatId, uid: Long) {
        SessionStore.clear(uid)
        bot.sendMessage(chat, "Главное меню.", replyMarkup = KeyboardFactory.mainMenu())
    }

    // Перенаправление в модули
    private fun handleModuleRoutes(bot: Bot, chat: ChatId, uid: Long, text: String, session: Session) {
        when (text) {
            "👤 Профиль" -> RouterProfile.showProfile(bot, chat, uid, users)
            // Кнопки профиля
            "✏️ Изменить профиль", "📊 Статистика аккаунта", "🛡️ Админ панель", "Изменить имя", "Показать username [👁️]", "Скрыть username [🙈]", "Изменить био", "Показать профиль [👁️]", "Скрыть профиль [🙈]", "⬅️ Назад" -> {
                RouterProfile.handleProfileAction(bot, chat, uid, text, users, session)
            }
            // Админские кнопки
            "👥 Список пользователей", "🔍 Поиск пользователей", "🚫 Забаненные пользователи", "📊 Статистика",
            " Показать всех", "✅ Показать разбаненных", "🚫 Показать забаненных",
            "⬅️ Предыдущий", "➡️ Следующий" -> {
                RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
            }
            "⬅️ В список" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    when (session.action) {
                        PendingAction.ADMIN_USER_LIST -> RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
                        PendingAction.ADMIN_SEARCH_RESULTS -> RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
                        else -> handleBack(bot, chat, uid)
                    }
                } else {
                    handleBack(bot, chat, uid)
                }
            }
            "🚫 Забанить", "✅ Разбанить" -> {
                if (session.context == FSMContext.PROFILE_VIEW && AdminService.isAdmin(uid)) {
                    RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
                } else {
                    handleBack(bot, chat, uid)
                }
            }
            "⚙️ Настройки" -> RouterSettings.handleSettingsAction(bot, chat, uid, text, users)
            // Кнопки настроек
            "Включить картинки [✅]", "Выключить картинки [❌]" -> {
                RouterSettings.handleSettingsAction(bot, chat, uid, text, users)
            }
            "🗂 Подборки" -> {
                if (checkUserBan(bot, chat, uid, "🗂 Подборки")) return
                RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)
            }
            "🔍 Поиск" -> {
                if (checkUserBan(bot, chat, uid, "🔍 Поиск")) return
                RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)
            }
            "⭐ Избранное" -> {
                if (checkUserBan(bot, chat, uid, "⭐ Избранное")) return
                RouterCollections.handleCollectionAction(bot, chat, uid, text, users, session)
            }
            "😂 Мемы" -> {
                if (checkUserBan(bot, chat, uid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            }
            "Следующий мем", "Добавить мем", "💾 Избр. мем", "Мои избр. мемы" -> {
                if (checkUserBan(bot, chat, uid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            }
            "👍" -> {
                if (checkUserBan(bot, chat, uid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            }
            "👎" -> {
                if (checkUserBan(bot, chat, uid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            }
            "Мои избранные" -> {
                if (checkUserBan(bot, chat, uid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, uid, text, memes, users, session)
            }
            "🔮 Предсказания" -> {
                if (checkUserBan(bot, chat, uid, "🔮 Предсказания")) return
                RouterPredictions.handlePredictionAction(bot, chat, uid, text, predictions, users, session)
            }
            "Получить предсказание", "Мои предсказания", "Поиск предсказаний", "Получить ещё", "Добавить в избранные" -> {
                if (checkUserBan(bot, chat, uid, "🔮 Предсказания")) return
                RouterPredictions.handlePredictionAction(bot, chat, uid, text, predictions, users, session)
            }
            "📝 Тесты" -> {
                if (checkUserBan(bot, chat, uid, "📝 Тесты")) return
                RouterTests.handleTestAction(bot, chat, uid, text, tests, users, session)
            }
            "📅 События" -> {
                if (checkUserBan(bot, chat, uid, "📅 События")) return
                RouterEvents.handleEventAction(bot, chat, uid, text, events, users, session)
            }
            "🎮 Мини-игры" -> {
                if (checkUserBan(bot, chat, uid, "🎮 Мини-игры")) return
                RouterGames.handleGameAction(bot, chat, uid, text, games, users, session)
            }
            "🖼 Пиксель-арт" -> {
                if (checkUserBan(bot, chat, uid, "🖼 Пиксель-арт")) return
                RouterPixelArt.handlePixelAction(bot, chat, uid, text, users)
            }
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
            else -> {
                // Админские кнопки с динамическим текстом
                if (text.startsWith("🔄 Фильтр:")) {
                    RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
                    return
                }
                bot.sendMessage(chat, "Не понял команду. Открой меню через /start.", replyMarkup = KeyboardFactory.mainMenu())
            }
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
                RouterProfile.handleProfileAction(bot, chat, uid, text, users, session)
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
            // Админские действия
            PendingAction.ADMIN_USER_MANAGEMENT, PendingAction.ADMIN_USER_LIST, PendingAction.ADMIN_BANNED_LIST, 
            PendingAction.ADMIN_SEARCH, PendingAction.ADMIN_SEARCH_RESULTS -> {
                RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
                true
            }
            else -> false
        }
    }
    
    // Обработка callback запросов - перенаправление в соответствующие модули
    fun handleCallback(bot: Bot, callback: com.github.kotlintelegrambot.entities.CallbackQuery) {
        val uid = callback.from.id
        val uname = callback.from.username ?: ""
        val dname = displayName(callback.from)

        users.ensure(uid, uname, dname)
        val data = callback.data

        when {
            // Админские функции
            data.startsWith("admin_") -> {
                RouterAdmin.handleCallback(bot, callback, users)
            }
            // Подборки - сохранение/удаление избранного
            data.startsWith("save_item_") || data.startsWith("delete_favorite_") || data.startsWith("nav_favorite_") || data.startsWith("nav_search_") || data.startsWith("save_single_") || data.startsWith("remove_item_") -> {
                RouterCollections.handleCallback(bot, callback, users)
            }
            // Другие callback запросы можно добавить здесь по мере необходимости
            else -> {
                bot.answerCallbackQuery(callback.id, "Неизвестная команда.")
            }
        }
    }
}