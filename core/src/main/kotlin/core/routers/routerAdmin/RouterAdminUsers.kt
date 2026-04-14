package core.routers.routerAdmin

import data.*
import data.models.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import core.keyboards.KeyboardAdmin
import core.keyboards.KeyboardFactory
import core.SessionStore
import core.PendingAction
import core.Session
import core.FSMContext

object RouterAdminUsers {
    fun showUserList(bot: Bot, chat: ChatId, uid: Long, index: Int = 0) {
        val session = SessionStore.get(uid)
        val filter = session.data["admin_filter"] ?: "Все"
        
        // Получаем всех пользователей и применяем фильтр
        val allUsers = AdminService.getAllUsers(50, 0)
        val filteredUsers = when (filter) {
            "Все" -> allUsers
            "Разбаненные" -> allUsers.filter { !it.isBanned }
            "Забаненные" -> allUsers.filter { it.isBanned }
            else -> allUsers
        }
        
        if (filteredUsers.isEmpty()) {
            val filterMessage = when (filter) {
                "Все" -> "пользователей"
                "Разбаненные" -> "разбаненных пользователей"
                "Забаненные" -> "забаненных пользователей"
                else -> "пользователей"
            }
            bot.sendMessage(chat, "❌ $filterMessage не найдены", replyMarkup = KeyboardAdmin.adminMenu())
            return
        }
        
        if (index >= filteredUsers.size) return
        
        session.action = PendingAction.ADMIN_USER_LIST
        session.data["admin_current_index"] = index.toString()
        session.data["admin_total_count"] = filteredUsers.size.toString()
        
        // Отображаем информацию о текущей странице
        val startIndex = index + 1
        val endIndex = minOf(index + 5, filteredUsers.size)
        val pageUsers = filteredUsers.subList(startIndex - 1, endIndex)
        
        val filterText = when (filter) {
            "Все" -> "всех пользователей"
            "Разбаненные" -> "разбаненных пользователей"
            "Забаненные" -> "забаненных пользователей"
            else -> "пользователей"
        }
        
        val message = """👥 <b>Список пользователей</b> ($startIndex-$endIndex из ${filteredUsers.size}) - $filterText

Выберите пользователя:"""

        // Создаем inline клавиатуру с действиями для каждого профиля
        val inlineRows = pageUsers.map { user ->
            val usernameButtonText = when {
                user.username.isBlank() -> "👤 не указан"
                else -> "👤 @${user.username}"
            }
            listOf(
                com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                    text = usernameButtonText,
                    callbackData = "admin_profile_view_${user.userId}"
                ),
                com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                    text = if (user.isBanned) "✅ Разбанить" else "🚫 Забанить",
                    callbackData = if (user.isBanned) "admin_unban_${user.userId}" else "admin_ban_${user.userId}"
                )
            )
        }

        // Добавляем навигацию (стрелки) сверху в inline клавиатуру
        val navRows = mutableListOf<List<com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton>>()

        // Стрелки показываем только если больше 5 пользователей
        if (filteredUsers.size > 5) {
            // Циклическая навигация: последняя страница -> первая, первая -> последняя
            val lastPageIndex = ((filteredUsers.size - 1) / 5) * 5
            val prevIndex = if (index == 0) lastPageIndex else index - 5
            val nextIndex = if (index + 5 >= filteredUsers.size) 0 else index + 5

            navRows.add(
                listOf(
                    com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                        text = "⬅️",
                        callbackData = "admin_users_page_$prevIndex"
                    ),
                    com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                        text = "➡️",
                        callbackData = "admin_users_page_$nextIndex"
                    )
                )
            )
        }

        val inlineKeyboard = com.github.kotlintelegrambot.entities.InlineKeyboardMarkup.create(navRows + inlineRows)

        bot.sendMessage(chatId = chat, text = message, parseMode = ParseMode.HTML, replyMarkup = KeyboardAdmin.adminUserListMenu(filter))
        bot.sendMessage(chatId = chat, text = "📌 Навигация и действия:", replyMarkup = inlineKeyboard)
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
            replyMarkup = KeyboardAdmin.adminBannedNav(bannedUsers.size))
        
        bot.sendMessage(chat, "Действия:", replyMarkup = KeyboardAdmin.adminBannedInline(bannedUser))
    }
    
    fun showSearchResults(bot: Bot, chat: ChatId, uid: Long, users: List<UserProfile>, index: Int = 0) {
        if (users.isEmpty()) return

        val session = SessionStore.get(uid)
        val query = session.data["admin_search_query"] ?: ""
        val filter = session.data["admin_search_filter"] ?: "Все"

        // Применяем фильтр к результатам поиска
        val filteredUsers = when (filter) {
            "Все" -> users
            "Разбаненные" -> users.filter { !it.isBanned }
            "Забаненные" -> users.filter { it.isBanned }
            else -> users
        }

        if (filteredUsers.isEmpty()) {
            bot.sendMessage(chat, "❌ По вашему поиску и фильтру не найдено пользователей", replyMarkup = KeyboardAdmin.adminMenu())
            return
        }

        val startIndex = index + 1
        val endIndex = minOf(index + 5, filteredUsers.size)
        val pageUsers = filteredUsers.subList(startIndex - 1, endIndex)

        session.data["admin_search_results"] = filteredUsers.joinToString("|") { "${it.userId}" }
        session.data["admin_current_index"] = index.toString()
        session.data["admin_total_count"] = filteredUsers.size.toString()

        val message = """🔍 <b>Поиск по</b> "$query" ($startIndex-$endIndex из ${filteredUsers.size})

Выберите пользователя:"""

        val inlineRows = pageUsers.map { user ->
            val usernameButtonText = when {
                user.username.isBlank() -> "👤 не указан"
                else -> "👤 @${user.username}"
            }
            listOf(
                com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                    text = usernameButtonText,
                    callbackData = "admin_profile_${user.userId}"
                ),
                com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                    text = if (user.isBanned) "✅ Разбанить" else "🚫 Забанить",
                    callbackData = if (user.isBanned) "admin_unban_${user.userId}" else "admin_ban_${user.userId}"
                )
            )
        }

        val navRows = mutableListOf<List<com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton>>()
        
        // Стрелки показываем только если больше 5 пользователей
        if (filteredUsers.size > 5) {
            // Циклическая навигация: последняя страница -> первая, первая -> последняя
            val lastPageIndex = ((filteredUsers.size - 1) / 5) * 5
            val prevIndex = if (index == 0) lastPageIndex else index - 5
            val nextIndex = if (index + 5 >= filteredUsers.size) 0 else index + 5

            navRows.add(
                listOf(
                    com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                        text = "⬅️",
                        callbackData = "admin_search_page_$prevIndex"
                    ),
                    com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                        text = "➡️",
                        callbackData = "admin_search_page_$nextIndex"
                    )
                )
            )
        }

        val inlineKeyboard = com.github.kotlintelegrambot.entities.InlineKeyboardMarkup.create(navRows + inlineRows)

        bot.sendMessage(chatId = chat, text = message, parseMode = ParseMode.HTML, replyMarkup = KeyboardAdmin.adminSearchMenu(filter))
        bot.sendMessage(chatId = chat, text = " ", replyMarkup = inlineKeyboard)
    }

    fun showUserProfileView(bot: Bot, chat: ChatId, uid: Long, targetUser: UserProfile) {
        val session = SessionStore.get(uid)
        session.context = FSMContext.PROFILE_VIEW
        session.data["admin_profile_view_user_id"] = targetUser.userId.toString()
        
        val usernameDisplay = "@${targetUser.username}"  // Админ видит все username

        val nameValue = targetUser.displayName
            .trim()
            .removePrefix("@")
            .ifBlank { "не указано" }
            .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }

        val registrationDate = targetUser.createdAt?.let { date: java.time.LocalDateTime ->
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            date.format(formatter)
        } ?: "неизвестно"

        val statusText = if (targetUser.isBanned) {
            """🚫 Забанен
📅 Дата бана: ${targetUser.bannedAt ?: "неизвестно"}
📝 Причина: ${targetUser.reason ?: "не указана"}"""
        } else {
            "✅ Активен"
        }

        val adminText = if (targetUser.isAdmin) {
            "\n\n🛡️ Администратор"
        } else {
            ""
        }

        val message = """👤 <b>Профиль пользователя</b>

🏷️ Имя: $nameValue
🆔 ID: ${targetUser.userId}
👤 Username: $usernameDisplay

📈 <b>Основная информация:</b>
⭐ Рейтинг: ${targetUser.rating}
📝 Био: ${targetUser.bio.ifBlank { "не указано" }}
📅 Дата регистрации: $registrationDate

<b>Статус:</b>
$statusText$adminText"""

        val currentAdmin = data.AdminService.isAdmin(uid)
        val keyboard = if (currentAdmin) {
            KeyboardAdmin.profileViewAdminMenu(targetUser.isBanned)
        } else {
            KeyboardAdmin.profileViewBackMenu()
        }

        bot.sendMessage(chatId = chat, text = message, parseMode = ParseMode.HTML, replyMarkup = keyboard)
    }

    fun showUserProfileView(bot: Bot, chat: ChatId, targetUser: UserProfile) {
        val usernameDisplay = if (targetUser.hideUsername) "скрыт" else "@${targetUser.username}"

        val nameValue = targetUser.displayName
            .trim()
            .removePrefix("@")
            .ifBlank { "не указано" }
            .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }

        val message = """👤 <b>Профиль пользователя</b>

🏷️ Имя: $nameValue
🆔 ID: ${targetUser.userId}
👤 Username: $usernameDisplay
⭐ Рейтинг: ${targetUser.rating}
📝 Био: ${targetUser.bio.ifBlank { "не указано" }}
👁️ Профиль: ${if (targetUser.hidden) "скрыт" else "видимый"}"""

        val inlineButtons = listOf(
            listOf(
                com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                    text = "⬅️ В список",
                    callbackData = "admin_back_to_user_list"
                )
            )
        )

        bot.sendMessage(chatId = chat, text = message, parseMode = ParseMode.HTML,
            replyMarkup = com.github.kotlintelegrambot.entities.InlineKeyboardMarkup.create(inlineButtons))
    }

    fun showUserProfile(bot: Bot, chat: ChatId, targetUser: UserProfile) {
        val usernameDisplay = if (targetUser.hideUsername) "скрыт" else "@${targetUser.username}"

        val nameValue = targetUser.displayName
            .trim()
            .removePrefix("@")
            .ifBlank { "не указано" }
            .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }

        val message = """👤 <b>Профиль пользователя</b>

🏷️ Имя: $nameValue
🆔 ID: ${targetUser.userId}
👤 Username: $usernameDisplay
⭐ Рейтинг: ${targetUser.rating}
📝 Био: ${targetUser.bio.ifBlank { "не указано" }}
👁️ Профиль: ${if (targetUser.hidden) "скрыт" else "видимый"}"""

        // Создаем inline клавиатуру с действиями
        val actionRows = mutableListOf<List<com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton>>()

        // Кнопка изменения профиля
        actionRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "✏️ Изменить профиль",
                callbackData = "admin_edit_profile_${targetUser.userId}"
            )
        ))

        // Кнопка статистики аккаунта
        actionRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "📊 Статистика аккаунта",
                callbackData = "admin_account_stats_${targetUser.userId}"
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

        // Кнопка админ панели (в конце)
        actionRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "🛡️ Админ панель",
                callbackData = "admin_back"
            )
        ))

        val inlineKeyboard = com.github.kotlintelegrambot.entities.InlineKeyboardMarkup.create(actionRows)
        bot.sendMessage(chat, message, parseMode = ParseMode.HTML, replyMarkup = inlineKeyboard)
    }
}
