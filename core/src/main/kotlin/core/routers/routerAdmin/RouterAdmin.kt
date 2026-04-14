package core.routers.routerAdmin

import data.*
import data.models.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
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

📊 <b>Статистика:</b>
• Всего пользователей: ${AdminService.getUsersCount()}
• Забанено пользователей: ${AdminService.getBannedUsersCount()}

Выберите действие:"""
        
        bot.sendMessage(chat, statsMessage, parseMode = ParseMode.HTML, replyMarkup = KeyboardAdmin.adminMenu())
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun handleAdminAction(bot: Bot, chat: ChatId, uid: Long, text: String, users: UserRepository, memes: MemeRepository, predictions: PredictionRepository, tests: TestRepository, events: EventRepository, games: GameRepository, session: Session) {
        println("DEBUG: RouterAdmin.handleAdminAction вызван с text: '$text' для пользователя $uid")
        
        if (!AdminService.isAdmin(uid)) {
            println("DEBUG: Пользователь $uid не является админом")
            bot.sendMessage(chat, "🚫 Доступ запрещен. У вас нет админских прав.")
            return
        }
        
        println("DEBUG: Пользователь $uid является админом, обрабатываем команду")

        if (text.startsWith("🔄 Фильтр:")) {
            val current = session.data["admin_filter"] ?: "Все"
            val next = when (current) {
                "Все" -> "Забаненные"
                "Забаненные" -> "Разбаненные"
                "Разбаненные" -> "Все"
                else -> "Все"
            }
            session.data["admin_filter"] = next
            session.action = PendingAction.ADMIN_USER_LIST
            RouterAdminUsers.showUserList(bot, chat, uid, 0)
            return
        }
        
        when (text) {
            "🛡️ Админ панель" -> {
                println("DEBUG: Показываем админскую панель для пользователя $uid")
                showAdminPanel(bot, chat, uid)
            }
            
            "👥 Управление пользователями" -> {
                session.action = PendingAction.ADMIN_USER_MANAGEMENT
                session.data.clear()
                session.data["admin_filter"] = "Все" // Устанавливаем фильтр по умолчанию
                bot.sendMessage(chat, """👥 <b>Управление пользователями</b>

Выберите действие:""", parseMode = ParseMode.HTML, 
                    replyMarkup = KeyboardAdmin.adminUserManagementMenu("Все"))
            }
            
            // Обработка кнопок из меню управления пользователями
            "👥 Список пользователей" -> {
                session.action = PendingAction.ADMIN_USER_LIST
                RouterAdminUsers.showUserList(bot, chat, uid)
            }
            
            "🔍 Поиск пользователей" -> {
                session.action = PendingAction.ADMIN_SEARCH
                session.data.clear()
                bot.sendMessage(chat, """🔍 <b>Поиск пользователей</b>

Введите ID, username или имя пользователя для поиска.

Примеры:
• 7266569446 (точный поиск по ID)
• @username (частичный поиск по username)
• Иван (частичный поиск по имени)""", parseMode = ParseMode.HTML, replyMarkup = KeyboardAdmin.adminBackOnlyMenu())
            }
            
            "🚫 Забаненные пользователи" -> {
                session.action = PendingAction.ADMIN_BANNED_LIST
                session.data.clear()
                RouterAdminUsers.showBannedUsers(bot, chat, uid)
            }
            
            "📊 Статистика" -> {
                RouterAdminStatistics.showAdminStats(bot, chat)
            }
            
            "⬅️ Обратно" -> {
                // Возвращаемся в админскую панель
                showAdminPanel(bot, chat, uid)
            }
            
            "⬅️ Предыдущий" -> {
                val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                val totalCount = session.data["admin_total_count"]?.toIntOrNull() ?: 1
                val newIndex = if (currentIndex == 0) totalCount - 1 else currentIndex - 1

                when (session.action) {
                    PendingAction.ADMIN_USER_LIST -> RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                    PendingAction.ADMIN_BANNED_LIST -> RouterAdminUsers.showBannedUsers(bot, chat, uid, newIndex)
                    else -> showAdminPanel(bot, chat, uid)
                }
            }

            "➡️ Следующий" -> {
                val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                val totalCount = session.data["admin_total_count"]?.toIntOrNull() ?: 1
                val newIndex = if (currentIndex == totalCount - 1) 0 else currentIndex + 1

                when (session.action) {
                    PendingAction.ADMIN_USER_LIST -> RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                    PendingAction.ADMIN_BANNED_LIST -> RouterAdminUsers.showBannedUsers(bot, chat, uid, newIndex)
                    else -> showAdminPanel(bot, chat, uid)
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
                else {
                    // Неизвестное действие
                    bot.sendMessage(chat, "❌ Неизвестное действие", replyMarkup = KeyboardAdmin.adminMenu())
                }
            }
        }
    }

    fun handleCallback(bot: Bot, callback: com.github.kotlintelegrambot.entities.CallbackQuery, users: UserRepository) {
        val chat = ChatId.fromId(callback.message!!.chat.id)
        val uid = callback.from.id
        val data = callback.data ?: return

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
                    if (AdminService.banUser(targetUserId)) {
                        bot.answerCallbackQuery(callback.id, "✅ Пользователь забанен")
                        // Обновляем сообщение
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            RouterAdminUsers.showUserProfile(bot, chat, targetUser)
                        }
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Ошибка при бане")
                    }
                }
            }

            data.startsWith("admin_unban_") -> {
                val targetUserId = data.substringAfter("admin_unban_").toLongOrNull()
                if (targetUserId != null) {
                    if (AdminService.unbanUser(targetUserId)) {
                        bot.answerCallbackQuery(callback.id, "✅ Пользователь разбанен")
                        // Обновляем сообщение
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            RouterAdminUsers.showUserProfile(bot, chat, targetUser)
                        }
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Ошибка при разбане")
                    }
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
                    val resultsString = session.data["admin_search_results"]
                    if (resultsString != null) {
                        val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                        val userProfiles = userIds.mapNotNull { users.profile(it) }
                        if (newIndex < userProfiles.size) {
                            RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                        } else {
                            bot.answerCallbackQuery(callback.id, "❌ Неверный индекс")
                        }
                    } else {
                        RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                    }
                }
            }

            data.startsWith("admin_filter_") -> {
                val filterType = data.substringAfter("admin_filter_")
                session.data["admin_filter"] = filterType
                RouterAdminUsers.showUserList(bot, chat, uid, 0)
                bot.answerCallbackQuery(callback.id, "🔄 Фильтр изменен на " + filterType)
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
