package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.entities.ChatId
import io.github.cdimascio.dotenv.dotenv
import com.github.kotlintelegrambot.dispatcher.message
import core.handlers.miniGames.gameMainKeyboard
import core.handlers.miniGames.GameStorage
import core.handlers.miniGames.stickersKeyboard
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import java.awt.Button
import com.github.kotlintelegrambot.entities.dice.DiceEmoji

fun awtKeyboardToReplyMarkup(
    keyboard: MutableList<MutableList<Button>>
): KeyboardReplyMarkup {
    return KeyboardReplyMarkup(
        keyboard = keyboard.map { row ->
            row.map { btn ->
                KeyboardButton(btn.label)
            }
        },
        resizeKeyboard = true
    )
}

fun main() {

    val env = dotenv()

    val bot = bot {
        token = env["BOT_TOKEN"]

        dispatch {

            message {

                val chatIdLong = message.chat.id
                val chatId = ChatId.fromId(chatIdLong)
                val text = message.text ?: return@message

                // ===== /start =====
                if (text == "/start") {
                    bot.sendMessage(
                        chatId = chatId,
                        text = """
🎡 Добро пожаловать в мини-игру!

• Победа даёт рейтинг
• Проигрыш — без штрафов
• Есть серии побед
                        """.trimIndent(),
                        replyMarkup = awtKeyboardToReplyMarkup(gameMainKeyboard())
                    )
                    return@message
                }

                // ===== Главное меню =====
                if (text == "⬅ Назад") {
                    bot.sendMessage(
                        chatId = chatId,
                        text = "Главное меню",
                        replyMarkup = awtKeyboardToReplyMarkup(gameMainKeyboard())
                    )
                    return@message
                }

                // ===== Переход к стикерам =====
                if (text == "🎡 Прокрутить колесо") {
                    bot.sendMessage(
                        chatId = chatId,
                        text = "Выберите игру:",
                        replyMarkup = awtKeyboardToReplyMarkup(stickersKeyboard())
                    )
                    return@message
                }

                // ===== ⚽ ГОЛ =====
                if (text == "⚽ Гол") {
                    val result = bot.sendDice(chatId, DiceEmoji.Football)
                    val win = result.get().dice?.value == 3
                    Thread.sleep(3500)
                    if (win) {
                        val points = GameStorage.win(chatIdLong)
                        bot.sendMessage(chatId, "⚽ ГОООЛ!\n🎉 Победа!\n+$points очков рейтинга")
                    } else {
                        GameStorage.lose(chatIdLong)
                        bot.sendMessage(chatId, "⚽ Мимо…\n😢 Проигрыш")
                    }
                    return@message
                }



                // ===== 🏀 В КОЛЬЦО =====
                if (text == "🏀 В кольцо") {
                    val result = bot.sendDice(chatId, DiceEmoji.Basketball)
                    val value = result.get().dice?.value
                    val win = value == 4 || value == 5
                    Thread.sleep(4500)
                    if (win) {
                        val points = GameStorage.win(chatIdLong)
                        bot.sendMessage(chatId, "🏀 Попадание!\n🎉 Победа!\n+$points очков рейтинга")
                    } else {
                        GameStorage.lose(chatIdLong)
                        bot.sendMessage(chatId, "🏀 Промах…\n😢 Проигрыш")
                    }
                    return@message
                }



                // ===== 🎰 КОЛЕСО ФОРТУНЫ =====
                if (text == "🎰 Колесо фортуны") {
                    val result = bot.sendDice(chatId, DiceEmoji.SlotMachine)
                    val win = result.get().dice?.value == 64
                    val win1 = result.get().dice?.value == 1
                    val win2 = result.get().dice?.value == 43
                    val win3 = result.get().dice?.value == 22
                    Thread.sleep(2000)
                    if (win) {
                        val points = GameStorage.win(chatIdLong)
                        bot.sendMessage(chatId, "🎰 ДЖЕКПОТ!\n🎉 Победа!\n+$points очков рейтинга")
                    } else if(win1) {
                        val points = GameStorage.win(chatIdLong)
                        bot.sendMessage(chatId, "🎰 ДЖЕКПОТ!\n🎉 Победа!\n+$points очков рейтинга")
                    } else if(win2) {
                        val points = GameStorage.win(chatIdLong)
                        bot.sendMessage(chatId, "🎰 ДЖЕКПОТ!\n🎉 Победа!\n+$points очков рейтинга")
                    } else if(win3){
                        val points = GameStorage.win(chatIdLong)
                        bot.sendMessage(chatId, "🎰 ДЖЕКПОТ!\n🎉 Победа!\n+$points очков рейтинга")
                    } else {
                        GameStorage.lose(chatIdLong)
                        bot.sendMessage(chatId, "🎰 Не повезло…\n😢 Проигрыш")
                    }
                    return@message
                }



                // ===== Статистика =====
                if (text == "📊 Моя статистика") {
                    val u = GameStorage.getUser(chatIdLong)
                    bot.sendMessage(
                        chatId,
                        """
📊 Ваша статистика

Рейтинг: ${u.rating}
Текущая серия: ${u.currentStreak}
Максимальная серия: ${u.maxStreak}
                        """.trimIndent()
                    )
                    return@message
                }

                // ===== Топ 10 =====
                if (text == "🏆 Топ 10 игроков") {
                    val top = GameStorage.getTop10()
                    val result = top.mapIndexed { i, u ->
                        "${i + 1}. ${u.rating} очков"
                    }.joinToString("\n")

                    bot.sendMessage(chatId, "🏆 Топ 10:\n$result")
                    return@message
                }
            }
        }
    }

    bot.startPolling()
}


