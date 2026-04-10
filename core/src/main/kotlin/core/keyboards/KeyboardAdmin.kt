package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import data.UserProfile
import data.BannedUser

// Вспомогательная функция для создания клавиатур
private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
    return KeyboardReplyMarkup(
        keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )
}

object KeyboardAdmin {
    fun adminMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("👥 Управление пользователями", "📊 Статистика"),
            listOf("⬅️ Обратно")
        )
    )
    
    fun adminUserManagementMenu(filter: String = "Все"): KeyboardReplyMarkup = kb(
        listOf(
            listOf("👥 Список пользователей", "🔍 Поиск пользователей"),
            listOf("🔄 Фильтр: $filter", "⬅️ Обратно")
        )
    )
    
    fun adminFilterMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("🔄 Показать всех", "✅ Показать разбаненных"),
            listOf("🚫 Показать забаненных", "⬅️ Обратно")
        )
    )
    
    fun adminUserNav(currentIndex: Int, totalCount: Int): KeyboardReplyMarkup {
        val rows = mutableListOf<List<String>>()
        
        if (totalCount > 1) {
            rows.add(listOf("⬅️ Предыдущий", "➡️ Следующий"))
        }
        
        rows.add(listOf("⬅️ Обратно"))
        
        return kb(rows)
    }
    
    fun adminBannedNav(currentIndex: Int, totalCount: Int): KeyboardReplyMarkup {
        val rows = mutableListOf<List<String>>()
        
        if (totalCount > 1) {
            rows.add(listOf("⬅️ Предыдущий", "➡️ Следующий"))
        }
        
        rows.add(listOf("⬅️ Обратно"))
        
        return kb(rows)
    }
    
    fun adminUserInline(user: UserProfile, isBanned: Boolean): InlineKeyboardMarkup {
        val rows = mutableListOf<List<InlineKeyboardButton>>()
        
        // Кнопка действия (бан/разбан)
        val actionButton = if (isBanned) {
            InlineKeyboardButton.CallbackData(
                text = "✅ Разбанить",
                callbackData = "admin_unban_${user.userId}"
            )
        } else {
            InlineKeyboardButton.CallbackData(
                text = "🚫 Забанить",
                callbackData = "admin_ban_${user.userId}"
            )
        }
        rows.add(listOf(actionButton))
        
        // Кнопка профиля
        val profileButton = InlineKeyboardButton.CallbackData(
            text = "👤 Профиль",
            callbackData = "admin_profile_${user.userId}"
        )
        rows.add(listOf(profileButton))
        
        return InlineKeyboardMarkup.create(rows)
    }
    
    fun adminBannedInline(bannedUser: BannedUser): InlineKeyboardMarkup {
        val rows = mutableListOf<List<InlineKeyboardButton>>()
        
        // Кнопка разбана
        val unbanButton = InlineKeyboardButton.CallbackData(
            text = "✅ Разбанить",
            callbackData = "admin_unban_${bannedUser.userId}"
        )
        rows.add(listOf(unbanButton))
        
        // Кнопка профиля
        val profileButton = InlineKeyboardButton.CallbackData(
            text = "👤 Профиль",
            callbackData = "admin_profile_${bannedUser.userId}"
        )
        rows.add(listOf(profileButton))
        
        return InlineKeyboardMarkup.create(rows)
    }
    
    fun adminSearchResultsInline(users: List<UserProfile>, currentIndex: Int): InlineKeyboardMarkup {
        val rows = mutableListOf<List<InlineKeyboardButton>>()
        
        if (users.isNotEmpty()) {
            val currentUser = users[currentIndex]
            
            // Кнопки действий для текущего пользователя
            val actionButton = if (currentUser.isBanned) {
                InlineKeyboardButton.CallbackData(
                    text = "✅ Разбанить",
                    callbackData = "admin_unban_${currentUser.userId}"
                )
            } else {
                InlineKeyboardButton.CallbackData(
                    text = "🚫 Забанить",
                    callbackData = "admin_ban_${currentUser.userId}"
                )
            }
            rows.add(listOf(actionButton))
            
            // Кнопка профиля
            val profileButton = InlineKeyboardButton.CallbackData(
                text = "👤 Профиль",
                callbackData = "admin_profile_${currentUser.userId}"
            )
            rows.add(listOf(profileButton))
            
            // Навигация если больше 1 пользователя
            if (users.size > 1) {
                val navButtons = mutableListOf<InlineKeyboardButton>()
                
                // Левая стрелочка
                navButtons.add(
                    InlineKeyboardButton.CallbackData(
                        text = "⬅️",
                        callbackData = "admin_search_nav_${if (currentIndex == 0) users.size - 1 else currentIndex - 1}"
                    )
                )
                
                // Правая стрелочка
                navButtons.add(
                    InlineKeyboardButton.CallbackData(
                        text = "➡️",
                        callbackData = "admin_search_nav_${if (currentIndex == users.size - 1) 0 else currentIndex + 1}"
                    )
                )
                
                rows.add(navButtons)
            }
        }
        
        return InlineKeyboardMarkup.create(rows)
    }
    
    fun adminSearchNavKeyboard(): KeyboardReplyMarkup {
        return kb(
            listOf(
                listOf("⬅️ Обратно")
            )
        )
    }
}
