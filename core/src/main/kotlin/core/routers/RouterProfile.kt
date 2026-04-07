package core.routers

import data.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import core.keyboards.KeyboardFactory
import core.SessionStore
import core.Session
import core.PendingAction
import data.AdminService // исправленный импорт

object RouterProfile {
    fun showProfile(bot: Bot, chat: ChatId, uid: Long, users: UserRepository, memes: MemeRepository, predictions: PredictionRepository, tests: TestRepository, events: EventRepository, games: GameRepository) {
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
        
        val memeCount = memes.total()
        val predCount = predictions.total()
        val testCount = tests.total()
        val eventCount = events.mine(uid).size
        val gameStats = games.stats(uid)
        bot.sendMessage(
            chat,
            buildString {
                append("👤 <b>Профиль пользователя</b>\n\n")
                
                // Блок основной информации
                append("📋 <b>Основная информация</b>\n")
                append("🔸 <i>Имя:</i> <b>${profile.displayName.ifBlank { "не указано" }}</b>\n")
                
                if (profile.hideUsername || profile.username.isBlank()) {
                    append("🔸 <i>Username:</i> <code>скрыт</code>\n")
                } else {
                    append("🔸 <i>Username:</i> <code>@${profile.username}</code>\n")
                }
                
                append("🔸 <i>Рейтинг:</i> ⭐ <b>${profile.rating}</b>\n")
                append("🔸 <i>Статус:</i> ${if (profile.hidden) "🔒 <b>скрыт</b>" else "🌐 <b>видим</b>"}\n")
                
                if (profile.bio.isNotBlank()) {
                    append("🔸 <i>О себе:</i> <em>${profile.bio}</em>\n")
                }
                
                append("\n")
                
                // Блок статистики
                append("📊 <b>Статистика активности</b>\n")
                append("🎭 <i>Мемов в базе:</i> <b>$memeCount</b>\n")
                append("🔮 <i>Предсказаний:</i> <b>$predCount</b>\n")
                append("📝 <i>Тестов:</i> <b>$testCount</b>\n")
                append("📅 <i>Событий создано:</i> <b>$eventCount</b>\n")
                
                // Игровая статистика
                append("\n🎮 <b>Игровая статистика</b>\n")
                append("🏆 <i>Рейтинг:</i> <b>${gameStats.rating}</b>\n")
                append("🎯 <i>Побед:</i> <b>${gameStats.wins}</b>\n")
                append("🔥 <i>Серия побед:</i> <b>${gameStats.streak}</b>\n")
                
                // Админский статус
                if (AdminService.isAdmin(uid)) {
                    append("\n🛡️ <b>Администратор</b>")
                }
            },
            parseMode = ParseMode.HTML,
            replyMarkup = KeyboardFactory.profileMenu(profile)
        )
    }

    fun handleProfileAction(bot: Bot, chat: ChatId, uid: Long, text: String, users: UserRepository, memes: MemeRepository, predictions: PredictionRepository, tests: TestRepository, events: EventRepository, games: GameRepository, session: Session): Boolean {
        println("DEBUG: handleProfileAction вызван с text: '$text' для пользователя $uid")
        
        when (text) {
            "🛡️ Админ панель" -> {
                println("DEBUG: Нажата кнопка админской панели пользователем $uid")
                RouterAdmin.handleAdminAction(bot, chat, uid, text, users, memes, predictions, tests, events, games, session)
                return true
            }
            "Изменить имя" -> {
                session.action = PendingAction.EDIT_NAME
                bot.sendMessage(chat, "Напиши новое имя.", replyMarkup = KeyboardFactory.profileMenu(users.profile(uid)))
                return true
            }
            "Показать username [👁️]", "Скрыть username [🙈]" -> {
                users.toggleHideUsername(uid)
                // Показываем профиль заново с обновленной кнопкой
                showProfile(bot, chat, uid, users, memes, predictions, tests, events, games)
                return true
            }
            "Изменить био" -> {
                session.action = PendingAction.EDIT_BIO
                bot.sendMessage(chat, "Напиши новое описание профиля.", replyMarkup = KeyboardFactory.profileMenu(users.profile(uid)))
                return true
            }
            "Показать профиль [👁️]", "Скрыть профиль [🙈]" -> {
                users.toggleHidden(uid)
                // Показываем профиль заново с обновленной кнопкой
                showProfile(bot, chat, uid, users, memes, predictions, tests, events, games)
                return true
            }
            "Включить картинки [✅]", "Выключить картинки [❌]" -> {
                val enabled = users.toggleMedia(uid)
                val profile = users.profile(uid)
                val resultMessage = if (enabled) "Картинки включены." else "Картинки выключены."
                bot.sendMessage(chat, resultMessage)
                val settingsMessage = "Настройки."
                bot.sendMessage(chat, settingsMessage, replyMarkup = KeyboardFactory.settingsMenu(profile))
                return true
            }
            else -> {
                // Обработка текстового ввода для состояний EDIT_NAME и EDIT_BIO
                when (session.action) {
                    PendingAction.EDIT_NAME -> {
                        val newName = text.trim()
                        if (newName.isNotEmpty()) {
                            users.updateName(uid, newName)
                            session.action = PendingAction.NONE
                            // Показываем профиль заново с обновленным именем
                            showProfile(bot, chat, uid, users, memes, predictions, tests, events, games)
                            return true
                        }
                    }
                    PendingAction.EDIT_BIO -> {
                        val newBio = text.trim()
                        if (newBio.isNotEmpty()) {
                            users.updateBio(uid, newBio)
                            session.action = PendingAction.NONE
                            // Показываем профиль заново с обновленным био
                            showProfile(bot, chat, uid, users, memes, predictions, tests, events, games)
                            return true
                        }
                    }
                    else -> return false
                }
                return false
            }
        }
    }
}