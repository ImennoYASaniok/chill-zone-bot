package core.routers.routerAdmin

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.CallbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import core.FSMContext
import core.PendingAction
import core.Session
import core.SessionStore
import core.keyboards.KeyboardAdmin
import core.keyboards.KeyboardFactory
import core.routers.RouterProfile
import data.*
import data.models.*
import data.repositories.UserRepository
import data.services.AdminService
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

object RouterAdmin {
    fun showAdminPanel(bot: Bot, chat: ChatId, uid: Long) {
        if (!AdminService.isAdmin(uid)) {
            bot.sendMessage(chat, "🚫 Доступ запрещен. У вас нет админских прав.")
            return
        }

        val statsMessage = """🛡️ <b>Админ панель</b>

Выберите действие:"""

        bot.sendMessage(
                chat,
                statsMessage,
                parseMode = ParseMode.HTML,
                replyMarkup = KeyboardAdmin.adminMenu()
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun handleAdminAction(
            bot: Bot,
            chat: ChatId,
            uid: Long,
            text: String,
            users: UserRepository,
            session: Session
    ) {
        if (!AdminService.isAdmin(uid)) {
            bot.sendMessage(chat, "🚫 Доступ запрещен. У вас нет админских прав.")
            return
        }

        fun formatRemainingBanDaysHours(banExpiresAt: String?): String {
            if (banExpiresAt.isNullOrBlank()) return "0 дней, 0 часов"
            return try {
                val expires =
                        try {
                            OffsetDateTime.parse(banExpiresAt)
                        } catch (_: Exception) {
                            OffsetDateTime.parse(banExpiresAt.replace(' ', 'T'))
                        }
                val now = OffsetDateTime.now(ZoneOffset.UTC)
                val duration = Duration.between(now, expires)
                val totalMinutes = duration.toMinutes().coerceAtLeast(0)
                val days = totalMinutes / (24 * 60)
                val hours = (totalMinutes % (24 * 60)) / 60
                val minutes = totalMinutes % 60
                "$days дней, $hours часов, $minutes минут"
            } catch (_: Exception) {
                "0 дней, 0 часов"
            }
        }

        fun showBanEditMenu() {
            bot.sendMessage(
                    chat,
                    "⏳ Выберите, сколько добавить ко времени бана:\n" +
                            "Команда для точной установки времени: /set_time_a_b_c\n" +
                            "a - дни, b - часы, c - минуты",
                    replyMarkup = KeyboardAdmin.banAddTimeMenu()
            )
        }

        // Команда точной установки времени бана работает только в меню изменения времени
        if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY &&
                        session.context == FSMContext.PROFILE_VIEW
        ) {
            val m = Regex("^/set_time_(\\d+)_(\\d+)_(\\d+)$").matchEntire(text.trim())
            if (m != null) {
                val days = m.groupValues[1].toIntOrNull()
                val hours = m.groupValues[2].toIntOrNull()
                val minutes = m.groupValues[3].toIntOrNull()

                val valid =
                        days != null &&
                                hours != null &&
                                minutes != null &&
                                days >= 0 &&
                                hours in 0..23 &&
                                minutes in 0..59

                val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                if (!valid || targetUserId == null) {
                    bot.sendMessage(chat, "Неправильный вызов команды")
                    session.action = PendingAction.ADMIN_EDIT_BAN_EXPIRY
                    showBanEditMenu()
                    return
                }

                val ok = AdminService.setBanExactDuration(targetUserId, days!!, hours!!, minutes!!)
                if (!ok) {
                    bot.sendMessage(chat, "❌ Не удалось изменить время бана")
                    session.action = PendingAction.ADMIN_EDIT_BAN_EXPIRY
                    showBanEditMenu()
                    return
                }

                session.action = PendingAction.ADMIN_EDIT_BAN_EXPIRY
                showBanEditMenu()

                val targetUser = users.profile(targetUserId)
                val remaining = formatRemainingBanDaysHours(targetUser?.banExpiresAt)
                bot.sendMessage(chat, "✅Вы успешно изменил время\nТекущее время бана $remaining")
                return
            }
        }

        // Обработка фильтра для списка или поиска
        if (text.startsWith("🔄 Фильтр:")) {
            val currentSearchFilter = session.data["admin_search_filter"] ?: "Все"
            val currentListFilter = session.data["admin_filter"] ?: "Все"
            val next =
                    when (session.action) {
                        PendingAction.ADMIN_SEARCH_RESULTS ->
                                when (currentSearchFilter) {
                                    "Все" -> "Забаненные"
                                    "Забаненные" -> "Разбаненные"
                                    "Разбаненные" -> "Все"
                                    else -> "Все"
                                }
                        PendingAction.ADMIN_USER_LIST ->
                                when (currentListFilter) {
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
                    bot.sendMessage(
                            chat,
                            "❌ Фильтр недоступен в текущем режиме",
                            replyMarkup = KeyboardAdmin.adminMenu()
                    )
                }
            }
            return
        }

        when (text) {
            "🛡️ Админ панель" -> {
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
                bot.sendMessage(
                        chat,
                        """🔍 <b>Поиск пользователей</b>

Введите ID, username или имя пользователя для поиска.

Примеры:
• xxxxxxxxxx (точный поиск по ID)
• @username (частичный поиск по username)
• Иван (частичный поиск по имени)""",
                        parseMode = ParseMode.HTML,
                        replyMarkup = KeyboardAdmin.adminBackOnlyMenu()
                )
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
                if (session.action in
                                listOf(
                                        PendingAction.ADMIN_EDIT_NAME,
                                        PendingAction.ADMIN_EDIT_USERNAME,
                                        PendingAction.ADMIN_EDIT_BIO,
                                        PendingAction.ADMIN_EDIT_BAN_REASON
                                )
                ) {
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
                session.context = FSMContext.MEMES // Сброс контекста ПРОФИЛЯ
                when (session.action) {
                    PendingAction.ADMIN_USER_LIST ->
                            RouterAdminUsers.showUserList(bot, chat, uid, currentIndex)
                    PendingAction.ADMIN_SEARCH_RESULTS -> {
                        val resultsString = session.data["admin_search_results"]
                        if (resultsString != null) {
                            val userIds = resultsString.split("|").mapNotNull { it.toLongOrNull() }
                            val searchUsers = userIds.mapNotNull { users.profile(it) }
                            RouterAdminUsers.showSearchResults(
                                    bot,
                                    chat,
                                    uid,
                                    searchUsers,
                                    currentIndex
                            )
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
                                RouterAdminUsers.showUserProfileView(
                                        bot,
                                        chat,
                                        uid,
                                        targetUser,
                                        users
                                )
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
                                RouterAdminUsers.showUserProfileView(
                                        bot,
                                        chat,
                                        uid,
                                        targetUser,
                                        users
                                )
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
                        bot.sendMessage(
                                chat,
                                "📝 Введите причину бана для пользователя:",
                                replyMarkup = KeyboardAdmin.banReasonInputMenu()
                        )
                    } else {
                        bot.sendMessage(chat, "❌ Ошибка: пользователь не найден")
                    }
                } else {
                    bot.sendMessage(
                            chat,
                            "❌ Эта кнопка доступна только при просмотре профиля пользователя"
                    )
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
                    PendingAction.ADMIN_USER_LIST ->
                            RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                    PendingAction.ADMIN_BANNED_LIST ->
                            RouterAdminUsers.showBannedUsers(bot, chat, uid, users, newIndex)
                    else -> showAdminPanel(bot, chat, uid)
                }
            }
            "➡️ Следующий" -> {
                val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                val totalCount = session.data["admin_total_count"]?.toIntOrNull() ?: 1
                val newIndex = if (currentIndex == totalCount - 1) 0 else currentIndex + 1

                when (session.action) {
                    PendingAction.ADMIN_USER_LIST ->
                            RouterAdminUsers.showUserList(bot, chat, uid, newIndex)
                    PendingAction.ADMIN_BANNED_LIST ->
                            RouterAdminUsers.showBannedUsers(bot, chat, uid, users, newIndex)
                    else -> showAdminPanel(bot, chat, uid)
                }
            }
            "Тип бана: навсегда" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.setBanTemporary(targetUserId)) {
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                RouterAdminUsers.showUserProfileView(
                                        bot,
                                        chat,
                                        uid,
                                        targetUser,
                                        users
                                )
                            }
                        }
                    }
                }
            }
            "Тип бана: временно" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.removeBanExpiry(targetUserId)) {
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                RouterAdminUsers.showUserProfileView(
                                        bot,
                                        chat,
                                        uid,
                                        targetUser,
                                        users
                                )
                            }
                        }
                    }
                }
            }
            "Изменить время бана" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    session.action = PendingAction.ADMIN_EDIT_BAN_EXPIRY
                    showBanEditMenu()
                }
            }
            "🕒 Получить точное время бана" -> {
                if (session.context == FSMContext.PROFILE_VIEW) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val info = AdminService.getBanExactTime(targetUserId)
                        if (info == null) {
                            bot.sendMessage(chat, "❌ Не удалось получить время бана из БД")
                        } else {
                            val serverTimeFormatted =
                                    try {
                                        val parsed =
                                                java.time.OffsetDateTime.parse(info.serverNowIsoUtc)
                                        val formatter =
                                                java.time.format.DateTimeFormatter.ofPattern(
                                                        "dd-MM-yyyy HH:mm:ss"
                                                )
                                        parsed.format(formatter)
                                    } catch (e: Exception) {
                                        info.serverNowIsoUtc
                                    }

                            val bannedAtFormatted =
                                    info.bannedAtIsoUtc?.let {
                                        try {
                                            val parsed = java.time.OffsetDateTime.parse(it)
                                            val formatter =
                                                    java.time.format.DateTimeFormatter.ofPattern(
                                                            "dd-MM-yyyy HH:mm:ss"
                                                    )
                                            parsed.format(formatter)
                                        } catch (e: Exception) {
                                            it
                                        }
                                    }
                                            ?: "неизвестно"

                            val remainingFormatted =
                                    if (info.remainingText == "∞") {
                                        "∞"
                                    } else {
                                        val parts = info.remainingText.split("д", "ч", "м", "с")
                                        if (parts.size >= 4) {
                                            "${parts[0].trim()}д ${parts[1].trim()}ч ${parts[2].trim()}м ${parts[3].trim()}с"
                                        } else {
                                            info.remainingText
                                        }
                                    }

                            bot.sendMessage(
                                    chat,
                                    "🕒 Точное время бана\n" +
                                            "ID: ${targetUserId}\n" +
                                            "Username: <code>@${info.username}</code>\n" +
                                            "Серверное время: $serverTimeFormatted\n" +
                                            "Бан получен в: $bannedAtFormatted\n" +
                                            "Осталось: $remainingFormatted",
                                    parseMode = ParseMode.HTML
                            )
                        }
                    }
                }
            }
            "➕ 5 часов",
            "➕ 10 часов",
            "➖ 5 часов",
            "➖ 10 часов",
            "➕ 1 дней",
            "➕ 10 дней",
            "➖ 1 дней",
            "➖ 10 дней" -> {
                if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY &&
                                session.context == FSMContext.PROFILE_VIEW
                ) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val ok =
                                when (text) {
                                    "➕ 5 часов" -> AdminService.addBanHours(targetUserId, 5)
                                    "➕ 10 часов" -> AdminService.addBanHours(targetUserId, 10)
                                    "➖ 5 часов" -> AdminService.addBanHours(targetUserId, -5)
                                    "➖ 10 часов" -> AdminService.addBanHours(targetUserId, -10)
                                    "➕ 1 дней" -> AdminService.addBanDays(targetUserId, 1)
                                    "➕ 10 дней" -> AdminService.addBanDays(targetUserId, 10)
                                    "➖ 1 дней" -> AdminService.addBanDays(targetUserId, -1)
                                    "➖ 10 дней" -> AdminService.addBanDays(targetUserId, -10)
                                    else -> false
                                }

                        if (ok) {
                            // остаёмся в режиме изменения времени
                            session.action = PendingAction.ADMIN_EDIT_BAN_EXPIRY
                            showBanEditMenu()

                            val targetUser = users.profile(targetUserId)
                            val remaining = formatRemainingBanDaysHours(targetUser?.banExpiresAt)
                            bot.sendMessage(
                                    chat,
                                    "✅Вы успешно изменил время\nТекущее время бана $remaining"
                            )
                        } else {
                            bot.sendMessage(chat, "❌ Не удалось изменить время бана")
                        }
                    }
                }
            }
            "⬅️ К профилю" -> {
                if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY &&
                                session.context == FSMContext.PROFILE_VIEW
                ) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            session.action = PendingAction.NONE
                            RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                        }
                    }
                    return
                }
            }
            "⬅️ Отмена" -> {
                if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY &&
                                session.context == FSMContext.PROFILE_VIEW
                ) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            session.action = PendingAction.NONE
                            RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                        }
                    }
                    return
                }

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
                    }
                }
            }
            else -> {
                // Обработка текстового поиска
                if (session.action == PendingAction.ADMIN_SEARCH) {
                    val searchResults = AdminService.searchUsers(text, 20)
                    if (searchResults.isEmpty()) {
                        bot.sendMessage(
                                chat,
                                "❌ Пользователи не найдены по запросу: \"$text\"",
                                replyMarkup = KeyboardAdmin.adminMenu()
                        )
                    } else {
                        session.action = PendingAction.ADMIN_SEARCH_RESULTS
                        session.data["admin_search_query"] = text
                        RouterAdminUsers.showSearchResults(bot, chat, uid, searchResults, 0)
                    }
                } else if (session.action == PendingAction.ADMIN_EDIT_BAN_REASON) {
                    // Эта обработка больше не нужна - должна быть в RouterCore.handlePending()
                    // но оставляю на случай если message попадет сюда
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        if (AdminService.setBanReason(targetUserId, text)) {
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                bot.sendMessage(
                                        chat,
                                        "✅ Причина бана установлена: \"<b>$text</b>\"",
                                        parseMode = ParseMode.HTML
                                )
                                session.action = PendingAction.NONE
                                session.data.clear()
                                RouterAdminUsers.showUserProfileView(
                                        bot,
                                        chat,
                                        uid,
                                        targetUser,
                                        users
                                )
                            } else {
                                bot.sendMessage(chat, "❌ Ошибка: пользователь не найден")
                            }
                        } else {
                            bot.sendMessage(chat, "❌ Ошибка при сохранении причины бана")
                        }
                    } else {
                        bot.sendMessage(chat, "❌ Ошибка: не указан пользователь")
                    }
                } else {
                    // Неизвестное действие
                    bot.sendMessage(
                            chat,
                            "❌ Неизвестное действие",
                            replyMarkup = KeyboardAdmin.adminMenu()
                    )
                }
            }
        }
    }

    fun handleCallback(bot: Bot, callback: CallbackQuery, users: UserRepository) {
        val chat = ChatId.fromId(callback.message!!.chat.id)
        val uid = callback.from.id
        val data = callback.data

        if (!AdminService.isAdmin(uid)) {
            bot.answerCallbackQuery(callback.id, "🚫 Доступ запрещен")
            return
        }
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
                            PendingAction.ADMIN_USER_LIST ->
                                    RouterAdminUsers.showUserList(bot, chat, uid, currentIndex)
                            PendingAction.ADMIN_SEARCH_RESULTS -> {
                                val resultsString = session.data["admin_search_results"]
                                if (resultsString != null) {
                                    val userIds =
                                            resultsString.split("|").mapNotNull {
                                                it.toLongOrNull()
                                            }
                                    val searchUsers = userIds.mapNotNull { users.profile(it) }
                                    RouterAdminUsers.showSearchResults(
                                            bot,
                                            chat,
                                            uid,
                                            searchUsers,
                                            currentIndex
                                    )
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
                    // Проверяем, что админ не пытается разбанить себя (на случай если будет попытка
                    // отправить callback вручную)
                    if (targetUserId == uid) {
                        bot.answerCallbackQuery(
                                callback.id,
                                "❌ Нельзя разбанить себя (не забанены)"
                        )
                    } else if (AdminService.unbanUser(targetUserId)) {
                        bot.answerCallbackQuery(callback.id, "✅ Пользователь разбанен")

                        // Обновляем список с переключенной кнопкой разбана
                        val currentIndex = session.data["admin_current_index"]?.toIntOrNull() ?: 0
                        when (session.action) {
                            PendingAction.ADMIN_USER_LIST ->
                                    RouterAdminUsers.showUserList(bot, chat, uid, currentIndex)
                            PendingAction.ADMIN_SEARCH_RESULTS -> {
                                val resultsString = session.data["admin_search_results"]
                                if (resultsString != null) {
                                    val userIds =
                                            resultsString.split("|").mapNotNull {
                                                it.toLongOrNull()
                                            }
                                    val searchUsers = userIds.mapNotNull { users.profile(it) }
                                    RouterAdminUsers.showSearchResults(
                                            bot,
                                            chat,
                                            uid,
                                            searchUsers,
                                            currentIndex
                                    )
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
                            RouterAdminUsers.showSearchResults(
                                    bot,
                                    chat,
                                    uid,
                                    searchUsers,
                                    newIndex
                            )
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
                session.context = FSMContext.MEMES // Сброс контекста при выходе из профиля
                RouterAdminUsers.showUserList(bot, chat, uid, currentIndex)
                bot.answerCallbackQuery(callback.id)
            }
            data.startsWith("admin_back_to_search") -> {
                session.action = PendingAction.ADMIN_SEARCH
                session.data.clear()
                bot.sendMessage(
                        chat,
                        "🔍 <b>Поиск пользователей</b>\n\nВведите ID, username или имя пользователя для поиска.\n\nПримеры:\n• 7266569446 (точный поиск по ID)\n• @username (частичный поиск по username)\n• Иван (частичный поиск по имени)",
                        parseMode = ParseMode.HTML,
                        replyMarkup = KeyboardFactory.mainMenu(isAdmin = true)
                )
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
                    bot.sendMessage(
                            chat,
                            "📝 Введите причину бана для пользователя:",
                            replyMarkup = KeyboardAdmin.banReasonInputMenu()
                    )
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
                        bot.sendMessage(
                                chat,
                                "✏️ <b>Изменение имени</b>\n\n👤 <b>${targetUser.displayName}</b>\n🆔 ID: ${targetUser.userId}\n\nВведите новое имя:",
                                parseMode = ParseMode.HTML
                        )
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
                        bot.sendMessage(
                                chat,
                                "✏️ <b>Изменение username</b>\n\n👤 <b>${targetUser.displayName}</b>\n🆔 ID: ${targetUser.userId}\n👤 Текущий username: ${if (targetUser.hideUsername) "скрыт" else "@${targetUser.username}"}\n\nВведите новый username (с @):",
                                parseMode = ParseMode.HTML
                        )
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
                        bot.sendMessage(
                                chat,
                                "✏️ <b>Изменение био</b>\n\n👤 <b>${targetUser.displayName}</b>\n🆔 ID: ${targetUser.userId}\n📝 Текущее био: ${targetUser.bio.ifBlank { "не указано" }}\n\nВведите новое био:",
                                parseMode = ParseMode.HTML
                        )
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
                        bot.answerCallbackQuery(
                                callback.id,
                                "👁️ Профиль ${if (!targetUser.hidden) "скрыт" else "открыт"}"
                        )
                        RouterProfile.showAdminEditProfileForm(
                                bot,
                                chat,
                                uid,
                                targetUser.copy(hidden = !targetUser.hidden)
                        )
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
                        bot.answerCallbackQuery(
                                callback.id,
                                "🎬 Медиа ${if (!targetUser.showMedia) "выключено" else "включено"}"
                        )
                        RouterProfile.showAdminEditProfileForm(
                                bot,
                                chat,
                                uid,
                                targetUser.copy(showMedia = !targetUser.showMedia)
                        )
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
                        bot.answerCallbackQuery(
                                callback.id,
                                "👁️ Username ${if (!targetUser.hideUsername) "скрыт" else "открыт"}"
                        )
                        RouterProfile.showAdminEditProfileForm(
                                bot,
                                chat,
                                uid,
                                targetUser.copy(hideUsername = !targetUser.hideUsername)
                        )
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
            data.startsWith("ban_add_") || data.startsWith("ban_sub_") -> {
                if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY &&
                                session.context == FSMContext.PROFILE_VIEW
                ) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val success =
                                when (data) {
                                    "ban_add_5h" -> AdminService.addBanHours(targetUserId, 5)
                                    "ban_add_1d" -> AdminService.addBanDays(targetUserId, 1)
                                    "ban_add_10h" -> AdminService.addBanHours(targetUserId, 10)
                                    "ban_add_10d" -> AdminService.addBanDays(targetUserId, 10)
                                    "ban_sub_5h" -> AdminService.addBanHours(targetUserId, -5)
                                    "ban_sub_1d" -> AdminService.addBanDays(targetUserId, -1)
                                    "ban_sub_10h" -> AdminService.addBanHours(targetUserId, -10)
                                    "ban_sub_10d" -> AdminService.addBanDays(targetUserId, -10)
                                    else -> false
                                }

                        if (success) {
                            val targetUser = users.profile(targetUserId)
                            if (targetUser != null) {
                                session.action = PendingAction.NONE
                                RouterAdminUsers.showUserProfileView(
                                        bot,
                                        chat,
                                        uid,
                                        targetUser,
                                        users
                                )
                                bot.answerCallbackQuery(callback.id, "✅ Время бана обновлено")
                            } else {
                                bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                            }
                        } else {
                            bot.answerCallbackQuery(callback.id, "❌ Ошибка при обновлении времени")
                        }
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Ошибка: пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный контекст")
                }
            }
            data == "ban_add_cancel" -> {
                if (session.action == PendingAction.ADMIN_EDIT_BAN_EXPIRY &&
                                session.context == FSMContext.PROFILE_VIEW
                ) {
                    val targetUserId = session.data["admin_profile_view_user_id"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            session.action = PendingAction.NONE
                            RouterAdminUsers.showUserProfileView(bot, chat, uid, targetUser, users)
                            bot.answerCallbackQuery(callback.id)
                        } else {
                            bot.answerCallbackQuery(callback.id, "❌ Пользователь не найден")
                        }
                    } else {
                        bot.answerCallbackQuery(callback.id, "❌ Ошибка: пользователь не найден")
                    }
                } else {
                    bot.answerCallbackQuery(callback.id, "❌ Неверный контекст")
                }
            }
            else -> {
                bot.answerCallbackQuery(callback.id, "❌ Неизвестная команда")
            }
        }
    }
}
