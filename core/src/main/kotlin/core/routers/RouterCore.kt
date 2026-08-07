package core.routers

import data.models.*
import data.services.AdminService
import data.services.AvatarService
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.*
import core.keyboards.KeyboardFactory
import core.keyboards.KeyboardProfile
import core.ImageManager
import core.SessionStore
import core.FSMContext
import core.PendingAction
import core.Session
import core.FileValidator
import core.routers.routerAdmin.RouterAdmin
import core.routers.routerAdmin.RouterAdminUsers
import core.routers.routerCollections.RouterCollections
import data.repositories.UserRepository
import data.repositories.MemeRepository
import data.repositories.PredictionRepository
import data.repositories.GameRepository
import data.repositories.TestRepository
import data.repositories.EventRepository
import data.repositories.FeedbackRepository

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
        val sender = sender(message) ?: return
        val chat = ChatId.fromId(chatId(message))
        val uid = sender.id
        val uname = sender.username ?: ""
        val dname = displayName(sender)

        users.ensure(uid, uname, dname)
        users.updateLastActivity(uid)  // Обновляем время последней активности
        val session = SessionStore.get(uid)
        val activeUid = session.data["profile_impersonated_user_id"]?.toLongOrNull() ?: uid
        val text = message.text?.trim()

        // Обработка фото для аватарки профиля
        if (session.action == PendingAction.EDIT_AVATAR && 
            (message.photo != null || message.document != null || message.video != null || 
             message.audio != null || message.voice != null || message.animation != null || message.sticker != null)) {
            
            val (isValid, errorMessage) = FileValidator.validateAvatarImage(message)
            
            if (!isValid) {
                bot.sendMessage(chat, errorMessage ?: "❌ Невозможно установить эту картинку на аватарку.")
                return
            }
            
            val photo = message.photo!!.lastOrNull() ?: return
            val (width, height) = Pair(photo.width, photo.height)
            
            // Проверяем, квадратное ли изображение
            if (FileValidator.isSquareImage(width, height)) {
                // Изображение квадратное - сразу сохраняем
                users.setAvatar(activeUid, photo.fileId)
                session.action = PendingAction.NONE
                session.data.clear()
                bot.sendMessage(chat, "✅ Аватарка сохранена!", replyMarkup = KeyboardFactory.profileMenu(users.profile(activeUid)))
            } else {
                // Изображение не квадратное - предлагаем варианты обработки
                val (minSize, dimension) = FileValidator.getImageProcessingInfo(width, height)
                
                session.action = PendingAction.AVATAR_PROCESSING
                session.data["avatar_file_id"] = photo.fileId
                session.data["avatar_width"] = width.toString()
                session.data["avatar_height"] = height.toString()
                
                val messageText = """📐 <b>Изображение не квадратное</b>

Размеры: ${width}x${height}
По ${dimension} больше на ${maxOf(width, height) - minOf(width, height)}px

Выберите, как установить аватарку:"""
                
                bot.sendMessage(
                    chat, 
                    messageText, 
                    parseMode = ParseMode.HTML,
                    replyMarkup = KeyboardProfile.avatarProcessingMenu(minSize)
                )
            }
            return
        }

        // Обработка фото для добавления мема (системная функция)
        if (session.action == PendingAction.ADD_MEME && 
            (message.photo != null || message.document != null || message.video != null || 
             message.audio != null || message.voice != null || message.animation != null || message.sticker != null)) {
            
            val (isValid, errorMessage) = FileValidator.validateMemeImage(message)
            
            if (!isValid) {
                bot.sendMessage(chat, errorMessage ?: "❌ Невозможно добавить этот файл как мем.")
                return
            }
            
            val photo = message.photo!!.lastOrNull() ?: return
            val caption = message.caption ?: session.data["caption"].orEmpty()
            memes.addMeme(photo.fileId, activeUid, caption)
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
                // Сначала проверяем контекст просмотра профиля
                if (session.context == FSMContext.PROFILE_VIEW) {
                    // Если просматривали профиль пользователя - возвращаемся в предыдущий список/действие
                    val prevActionName = session.data["admin_profile_view_prev_action"]
                    val prevAction = if (prevActionName != null) {
                        try {
                            PendingAction.valueOf(prevActionName)
                        } catch (e: Exception) {
                            PendingAction.NONE
                        }
                    } else {
                        PendingAction.NONE
                    }

                    session.context = FSMContext.MEMES
                    session.action = PendingAction.NONE

                    // Восстанавливаем соответствующий список на основе предыдущего action
                    when (prevAction) {
                        PendingAction.ADMIN_USER_LIST -> {
                            session.action = PendingAction.ADMIN_USER_LIST
                            session.data.remove("admin_profile_view_prev_action")
                            RouterAdminUsers.showUserList(bot, chat, uid, session.data["admin_current_index"]?.toIntOrNull() ?: 0)
                        }
                        PendingAction.ADMIN_BANNED_LIST -> {
                            session.action = PendingAction.ADMIN_BANNED_LIST
                            session.data.remove("admin_profile_view_prev_action")
                            RouterAdminUsers.showBannedUsers(bot, chat, uid, users, session.data["admin_current_index"]?.toIntOrNull() ?: 0)
                        }
                        PendingAction.ADMIN_SEARCH_RESULTS,
                        PendingAction.ADMIN_EDIT_BAN_EXPIRY -> {
                            session.action = PendingAction.ADMIN_SEARCH_RESULTS
                            session.data.remove("admin_profile_view_prev_action")
                            val resultsString = session.data["admin_search_results"]
                            if (resultsString != null) {
                                val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                                val searchUsers = userIds.mapNotNull { users.profile(it) }
                                RouterAdminUsers.showSearchResults(bot, chat, uid, searchUsers, session.data["admin_current_index"]?.toIntOrNull() ?: 0)
                            } else {
                                RouterAdmin.showAdminPanel(bot, chat, uid)
                            }
                        }
                        else -> {
                            // Если не удалось восстановить - возвращаем в админ меню
                            RouterAdmin.showAdminPanel(bot, chat, uid)
                        }
                    }
                }
                // Проверяем, находимся ли мы в контексте редактирования профиля
                else if (session.action in listOf(
                    PendingAction.EDIT_PROFILE,
                    PendingAction.EDIT_NAME,
                    PendingAction.EDIT_BIO,
                    PendingAction.EDIT_AVATAR,
                    PendingAction.AVATAR_PROCESSING
                )) {
                    // Если в контексте редактирования профиля - возвращаемся в меню профиля
                    session.action = PendingAction.NONE
                    session.data.clear()
                    RouterProfile.showProfile(bot, chat, uid, users)
                } else if (session.action in listOf(
                    PendingAction.ADMIN_USER_MANAGEMENT,
                    PendingAction.ADMIN_USER_LIST,
                    PendingAction.ADMIN_BANNED_LIST,
                    PendingAction.ADMIN_SEARCH,
                    PendingAction.ADMIN_SEARCH_RESULTS,
                    PendingAction.ADMIN_EDIT_BAN_EXPIRY
                )) {
                    // Если в админском контексте - передаем в RouterAdmin
                    RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
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
                handleModuleRoutes(bot, chat, uid, activeUid, text, session)
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
    private fun handleModuleRoutes(
        bot: Bot,
        chat: ChatId,
        uid: Long,
        activeUid: Long,
        text: String,
        session: Session
    ) {
        when (text) {
            "👤 Профиль" -> RouterProfile.showProfile(bot, chat, uid, users)
            // Кнопки профиля
            "✏️ Изменить профиль", "📊 Статистика аккаунта", "🛡️ Админ панель", "🛡️ Панель модератора", "Изменить имя", "Показать username [👁️]", "Скрыть username [🙈]", "Изменить био", "Показать профиль [👁️]", "Скрыть профиль [🙈]", "🖼️ Добавить аватарку", "🖼️ Изменить аватарку", "⬅️ Назад", "Сменить аккаунт на пользователя", "Сменить аккаунт на админа" -> {
                RouterProfile.handleProfileAction(bot, chat, uid, text, users, session)
            }
            // Админские кнопки
            "👥 Список пользователей", "🔍 Поиск пользователей", "🚫 Забаненные пользователи", "📊 Статистика",
            " Показать всех", "✅ Показать разбаненных", "🚫 Показать забаненных",
            "⬅️ Предыдущий", "➡️ Следующий", "⬅️ В список", "⬅️ Отмена", "🚪 Выйти из поиска",
            "🚫 Забанить", "✅ Разбанить", "📝 Написать причину бана" -> {
                RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
            }
            "⚙️ Настройки" -> RouterSettings.handleSettingsAction(bot, chat, uid, text, users)
            // Кнопки настроек
            "Включить картинки [✅]", "Выключить картинки [❌]" -> {
                RouterSettings.handleSettingsAction(bot, chat, uid, text, users)
            }
            "🗂 Подборки" -> {
                if (checkUserBan(bot, chat, activeUid, "🗂 Подборки")) return
                RouterCollections.handleCollectionAction(bot, chat, activeUid, text, users, session)
            }
            "🔍 Поиск" -> {
                if (checkUserBan(bot, chat, activeUid, "🔍 Поиск")) return
                RouterCollections.handleCollectionAction(bot, chat, activeUid, text, users, session)
            }
            "⭐ Избранное" -> {
                if (checkUserBan(bot, chat, activeUid, "⭐ Избранное")) return
                RouterCollections.handleCollectionAction(bot, chat, activeUid, text, users, session)
            }
            "😂 Мемы" -> {
                if (checkUserBan(bot, chat, activeUid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, activeUid, text, memes, users, session)
            }
            "Следующий мем", "Добавить мем", "💾 Избр. мем", "Мои избр. мемы" -> {
                if (checkUserBan(bot, chat, activeUid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, activeUid, text, memes, users, session)
            }
            "👍" -> {
                if (checkUserBan(bot, chat, activeUid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, activeUid, text, memes, users, session)
            }
            "👎" -> {
                if (checkUserBan(bot, chat, activeUid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, activeUid, text, memes, users, session)
            }
            "Мои избранные" -> {
                if (checkUserBan(bot, chat, activeUid, "😂 Мемы")) return
                RouterMemes.handleMemeAction(bot, chat, activeUid, text, memes, users, session)
            }
            "🔮 Предсказания" -> {
                if (checkUserBan(bot, chat, activeUid, "🔮 Предсказания")) return
                RouterPredictions.handlePredictionAction(bot, chat, activeUid, text, predictions, users, session)
            }
            "Получить предсказание", "Мои предсказания", "Поиск предсказаний", "Получить ещё", "Добавить в избранные" -> {
                if (checkUserBan(bot, chat, activeUid, "🔮 Предсказания")) return
                RouterPredictions.handlePredictionAction(bot, chat, activeUid, text, predictions, users, session)
            }
            "📝 Тесты" -> {
                if (checkUserBan(bot, chat, activeUid, "📝 Тесты")) return
                RouterTests.handleTestAction(bot, chat, activeUid, text, tests, users, session)
            }
            "📅 События" -> {
                if (checkUserBan(bot, chat, activeUid, "📅 События")) return
                RouterEvents.handleEventAction(bot, chat, activeUid, text, events, users, session)
            }
            "🎮 Мини-игры" -> {
                if (checkUserBan(bot, chat, activeUid, "🎮 Мини-игры")) return
                RouterGames.handleGameAction(bot, chat, activeUid, text, games, users, session)
            }
            "🖼 Пиксель-арт" -> {
                if (checkUserBan(bot, chat, activeUid, "🖼 Пиксель-арт")) return
                RouterPixelArt.handlePixelAction(bot, chat, activeUid, text, users)
            }
            "💬 Обратная связь" -> RouterFeedBack.handleFeedbackAction(bot, chat, activeUid, text, feedback, users, session)
            "💾 Сохранить" -> handleSaveAction(bot, chat, activeUid, session)
            "➡️ Ещё" -> handleMoreAction(bot, chat, activeUid, session)
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
            PendingAction.AVATAR_PROCESSING -> handleAvatarProcessing(bot, chat, uid, text, session)
            PendingAction.EDIT_PROFILE, PendingAction.EDIT_NAME, PendingAction.EDIT_BIO -> {
                RouterProfile.handleProfileAction(bot, chat, uid, text, users, session)
            }
            PendingAction.ADMIN_EDIT_BAN_REASON -> {
                // Обработка ввода причины бана
                val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                if (targetUserId != null) {
                    if (AdminService.setBanReason(targetUserId, text)) {
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            bot.sendMessage(chat, "✅ Причина бана установлена: \"<b>$text</b>\"", parseMode = ParseMode.HTML)
                            session.action = PendingAction.NONE
                            session.data.remove("admin_edit_target_user")
                            RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                        } else {
                            bot.sendMessage(chat, "❌ Ошибка: пользователь не найден")
                        }
                    } else {
                        bot.sendMessage(chat, "❌ Ошибка при сохранении причины бана")
                    }
                } else {
                    bot.sendMessage(chat, "❌ Ошибка: не указан пользователь")
                }
                true
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
            PendingAction.ADMIN_USER_MANAGEMENT,
            PendingAction.ADMIN_USER_LIST,
            PendingAction.ADMIN_BANNED_LIST,
            PendingAction.ADMIN_SEARCH,
            PendingAction.ADMIN_SEARCH_RESULTS,
            PendingAction.ADMIN_EDIT_BAN_EXPIRY,
            PendingAction.ADMIN_EDIT_BAN_REASON,
            PendingAction.ADMIN_EDIT_PROFILE,
            PendingAction.ADMIN_EDIT_NAME,
            PendingAction.ADMIN_EDIT_USERNAME,
            PendingAction.ADMIN_EDIT_BIO -> {
                RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
                true
            }
            else -> false
        }
    }
    
    private fun handleAvatarProcessing(bot: Bot, chat: ChatId, uid: Long, text: String, session: Session): Boolean {
        return when {
            text.startsWith("✂️ Обрезать") -> {
                val fileId = session.data["avatar_file_id"] ?: return true
                val processedFilePath = AvatarService.processAvatarImage(fileId, "crop")
                if (processedFilePath != null) {
                    users.setAvatar(uid, processedFilePath)
                    session.action = PendingAction.NONE
                    session.data.clear()
                    RouterProfile.showProfile(bot, chat, uid, users)
                } else {
                    bot.sendMessage(chat, "❌ Ошибка при обработке изображения")
                }
                true
            }
            text == "📦 Добавить границы до квадрата" -> {
                val fileId = session.data["avatar_file_id"] ?: return true
                val processedFilePath = AvatarService.processAvatarImage(fileId, "letterbox")
                if (processedFilePath != null) {
                    users.setAvatar(uid, processedFilePath)
                    session.action = PendingAction.NONE
                    session.data.clear()
                    RouterProfile.showProfile(bot, chat, uid, users)
                } else {
                    bot.sendMessage(chat, "❌ Ошибка при обработке изображения")
                }
                true
            }
            text == "⬅️ Отмена" -> {
                session.action = PendingAction.NONE
                session.data.clear()
                RouterProfile.showProfile(bot, chat, uid, users)
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
        users.updateLastActivity(uid)  // Обновляем время последней активности
        val data = callback.data

        when {
            // Админские функции
            data.startsWith("admin_") || data.startsWith("ban_add_") || data.startsWith("ban_sub_") || data == "ban_add_cancel" -> {
                RouterAdmin.handleCallback(bot, callback, users)
            }
            // Подборки - сохранение/удаление избранного
            data.startsWith("save_item_") || data.startsWith("delete_favorite_") || data.startsWith("nav_favorite_") || data.startsWith("nav_search_") || data.startsWith("save_single_") || data.startsWith("remove_item_") -> {
                RouterCollections.handleCallback(bot, callback, users)
            }
            data.startsWith("meme_") -> {
                RouterMemes.handleCallback(bot, callback, memes)
            }
            // Другие callback запросы можно добавить здесь по мере необходимости
            else -> {
                bot.answerCallbackQuery(callback.id, "Неизвестная команда.")
            }
        }
    }
}
