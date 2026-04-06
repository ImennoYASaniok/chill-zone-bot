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
                append("👤 Профиль\n")
                append("Username: ")
                if (profile.hideUsername || profile.username.isBlank()) {
                    append("скрыт\n")
                } else {
                    append("@").append(profile.username).append("\n")
                }
                append("Имя: ").append(profile.displayName.ifBlank { "не указано" }).append("\n")
                append("Рейтинг аккаунта: ").append(profile.rating).append("\n")
                append("Био: ").append(profile.bio.ifBlank { "не заполнено" }).append("\n")
                append("Профиль: ").append(if (profile.hidden) "скрыт" else "видим").append("\n\n")
                append("Мемов в базе: ").append(memeCount).append("\n")
                append("Предсказаний: ").append(predCount).append("\n")
                append("Тестов: ").append(testCount).append("\n")
                append("Событий создано: ").append(eventCount).append("\n")
                append("Игровой рейтинг: ").append(gameStats.rating).append(" (побед ").append(gameStats.wins).append(", streak ").append(gameStats.streak).append(")")
            },
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