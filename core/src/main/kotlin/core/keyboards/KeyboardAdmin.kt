package core.keyboards

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import data.models.UserProfile
import data.models.BannedUser

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
            listOf("👥 Список пользователей", "🔍 Поиск пользователей"),
            listOf("📊 Статистика"),
            listOf("⬅️ Обратно")
        )
    )
    
    fun adminUserListMenu(filter: String = "Все"): KeyboardReplyMarkup = kb(
        listOf(
            listOf("🔄 Фильтр: $filter"),
            listOf("⬅️ Обратно")
        )
    )

    fun adminSearchMenu(filter: String = "Все"): KeyboardReplyMarkup = kb(
        listOf(
            listOf("🔄 Фильтр: $filter"),
            listOf("⬅️ Обратно")
        )
    )

    fun adminBackOnlyMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("⬅️ Обратно")
        )
    )
    
    fun banReasonInputMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("⬅️ Отмена")
        )
    )

    fun profileViewBackMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("⬅️ Обратно")
        )
    )

    fun profileViewAdminMenu(isBanned: Boolean): KeyboardReplyMarkup = kb(
        listOf(
            listOf(if (isBanned) "✅ Разбанить" else "🚫 Забанить"),
            listOf(if (isBanned) "📝 Написать причину бана" else ""),
            listOf("⬅️ Обратно")
        ).filter { row -> row.isNotEmpty() }
    )
    
    fun adminBannedNav(totalCount: Int): KeyboardReplyMarkup {
        val rows = mutableListOf<List<String>>()
        
        if (totalCount > 1) {
            rows.add(listOf("⬅️ Предыдущий", "➡️ Следующий"))
        }
        
        rows.add(listOf("⬅️ Обратно"))
        
        return kb(rows)
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
    
    fun adminBannedInline(userProfile: UserProfile): InlineKeyboardMarkup {
        val rows = mutableListOf<List<InlineKeyboardButton>>()
        
        // Кнопка разбана
        val unbanButton = InlineKeyboardButton.CallbackData(
            text = "✅ Разбанить",
            callbackData = "admin_unban_${userProfile.userId}"
        )
        rows.add(listOf(unbanButton))
        
        // Кнопка профиля
        val profileButton = InlineKeyboardButton.CallbackData(
            text = "👤 Профиль",
            callbackData = "admin_profile_${userProfile.userId}"
        )
        rows.add(listOf(profileButton))
        
        return InlineKeyboardMarkup.create(rows)
    }
    
    fun profileViewAdminBanMenu(isBanned: Boolean, hasBanExpiry: Boolean): KeyboardReplyMarkup = kb(
        listOf(
            listOf(if (isBanned) "✅ Разбанить" else "🚫 Забанить"),
            listOf(if (isBanned) "📝 Написать причину бана" else ""),
            listOf(if (isBanned) "⏰ Окончание бана: ${if (hasBanExpiry) "есть" else "нет"}" else ""),
            listOf(if (isBanned && hasBanExpiry) "🔧 Изменить окончание бана" else ""),
            listOf("⬅️ Обратно")
        ).filter { row -> row.isNotEmpty() }
    )
    
    fun banExpiryDaysMenu(): KeyboardReplyMarkup = kb(
        listOf(
            listOf("➕ +1 день", "➕ +3 дня", "➕ +5 дней"),
            listOf("➕ +10 дней"),
            listOf("⬅️ Отмена")
        )
    )
}

