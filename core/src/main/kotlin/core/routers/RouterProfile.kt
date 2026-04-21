package core.routers

import data.*
import data.models.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import core.keyboards.KeyboardFactory
import core.keyboards.KeyboardProfile
import core.SessionStore
import core.Session
import core.PendingAction
import data.AdminService
import core.routers.routerAdmin.RouterAdmin

object RouterProfile {
    fun showProfile(bot: Bot, chat: ChatId, uid: Long, users: UserRepository) {
        println("DEBUG: Вызов showProfile для пользователя $uid")
        val profile = users.profile(uid)
        println("DEBUG: Получен профиль в showProfile: displayName='${profile?.displayName}'")
        
        if (profile == null) {
            println("ERROR: Профиль пользователя $uid не найден")
            return
        }
        
        // Проверяем админские права
        val isAdmin = AdminService.isAdmin(uid)
        println("DEBUG: Проверка админских прав для пользователя $uid: $isAdmin")
        println("DEBUG: AdminService.adminIds: ${AdminService.adminIds}")
        
        val statusText = if (profile.isBanned) {
            "🚫 Забанен\n📅 Дата бана: ${profile.bannedAt ?: "неизвестно"}\n📝 Причина: ${profile.reason ?: "не указана"}"
        } else {
            "✅ Активен"
        }

        val messageText = buildString {
            append("👤 <b>Профиль пользователя</b>\n\n")

            val nameValue = profile.displayName
                .trim()
                .removePrefix("@")
                .ifBlank { "не указано" }
                .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
            append("🏷️ Имя: $nameValue\n")

            if (profile.hideUsername || profile.username.isBlank()) {
                append("👤 Username: <code>скрыт</code>\n")
            } else {
                append("👤 Username: <code>@${profile.username}</code>\n")
            }

            append("⭐ Рейтинг: ${profile.rating}\n")
            append("👁️ Профиль: ${if (profile.hidden) "скрыт" else "видимый"}\n\n")
            
            append("📝 <b>Био:</b>\n")
            append("${profile.bio.ifBlank { "не указано" }}\n\n")
            
            append("<b>Статус:</b>\n")
            append("$statusText\n")
        }

        // Если есть аватарка, отправляем фото с подписью
        profile.avatarFileId?.let { fileId ->
            // Проверяем, является ли это локальным файлом (для мок пользователей)
            if (fileId.contains(":\\") || fileId.contains("/") || java.io.File(fileId).exists()) {
                try {
                    val photo = java.io.File(fileId)
                    if (photo.exists()) {
                        bot.sendPhoto(
                            chatId = chat,
                            photo = photo,
                            caption = messageText,
                            parseMode = ParseMode.HTML,
                            replyMarkup = KeyboardFactory.profileMenu(profile)
                        )
                    } else {
                        // Файл не существует, отправляем только текст
                        bot.sendMessage(
                            chat,
                            messageText,
                            parseMode = ParseMode.HTML,
                            replyMarkup = KeyboardFactory.profileMenu(profile)
                        )
                    }
                } catch (e: Exception) {
                    // Ошибка при чтении файла, отправляем только текст
                    println("❌ Ошибка при отправке локального файла аватарки: ${e.message}")
                    bot.sendMessage(
                        chat,
                        messageText,
                        parseMode = ParseMode.HTML,
                        replyMarkup = KeyboardFactory.profileMenu(profile)
                    )
                }
            } else {
                // Это file_id из Telegram
                bot.sendPhoto(
                    chatId = chat,
                    photo = fileId,
                    caption = messageText,
                    parseMode = ParseMode.HTML,
                    replyMarkup = KeyboardFactory.profileMenu(profile)
                )
            }
        } ?: run {
            // Если нет аватарки, отправляем просто сообщение
            bot.sendMessage(
                chat,
                messageText,
                parseMode = ParseMode.HTML,
                replyMarkup = KeyboardFactory.profileMenu(profile)
            )
        }
    }

    private fun showEditProfileForm(bot: Bot, chat: ChatId, uid: Long, users: UserRepository) {
        val profile = users.profile(uid)
        if (profile == null) {
            bot.sendMessage(chat, "❌ Профиль пользователя не найден")
            return
        }

        val message = buildString {
            append("✏️ <b>Редактирование профиля</b>\n\n")
            append("🏷️ <i>ИМЯ:</i> <b>${profile.displayName.removePrefix("@").ifBlank { "не указано" }.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }}</b>\n")
            append("🔸 <i>Username:</i> <code>${if (profile.username.isBlank()) "не указан" else "@${profile.username}"}</code>\n")
            append("📝 <i>Био:</i> <em>${profile.bio.ifBlank { "не указано" }}</em>\n\n")
            append("👁️ <i>Профиль:</i> ${if (profile.hidden) "скрыт" else "видим"}\n")
            append("👤 <i>Username:</i> ${if (profile.hideUsername) "скрыт" else "показывается"}\n")
        }

        bot.sendMessage(
            chat,
            message,
            parseMode = ParseMode.HTML,
            replyMarkup = KeyboardProfile.editProfileMenu(profile)
        )
    }

    fun showAdminEditProfileForm(bot: Bot, chat: ChatId, adminUid: Long, targetUser: UserProfile) {
        val session = SessionStore.get(adminUid)
        session.action = PendingAction.ADMIN_EDIT_PROFILE
        session.data["admin_edit_target_user"] = targetUser.userId.toString()

        val usernameDisplay = if (targetUser.hideUsername) "скрыт" else "@${targetUser.username}"

        val nameValue = targetUser.displayName
            .trim()
            .removePrefix("@")
            .ifBlank { "не указано" }
            .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }

        val message = """✏️ <b>Редактирование профиля</b>

🏷️ ИМЯ: $nameValue
🆔 ID: ${targetUser.userId}
👤 Username: $usernameDisplay

Выберите, что хотите изменить:"""

        val actionRows = mutableListOf<List<com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton>>()

        actionRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "📝 Имя",
                callbackData = "admin_edit_name_${targetUser.userId}"
            ),
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "👤 Username",
                callbackData = "admin_edit_username_${targetUser.userId}"
            )
        ))

        actionRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "📄 Био",
                callbackData = "admin_edit_bio_${targetUser.userId}"
            ),
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "👁️ Скрыть профиль",
                callbackData = "admin_toggle_hidden_${targetUser.userId}"
            )
        ))

        actionRows.add(listOf(
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "🎬 Медиа",
                callbackData = "admin_toggle_media_${targetUser.userId}"
            ),
            com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData(
                text = "👁️ Username",
                callbackData = "admin_toggle_username_${targetUser.userId}"
            )
        ))

        val inlineKeyboard = com.github.kotlintelegrambot.entities.InlineKeyboardMarkup.create(actionRows)
        bot.sendMessage(chat, message, parseMode = ParseMode.HTML, replyMarkup = inlineKeyboard)
    }

    fun showAccountStats(bot: Bot, chat: ChatId, targetUser: UserProfile) {
        val message = """📊 <b>Дополнительная информация</b>

😂 <b>Статистика мемов:</b>
• Загружено мемов: 0

🔮 <b>Статистика предсказаний:</b>
• Всего выбитых предсказаний: 0
  • Редкие: 0
  • Эпические: 0
  • Легендарные: 0
• Добавлено предсказаний: 0

📝 <b>Статистика тестов:</b>
• Создано тестов: 0
• Пройдено тестов: 0
• Средний процент прохождения: 0%

🎮 <b>Статистика мини-игр:</b>
• Общий рейтинг: ${targetUser.rating}
• Общее количество побед: 0
• Общее количество поражений: 0

⚽ <b>Футбол:</b>
  • Рейтинг: 0
  • Побед: 0
  • Поражений: 0
  • Текущая серия: 0
  • Лучшая серия: 0

🏀 <b>Баскетбол:</b>
  • Рейтинг: 0
  • Побед: 0
  • Поражений: 0
  • Текущая серия: 0
  • Лучшая серия: 0

🎡 <b>Колесо фортуны:</b>
  • Рейтинг: 0
  • Побед: 0
  • Поражений: 0
  • Текущая серия: 0
  • Лучшая серия: 0

✂️ <b>Камень-ножницы-бумага:</b>
  • Рейтинг: 0
  • Побед: 0
  • Поражений: 0
  • Ничьей: 0
  • Текущая серия: 0
  • Лучшая серия: 0"""

        bot.sendMessage(chat, message, parseMode = ParseMode.HTML, replyMarkup = KeyboardProfile.statsMenu())
    }

    fun handleProfileAction(bot: Bot, chat: ChatId, uid: Long, text: String, users: UserRepository, session: Session): Boolean {
        println("DEBUG: handleProfileAction вызван с text: '$text' для пользователя $uid")

        when (text) {
            "✏️ Изменить профиль" -> {
                session.action = PendingAction.EDIT_PROFILE
                showEditProfileForm(bot, chat, uid, users)
                return true
            }
            "📊 Статистика аккаунта" -> {
                val profile = users.profile(uid)
                if (profile != null) {
                    showAccountStats(bot, chat, profile)
                } else {
                    bot.sendMessage(chat, "❌ Профиль пользователя не найден")
                }
                return true
            }
            "🛡️ Админ панель" -> {
                println("DEBUG: Нажата кнопка админской панели пользователем $uid")
                RouterAdmin.handleAdminAction(bot, chat, uid, text, users, session)
                return true
            }
            "Изменить имя" -> {
                session.action = PendingAction.EDIT_NAME
                bot.sendMessage(chat, "Напиши новое имя.", replyMarkup = KeyboardProfile.editProfileMenu(users.profile(uid)))
                return true
            }
            "Показать username [👁️]", "Скрыть username [🙈]" -> {
                users.toggleHideUsername(uid)
                showEditProfileForm(bot, chat, uid, users)
                return true
            }
            "Изменить био" -> {
                session.action = PendingAction.EDIT_BIO
                bot.sendMessage(chat, "Напиши новое описание профиля.", replyMarkup = KeyboardProfile.editProfileMenu(users.profile(uid)))
                return true
            }
            "Показать профиль [👁️]", "Скрыть профиль [🙈]" -> {
                users.toggleHidden(uid)
                showEditProfileForm(bot, chat, uid, users)
                return true
            }
            "🖼️ Добавить аватарку", "🖼️ Изменить аватарку" -> {
                session.action = PendingAction.EDIT_AVATAR
                bot.sendMessage(chat, "Отправь фотографию для аватарки профиля.", replyMarkup = KeyboardProfile.editProfileMenu(users.profile(uid)))
                return true
            }
            "⬅️ Назад", "⬅️ Обратно" -> {
                session.action = PendingAction.NONE
                showProfile(bot, chat, uid, users)
                return true
            }
            else -> {
                if (session.action == PendingAction.EDIT_NAME) {
                    users.updateName(uid, text)
                    session.action = PendingAction.NONE
                    showEditProfileForm(bot, chat, uid, users)
                    return true
                }

                if (session.action == PendingAction.EDIT_BIO) {
                    users.updateBio(uid, text)
                    session.action = PendingAction.NONE
                    showEditProfileForm(bot, chat, uid, users)
                    return true
                }

                // Админское редактирование профиля
                if (session.action == PendingAction.ADMIN_EDIT_NAME) {
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            users.updateName(targetUserId, text)
                            session.action = PendingAction.NONE
                            bot.sendMessage(chat, "✅ Имя пользователя изменено на: \"$text\"")
                            showAdminEditProfileForm(bot, chat, uid, targetUser.copy(displayName = text))
                        } else {
                            bot.sendMessage(chat, "❌ Пользователь не найден")
                        }
                    }
                    return true
                }

                if (session.action == PendingAction.ADMIN_EDIT_USERNAME) {
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            val newUsername = text.trim().removePrefix("@")
                            users.updateUsername(targetUserId, newUsername)
                            session.action = PendingAction.NONE
                            bot.sendMessage(chat, "✅ Username изменен на: \"@$newUsername\"")
                            showAdminEditProfileForm(bot, chat, uid, targetUser.copy(username = newUsername))
                        } else {
                            bot.sendMessage(chat, "❌ Пользователь не найден")
                        }
                    }
                    return true
                }

                if (session.action == PendingAction.ADMIN_EDIT_BIO) {
                    val targetUserId = session.data["admin_edit_target_user"]?.toLongOrNull()
                    if (targetUserId != null) {
                        val targetUser = users.profile(targetUserId)
                        if (targetUser != null) {
                            users.updateBio(targetUserId, text)
                            session.action = PendingAction.NONE
                            bot.sendMessage(chat, "✅ Био изменено")
                            showAdminEditProfileForm(bot, chat, uid, targetUser.copy(bio = text))
                        } else {
                            bot.sendMessage(chat, "❌ Пользователь не найден")
                        }
                    }
                    return true
                }

                return false
            }
        }
    }
}