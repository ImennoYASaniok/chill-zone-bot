package core.routers.routerAdmin

import data.AdminService
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode

object RouterAdminStatistics {
    fun showAdminStats(bot: Bot, chat: ChatId) {
        val totalUsers = AdminService.getUsersCount()
        val bannedUsers = AdminService.getBannedUsersCount()
        val activeUsers = totalUsers - bannedUsers

        val message = "📊 <b>Админская статистика</b>\n\n👥 <b>Пользователи:</b>\n• Всего пользователей: $totalUsers\n• Забаненных пользователей: $bannedUsers\n• Активных пользователей: $activeUsers\n\n📈 <b>Активность:</b>\n• Процент забаненных: ${if (totalUsers > 0) String.format("%.1f", (bannedUsers.toDouble() / totalUsers * 100)) else "0"}%\n• Процент активных: ${if (totalUsers > 0) String.format("%.1f", (activeUsers.toDouble() / totalUsers * 100)) else "0"}%"

        bot.sendMessage(chat, message, parseMode = ParseMode.HTML)
    }
}
