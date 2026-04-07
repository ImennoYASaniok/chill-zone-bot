package core.routers

import data.*
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
    
    fun handleAdminAction(bot: Bot, chat: ChatId, uid: Long, text: String, users: UserRepository, memes: MemeRepository, predictions: PredictionRepository, tests: TestRepository, events: EventRepository, games: GameRepository, session: Session) {
        println("DEBUG: RouterAdmin.handleAdminAction вызван с text: '$text' для пользователя $uid")
        
        if (!AdminService.isAdmin(uid)) {
            println("DEBUG: Пользователь $uid не является админом")
            bot.sendMessage(chat, "🚫 Доступ запрещен. У вас нет админских прав.")
            return
        }
        
        println("DEBUG: Пользователь $uid является админом, обрабатываем команду")
        
        when (text) {
            "🛡️ Админ панель" -> {
                println("DEBUG: Показываем админскую панель для пользователя $uid")
                showAdminPanel(bot, chat, uid)
            }
            
            "👥 Управление пользователями" -> {
                session.action = PendingAction.ADMIN_USER_MANAGEMENT
                session.data.clear()
                bot.sendMessage(chat, """👥 <b>Управление пользователями</b>

Выберите действие:""", parseMode = ParseMode.HTML, 
                    replyMarkup = KeyboardAdmin.adminUserManagementMenu())
            }
            
            // Обработка кнопок из меню управления пользователями
            "👥 Список пользователей" -> {
                session.action = PendingAction.ADMIN_USER_LIST
                session.data.clear()
                showUserList(bot, chat, uid)
            }
            
            "🔍 Поиск пользователей" -> {
                session.action = PendingAction.ADMIN_SEARCH
                session.data.clear()
                bot.sendMessage(chat, """🔍 <b>Поиск пользователей</b>

Введите ID, username или имя пользователя для поиска.

Примеры:
• 7266569446 (точный поиск по ID)
• @username (частичный поиск по username)
• Иван (частичный поиск по имени)""", parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.mainMenu(isAdmin = true))
            }
            
            "🚫 Забаненные пользователи" -> {
                session.action = PendingAction.ADMIN_BANNED_LIST
                session.data.clear()
                showBannedUsers(bot, chat, uid)
            }
            
            " Статистика" -> {
                val totalUsers = AdminService.getUsersCount()
                val bannedUsers = AdminService.getBannedUsersCount()
                val activeUsers = totalUsers - bannedUsers
                
                val statsMessage = """📊 <b>Статистика бота</b>

👥 <b>Пользователи:</b>
• Всего: $totalUsers
• Активных: $activeUsers
• Забанено: $bannedUsers
• Процент забаненных: ${(bannedUsers.toDouble() / totalUsers * 100).toInt()}%

🔗 <b>Администраторы:</b>
• Всего: ${AdminService.adminIds.size}
• Ваш ID: $uid"""
                
                bot.sendMessage(chat, statsMessage, parseMode = ParseMode.HTML, replyMarkup = KeyboardAdmin.adminMenu())
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
                    PendingAction.ADMIN_USER_LIST -> showUserList(bot, chat, uid, newIndex)
                    PendingAction.ADMIN_BANNED_LIST -> showBannedUsers(bot, chat, uid, newIndex)
                    else -> showAdminPanel(bot, chat, uid)
                }
            }
            
            "➡️ Следующий" -> {
                val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                val totalCount = session.data["admin_total_count"]?.toIntOrNull() ?: 1
                val newIndex = if (currentIndex == totalCount - 1) 0 else currentIndex + 1
                
                when (session.action) {
                    PendingAction.ADMIN_USER_LIST -> showUserList(bot, chat, uid, newIndex)
                    PendingAction.ADMIN_BANNED_LIST -> showBannedUsers(bot, chat, uid, newIndex)
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
                        showSearchResults(bot, chat, uid, searchResults, 0)
                    }
                }
            }
        }
    }
    
    fun showUserList(bot: Bot, chat: ChatId, uid: Long, index: Int = 0) {
        val allUsers = AdminService.getAllUsers(50, 0)
        if (allUsers.isEmpty()) {
            bot.sendMessage(chat, "❌ Пользователи не найдены", replyMarkup = KeyboardAdmin.adminMenu())
            return
        }
        
        if (index >= allUsers.size) return
        
        val session = SessionStore.get(uid)
        session.action = PendingAction.ADMIN_USER_LIST
        session.data["admin_current_index"] = index.toString()
        session.data["admin_total_count"] = allUsers.size.toString()
        
        // Отображаем информацию о текущей странице
        val startIndex = index + 1
        val endIndex = minOf(index + 5, allUsers.size)
        val pageUsers = allUsers.subList(startIndex - 1, endIndex)
        
        val message = """👥 <b>Список пользователей</b> ($startIndex-$endIndex из ${allUsers.size})

${pageUsers.joinToString("\n") { user ->
            val status = if (user.isBanned) "🚫" else "✅"
            val usernameDisplay = if (user.hideUsername) "скрыт" else "@${user.username}"
            
            "$status | <b>${user.displayName}</b> | <code>$usernameDisplay</code> | ID: <code>${user.userId}</code>"
        }}

Выберите профиль для действий:"""
        
        bot.sendMessage(chat, message, parseMode = ParseMode.HTML)
        
        // Отправляем inline клавиатуру с действиями для каждого профиля
        val inlineRows = pageUsers.map { user ->
            listOf(
                com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                    text = "👤 Профиль",
                    callbackData = "admin_profile_${user.userId}"
                ),
                com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                    text = if (user.isBanned) "✅ Разбанить" else "🚫 Забанить",
                    callbackData = if (user.isBanned) "admin_unban_${user.userId}" else "admin_ban_${user.userId}"
                )
            )
        }
        
        // Добавляем навигацию
        val navRows = mutableListOf<List<com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton>>()
        
        if (index > 0) {
            navRows.add(listOf(
                com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                    text = "⬅️ Предыдущая страница",
                    callbackData = "admin_users_page_${index - 1}"
                )
            ))
        }
        
        if (endIndex < allUsers.size) {
            navRows.add(listOf(
                com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                    text = "➡️ Следующая страница",
                    callbackData = "admin_users_page_${index + 5}"
                )
            ))
        }
        
        navRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "⬅️ Обратно",
                callbackData = "admin_back"
            )
        ))
        
        val inlineKeyboard = com.github.kotlintelegrambot.entities.InlineKeyboardMarkup.create(navRows + inlineRows)
        bot.sendMessage(chat, "Действия:", replyMarkup = inlineKeyboard)
    }
    
    fun showBannedUsers(bot: Bot, chat: ChatId, uid: Long, index: Int = 0) {
        val bannedUsers = AdminService.getBannedUsers(50)
        if (bannedUsers.isEmpty()) {
            bot.sendMessage(chat, "✅ Забаненных пользователей нет", replyMarkup = KeyboardAdmin.adminMenu())
            return
        }
        
        if (index >= bannedUsers.size) return
        
        val bannedUser = bannedUsers[index]
        val session = SessionStore.get(uid)
        session.data["admin_current_index"] = index.toString()
        session.data["admin_total_count"] = bannedUsers.size.toString()
        
        val message = """🚫 <b>Забаненные пользователи</b> (${index + 1} из ${bannedUsers.size})

👤 <b>${bannedUser.displayName}</b>
🆔 ID: ${bannedUser.userId}
👤 Username: @${bannedUser.username}
⏰ Забанен: ${bannedUser.bannedAt}
📝 Причина: ${bannedUser.reason ?: "не указана"}"""
        
        bot.sendMessage(chat, message, parseMode = ParseMode.HTML,
            replyMarkup = KeyboardAdmin.adminBannedNav(index, bannedUsers.size))
        
        bot.sendMessage(chat, "Действия:", replyMarkup = KeyboardAdmin.adminBannedInline(bannedUser))
    }
    
    fun showSearchResults(bot: Bot, chat: ChatId, uid: Long, users: List<UserProfile>, index: Int = 0) {
        if (users.isEmpty()) return
        
        val user = users[index]
        val session = SessionStore.get(uid)
        session.data["admin_search_results"] = users.joinToString("|") { "${it.userId}" }
        session.data["admin_current_index"] = index.toString()
        session.data["admin_total_count"] = users.size.toString()
        
        val userStatus = if (user.isBanned) "🚫" else "✅"
        val usernameDisplay = if (user.hideUsername) "скрыт" else "@${user.username}"
        val query = session.data["admin_search_query"] ?: ""
        
        val message = """🔍 <b>Результаты поиска</b> "${query}" (${index + 1} из ${users.size})

$userStatus <b>${user.displayName}</b>
🆔 ID: ${user.userId}
👤 Username: $usernameDisplay
⭐ Рейтинг: ${user.rating}
📝 Био: ${user.bio.ifBlank { "не указано" }}
👁️ Профиль: ${if (user.hidden) "скрыт" else "видимый"}"""
        
        bot.sendMessage(chat, message, parseMode = ParseMode.HTML,
            replyMarkup = KeyboardAdmin.adminSearchNavKeyboard())
        
        bot.sendMessage(chat, "Действия:", replyMarkup = KeyboardAdmin.adminSearchResultsInline(users, index))
    }
    
    fun showUserProfile(bot: Bot, chat: ChatId, uid: Long, targetUser: UserProfile) {
        val userStatus = if (targetUser.isBanned) "🚫" else "✅"
        val usernameDisplay = if (targetUser.hideUsername) "скрыт" else "@${targetUser.username}"
        
        val message = """👤 <b>Профиль пользователя</b>

$userStatus <b>${targetUser.displayName}</b>
🆔 ID: ${targetUser.userId}
👤 Username: $usernameDisplay
⭐ Рейтинг: ${targetUser.rating}
📝 Био: ${targetUser.bio.ifBlank { "не указано" }}
👁️ Профиль: ${if (targetUser.hidden) "скрыт" else "видимый"}
🎬 Медиа: ${if (targetUser.showMedia) "включено" else "выключено"}"""
        
        bot.sendMessage(chat, message, parseMode = ParseMode.HTML)
        
        // Создаем inline клавиатуру с действиями и кнопкой возврата
        val actionRows = mutableListOf<List<com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton>>()
        
        // Кнопка профиля
        actionRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "👤 Профиль",
                callbackData = "admin_profile_${targetUser.userId}"
            )
        ))
        
        // Кнопка бана/разбана
        actionRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = if (targetUser.isBanned) "✅ Разбанить" else "🚫 Забанить",
                callbackData = if (targetUser.isBanned) "admin_unban_${targetUser.userId}" else "admin_ban_${targetUser.userId}"
            )
        ))
        
        // Кнопка возврата в поиск
        actionRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "⬅️ Обратно в поиск",
                callbackData = "admin_back_to_search"
            )
        ))
        
        val inlineKeyboard = com.github.kotlintelegrambot.entities.InlineKeyboardMarkup.create(actionRows)
        bot.sendMessage(chat, "Действия:", replyMarkup = inlineKeyboard)
    }
    
    fun handleCallback(bot: Bot, callback: com.github.kotlintelegrambot.entities.CallbackQuery, users: UserRepository) {
        val chat = ChatId.fromId(callback.message!!.chat.id)
        val uid = callback.from.id
        
        if (!AdminService.isAdmin(uid)) {
            bot.answerCallbackQuery(callback.id, "🚫 Доступ запрещен")
            return
        }
        
        val data = callback.data ?: return
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
                            showUserProfile(bot, chat, uid, targetUser)
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
                            showUserProfile(bot, chat, uid, targetUser)
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
                        showUserProfile(bot, chat, uid, targetUser)
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
                            showSearchResults(bot, chat, uid, searchUsers, newIndex)
                            bot.answerCallbackQuery(callback.id)
                        } else {
                            bot.answerCallbackQuery(callback.id, "❌ Неверный индекс")
                        }
                    } else {
                        // Это случай когда нет результатов поиска, но пользователь нажал навигацию
                        // Создаем пустой список для предотвращения ошибки
                        showUserList(bot, chat, uid, newIndex)
                    }
                }
            }
            
            data.startsWith("admin_users_page_") -> {
                val newIndex = data.substringAfter("admin_users_page_").toIntOrNull()
                if (newIndex != null) {
                    val resultsString = session.data["admin_search_results"]
                    if (resultsString != null) {
                        val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                        val users = userIds.mapNotNull { users.profile(it) }
                        if (newIndex < users.size) {
                            showUserList(bot, chat, uid, newIndex)
                        } else {
                            bot.answerCallbackQuery(callback.id, "❌ Неверный индекс")
                        }
                    } else {
                        showUserList(bot, chat, uid, newIndex)
                    }
                }
            }
            
            data.startsWith("admin_back_to_search") -> {
                session.action = PendingAction.ADMIN_SEARCH
                session.data.clear()
                bot.sendMessage(chat, """🔍 <b>Поиск пользователей</b>

Введите ID, username или имя пользователя для поиска.

Примеры:
• 7266569446 (точный поиск по ID)
• @username (частичный поиск по username)
• Иван (частичный поиск по имени)""", parseMode = ParseMode.HTML, replyMarkup = KeyboardFactory.mainMenu(isAdmin = true))
                bot.answerCallbackQuery(callback.id)
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
