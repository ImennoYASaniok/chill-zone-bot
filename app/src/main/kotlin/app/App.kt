package app

import BooksPart.BooksStates
import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineButton
import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineClass1
import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass1
import KinoPart.KinoStates
import KinoPart.searchMoviesDB
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.entities.dice.DiceEmoji
import io.github.cdimascio.dotenv.dotenv
import BooksPart.getInfo

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
import keyboards.base.getInlineKeyboardMenu
import keyboards.base.getKeyboardMenu
import kotlin.math.min

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

        val inlineMenu = InlineClass1(
            keyboard = getInlineKeyboardMenu(),
            startCommand = "/paodsapodapodsda",
            textMessage = "inlineMenu"
        )

        val replyMenu = ReplyClass1(
            keyboard = getKeyboardMenu(),
            startCommand = "/start",
            textMessage = "replyMenu"
        )

        dispatch {

            callbackQuery("UNLIKEitSERIES") {

            }
            callbackQuery("LIKEDitSERIES") {

            }
            callbackQuery("UNLIKEitMOVIES") {

            }
            callbackQuery("LIKEDitMOVIES") {

            }
            callbackQuery("stopScrolling") {
                currState = States.GeneralMenu
                openGeneralMenu("/start", bot, ChatId.fromId(callbackQuery.message!!.chat.id), callbackQuery.from.username!!, callbackQuery.from.id)
            }


            callbackQuery("dislikeITmovies") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                KinoStates.filmIndex++
                if (KinoStates.filmIndex >= KinoStates.foundFilms.size) {
                    bot.sendMessage(
                        chatId,
                        "К сожалению, фильмы, которые я знаю на эту тему, закончились. Возвращаю вас в главное меню."
                    )
                    currState = States.GeneralMenu
                    openGeneralMenu("/start", bot, ChatId.fromId(callbackQuery.message!!.chat.id), callbackQuery.from.username!!, callbackQuery.from.id)
                } else {

                    val desc =
                        KinoStates.foundFilms[KinoStates.filmIndex].replace("Описание: null", "Описание: отсутствует")

                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("Нравится", "LIKEDitMOVIES"), InlineButton("Не нравится", "UNLIKEitMOVIES")
                        ), mutableListOf(
                            InlineButton("Далее", "dislikeITmovies")
                        ),
                        mutableListOf(
                            InlineButton("Завершить", "stopScrolling")
                        )
                    )
                    inlineMenu.update()
                    bot.sendMessage(chatId, desc, replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                }
            }
            callbackQuery("endChoosingGenresMovies") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                bot.sendMessage(chatId, "Вот один из фильмов, которые подходят под ваш выбор:")

                searchMoviesDB()

                if (KinoStates.filmIndex == KinoStates.foundFilms.size) {
                    bot.sendMessage(
                        chatId,
                        "К сожалению, фильмы, которые я знаю на эту тему, закончились. Возвращаю вас в главное меню."
                    )
                    currState = States.GeneralMenu
                    openGeneralMenu("/start", bot, ChatId.fromId(callbackQuery.message!!.chat.id), callbackQuery.from.username!!, callbackQuery.from.id)
                } else {

                    val desc =
                        KinoStates.foundFilms[KinoStates.filmIndex].replace("Описание: null", "Описание: отсутствует")
                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("Нравится", "LIKEDitMOVIES"), InlineButton("Не нравится", "UNLIKEitMOVIES")
                        ), mutableListOf(
                            InlineButton("Далее", "dislikeITmovies")
                        ), mutableListOf(
                            InlineButton("Завершить", "stopScrolling")
                        )
                    )
                    inlineMenu.update()
                    bot.sendMessage(chatId, desc, replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                }
            }

            callbackQuery("dislikeITseries") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                KinoStates.filmIndex++
                if (KinoStates.filmIndex >= KinoStates.foundFilms.size) {
                    bot.sendMessage(
                        chatId,
                        "К сожалению, сериалы, которые я знаю на эту тему, закончились. Возвращаю вас в главное меню."
                    )
                    currState = States.GeneralMenu
                    openGeneralMenu("/start", bot, ChatId.fromId(callbackQuery.message!!.chat.id), callbackQuery.from.username!!, callbackQuery.from.id)
                } else {

                    val desc =
                        KinoStates.foundFilms[KinoStates.filmIndex].replace("Описание: null", "Описание: отсутствует")

                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("Нравится", "LIKEDitSERIES"), InlineButton("Не нравится", "UNLIKEitSERIES")
                        ), mutableListOf(
                            InlineButton("Далее", "dislikeITseries")
                        ), mutableListOf(
                            InlineButton("Завершить", "stopScrolling")
                        )
                    )
                    currState = States.GeneralMenu
                    openGeneralMenu("/start", bot, ChatId.fromId(callbackQuery.message!!.chat.id), callbackQuery.from.username!!, callbackQuery.from.id)
                    inlineMenu.update()
                    bot.sendMessage(chatId, desc, replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                }
            }
            callbackQuery("endChoosingGenresSeries") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                bot.sendMessage(chatId, "Вот один из сериалов, которые подходят под ваш выбор:")

                KinoPart.searchSeriesDB()

                if (KinoStates.filmIndex == KinoStates.foundFilms.size) {
                    bot.sendMessage(
                        chatId,
                        "К сожалению, сериалы, которые я знаю на эту тему, закончились. Возвращаю вас в главное меню."
                    )
                    currState = States.GeneralMenu
                    openGeneralMenu("/start", bot, ChatId.fromId(callbackQuery.message!!.chat.id), callbackQuery.from.username!!, callbackQuery.from.id)
                } else {

                    val desc =
                        KinoStates.foundFilms[KinoStates.filmIndex].replace("Описание: null", "Описание: отсутствует")
                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("Нравится", "LIKEDitSERIES"), InlineButton("Не нравится", "UNLIKEitSERIES")
                        ), mutableListOf(
                            InlineButton("Далее", "dislikeITseries")
                        ), mutableListOf(
                            InlineButton("Завершить", "stopScrolling")
                        )
                    )
                    inlineMenu.update()
                    bot.sendMessage(chatId, desc, replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                }
            }

            callbackQuery("биография") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[0][0] = InlineButton("✅ биография", "biografiya")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("биография")
            }
            callbackQuery("biografiya") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[0][0] = InlineButton("биография", "биография")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("биография")
            }


            callbackQuery("музыка") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[0][1] = InlineButton("✅ музыка", "muzyka")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("музыка")
            }
            callbackQuery("muzyka") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[0][1] = InlineButton("музыка", "музыка")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("музыка")
            }


            callbackQuery("триллер") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[1][0] = InlineButton("✅ триллер", "triller")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("триллер")
            }
            callbackQuery("triller") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[1][0] = InlineButton("триллер", "триллер")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("триллер")
            }


            callbackQuery("ток-шоу") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[1][1] = InlineButton("✅ ток-шоу", "tokshou")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("ток-шоу")
            }
            callbackQuery("tokshou") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[1][1] = InlineButton("ток-шоу", "ток-шоу")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("ток-шоу")
            }


            callbackQuery("вестерн") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[2][0] = InlineButton("✅ вестерн", "vestern")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("вестерн")
            }
            callbackQuery("vestern") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[2][0] = InlineButton("вестерн", "вестерн")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("вестерн")
            }


            callbackQuery("приключения") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[2][1] = InlineButton("✅ приключения", "priklyucheniya")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("приключения")
            }
            callbackQuery("priklyucheniya") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[2][1] = InlineButton("приключения", "приключения")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("приключения")
            }


            callbackQuery("военный") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[3][0] = InlineButton("✅ военный", "voennyj")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("военный")
            }
            callbackQuery("voennyj") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[3][0] = InlineButton("военный", "военный")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("военный")
            }


            callbackQuery("игра") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[3][1] = InlineButton("✅ игра", "igra")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("игра")
            }
            callbackQuery("igra") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[3][1] = InlineButton("игра", "игра")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("игра")
            }


            callbackQuery("семейный") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[4][0] = InlineButton("✅ семейный", "semejnyj")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("семейный")
            }
            callbackQuery("semejnyj") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[4][0] = InlineButton("семейный", "семейный")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("семейный")
            }


            callbackQuery("ужасы") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[4][1] = InlineButton("✅ ужасы", "uzhasy")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("ужасы")
            }
            callbackQuery("uzhasy") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[4][1] = InlineButton("ужасы", "ужасы")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("ужасы")
            }


            callbackQuery("фэнтези") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[5][0] = InlineButton("✅ фэнтези", "fentezi")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("фэнтези")
            }
            callbackQuery("fentezi") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[5][0] = InlineButton("фэнтези", "фэнтези")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("фэнтези")
            }


            callbackQuery("аниме") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[5][1] = InlineButton("✅ аниме", "anime")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("аниме")
            }
            callbackQuery("anime") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[5][1] = InlineButton("аниме", "аниме")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("аниме")
            }


            callbackQuery("для взрослых") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[6][0] = InlineButton("✅ для взрослых", "dlya vzroslyh")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("для взрослых")
            }
            callbackQuery("dlya vzroslyh") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[6][0] = InlineButton("для взрослых", "для взрослых")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("для взрослых")
            }


            callbackQuery("короткометражка") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[6][1] = InlineButton("✅ короткометражка", "korotkometrazhka")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("короткометражка")
            }
            callbackQuery("korotkometrazhka") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[6][1] = InlineButton("короткометражка", "короткометражка")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("короткометражка")
            }


            callbackQuery("комедия") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[7][0] = InlineButton("✅ комедия", "komediya")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("комедия")
            }
            callbackQuery("komediya") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[7][0] = InlineButton("комедия", "комедия")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("комедия")
            }


            callbackQuery("фильм-нуар") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[7][1] = InlineButton("✅ фильм-нуар", "filmnuar")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("фильм-нуар")
            }
            callbackQuery("filmnuar") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[7][1] = InlineButton("фильм-нуар", "фильм-нуар")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("фильм-нуар")
            }


            callbackQuery("церемония") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[8][0] = InlineButton("✅ церемония", "ceremoniya")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("церемония")
            }
            callbackQuery("ceremoniya") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[8][0] = InlineButton("церемония", "церемония")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("церемония")
            }


            callbackQuery("боевик") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[8][1] = InlineButton("✅ боевик", "boevik")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("боевик")
            }
            callbackQuery("boevik") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[8][1] = InlineButton("боевик", "боевик")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("боевик")
            }


            callbackQuery("детектив") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[9][0] = InlineButton("✅ детектив", "detektiv")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("детектив")
            }
            callbackQuery("detektiv") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[9][0] = InlineButton("детектив", "детектив")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("детектив")
            }


            callbackQuery("новости") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[9][1] = InlineButton("✅ новости", "novosti")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("новости")
            }
            callbackQuery("novosti") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[9][1] = InlineButton("новости", "новости")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("новости")
            }


            callbackQuery("мелодрама") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[10][0] = InlineButton("✅ мелодрама", "melodrama")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("мелодрама")
            }
            callbackQuery("melodrama") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[10][0] = InlineButton("мелодрама", "мелодрама")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("мелодрама")
            }


            callbackQuery("мюзикл") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[10][1] = InlineButton("✅ мюзикл", "myuzikl")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("мюзикл")
            }
            callbackQuery("myuzikl") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[10][1] = InlineButton("мюзикл", "мюзикл")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("мюзикл")
            }


            callbackQuery("фантастика") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[11][0] = InlineButton("✅ фантастика", "fantastika")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("фантастика")
            }
            callbackQuery("fantastika") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[11][0] = InlineButton("фантастика", "фантастика")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("фантастика")
            }


            callbackQuery("реальное ТВ") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[11][1] = InlineButton("✅ реальное ТВ", "realnoe TV")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("реальное ТВ")
            }
            callbackQuery("realnoe TV") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[11][1] = InlineButton("реальное ТВ", "реальное ТВ")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("реальное ТВ")
            }


            callbackQuery("криминал") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[12][0] = InlineButton("✅ криминал", "kriminal")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("криминал")
            }
            callbackQuery("kriminal") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[12][0] = InlineButton("криминал", "криминал")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("криминал")
            }
// ===================== MINI-GAMES (DICE) =====================

            fun showMiniGamesMain(bot: com.github.kotlintelegrambot.Bot, chatId: ChatId) {
                bot.sendMessage(chatId, "🎮 Мини-игры", replyMarkup = getKeyboardMiniGamesMain())
            }

            callbackQuery("мультфильм") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[12][1] = InlineButton("✅ мультфильм", "multfilm")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("мультфильм")
            }
            callbackQuery("multfilm") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[12][1] = InlineButton("мультфильм", "мультфильм")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("мультфильм")
            }


            callbackQuery("детский") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[13][0] = InlineButton("✅ детский", "detskij")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("детский")
            }
            callbackQuery("detskij") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                inlineMenu.keyboard[13][0] = InlineButton("детский", "детский")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("детский")
            }
            callbackQuery("спорт") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                inlineMenu.keyboard[13][1] = InlineButton("✅ спорт", "sport")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("спорт")
            }
            callbackQuery("sport") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                inlineMenu.keyboard[13][1] = InlineButton("спорт", "спорт")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("спорт")
            }
            callbackQuery("концерт") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                inlineMenu.keyboard[14][0] = InlineButton("✅ концерт", "koncert")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("концерт")
            }
            callbackQuery("koncert") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                inlineMenu.keyboard[14][0] = InlineButton("концерт", "концерт")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("концерт")
            }
            callbackQuery("история") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[14][1] = InlineButton("✅ история", "istoriya")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("история")
            }
            callbackQuery("istoriya") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                inlineMenu.keyboard[14][1] = InlineButton("история", "история")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("история")
            }
            callbackQuery("документальный") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                inlineMenu.keyboard[15][0] = InlineButton("✅ документальный", "dokumentalnyj")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("документальный")
            }
            callbackQuery("dokumentalnyj") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                inlineMenu.keyboard[15][0] = InlineButton("документальный", "документальный")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("документальный")
            }
            callbackQuery("драма") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                inlineMenu.keyboard[15][1] = InlineButton("✅ драма", "droma")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.add("драма")
            }
            callbackQuery("droma") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                inlineMenu.keyboard[15][1] = InlineButton("драма", "драма")
                inlineMenu.update()
                bot.sendMessage(chatId, "Выберите жанры:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                KinoStates.chosenGenres.remove("драма")
            }
            callbackQuery("endSearchingBooks") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                println(chatId)
                currState = States.GeneralMenu
                openGeneralMenu("/start", bot, ChatId.fromId(callbackQuery.message!!.chat.id), callbackQuery.from.username!!, callbackQuery.from.id)
            }
            callbackQuery("checkNextBook") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Вот что мне удалось найти:")
                val maxIndex = min(BooksStates.searchResult.size, BooksStates.bookResponseIndex + 5) - 1
                if (BooksStates.bookResponseIndex == maxIndex) {
                    bot.sendMessage(chatId, "К сожалению это всё что я сумел найти. Возвращаю вас в главное меню.")
                } else {
                    for (outputIndex in BooksStates.bookResponseIndex..maxIndex) {
                        bot.sendMessage(chatId, BooksStates.searchResult[outputIndex])
                    }
                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("Далее", "checkNextBook")
                        ),
                        mutableListOf(
                            InlineButton("Завершить", "endSearchingBooks")
                        )
                    )
                    BooksStates.bookResponseIndex = maxIndex
                    inlineMenu.update()
                    bot.sendMessage(
                        chatId,
                        "Выберите, искать книгу по заданному запросу далее или завершить поиск:",
                        replyMarkup = inlineMenu.getKeyboardInlineMarkup()
                    )
                }
            }




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

                if (BooksStates.waitingForKeyWord) {
                    BooksStates.waitingForKeyWord = false
                    BooksStates.searchResult = getInfo(t)
                    if (BooksStates.searchResult.isEmpty()) {
                        bot.sendMessage(chatId, "К сожалению по вашему запросу я не смог ничего найти.")
                    } else {
                        bot.sendMessage(chatId, "Вот что мне удалось найти:")
                        val maxIndex = min(BooksStates.searchResult.size, BooksStates.bookResponseIndex + 5) - 1
                        for (outputIndex in BooksStates.bookResponseIndex..maxIndex) {
                            bot.sendMessage(chatId, BooksStates.searchResult[outputIndex])
                        }
                        BooksStates.bookResponseIndex = maxIndex
                        inlineMenu.keyboard = mutableListOf(
                            mutableListOf(
                                InlineButton("Далее", "checkNextBook")
                            ),
                            mutableListOf(
                                InlineButton("Завершить", "endSearchingBooks")
                            )
                        )
                        inlineMenu.update()
                        bot.sendMessage(
                            chatId,
                            "Выберите, искать книгу по заданному запросу далее или завершить поиск:",
                            replyMarkup = inlineMenu.getKeyboardInlineMarkup()
                        )
                    } } else if (t == "\uD83C\uDFA5 Фильмы") {
                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("биография", "биография"), InlineButton("музыка", "музыка")
                        ),
                        mutableListOf(
                            InlineButton("триллер", "триллер"), InlineButton("ток-шоу", "ток-шоу")
                        ),
                        mutableListOf(
                            InlineButton("вестерн", "вестерн"), InlineButton("приключения", "приключения")
                        ),
                        mutableListOf(
                            InlineButton("военный", "военный"), InlineButton("игра", "игра")
                        ),
                        mutableListOf(
                            InlineButton("семейный", "семейный"), InlineButton("ужасы", "ужасы")
                        ),
                        mutableListOf(
                            InlineButton("фэнтези", "фэнтези"), InlineButton("аниме", "аниме")
                        ),
                        mutableListOf(
                            InlineButton("для взрослых", "для взрослых"),
                            InlineButton("короткометражка", "короткометражка")
                        ),
                        mutableListOf(
                            InlineButton("комедия", "комедия"), InlineButton("фильм-нуар", "фильм-нуар")
                        ),
                        mutableListOf(
                            InlineButton("церемония", "церемония"), InlineButton("боевик", "боевик")
                        ),
                        mutableListOf(
                            InlineButton("детектив", "детектив"), InlineButton("новости", "новости")
                        ),
                        mutableListOf(
                            InlineButton("мелодрама", "мелодрама"), InlineButton("мюзикл", "мюзикл")
                        ),
                        mutableListOf(
                            InlineButton("фантастика", "фантастика"), InlineButton("реальное ТВ", "реальное ТВ")
                        ),
                        mutableListOf(
                            InlineButton("криминал", "криминал"), InlineButton("мультфильм", "мультфильм")
                        ),
                        mutableListOf(
                            InlineButton("детский", "детский"), InlineButton("спорт", "спорт")
                        ),
                        mutableListOf(
                            InlineButton("концерт", "концерт"), InlineButton("история", "история")
                        ),
                        mutableListOf(
                            InlineButton("документальный", "документальный"), InlineButton("драма", "драма")
                        ),
                        mutableListOf(InlineButton("Завершить выбор жанров", "endChoosingGenresMovies"))
                    )
                    inlineMenu.update()
//                    bot.sendPhoto(
//                        chatId,
//                        getFilmImg(),
//                        "Хороший выбор! Вот список жанров, которые вы можете выбрать для просмотра:",
//                        replyMarkup = inlineMenu.getKeyboardInlineMarkup()
//                    )
                    bot.sendMessage(
                        chatId,
                        "Хороший выбор! Вот список жанров, которые вы можете выбрать для просмотра:",
                        replyMarkup = inlineMenu.getKeyboardInlineMarkup()
                    )
                } else if (t == "\uD83C\uDF9E Сериалы") {
//                    bot.sendPhoto(chatId,
//                        getSerialImg(), "Хороший выбор! Вот список жанров, которые вы можете выбрать для просмотра:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("биография", "биография"), InlineButton("музыка", "музыка")
                        ),
                        mutableListOf(
                            InlineButton("триллер", "триллер"), InlineButton("ток-шоу", "ток-шоу")
                        ),
                        mutableListOf(
                            InlineButton("вестерн", "вестерн"), InlineButton("приключения", "приключения")
                        ),
                        mutableListOf(
                            InlineButton("военный", "военный"), InlineButton("игра", "игра")
                        ),
                        mutableListOf(
                            InlineButton("семейный", "семейный"), InlineButton("ужасы", "ужасы")
                        ),
                        mutableListOf(
                            InlineButton("фэнтези", "фэнтези"), InlineButton("аниме", "аниме")
                        ),
                        mutableListOf(
                            InlineButton("для взрослых", "для взрослых"),
                            InlineButton("короткометражка", "короткометражка")
                        ),
                        mutableListOf(
                            InlineButton("комедия", "комедия"), InlineButton("фильм-нуар", "фильм-нуар")
                        ),
                        mutableListOf(
                            InlineButton("церемония", "церемония"), InlineButton("боевик", "боевик")
                        ),
                        mutableListOf(
                            InlineButton("детектив", "детектив"), InlineButton("новости", "новости")
                        ),
                        mutableListOf(
                            InlineButton("мелодрама", "мелодрама"), InlineButton("мюзикл", "мюзикл")
                        ),
                        mutableListOf(
                            InlineButton("фантастика", "фантастика"), InlineButton("реальное ТВ", "реальное ТВ")
                        ),
                        mutableListOf(
                            InlineButton("криминал", "криминал"), InlineButton("мультфильм", "мультфильм")
                        ),
                        mutableListOf(
                            InlineButton("детский", "детский"), InlineButton("спорт", "спорт")
                        ),
                        mutableListOf(
                            InlineButton("концерт", "концерт"), InlineButton("история", "история")
                        ),
                        mutableListOf(
                            InlineButton("документальный", "документальный"), InlineButton("драма", "драма")
                        ),
                        mutableListOf(InlineButton("Завершить выбор жанров", "endChoosingGenresSeries"))
                    )
                    inlineMenu.update()
                    bot.sendMessage(
                        chatId,
                        "Хороший выбор! Вот список жанров, которые вы можете выбрать для просмотра:",
                        replyMarkup = inlineMenu.getKeyboardInlineMarkup()
                    )
                }

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
                    "\uD83D\uDDC2\uFE0F Подборки" -> {
                        replyMenu.keyboard = mutableListOf(
                            mutableListOf(
                                Button("\uD83C\uDFA5 Фильмы")
                            ),
                            mutableListOf(
                                Button("\uD83C\uDF9E Сериалы")
                            ),
                            mutableListOf(
                                Button("\uD83D\uDCD6 Книги")
                            )
                        )

                        replyMenu.update()
                        //bot.sendPhoto(chatId, getPodbImg(), "Выберите тип подборки", replyMarkup = replyMenu.getKeyboardReplyMarkup())
                        bot.sendMessage(chatId, "Выберите тип подборки", replyMarkup = replyMenu.getKeyboardReplyMarkup())

                        currState = States.CollectionsMenu
                    }
                    "\uD83D\uDCD6 Книги" -> {
                        bot.sendMessage(chatId, "Хороший выбор! Напишите ключевые слова, по которым будем искать книги:")
                        BooksStates.waitingForKeyWord = true
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
