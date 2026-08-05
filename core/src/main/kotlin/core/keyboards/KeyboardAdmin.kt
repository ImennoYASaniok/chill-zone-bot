package core.keyboards

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import data.models.BannedUser
import data.models.UserProfile

// Вспомогательная функция для создания клавиатур
private fun kb(rows: List<List<String>>): KeyboardReplyMarkup {
    return KeyboardReplyMarkup(
            keyboard = rows.map { row -> row.map { KeyboardButton(text = it) } },
            resizeKeyboard = true,
            oneTimeKeyboard = false
    )
}

object KeyboardAdmin {
    fun adminMenu(): KeyboardReplyMarkup =
            kb(
                    listOf(
                            listOf("👥 Список пользователей", "🔍 Поиск пользователей"),
                            listOf("📊 Статистика"),
                            listOf("⬅️ Обратно")
                    )
            )

    fun moderatorMenu(): KeyboardReplyMarkup =
            kb(
                    listOf(
                            listOf("👥 Список пользователей", "🔍 Поиск пользователей"),
                            listOf("🚫 Забаненные пользователи"),
                            listOf("⬅️ Обратно")
                    )
            )

    fun adminUserListMenu(filter: String = "Все"): KeyboardReplyMarkup =
            kb(listOf(listOf("🔄 Фильтр: $filter"), listOf("⬅️ Обратно")))

    fun adminSearchMenu(filter: String = "Все"): KeyboardReplyMarkup =
            kb(listOf(listOf("🔄 Фильтр: $filter"), listOf("🚪 Выйти из поиска")))

    fun adminSearchInputMenu(): KeyboardReplyMarkup =
            kb(listOf(listOf("🚪 Выйти из поиска")))

    fun moderatorUserListMenu(filter: String = "Все"): KeyboardReplyMarkup =
            kb(listOf(listOf("🔄 Фильтр: $filter"), listOf("⬅️ Обратно")))

    fun moderatorSearchInputMenu(): KeyboardReplyMarkup =
            kb(listOf(listOf("🚪 Выйти из поиска")))

    fun adminBackOnlyMenu(): KeyboardReplyMarkup = kb(listOf(listOf("⬅️ Обратно")))

    fun banReasonInputMenu(): KeyboardReplyMarkup = kb(listOf(listOf("⬅️ Отмена")))

    fun profileViewBackMenu(): KeyboardReplyMarkup = kb(listOf(listOf("⬅️ Обратно")))

    fun profileViewModeratorMenu(isBanned: Boolean): KeyboardReplyMarkup =
            kb(
                    listOf(
                            listOf(if (isBanned) "✅ Разбанить" else "🚫 Забанить"),
                            listOf("⬅️ Обратно")
                    )
            )

    fun profileViewAdminMenu(isBanned: Boolean): KeyboardReplyMarkup =
            kb(
                    listOf(
                                    listOf(if (isBanned) "✅ Разбанить" else "🚫 Забанить"),
                                    listOf(if (isBanned) "📝 Написать причину бана" else ""),
                                    listOf("⬅️ Обратно")
                            )
                            .filter { row -> row.isNotEmpty() }
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
        val unbanButton =
                InlineKeyboardButton.CallbackData(
                        text = "✅ Разбанить",
                        callbackData = "admin_unban_${bannedUser.userId}"
                )
        rows.add(listOf(unbanButton))

        // Кнопка профиля
        val profileButton =
                InlineKeyboardButton.CallbackData(
                        text = "👤 Профиль",
                        callbackData = "admin_profile_${bannedUser.userId}"
                )
        rows.add(listOf(profileButton))

        return InlineKeyboardMarkup.create(rows)
    }

    fun adminBannedInline(userProfile: UserProfile): InlineKeyboardMarkup {
        val rows = mutableListOf<List<InlineKeyboardButton>>()

        // Кнопка разбана
        val unbanButton =
                InlineKeyboardButton.CallbackData(
                        text = "✅ Разбанить",
                        callbackData = "admin_unban_${userProfile.userId}"
                )
        rows.add(listOf(unbanButton))

        // Кнопка профиля
        val profileButton =
                InlineKeyboardButton.CallbackData(
                        text = "👤 Профиль",
                        callbackData = "admin_profile_${userProfile.userId}"
                )
        rows.add(listOf(profileButton))

        return InlineKeyboardMarkup.create(rows)
    }

    fun profileViewAdminBanMenu(isBanned: Boolean, isTemporaryBan: Boolean): KeyboardReplyMarkup =
            kb(
                    listOf(
                                    listOf(if (isBanned) "✅ Разбанить" else "🚫 Забанить"),
                                    listOf(if (isBanned) "📝 Написать причину бана" else ""),
                                    listOf(
                                            if (isBanned)
                                                    "Тип бана: ${if (isTemporaryBan) "временно" else "навсегда"}"
                                            else ""
                                    ),
                                    listOf(
                                            if (isBanned && isTemporaryBan) "Изменить время бана"
                                            else ""
                                    ),
                                    listOf(
                                            if (isBanned && isTemporaryBan)
                                                    "🕒 Получить точное время бана"
                                            else ""
                                    ),
                                    listOf("⬅️ Обратно")
                            )
                            .filter { row -> row.isNotEmpty() }
            )

    fun banAddTimeMenu(): KeyboardReplyMarkup =
            kb(
                    listOf(
                            listOf("➕ 5 часов", "➕ 1 дней"),
                            listOf("➕ 10 часов", "➕ 10 дней"),
                            listOf("➖ 5 часов", "➖ 1 дней"),
                            listOf("➖ 10 часов", "➖ 10 дней"),
                            listOf("⬅️ К профилю")
                    )
            )
}
