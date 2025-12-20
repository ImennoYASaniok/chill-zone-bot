package app

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.entities.dice.DiceEmoji
import io.github.cdimascio.dotenv.dotenv

import core.States
import core.currState
import core.messageClasses.ReplyList
import core.utils.Logger

import core.handlers.FeedbackHandlers

import core.keyboards.getKeyboardMemes
import core.keyboards.getKeyboardMiniGamesMain
import core.keyboards.getKeyboardMiniGamesChoose
import core.keyboards.getKeyboardPredictionsMain
import core.keyboards.getKeyboardPredictionsRarity
import core.keyboards.getKeyboardTestsMain
import core.keyboards.getKeyboardTestsOptions

import core.memes.FavoritesStorage
import core.memes.MemeAddState
import core.memes.MemeStorage
import core.memes.UserMemeHistory
import core.memes.UserMemeSession
import core.memes.VotesStorage

import core.minigames.dice.GameStorage

import core.predictions.PredictionStorage
import core.predictions.UserPredictionHistory
import core.predictions.PredictionsFlow

import core.tests.TestsFlow

fun getUsername(user: User?): String = user?.username ?: "Не указан"
fun getUserId(user: User?): Long = user?.id!!

private fun openGeneralMenu(text: String, bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, username: String, userId: Long) {
    val argsGeneralMenu = ReplyList.replyGeneralMenu.processing(text)
    val argsSettings = ReplyList.replySettings.processing(text)
    val argsAccount = ReplyList.replyAccount.processing(text, mapOf("username" to username, "userId" to userId))
    val argsFeedback = ReplyList.replyFeedback.processing(text)

    ReplyList.replyGeneralMenu.callMain(text, bot = bot, chatId = chatId, pair = argsGeneralMenu)
    ReplyList.replySettings.callMain(text, bot = bot, chatId = chatId, pair = argsSettings)
    ReplyList.replyAccount.callMain(text, bot = bot, chatId = chatId, pair = argsAccount)
    ReplyList.replyFeedback.callMain(text, bot = bot, chatId = chatId, pair = argsFeedback)
}

// ===================== MEMES =====================

private fun sendMemeWithCounts(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, memeId: Int, fileId: String?) {
    if (fileId != null) bot.sendPhoto(chatId, fileId)
    val counts = VotesStorage.getCounts(memeId)
    bot.sendMessage(chatId, "👍 ${counts.likes} | 👎 ${counts.dislikes}")
}

private fun showMemeMenu(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, chatIdLong: Long) {
    val meme = UserMemeHistory.getNextRandomUnseen(chatIdLong)

    if (meme == null) {
        UserMemeSession.setLastShown(chatIdLong, null)
        if (MemeStorage.size() == 0) {
            bot.sendMessage(
                chatId,
                "Здесь пока нет мемов. Нажми «Добавить мем», чтобы загрузить первый.",
                replyMarkup = getKeyboardMemes()
            )
        } else {
            bot.sendMessage(
                chatId,
                "Ты уже посмотрел все мемы. Нажми «Следующий мем» после того, как добавят новые, или сбрось просмотр командой /memes_reset.",
                replyMarkup = getKeyboardMemes()
            )
        }
        return
    }

    UserMemeSession.setLastShown(chatIdLong, meme.id)
    sendMemeWithCounts(bot, chatId, meme.id, meme.fileId)
    bot.sendMessage(chatId, "Меню мемов", replyMarkup = getKeyboardMemes())
}

private fun handleMemes(text: String, bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, chatIdLong: Long): Boolean {
    when (text) {
        "/memes_reset" -> {
            UserMemeHistory.reset(chatIdLong)
            bot.sendMessage(chatId, "Ок, сбросил историю просмотренных мемов.", replyMarkup = getKeyboardMemes())
            return true
        }
        "Следующий мем" -> {
            val meme = UserMemeHistory.getNextRandomUnseen(chatIdLong)
            if (meme == null) {
                UserMemeSession.setLastShown(chatIdLong, null)
                if (MemeStorage.size() == 0) bot.sendMessage(chatId, "Здесь пока нет мемов. Нажми «Добавить мем», чтобы загрузить первый.")
                else bot.sendMessage(chatId, "Ты уже посмотрел все мемы. Добавь новый мем или подожди, пока его добавит кто-то другой.")
                return true
            }
            UserMemeSession.setLastShown(chatIdLong, meme.id)
            sendMemeWithCounts(bot, chatId, meme.id, meme.fileId)
            return true
        }
        "Добавить мем" -> {
            MemeAddState.waitingForPhoto = true
            bot.sendMessage(chatId, "Отправь картинку с мемом одним сообщением.")
            return true
        }
        "👍" -> {
            val lastId = UserMemeSession.getLastShown(chatIdLong)
            if (lastId == null) {
                bot.sendMessage(chatId, "Сначала открой мем.")
                return true
            }
            val counts = VotesStorage.like(chatIdLong, lastId)
            bot.sendMessage(chatId, "👍 ${counts.likes} | 👎 ${counts.dislikes}")
            return true
        }
        "👎" -> {
            val lastId = UserMemeSession.getLastShown(chatIdLong)
            if (lastId == null) {
                bot.sendMessage(chatId, "Сначала открой мем.")
                return true
            }
            val counts = VotesStorage.dislike(chatIdLong, lastId)
            bot.sendMessage(chatId, "👍 ${counts.likes} | 👎 ${counts.dislikes}")
            return true
        }
        "В избранное" -> {
            val lastId = UserMemeSession.getLastShown(chatIdLong)
            if (lastId == null) {
                bot.sendMessage(chatId, "Сначала открой мем, потом добавляй в избранное.")
                return true
            }
            val added = FavoritesStorage.add(chatIdLong, lastId)
            bot.sendMessage(chatId, if (added) "Добавлено в избранное." else "Этот мем уже в избранном.")
            return true
        }
        "Удалить из избранного" -> {
            val lastId = UserMemeSession.getLastShown(chatIdLong)
            if (lastId == null) {
                bot.sendMessage(chatId, "Сначала открой мем, потом удаляй из избранного.")
                return true
            }
            val removed = FavoritesStorage.remove(chatIdLong, lastId)
            bot.sendMessage(chatId, if (removed) "Удалено из избранного." else "Этого мема нет в избранном.")
            return true
        }
        "Избранное" -> {
            val favs = FavoritesStorage.list(chatIdLong)
            if (favs.isEmpty()) {
                bot.sendMessage(chatId, "В избранном пока пусто.")
                return true
            }
            for (m in favs) sendMemeWithCounts(bot, chatId, m.id, m.fileId)
            return true
        }
        "⬅️ Обратно" -> {
            currState = States.GeneralMenu
            return true
        }
    }
    return false
}

// ===================== MINI-GAMES (DICE) =====================

private fun showMiniGamesMain(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId) {
    bot.sendMessage(chatId, "🎮 Мини-игры", replyMarkup = getKeyboardMiniGamesMain())
}

private fun showMiniGamesChoose(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId) {
    bot.sendMessage(chatId, "Выберите игру:", replyMarkup = getKeyboardMiniGamesChoose())
}

private fun handleMiniGames(text: String, bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, chatIdLong: Long): Boolean {
    when (text) {
        "🎡 Прокрутить колесо" -> {
            showMiniGamesChoose(bot, chatId)
            return true
        }
        "📊 Моя статистика" -> {
            val u = GameStorage.getUser(chatIdLong)
            bot.sendMessage(
                chatId,
                """
📊 Ваша статистика

Рейтинг: ${u.rating}
Текущая серия: ${u.currentStreak}
Максимальная серия: ${u.maxStreak}
                """.trimIndent(),
                replyMarkup = getKeyboardMiniGamesMain()
            )
            return true
        }
        "🏆 Топ 10 игроков" -> {
            val top = GameStorage.getTop10()
            val result = if (top.isEmpty()) "Пока пусто." else top.mapIndexed { i, u -> "${i + 1}. ${u.rating} очков" }.joinToString("\n")
            bot.sendMessage(chatId, "🏆 Топ 10:\n$result", replyMarkup = getKeyboardMiniGamesMain())
            return true
        }
        "⚽ Гол" -> {
            val res = bot.sendDice(chatId, DiceEmoji.Football).get()
            val win = res.dice?.value == 3
            Thread.sleep(3500)
            if (win) {
                val points = GameStorage.win(chatIdLong)
                bot.sendMessage(chatId, "⚽ ГОООЛ!\n🎉 Победа!\n+$points очков рейтинга")
            } else {
                GameStorage.lose(chatIdLong)
                bot.sendMessage(chatId, "⚽ Мимо…\n😢 Проигрыш")
            }
            showMiniGamesChoose(bot, chatId)
            return true
        }
        "🏀 В кольцо" -> {
            val res = bot.sendDice(chatId, DiceEmoji.Basketball).get()
            val value = res.dice?.value
            val win = value == 4 || value == 5
            Thread.sleep(4500)
            if (win) {
                val points = GameStorage.win(chatIdLong)
                bot.sendMessage(chatId, "🏀 Попадание!\n🎉 Победа!\n+$points очков рейтинга")
            } else {
                GameStorage.lose(chatIdLong)
                bot.sendMessage(chatId, "🏀 Промах…\n😢 Проигрыш")
            }
            showMiniGamesChoose(bot, chatId)
            return true
        }
        "🎰 Колесо фортуны" -> {
            val res = bot.sendDice(chatId, DiceEmoji.SlotMachine).get()
            val v = res.dice?.value
            val win = (v == 64 || v == 1 || v == 43 || v == 22)
            Thread.sleep(2000)
            if (win) {
                val points = GameStorage.win(chatIdLong)
                bot.sendMessage(chatId, "🎰 ДЖЕКПОТ!\n🎉 Победа!\n+$points очков рейтинга")
            } else {
                GameStorage.lose(chatIdLong)
                bot.sendMessage(chatId, "🎰 Не повезло…\n😢 Проигрыш")
            }
            showMiniGamesChoose(bot, chatId)
            return true
        }
        "⬅️ Обратно" -> {
            currState = States.GeneralMenu
            return true
        }
    }
    return false
}

// ===================== PREDICTIONS =====================

private fun showPredictionsMain(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId) {
    bot.sendMessage(
        chatId,
        """
🔮 Предсказания

• Обычное (70%)
• Редкое (24%)
• Эпическое (5%)
• Легендарное (1%)

Выберите действие ниже.
        """.trimIndent(),
        replyMarkup = getKeyboardPredictionsMain()
    )
}

private fun handlePredictions(text: String, bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, chatIdLong: Long): Boolean {
    if (text == "⬅️ Обратно") {
        PredictionsFlow.stopAdd(chatIdLong)
        PredictionsFlow.stopSearch(chatIdLong)
        currState = States.GeneralMenu
        return true
    }

    // --- Поиск ---
    if (PredictionsFlow.isSearching(chatIdLong)) {
        val results = PredictionStorage.search(text)
        val out = if (results.isEmpty()) "Ничего не найдено." else results.joinToString("\n") { "• ${it.text} (${it.rarity})" }
        bot.sendMessage(chatId, "Результаты поиска:\n$out", replyMarkup = getKeyboardPredictionsMain())
        PredictionsFlow.stopSearch(chatIdLong)
        return true
    }

    // --- Добавление (шаг 2: ждём текст) ---
    val rarity = PredictionsFlow.getRarity(chatIdLong)
    if (rarity != null && rarity.isNotBlank() && text !in PredictionsFlow.rarities) {
        PredictionStorage.add(text, rarity)
        PredictionsFlow.stopAdd(chatIdLong)
        bot.sendMessage(chatId, "Предсказание добавлено!", replyMarkup = getKeyboardPredictionsMain())
        return true
    }

    when (text) {
        "Получить предсказание" -> {
            val p = PredictionStorage.randomByWeightedRarity()
            if (p == null) {
                bot.sendMessage(chatId, "Пока нет предсказаний.", replyMarkup = getKeyboardPredictionsMain())
                return true
            }
            val added = UserPredictionHistory.add(chatIdLong, p)
            val prefix = if (added) "" else "(повтор)\n"
            bot.sendMessage(chatId, "${prefix}${p.text} (${p.rarity})", replyMarkup = getKeyboardPredictionsMain())
            return true
        }
        "Мои предсказания" -> {
            val list = UserPredictionHistory.list(chatIdLong)
            val out = if (list.isEmpty()) "У вас пока нет предсказаний." else list.joinToString("\n") { "• ${it.text} (${it.rarity})" }
            bot.sendMessage(chatId, out, replyMarkup = getKeyboardPredictionsMain())
            return true
        }
        "Добавить предсказание" -> {
            PredictionsFlow.startAdd(chatIdLong)
            bot.sendMessage(chatId, "Выберите редкость:", replyMarkup = getKeyboardPredictionsRarity())
            return true
        }
        "Поиск предсказаний" -> {
            PredictionsFlow.startSearch(chatIdLong)
            bot.sendMessage(chatId, "Введите текст для поиска:")
            return true
        }
    }

    // --- Добавление (шаг 1: выбор редкости) ---
    if (text in PredictionsFlow.rarities) {
        PredictionsFlow.setRarity(chatIdLong, text)
        bot.sendMessage(chatId, "Напишите текст предсказания:")
        return true
    }

    // если неизвестная команда — просто меню
    bot.sendMessage(chatId, "Меню предсказаний", replyMarkup = getKeyboardPredictionsMain())
    return true
}

// ===================== TESTS =====================

private fun showTestsMain(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId) {
    bot.sendMessage(chatId, "📝 Тесты", replyMarkup = getKeyboardTestsMain())
}

private fun showCurrentQuestion(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, session: TestsFlow.Session) {
    val q = session.test.questions[session.qIndex]
    bot.sendMessage(
        chatId,
        "${session.test.title}\n\nВопрос ${session.qIndex + 1}/${session.test.questions.size}:\n${q.text}",
        replyMarkup = getKeyboardTestsOptions(q.options)
    )
}

private fun handleTests(text: String, bot: com.github.kotlintelegrambot.Bot, chatId: ChatId, chatIdLong: Long): Boolean {
    if (text == "⬅️ Обратно") {
        TestsFlow.stop(chatIdLong)
        currState = States.GeneralMenu
        return true
    }

    val session = TestsFlow.get(chatIdLong)

    if (session == null) {
        if (text == "Начать тест") {
            val s = TestsFlow.start(chatIdLong)
            showCurrentQuestion(bot, chatId, s)
            return true
        }
        bot.sendMessage(chatId, "Меню тестов", replyMarkup = getKeyboardTestsMain())
        return true
    }

    // ждём ответы "1) ..." / "2) ..."
    val idx = Regex("^(\\d)").find(text)?.groupValues?.get(1)?.toIntOrNull()?.minus(1)
    if (idx == null || idx !in 0..3) {
        bot.sendMessage(chatId, "Нажми на вариант ответа кнопкой.")
        return true
    }

    val (isCorrect, s) = TestsFlow.answer(chatIdLong, idx)
    if (s == null) {
        bot.sendMessage(chatId, "Сессия теста не найдена.", replyMarkup = getKeyboardTestsMain())
        return true
    }

    if (s.qIndex >= s.test.questions.size) {
        val total = s.test.questions.size
        val score = s.correct
        TestsFlow.stop(chatIdLong)
        bot.sendMessage(chatId, "Готово! ✅\nРезультат: $score/$total", replyMarkup = getKeyboardTestsMain())
        return true
    }

    if (isCorrect) bot.sendMessage(chatId, "✅ Верно!") else bot.sendMessage(chatId, "❌ Неверно!")
    showCurrentQuestion(bot, chatId, s)
    return true
}

// ===================== MAIN =====================

fun main() {
    val dotenv = dotenv()
    Logger.info("bot", "Запущен бот")

    val bot = bot {
        token = dotenv["BOT_TOKEN"]
        Logger.info("env", "Получен токен")

        dispatch {
            message {
                val chatIdLong = message.chat.id
                val chatId = ChatId.fromId(chatIdLong)
                val username = getUsername(message.from)
                val userId = getUserId(message.from)

                // 1) Photo handling (used for meme upload)
                val photo = message.photo?.lastOrNull()
                if (photo != null) {
                    val fileId = photo.fileId
                    if (currState == States.MemeMenu && MemeAddState.waitingForPhoto) {
                        MemeAddState.waitingForPhoto = false
                        val meme = MemeStorage.addMeme(fileId)
                        bot.sendMessage(chatId, "Мем сохранён под номером ${meme.id}", replyMarkup = getKeyboardMemes())
                    } else {
                        bot.sendMessage(chatId, "Чтобы добавить мем, нажми «😂 Мемы» → «Добавить мем», а потом отправь фото.")
                    }
                    return@message
                }

                val t = message.text ?: return@message

                // 0) /start всегда доступен
                if (t == "/start") {
                    currState = States.GeneralMenu
                    openGeneralMenu("/start", bot, chatId, username, userId)
                    return@message
                }

                // 2) Global entries from general menu
                when (t) {
                    "😂 Мемы" -> {
                        currState = States.MemeMenu
                        showMemeMenu(bot, chatId, chatIdLong)
                        return@message
                    }
                    "🎮 Мини-игры" -> {
                        currState = States.MiniGamesMenu
                        showMiniGamesMain(bot, chatId)
                        return@message
                    }
                    "🔮 Предсказания" -> {
                        currState = States.PredictionsMenu
                        showPredictionsMain(bot, chatId)
                        return@message
                    }
                    "📝 Тесты" -> {
                        currState = States.TestsMenu
                        showTestsMain(bot, chatId)
                        return@message
                    }
                    "💬 Обратная связь", "/feedback" -> {
                        currState = States.FeedbackMenu
                        // Показываем инструкцию + клавиатуру с "Обратно"
                        openGeneralMenu("/feedback", bot, chatId, username, userId)
                        return@message
                    }
                }

                // 3) State-driven flows
                when (currState) {
                    States.MemeMenu -> {
                        val handled = handleMemes(t, bot, chatId, chatIdLong)
                        if (handled) {
                            if (t == "⬅️ Обратно") openGeneralMenu("/start", bot, chatId, username, userId)
                            return@message
                        }
                        bot.sendMessage(chatId, "Меню мемов", replyMarkup = getKeyboardMemes())
                        return@message
                    }
                    States.MiniGamesMenu -> {
                        val handled = handleMiniGames(t, bot, chatId, chatIdLong)
                        if (handled) {
                            if (t == "⬅️ Обратно") openGeneralMenu("/start", bot, chatId, username, userId)
                            return@message
                        }
                        showMiniGamesMain(bot, chatId)
                        return@message
                    }
                    States.PredictionsMenu -> {
                        val handled = handlePredictions(t, bot, chatId, chatIdLong)
                        if (handled) {
                            if (t == "⬅️ Обратно") openGeneralMenu("/start", bot, chatId, username, userId)
                            return@message
                        }
                        return@message
                    }
                    States.TestsMenu -> {
                        val handled = handleTests(t, bot, chatId, chatIdLong)
                        if (handled) {
                            if (t == "⬅️ Обратно") openGeneralMenu("/start", bot, chatId, username, userId)
                            return@message
                        }
                        return@message
                    }
                    States.FeedbackMenu -> {
                        // ReplyClass уже обработает кнопку "⬅️ Обратно".
                        if (t == "⬅️ Обратно") {
                            currState = States.GeneralMenu
                            openGeneralMenu("/start", bot, chatId, username, userId)
                            return@message
                        }
                        val reply = FeedbackHandlers.saveFeedback(chatIdLong, username, t)
                        bot.sendMessage(chatId, reply)
                        openGeneralMenu("/start", bot, chatId, username, userId)
                        return@message
                    }
                    else -> {
                        // fallthrough to menu system
                    }
                }

                // 4) Default flow (menus / settings / account)
                openGeneralMenu(t, bot, chatId, username, userId)
            }
        }
    }

    bot.startPolling()
}
