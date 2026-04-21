package core.routers.routerAdmin

import data.*
import data.models.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.CallbackQuery
import core.keyboards.KeyboardAdmin
import core.keyboards.KeyboardFactory
import core.SessionStore
import core.FSMContext
import core.PendingAction
import core.Session
import core.routers.routerAdmin.RouterAdminStatistics
import core.routers.routerAdmin.RouterAdminUsers
import core.routers.RouterProfile

object RouterAdmin {
    fun showAdminPanel(bot: Bot, chat: ChatId, uid: Long) {
        if (!AdminService.isAdmin(uid)) {
            bot.sendMessage(chat, "🚫 Доступ запрещен. У вас нет админских прав.")
            return
        }
        
        val statsMessage = """🛡️ <b>Админ панель</b>

Выберите действие:"""
        
        bot.sendMessage(chat, statsMessage, parseMode = ParseMode.HTML, replyMarkup = KeyboardAdmin.adminMenu())
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun handleAdminAction(bot: Bot, chat: ChatId, uid: Long, text: String, users: UserRepository, session: Session) {
        if (!AdminService.isAdmin(uid)) {
            bot.sendMessage(chat, "🚫 Доступ запрещен. У вас нет админских прав.")
            return
        }

        // Обработка фильтра для списка или поиска
        if (text.startsWith("🔄 Фильтр:")) {
            val currentSearchFilter = session.data["admin_search_filter"] ?: "Все"
            val currentListFilter = session.data["admin_filter"] ?: "Все"
            val next = when (session.action) {
                PendingAction.ADMIN_SEARCH_RESULTS -> when (currentSearchFilter) {
                    "Все" -> "Забаненные"
                    "Забаненные" -> "Разбаненные"
                    "Разбаненные" -> "Все"
                    else -> "Все"
                }
                PendingAction.ADMIN_USER_LIST -> when (currentListFilter) {
                    "Все" -> "Забаненные"
                    "Забаненные" -> "Разбаненные"
                    "Разбаненные" -> "Все"
                    else -> "Все"
                }
                else -> "Все"
            }

            when (session.action) {
                PendingAction.ADMIN_SEARCH_RESULTS -> {
                    session.data["admin_search_filter"] = next
                    val resultsString = session.data["admin_search_results"]
                    if (resultsString != null) {
                        val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                        val searchUsers = userIds.mapNotNull { users.profile(it) }
                        RouterAdminUsers.showSearchResults(bot, chat, uid, searchUsers, 0)
                    }
                }
                PendingAction.ADMIN_USER_LIST -> {
                    session.data["admin_filter"] = next
                    RouterAdminUsers.showUserList(bot, chat, uid, 0)
                }
                else -> {
                    bot.sendMessage(chat, "❌ Фильтр недоступен в текущем режиме", replyMarkup = KeyboardAdmin.adminMenu())
                }
            }
            return
        }
        
        when (text) {
            "🛡️ Админ панель" -> {
                println("DEBUG: Показываем админскую панель для пользователя $uid")
                showAdminPanel(bot, chat, uid)
            }

            "👥 Список пользователей" -> {
                session.action = PendingAction.ADMIN_USER_LIST
                if (session.data["admin_filter"].isNullOrBlank()) {
                    session.data["admin_filter"] = "Все"
                }
                RouterAdminUsers.showUserList(bot, chat, uid)
            }
            
            "🔍 Поиск пользователей" -> {
                session.action = PendingAction.ADMIN_SEARCH
                session.data.clear()
                session.data["admin_search_filter"] = "Все"
                bot.sendMessage(chat, """🔍 <b>Поиск пользователей</b>

Введите ID, username или имя пользователя для поиска.

Примеры:
• xxxxxxxxxx (точный поиск по ID)
• @username (частичный поиск по username)
• Иван (частичный поиск по имени)""", parseMode = ParseMode.HTML, replyMarkup = KeyboardAdmin.adminBackOnlyMenu())
            }
            
            "🚫 Забаненные пользователи" -> {
                session.action = PendingAction.ADMIN_BANNED_LIST
                session.data.clear()
                RouterAdminUsers.showBannedUsers(bot, chat, uid, users)
            }
            
            "📊 Статистика" -> {
                RouterAdminStatistics.showAdminStats(bot, chat)
            }
            
            "⬅️ Обратно" -> {
                // Проверяем, находимся ли мы в режиме редактирования профиля
                if (session.action in listOf(
                    PendingAction.ADMIN_EDIT_NAME,
                    PendingAction.ADMIN_EDIT_USERNAME,
                    PendingAction.ADMIN_EDIT_BIO,
                    PendingAction.ADMIN_EDIT_BAN_REASON
                )) {
                    // Возвращаемся в меню редактирования профиля
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            RouterProfile.showAdminEditProfileForm(bot, chat, uid, targetUser)
                        } else {
                            showAdminPanel(bot, chat, uid)
                        }
                    } else {
                        showAdminPanel(bot, chat, uid)
                    }
                } else {
                    // Возвращаемся в админскую панель
                    showAdminPanel(bot, chat, uid)
                }
            }
            
            "⬅️ В список" -> {
                val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                session.context = FSMContext.MEMES  // Сброс контекста ПРОФИЛЯ
                when (session.action) {
                    PendingAction.ADMIN_USER_LIST -> RouterAdminUsers.showUserList(bot, chat, uid, currentIndex)
                    PendingAction.ADMIN_SEARCH_RESULTS -> {
                        val resultsString = session.data["admin_search_results"]
                        if (resultsString != null) {
                            val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                            val searchUsers = userIds.mapNotNull { users.profile(it) }
                            RouterAdminUsers.showSearchResults(bot, chat, uid, searchUsers, currentIndex)
                        } else {
                            showAdminPanel(bot, chat, uid)
                        }
                    }
                    else -> showAdminPanel(bot, chat, uid)
                }
            }
            
            "🚫 Забанить" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.banUser(targetUserId)) {
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                            }
                        }
                    }
                }
            }
            
            "✅ Разбанить" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.unbanUser(targetUserId)) {
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                            }
                        }
                    }
                }
            }
            
            "📝 Написать причину бана" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        session.action = PendingAction.ADMIN_EDIT_BAN_REASON
                        session.data["admin_edit_target_user"] = targetUserId.toString()
                        bot.sendMessage(chat, "📝 Введите причину бана для пользователя:", replyMarkup = KeyboardAdmin.banReasonInputMenu())
                    } else {
                        bot.sendMessage(chat, "❌ Ошибка: пользователь не найден")
                    }
                } else {
                    bot.sendMessage(chat, "❌ Эта кнопка доступна только при просмотре профиля пользователя")
                }
            }
            
            "⬅️ Отмена" -> {
                // Отмена ввода причины бана - возвращаемся в профиль пользователя
                if (session.action == PendingAction.ADMIN_EDIT_BAN_REASON) {
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            session.action = PendingAction.NONE
                            RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                        } else {
                            showAdminPanel(bot, chat, uid)
                        }
                    } else {
                        showAdminPanel(bot, chat, uid)
                    }
                } else {
                    // Для других контекстов - обычная отмена
                    showAdminPanel(bot, chat, uid)
                }
            }
            
            "⬅️ Предыдущий" -> {
                val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                val totalCount = session.data["admin_total_count"]?.toIntOrNull() ?: 1
                val newIndex = if (currentIndex == 0) totalCount - 1 else currentIndex - 1

                when (session.action) {
                    PendingAction.ADMIN_USER_LIST -> RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                    PendingAction.ADMIN_BANNED_LIST -> RouterAdminUsers.showBannedUsers(bot, chat, uid, users, newIndex)
                    else -> showAdminPanel(bot, chat, uid)
                }
            }

            "➡️ Следующий" -> {
                val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                val totalCount = session.data["admin_total_count"]?.toIntOrNull() ?: 1
                val newIndex = if (currentIndex == totalCount - 1) 0 else currentIndex + 1

                when (session.action) {
                    PendingAction.ADMIN_USER_LIST -> RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                    PendingAction.ADMIN_BANNED_LIST -> RouterAdminUsers.showBannedUsers(bot, chat, uid, users, newIndex)
                    else -> showAdminPanel(bot, chat, uid)
                }
            }
            
            "⏰ Окончание бана: есть" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.setBanExpiry(targetUserId, 5)) {
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                bot.sendMessage(chat, "✅ Установлено окончание бана: +5 дней", parseMode = ParseMode.HTML)
                                RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                            }
                        } else {
                            bot.sendMessage(chat, "❌ Ошибка при установке времени бана")
                        }
                    }
                }
            }
            
            "⏰ Окончание бана: нет" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.removeBanExpiry(targetUserId)) {
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                bot.sendMessage(chat, "✅ Окончание бана удалено", parseMode = ParseMode.HTML)
                                RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                            }
                        } else {
                            bot.sendMessage(chat, "❌ Ошибка при удалении времени бана")
                        }
                    }
                }
            }
            
            "🔧 Изменить окончание бана" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        session.action = PendingAction.ADMIN_EDIT_BAN_EXPIRY
                        session.data["admin_edit_target_user"] = targetUserId.toString()
                        bot.sendMessage(chat, "📅 Выберите количество дней для добавления к времени бана:", replyMarkup = KeyboardAdmin.banExpiryDaysMenu())
                    } else {
                        bot.sendMessage(chat, "❌ Ошибка: пользователь не найден")
                    }
                }
            }
            
            "➕ +1 день" -> {
                if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY) {
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.addBanDays(targetUserId, 1)) {
                            bot.sendMessage(chat, "✅ Добавлено 1 день к времени бана", parseMode = ParseMode.HTML)
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                session.action = PendingAction.NONE
                                RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                            }
                        }
                    }
                }
            }
            
            "➕ +3 дня" -> {
                if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY) {
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.addBanDays(targetUserId, 3)) {
                            bot.sendMessage(chat, "✅ Добавлено 3 дня к времени бана", parseMode = ParseMode.HTML)
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                session.action = PendingAction.NONE
                                RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                            }
                        }
                    }
                }
            }
            
            "➕ +5 дней" -> {
                if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY) {
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.addBanDays(targetUserId, 5)) {
                            bot.sendMessage(chat, "✅ Добавлено 5 дней к времени бана", parseMode = ParseMode.HTML)
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                session.action = PendingAction.NONE
                                RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                            }
                        }
                    }
                }
            }
            
            "➕ +10 дней" -> {
                if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY) {
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.addBanDays(targetUserId, 10)) {
                            bot.sendMessage(chat, "✅ Добавлено 10 дней к времени бана", parseMode = ParseMode.HTML)
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                session.action = PendingAction.NONE
                                RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                            }
                        }
                    }
                }
            }
            
            else -> {
                // Обработка текстового поиска
                if (session.action == PendingAction.ADMIN_SEARCH) {
                    val searchResults = AdminService.searchUsers(text, 20)
                    if (searchResults.isEmpty()) {
                        bot.sendMessage(chat, "❌ Пользователи не найдены по запросу: \"$text\"", replyMarkup = KeyboardAdmin.adminMenu())
                    } else {
                        session.action = PendingAction.ADMIN_SEARCH_RESULTS
                        session.data["admin_search_query"] = text
                        RouterAdminUsers.showSearchResults(bot, chat, uid, searchResults, 0)
                    }
                }
                else if (session.action == PendingAction.ADMIN_EDIT_BAN_REASON) {
                    // Эта обработка больше не нужна - должна быть в RouterCore.handlePending()
                    // но оставляю на случай если message попадет сюда
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.setBanReason(targetUserId, text)) {
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                bot.sendMessage(chat, "✅ Причина бана установлена: \"<b>$text</b>\"", parseMode = ParseMode.HTML)
                                session.action = PendingAction.NONE
                                session.data.clear()
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
                }
                else {
                    // Неизвестное действие
                    bot.sendMessage(chat, "❌ Неизвестное действие", replyMarkup = KeyboardAdmin.adminMenu())
                }
            }
        }
    }

    fun handleCallback(bot: Bot, callback: CallbackQuery, users: UserRepository) {
        val chat = ChatId.fromId(callback.message!!.chat.id)
        val uid = callback.from.id
        val data = callback.data

        println("DEBUG: RouterAdmin.handleCallback called with uid=$uid, data=$data")
        println("DEBUG: AdminService.isAdmin($uid) = ${AdminService.isAdmin(uid)}, adminIds = ${AdminService.adminIds}")

        if (!AdminService.isAdmin(uid)) {
            println("DEBUG: User $uid is not admin, denying access")
            bot.answerCallbackQuery(callback.id, "🚫 Доступ запрещен")
            return
        }

        println("DEBUG: User $uid is admin, processing callback")
        val session = SessionStore.get(uid)
        
        when {
            data.startsWith("admin_ban_") -> {
                val targetUserId = data.substringAfter("admin_ban_").toLongOrNull()
                if (targetUserId != null) {
                    // Проверяем, что админ не пытается забанить себя
                    if (targetUserId == uid) {
                        bot.answerCallbackQuery(callback.id, "❌ Нельзя забанить себя")
                    } else if (AdminService.banUser(targetUserId)) {
                        bot.answerCallbackQuery(callback.id, "✅ Пользователь забанен")
                        
                        // Обновляем список с переключенной кнопкой бана
                        val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                        when (session.action) {
                            PendingAction.ADMIN_USER_LIST -> RouterAdminUsers.showUserList(bot, chat, uid, currentIndex)
                            PendingAction.ADMIN_SEARCH_RESULTS -> {
                                val resultsString = session.data["admin_search_results"]
                                if (resultsString != null) {
                                    val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                                    val searchUsers = userIds.mapNotNull { users.profile(it) }
                                    RouterAdminUsers.showSearchResults(bot, chat, uid, searchUsers, currentIndex)
                                }
                            }
                            else -> {}
                        }
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Ошибка при бане")
                    }
                }
            }

            data.startsWith("admin_unban_") -> {
                val targetUserId = data.substringAfter("admin_unban_").toLongOrNull()
                if (targetUserId != null) {
                    // Проверяем, что админ не пытается разбанить себя (на случай если будет попытка отправить callback вручную)
                    if (targetUserId == uid) {
                        bot.answerCallbackQuery(callback.id, "❌ Нельзя разбанить себя (не забанены)")
                    } else if (AdminService.unbanUser(targetUserId)) {
                        bot.answerCallbackQuery(callback.id, "✅ Пользователь разбанен")
                        
                        // Обновляем список с переключенной кнопкой разбана
                        val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                        when (session.action) {
                            PendingAction.ADMIN_USER_LIST -> RouterAdminUsers.showUserList(bot, chat, uid, currentIndex)
                            PendingAction.ADMIN_SEARCH_RESULTS -> {
                                val resultsString = session.data["admin_search_results"]
                                if (resultsString != null) {
                                    val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                                    val searchUsers = userIds.mapNotNull { users.profile(it) }
                                    RouterAdminUsers.showSearchResults(bot, chat, uid, searchUsers, currentIndex)
                                }
                            }
                            else -> {}
                        }
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Ошибка при разбане")
                    }
                }
            }

            data.startsWith("admin_profile_view_") -> {
                val targetUserId = data.substringAfter("admin_profile_view_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                        bot.answerCallbackQuery(callback.id)
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Неверный индекс")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Результаты поиска не найдены")
                }
            }
            data.startsWith("admin_profile_") -> {
                val targetUserId = data.substringAfter("admin_profile_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        RouterAdminUsers.showUserProfile(bot, chat, targetUser)
                        bot.answerCallbackQuery(callback.id)
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Неверный индекс")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Результаты поиска не найдены")
                }
            }
            
            data.startsWith("admin_search_nav_") -> {
                val newIndex = data.substringAfter("admin_search_nav_").toIntOrNull()
                if (newIndex != null) {
                    val resultsString = session.data["admin_search_results"]
                    if (resultsString != null) {
                        val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                        val searchUsers = userIds.mapNotNull { users.profile(it) }
                        if (newIndex < searchUsers.size) {
                            RouterAdminUsers.showSearchResults(bot, chat, uid, searchUsers, newIndex)
                            bot.answerCallbackQuery(callback.id)
                        } else {
                            bot.answerCallbackQuery(callback.id, "❌ Неверный индекс")
                        }
                    } else {
                        // Это случай когда нет результатов поиска, но пользователь нажал навигацию
                        // Создаем пустой список для предотвращения ошибки
                        RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                    }
                }
            }

            data.startsWith("admin_search_page_") -> {
                val newIndex = data.substringAfter("admin_search_page_").toIntOrNull()
                if (newIndex != null) {
                    val resultsString = session.data["admin_search_results"]
                    if (resultsString != null) {
                        val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                        val searchUsers = userIds.mapNotNull { users.profile(it) }
                        RouterAdminUsers.showSearchResults(bot, chat, uid, searchUsers, newIndex)
                        bot.answerCallbackQuery(callback.id)
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Результаты поиска не найдены")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный индекс")
                }
            }

            data.startsWith("admin_users_page_") -> {
                val newIndex = data.substringAfter("admin_users_page_").toIntOrNull()
                if (newIndex != null) {
                    RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                    bot.answerCallbackQuery(callback.id)
                }
            }

            data.startsWith("admin_filter_") -> {
                val filterType = data.substringAfter("admin_filter_")
                session.data["admin_filter"] = filterType
                RouterAdminUsers.showUserList(bot, chat, uid, 0)
                bot.answerCallbackQuery(callback.id, "🔄 Фильтр изменен на " + filterType)
            }
            
            data == "admin_back_to_user_list" -> {
                val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                session.context = FSMContext.MEMES  // Сброс контекста при выходе из профиля
                RouterAdminUsers.showUserList(bot, chat, uid, currentIndex)
                bot.answerCallbackQuery(callback.id)
            }
            
            data.startsWith("admin_back_to_search") -> {
                session.action = PendingAction.ADMIN_SEARCH
                session.data.clear()
                bot.sendMessage(chat, "🔍 <b>Поиск пользователей</b>\n\nВведите ID, username или имя пользователя для поиска.\n\nПримеры:\n• 7266569446 (точный поиск по ID)\n• @username (частичный поиск по username)\n• Иван (частичный поиск по имени)", parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.mainMenu(isAdmin = true))
                bot.answerCallbackQuery(callback.id)
            }
            
            data.startsWith("admin_edit_profile_") -> {
                val targetUserId = data.substringAfter("admin_edit_profile_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        RouterProfile.showAdminEditProfileForm(bot, chat, uid, targetUser)
                        bot.answerCallbackQuery(callback.id)
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }
            
            data.startsWith("admin_account_stats_") -> {
                val targetUserId = data.substringAfter("admin_account_stats_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        RouterProfile.showAccountStats(bot, chat, targetUser)
                        bot.answerCallbackQuery(callback.id)
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }

            data == "admin_back_to_panel" -> {
                showAdminPanel(bot, chat, uid)
                bot.answerCallbackQuery(callback.id)
            }
            
            data.startsWith("admin_ban_reason_") -> {
                val targetUserId = data.substringAfter("admin_ban_reason_").toLongOrNull()
                if (targetUserId != null) {
                    session.action = PendingAction.ADMIN_EDIT_BAN_REASON
                    session.data["admin_edit_target_user"] = targetUserId.toString()
                    bot.sendMessage(chat, "📝 Введите причину бана для пользователя:", replyMarkup = KeyboardAdmin.banReasonInputMenu())
                    bot.answerCallbackQuery(callback.id)
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }
            
            data.startsWith("admin_edit_name_") -> {
                val targetUserId = data.substringAfter("admin_edit_name_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        session.action = PendingAction.ADMIN_EDIT_NAME
                        session.data["admin_edit_target_user"] = targetUserId.toString()
                        bot.sendMessage(chat, "✏️ <b>Изменение имени</b>\n\n👤 <b>${targetUser.displayName}</b>\n🆔 ID: ${targetUser.userId}\n\nВведите новое имя:", parseMode = ParseMode.HTML)
                        bot.answerCallbackQuery(callback.id)
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }
            
            data.startsWith("admin_edit_username_") -> {
                val targetUserId = data.substringAfter("admin_edit_username_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        session.action = PendingAction.ADMIN_EDIT_USERNAME
                        session.data["admin_edit_target_user"] = targetUserId.toString()
                        bot.sendMessage(chat, "✏️ <b>Изменение username</b>\n\n👤 <b>${targetUser.displayName}</b>\n🆔 ID: ${targetUser.userId}\n👤 Текущий username: ${if (targetUser.hideUsername) "скрыт" else "@${targetUser.username}"}\n\nВведите новый username (с @):", parseMode = ParseMode.HTML)
                        bot.answerCallbackQuery(callback.id)
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }
            
            data.startsWith("admin_edit_bio_") -> {
                val targetUserId = data.substringAfter("admin_edit_bio_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        session.action = PendingAction.ADMIN_EDIT_BIO
                        session.data["admin_edit_target_user"] = targetUserId.toString()
                        bot.sendMessage(chat, "✏️ <b>Изменение био</b>\n\n👤 <b>${targetUser.displayName}</b>\n🆔 ID: ${targetUser.userId}\n📝 Текущее био: ${targetUser.bio.ifBlank { "не указано" }}\n\nВведите новое био:", parseMode = ParseMode.HTML)
                        bot.answerCallbackQuery(callback.id)
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }
            
            data.startsWith("admin_toggle_hidden_") -> {
                val targetUserId = data.substringAfter("admin_toggle_hidden_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        users.toggleHidden(targetUserId)
                        bot.answerCallbackQuery(callback.id, "👁️ Профиль ${if (!targetUser.hidden) "скрыт" else "открыт"}")
                        RouterProfile.showAdminEditProfileForm(bot, chat, uid, targetUser.copy(hidden = !targetUser.hidden))
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }
            
            data.startsWith("admin_toggle_media_") -> {
                val targetUserId = data.substringAfter("admin_toggle_media_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        users.toggleMedia(targetUserId)
                        bot.answerCallbackQuery(callback.id, "🎬 Медиа ${if (!targetUser.showMedia) "выключено" else "включено"}")
                        RouterProfile.showAdminEditProfileForm(bot, chat, uid, targetUser.copy(showMedia = !targetUser.showMedia))
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }
            
            data.startsWith("admin_toggle_username_") -> {
                val targetUserId = data.substringAfter("admin_toggle_username_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        users.toggleHideUsername(targetUserId)
                        bot.answerCallbackQuery(callback.id, "👁️ Username ${if (!targetUser.hideUsername) "скрыт" else "открыт"}")
                        RouterProfile.showAdminEditProfileForm(bot, chat, uid, targetUser.copy(hideUsername = !targetUser.hideUsername))
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }

            data.startsWith("admin_account_stats_") -> {
                val targetUserId = data.substringAfter("admin_account_stats_").toLongOrNull()
                if (targetUserId != null) {
                    val targetUser = users.profile(targetUserId)
                    if (targetUser != null) {
                        RouterProfile.showAccountStats(bot, chat, targetUser)
                        bot.answerCallbackQuery(callback.id)
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный ID пользователя")
                }
            }

            data.startsWith("admin_back") -> {
                showAdminPanel(bot, chat, uid)
                bot.answerCallbackQuery(callback.id)
            }
            
            else -> {
                bot.answerCallbackQuery(callback.id, "❌ Неизвестная команда")
            }
        }
    }
}
