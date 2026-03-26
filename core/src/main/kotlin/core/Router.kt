package core

import data.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.User
import kotlin.random.Random

class Router(
    private val users: UserRepository,
    private val memes: MemeRepository,
    private val predictions: PredictionRepository,
    private val games: GameRepository,
    private val tests: TestRepository,
    private val events: EventRepository,
    private val feedback: FeedbackRepository
) {
    private fun chatId(message: Message): Long = message.chat.id
    private fun sender(message: Message): User? = message.from

    private fun displayName(user: User?): String = when {
        user?.username != null -> "@${user.username}"
        !user?.firstName.isNullOrBlank() -> user!!.firstName
        else -> "Пользователь"
    }

    fun handle(bot: Bot, message: Message) {
        val user = sender(message) ?: return
        val chat = ChatId.fromId(chatId(message))
        val uid = user.id
        val uname = user.username ?: ""
        val dname = displayName(user)

        users.ensure(uid, uname, dname)
        val session = SessionStore.get(uid)

        val text = message.text?.trim()

        if (text == "/start" || text == "/menu" || text?.startsWith("/start@") == true || text?.startsWith("/menu@") == true) {
            SessionStore.clear(uid)
            bot.sendMessage(
                chat,
                "Привет! Это Chill Zone Bot — всё для досуга в одном месте.",
                replyMarkup = KeyboardFactory.mainMenu()
            )
            return
        }

        if (message.photo != null && session.action == PendingAction.ADD_MEME) {
            val photo = message.photo!!.lastOrNull() ?: return
            val caption = message.caption ?: session.data["caption"].orEmpty()
            memes.addMeme(photo.fileId, uid, caption)
            session.action = PendingAction.NONE
            session.data.clear()
            bot.sendMessage(chat, "Мем сохранён.", replyMarkup = KeyboardFactory.memesMenu())
            return
        }

        if (text.isNullOrBlank()) return

        if (text == "⬅️ Обратно") {
            SessionStore.clear(uid)
            bot.sendMessage(chat, "Главное меню.", replyMarkup = KeyboardFactory.mainMenu())
            return
        }

        if (session.action != PendingAction.NONE && handlePending(bot, chat, uid, text, session)) {
            return
        }

        when (text) {
            "👤 Профиль" -> showProfile(bot, chat, uid)
            "Изменить имя" -> {
                session.action = PendingAction.EDIT_NAME
                bot.sendMessage(chat, "Напиши новое имя.", replyMarkup = KeyboardFactory.profileMenu())
            }
            "Изменить био" -> {
                session.action = PendingAction.EDIT_BIO
                bot.sendMessage(chat, "Напиши новое описание профиля.", replyMarkup = KeyboardFactory.profileMenu())
            }
            "Скрыть/показать профиль" -> {
                val hidden = users.toggleHidden(uid)
                bot.sendMessage(chat, if (hidden) "Профиль скрыт." else "Профиль снова видим.", replyMarkup = KeyboardFactory.profileMenu())
            }
            "Переключить картинки" -> {
                val enabled = users.toggleMedia(uid)
                bot.sendMessage(chat, if (enabled) "Картинки включены." else "Картинки выключены.", replyMarkup = KeyboardFactory.settingsMenu())
            }
            "⚙️ Настройки" -> bot.sendMessage(chat, "Настройки.", replyMarkup = KeyboardFactory.settingsMenu())
            "🗂 Подборки" -> bot.sendMessage(chat, "Выбери тип подборки.", replyMarkup = KeyboardFactory.collectionsMenu())
            "🎬 Фильм", "📺 Сериал", "📚 Книга", "🎮 Игра", "По запросу" -> startCollectionFlow(bot, chat, uid, text)
            "😂 Мемы" -> {
                bot.sendMessage(chat, "Раздел мемов.", replyMarkup = KeyboardFactory.memesMenu())
                showMeme(bot, chat, uid)
            }
            "Следующий мем" -> showMeme(bot, chat, uid)
            "👍" -> rateLastMeme(bot, chat, uid, +1)
            "👎" -> rateLastMeme(bot, chat, uid, -1)
            "⭐ Избранное" -> favoriteLastMeme(bot, chat, uid)
            "Мои избранные" -> showFavorites(bot, chat, uid)
            "Добавить мем" -> {
                session.action = PendingAction.ADD_MEME
                bot.sendMessage(chat, "Отправь фото мема одним сообщением.", replyMarkup = KeyboardFactory.memesMenu())
            }
            "🔮 Предсказания" -> bot.sendMessage(chat, "Раздел предсказаний.", replyMarkup = KeyboardFactory.predictionsMenu())
            "Получить предсказание" -> givePrediction(bot, chat, uid)
            "Мои предсказания" -> showCollectedPredictions(bot, chat, uid)
            "Добавить предсказание" -> {
                session.action = PendingAction.ADD_PREDICTION_TEXT
                bot.sendMessage(chat, "Напиши текст предсказания.", replyMarkup = KeyboardFactory.predictionsMenu())
            }
            "Поиск предсказаний" -> {
                session.action = PendingAction.SEARCH_PREDICTIONS
                bot.sendMessage(chat, "Напиши часть текста или редкость.", replyMarkup = KeyboardFactory.predictionsMenu())
            }
            "📝 Тесты" -> bot.sendMessage(chat, "Раздел тестов.", replyMarkup = KeyboardFactory.testsMenu())
            "Случайный тест" -> startRandomTest(bot, chat, uid)
            "Создать тест" -> {
                session.action = PendingAction.CREATE_TEST_TITLE
                session.data.clear()
                bot.sendMessage(chat, "Придумай название теста.", replyMarkup = KeyboardFactory.testsMenu())
            }
            "Мои тесты" -> showUserTests(bot, chat)
            "📅 События" -> bot.sendMessage(chat, "Раздел событий.", replyMarkup = KeyboardFactory.eventsMenu())
            "Ближайшие события" -> showEvents(bot, chat)
            "Создать событие" -> {
                session.action = PendingAction.CREATE_EVENT_TITLE
                session.data.clear()
                bot.sendMessage(chat, "Напиши название события.", replyMarkup = KeyboardFactory.eventsMenu())
            }
            "Мои события" -> showMineEvents(bot, chat, uid)
            "🎮 Мини-игры" -> bot.sendMessage(chat, "Мини-игры.", replyMarkup = KeyboardFactory.gamesMenu())
            "⚽ Гол" -> playSimpleGame(bot, chat, uid, "football")
            "🏀 В кольцо" -> playSimpleGame(bot, chat, uid, "basketball")
            "🎡 Колесо фортуны" -> playWheel(bot, chat, uid)
            "✂️ Камень-ножницы-бумага" -> {
                session.action = PendingAction.RPS_CHOICE
                bot.sendMessage(chat, "Выбери ход: камень, ножницы или бумага.", replyMarkup = KeyboardFactory.gamesMenu())
            }
            "📊 Моя статистика" -> showGameStats(bot, chat, uid)
            "🏆 Топ игроков" -> showTopGames(bot, chat)
            "🖼 Пиксель-арт" -> bot.sendMessage(chat, "Пиксель-арт пока доступен как отдельный веб-модуль, но раздел уже предусмотрен в меню.", replyMarkup = KeyboardFactory.pixelMenu())
            "Публичный холст" -> bot.sendMessage(chat, "Публичный холст можно подключить как Web App поверх этого меню.", replyMarkup = KeyboardFactory.pixelMenu())
            "💬 Обратная связь" -> bot.sendMessage(chat, "Раздел поддержки.", replyMarkup = KeyboardFactory.feedbackMenu())
            "Оставить отзыв" -> {
                session.action = PendingAction.FEEDBACK_TEXT
                bot.sendMessage(chat, "Напиши отзыв одним сообщением.", replyMarkup = KeyboardFactory.feedbackMenu())
            }
            "Посмотреть поддержку" -> bot.sendMessage(chat, "Если что-то сломалось, просто напиши сюда отзыв — он сохранится в PostgreSQL.", replyMarkup = KeyboardFactory.feedbackMenu())
            else -> bot.sendMessage(chat, "Не понял команду. Открой меню через /start.", replyMarkup = KeyboardFactory.mainMenu())
        }
    }

    private fun handlePending(bot: Bot, chat: ChatId, uid: Long, text: String, session: Session): Boolean {
        when (session.action) {
            PendingAction.EDIT_NAME -> {
                users.updateName(uid, text)
                session.action = PendingAction.NONE
                bot.sendMessage(chat, "Имя обновлено.", replyMarkup = KeyboardFactory.profileMenu())
                return true
            }

            PendingAction.EDIT_BIO -> {
                users.updateBio(uid, text)
                session.action = PendingAction.NONE
                bot.sendMessage(chat, "Описание обновлено.", replyMarkup = KeyboardFactory.profileMenu())
                return true
            }

            PendingAction.FEEDBACK_TEXT -> {
                feedback.save(uid, "", "support", text)
                session.action = PendingAction.NONE
                session.data.clear()
                bot.sendMessage(chat, "Спасибо! Отзыв сохранён.", replyMarkup = KeyboardFactory.feedbackMenu())
                return true
            }

            PendingAction.ADD_PREDICTION_TEXT -> {
                session.data["prediction_text"] = text
                session.action = PendingAction.ADD_PREDICTION_RARITY
                bot.sendMessage(chat, "Выбери редкость.", replyMarkup = KeyboardFactory.predictionRarity())
                return true
            }

            PendingAction.ADD_PREDICTION_RARITY -> {
                val predText = session.data["prediction_text"] ?: text
                predictions.add(predText, text, uid)
                session.action = PendingAction.NONE
                session.data.clear()
                bot.sendMessage(chat, "Предсказание добавлено.", replyMarkup = KeyboardFactory.predictionsMenu())
                return true
            }

            PendingAction.SEARCH_PREDICTIONS -> {
                val result = predictions.search(text)
                session.action = PendingAction.NONE
                if (result.isEmpty()) {
                    bot.sendMessage(chat, "Ничего не найдено.", replyMarkup = KeyboardFactory.predictionsMenu())
                } else {
                    bot.sendMessage(chat, result.joinToString("\n\n") { "• ${it.rarity}: ${it.text}" }, replyMarkup = KeyboardFactory.predictionsMenu())
                }
                return true
            }

            PendingAction.CREATE_TEST_TITLE -> {
                val testId = tests.createTest(text, uid)
                session.data["test_id"] = testId.toString()
                session.action = PendingAction.CREATE_TEST_KIND
                bot.sendMessage(chat, "Выбери тип вопроса.", replyMarkup = KeyboardFactory.testKinds())
                return true
            }

            PendingAction.CREATE_TEST_KIND -> {
                if (text !in listOf("SINGLE", "MULTI", "NUMBER", "MATCH")) {
                    bot.sendMessage(chat, "Нужно выбрать SINGLE, MULTI, NUMBER или MATCH.", replyMarkup = KeyboardFactory.testKinds())
                    return true
                }
                session.data["kind"] = text
                session.action = PendingAction.CREATE_TEST_PROMPT
                bot.sendMessage(
                    chat,
                    when (text) {
                        "SINGLE" -> "Отправь вопрос в формате: Вопрос | вариант1;вариант2;вариант3 | 2"
                        "MULTI" -> "Отправь вопрос в формате: Вопрос | вариант1;вариант2;вариант3 | 1,3"
                        "NUMBER" -> "Отправь вопрос в формате: Вопрос | 42"
                        else -> "Отправь вопрос в формате: Вопрос | лев1=прав1;лев2=прав2"
                    },
                    replyMarkup = KeyboardFactory.testKinds()
                )
                return true
            }

            PendingAction.CREATE_TEST_PROMPT -> {
                val testId = session.data["test_id"]?.toLongOrNull() ?: return true
                val kind = session.data["kind"] ?: return true
                val parts = text.split("|").map { it.trim() }
                if (parts.isEmpty()) {
                    bot.sendMessage(chat, "Неверный формат.", replyMarkup = KeyboardFactory.testKinds())
                    return true
                }
                val prompt = parts[0]
                val options = if (parts.size > 1) normalizeList(parts[1]) else emptyList()
                val answer = if (parts.size > 2) parts[2] else parts.getOrNull(1).orEmpty()
                val questionKind = QuestionKind.valueOf(kind)
                val position = tests.questions(testId).size + 1
                tests.addQuestion(testId, position, questionKind, prompt, options, answer)
                session.action = PendingAction.CREATE_TEST_MORE
                bot.sendMessage(chat, "Вопрос добавлен. Добавить ещё?", replyMarkup = KeyboardFactory.testsContinue())
                return true
            }

            PendingAction.CREATE_TEST_MORE -> {
                when (text.lowercase()) {
                    "да" -> {
                        session.action = PendingAction.CREATE_TEST_KIND
                        bot.sendMessage(chat, "Выбери тип следующего вопроса.", replyMarkup = KeyboardFactory.testKinds())
                    }

                    "нет" -> {
                        session.action = PendingAction.NONE
                        session.data.clear()
                        bot.sendMessage(chat, "Тест сохранён.", replyMarkup = KeyboardFactory.testsMenu())
                    }

                    else -> bot.sendMessage(chat, "Ответь Да или Нет.", replyMarkup = KeyboardFactory.testsContinue())
                }
                return true
            }

            PendingAction.PLAY_TEST -> {
                handleTestAnswer(bot, chat, uid, text, session)
                return true
            }

            PendingAction.CREATE_EVENT_TITLE -> {
                session.data["title"] = text
                session.action = PendingAction.CREATE_EVENT_DESC
                bot.sendMessage(chat, "Опиши событие.", replyMarkup = KeyboardFactory.eventsMenu())
                return true
            }

            PendingAction.CREATE_EVENT_DESC -> {
                session.data["description"] = text
                session.action = PendingAction.CREATE_EVENT_PLACE
                bot.sendMessage(chat, "Укажи место проведения.", replyMarkup = KeyboardFactory.eventsMenu())
                return true
            }

            PendingAction.CREATE_EVENT_PLACE -> {
                session.data["place"] = text
                session.action = PendingAction.CREATE_EVENT_TIME
                bot.sendMessage(chat, "Укажи время и дату в свободной форме.", replyMarkup = KeyboardFactory.eventsMenu())
                return true
            }

            PendingAction.CREATE_EVENT_TIME -> {
                session.data["time"] = text
                session.action = PendingAction.CREATE_EVENT_MAX
                bot.sendMessage(chat, "Сколько максимум человек?", replyMarkup = KeyboardFactory.eventsMenu())
                return true
            }

            PendingAction.CREATE_EVENT_MAX -> {
                session.data["max"] = text
                session.action = PendingAction.CREATE_EVENT_KIND
                bot.sendMessage(chat, "Укажи тип события: театр, кино, кастом и т.д.", replyMarkup = KeyboardFactory.eventsMenu())
                return true
            }

            PendingAction.CREATE_EVENT_KIND -> {
                val title = session.data["title"] ?: return true
                val description = session.data["description"] ?: return true
                val place = session.data["place"] ?: return true
                val time = session.data["time"] ?: return true
                val max = session.data["max"]?.toIntOrNull() ?: 0
                val eventId = events.add(uid, title, description, place, time, max, text)
                session.action = PendingAction.NONE
                session.data.clear()
                bot.sendMessage(chat, "Событие #$eventId создано.", replyMarkup = KeyboardFactory.eventsMenu())
                return true
            }

            PendingAction.COLLECTION_QUERY -> {
                val type = session.data["collection_type"]?.let { RecommendationType.valueOf(it) } ?: RecommendationType.FILM
                val item = RecommendationRepository.random(type, text)
                session.action = PendingAction.NONE
                session.data.clear()
                if (item == null) {
                    bot.sendMessage(chat, "Ничего не нашёл.", replyMarkup = KeyboardFactory.collectionsMenu())
                } else {
                    bot.sendMessage(chat, RecommendationRepository.summarize(item), replyMarkup = KeyboardFactory.collectionsMenu())
                }
                return true
            }

            PendingAction.RPS_CHOICE -> {
                val userChoice = text.lowercase()
                val botChoice = listOf("камень", "ножницы", "бумага").random()
                val result = when {
                    userChoice == botChoice -> "Ничья"
                    userChoice == "камень" && botChoice == "ножницы" -> "Победа"
                    userChoice == "ножницы" && botChoice == "бумага" -> "Победа"
                    userChoice == "бумага" && botChoice == "камень" -> "Победа"
                    else -> "Поражение"
                }
                if (result == "Победа") {
                    games.win(uid, 5)
                    users.addRating(uid, 1)
                } else if (result == "Поражение") {
                    games.lose(uid)
                }
                session.action = PendingAction.NONE
                bot.sendMessage(chat, "Я выбрал: $botChoice. Результат: $result.", replyMarkup = KeyboardFactory.gamesMenu())
                return true
            }

            else -> return false
        }
    }

    private fun showProfile(bot: Bot, chat: ChatId, uid: Long) {
        val profile = users.profile(uid) ?: return
        val memeCount = memes.total()
        val predCount = predictions.total()
        val testCount = tests.total()
        val eventCount = events.mine(uid).size
        val gameStats = games.stats(uid)
        bot.sendMessage(
            chat,
            buildString {
                append("👤 Профиль\n\n")
                append("Имя: ").append(profile.displayName.ifBlank { profile.username }).append("\n")
                append("Username: ").append(profile.username.ifBlank { "не указан" }).append("\n")
                append("Рейтинг аккаунта: ").append(profile.rating).append("\n")
                append("Био: ").append(profile.bio.ifBlank { "не заполнено" }).append("\n")
                append("Профиль: ").append(if (profile.hidden) "скрыт" else "видим").append("\n")
                append("Картинки в сообщениях: ").append(formatBool(profile.showMedia)).append("\n\n")
                append("Мемов в базе: ").append(memeCount).append("\n")
                append("Предсказаний: ").append(predCount).append("\n")
                append("Тестов: ").append(testCount).append("\n")
                append("Событий создано: ").append(eventCount).append("\n")
                append("Игровой рейтинг: ").append(gameStats.rating).append(" (побед ").append(gameStats.wins).append(", streak ").append(gameStats.streak).append(")")
            },
            replyMarkup = KeyboardFactory.profileMenu()
        )
    }

    private fun showMeme(bot: Bot, chat: ChatId, uid: Long) {
        val meme = memes.randomUnseen(uid)
        if (meme == null) {
            bot.sendMessage(chat, "Пока мемов нет или ты уже просмотрел все.", replyMarkup = KeyboardFactory.memesMenu())
            return
        }
        memes.markSeen(uid, meme.id)
        val counts = memes.counts(meme.id)
        SessionStore.get(uid).data["last_meme"] = meme.id.toString()
        bot.sendPhoto(chat, meme.fileId)
        bot.sendMessage(chat, "👍 ${counts.likes} | 👎 ${counts.dislikes}\n${meme.caption}", replyMarkup = KeyboardFactory.memesMenu())
    }

    private fun lastMeme(uid: Long): MemeItem? {
        val id = SessionStore.get(uid).data["last_meme"]?.toLongOrNull() ?: return null
        return memes.getById(id)
    }

    private fun rateLastMeme(bot: Bot, chat: ChatId, uid: Long, vote: Int) {
        val meme = lastMeme(uid) ?: run {
            bot.sendMessage(chat, "Сначала открой мем через «Следующий мем».", replyMarkup = KeyboardFactory.memesMenu())
            return
        }
        val counts = memes.vote(uid, meme.id, vote)
        bot.sendMessage(chat, "👍 ${counts.likes} | 👎 ${counts.dislikes}", replyMarkup = KeyboardFactory.memesMenu())
    }

    private fun favoriteLastMeme(bot: Bot, chat: ChatId, uid: Long) {
        val meme = lastMeme(uid) ?: run {
            bot.sendMessage(chat, "Сначала открой мем.", replyMarkup = KeyboardFactory.memesMenu())
            return
        }
        val added = memes.toggleFavorite(uid, meme.id)
        bot.sendMessage(chat, if (added) "Добавлено в избранное." else "Убрано из избранного.", replyMarkup = KeyboardFactory.memesMenu())
    }

    private fun showFavorites(bot: Bot, chat: ChatId, uid: Long) {
        val favs = memes.favorites(uid)
        if (favs.isEmpty()) {
            bot.sendMessage(chat, "Пока избранное пусто.", replyMarkup = KeyboardFactory.memesMenu())
            return
        }
        favs.forEach {
            bot.sendPhoto(chat, it.fileId)
            bot.sendMessage(chat, it.caption.ifBlank { "Избранный мем #${it.id}" })
        }
        bot.sendMessage(chat, "Готово.", replyMarkup = KeyboardFactory.memesMenu())
    }

    private fun givePrediction(bot: Bot, chat: ChatId, uid: Long) {
        val p = predictions.randomWeighted()
        if (p == null) {
            bot.sendMessage(chat, "Пока нет предсказаний.", replyMarkup = KeyboardFactory.predictionsMenu())
            return
        }
        predictions.collect(uid, p.id)
        users.addRating(uid, 1)
        bot.sendMessage(chat, "🔮 ${p.text}\n\nРедкость: ${p.rarity}", replyMarkup = KeyboardFactory.predictionsMenu())
    }

    private fun showCollectedPredictions(bot: Bot, chat: ChatId, uid: Long) {
        val list = predictions.listCollected(uid)
        if (list.isEmpty()) {
            bot.sendMessage(chat, "Коллекция предсказаний пуста.", replyMarkup = KeyboardFactory.predictionsMenu())
            return
        }
        bot.sendMessage(chat, list.joinToString("\n\n") { "• ${it.rarity}: ${it.text}" }, replyMarkup = KeyboardFactory.predictionsMenu())
    }

    private fun startRandomTest(bot: Bot, chat: ChatId, uid: Long) {
        val test = tests.randomTest()
        if (test == null) {
            bot.sendMessage(chat, "Пока нет тестов.", replyMarkup = KeyboardFactory.testsMenu())
            return
        }
        val runId = tests.createRun(uid, test.id)
        val session = SessionStore.get(uid)
        session.action = PendingAction.PLAY_TEST
        session.data.clear()
        session.data["run_id"] = runId.toString()
        session.data["test_id"] = test.id.toString()
        session.data["index"] = "0"
        session.data["correct"] = "0"
        sendTestQuestion(bot, chat, uid, runId)
    }

    private fun handleTestAnswer(bot: Bot, chat: ChatId, uid: Long, text: String, session: Session) {
        val runId = session.data["run_id"]?.toLongOrNull() ?: return
        val testId = session.data["test_id"]?.toLongOrNull() ?: return
        val index = session.data["index"]?.toIntOrNull() ?: 0
        val questions = tests.questions(testId)
        if (index >= questions.size) {
            session.action = PendingAction.NONE
            session.data.clear()
            bot.sendMessage(chat, "Тест уже завершён.", replyMarkup = KeyboardFactory.testsMenu())
            return
        }

        val q = questions[index]
        val correct = isCorrect(q, text)
        val newCorrect = session.data["correct"]?.toIntOrNull()?.plus(if (correct) 1 else 0) ?: 0
        tests.saveAnswer(runId, q.id, text, correct)
        tests.updateRun(runId, index + 1, newCorrect, index + 1 >= questions.size)

        if (correct) users.addRating(uid, 1)

        if (index + 1 >= questions.size) {
            session.action = PendingAction.NONE
            session.data.clear()
            bot.sendMessage(chat, "Тест завершён. Результат: $newCorrect/${questions.size}", replyMarkup = KeyboardFactory.testsMenu())
            return
        }

        session.data["index"] = (index + 1).toString()
        session.data["correct"] = newCorrect.toString()
        sendTestQuestion(bot, chat, uid, runId)
    }

    private fun sendTestQuestion(bot: Bot, chat: ChatId, uid: Long, runId: Long) {
        val runInfo = tests.getRun(runId) ?: return
        val testId = runInfo.second
        val index = runInfo.third
        val questions = tests.questions(testId)
        if (index >= questions.size) return

        val q = questions[index]
        val header = "Вопрос ${index + 1}/${questions.size}:\n${q.prompt}"
        when (q.kind) {
            QuestionKind.SINGLE -> bot.sendMessage(chat, header + "\n\nОтветь одним из вариантов: ${q.options.joinToString(", ")}", replyMarkup = KeyboardFactory.testsMenu())
            QuestionKind.MULTI -> bot.sendMessage(chat, header + "\n\nОтправь несколько вариантов через запятую.", replyMarkup = KeyboardFactory.testsMenu())
            QuestionKind.NUMBER -> bot.sendMessage(chat, header + "\n\nОтветь числом.", replyMarkup = KeyboardFactory.testsMenu())
            QuestionKind.MATCH -> bot.sendMessage(chat, header + "\n\nОтправь пары в виде левый=правый;левый2=правый2.", replyMarkup = KeyboardFactory.testsMenu())
        }
    }

    private fun isCorrect(q: TestQuestion, answer: String): Boolean {
        val user = answer.trim().lowercase()
        return when (q.kind) {
            QuestionKind.SINGLE -> {
                val expected = q.answer.trim()
                val optionIndex = expected.toIntOrNull()
                if (optionIndex != null && optionIndex in 1..q.options.size) {
                    q.options[optionIndex - 1].trim().lowercase() == user || user == expected.lowercase()
                } else {
                    q.answer.trim().lowercase() == user
                }
            }

            QuestionKind.MULTI -> {
                val expected = q.answer.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                val got = user.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                expected == got
            }

            QuestionKind.NUMBER -> q.answer.trim() == user
            QuestionKind.MATCH -> normalizePairs(q.answer) == normalizePairs(answer)
        }
    }

    private fun normalizePairs(text: String): Map<String, String> {
        return text.split(";", ",")
            .mapNotNull { token ->
                val idx = token.indexOf("=")
                if (idx <= 0) return@mapNotNull null
                token.substring(0, idx).trim().lowercase() to token.substring(idx + 1).trim().lowercase()
            }
            .toMap()
    }

    private fun showUserTests(bot: Bot, chat: ChatId) {
        val list = tests.listTests()
        if (list.isEmpty()) {
            bot.sendMessage(chat, "Пока тестов нет.", replyMarkup = KeyboardFactory.testsMenu())
            return
        }
        val my = list.take(10).joinToString("\n") { "• #${it.id} ${it.title}" }
        bot.sendMessage(chat, "Доступные тесты:\n$my", replyMarkup = KeyboardFactory.testsMenu())
    }

    private fun showEvents(bot: Bot, chat: ChatId) {
        val list = events.upcoming()
        if (list.isEmpty()) {
            bot.sendMessage(chat, "Пока событий нет.", replyMarkup = KeyboardFactory.eventsMenu())
            return
        }
        bot.sendMessage(chat, list.joinToString("\n\n") { "• #${it.id} ${it.title}\n${it.place}\n${it.startsAt}\n${it.kind}" }, replyMarkup = KeyboardFactory.eventsMenu())
    }

    private fun showMineEvents(bot: Bot, chat: ChatId, uid: Long) {
        val list = events.mine(uid)
        if (list.isEmpty()) {
            bot.sendMessage(chat, "У тебя пока нет своих событий.", replyMarkup = KeyboardFactory.eventsMenu())
            return
        }
        bot.sendMessage(chat, list.joinToString("\n\n") { "• #${it.id} ${it.title}\n${it.place}\n${it.startsAt}" }, replyMarkup = KeyboardFactory.eventsMenu())
    }

    private fun startCollectionFlow(bot: Bot, chat: ChatId, uid: Long, typeText: String) {
        val session = SessionStore.get(uid)
        session.action = PendingAction.COLLECTION_QUERY
        session.data["collection_type"] = when (typeText) {
            "🎬 Фильм" -> RecommendationType.FILM.name
            "📺 Сериал" -> RecommendationType.SERIES.name
            "📚 Книга" -> RecommendationType.BOOK.name
            else -> RecommendationType.GAME.name
        }
        bot.sendMessage(chat, "Напиши запрос, настроение или жанр.", replyMarkup = KeyboardFactory.collectionsMenu())
    }

    private fun playSimpleGame(bot: Bot, chat: ChatId, uid: Long, mode: String) {
        val points = when (mode) {
            "football" -> listOf(0, 5, 10, 15).random()
            "basketball" -> listOf(5, 10, 15, 20).random()
            else -> Random.nextInt(1, 10)
        }
        if (points > 0) {
            games.win(uid, points)
            users.addRating(uid, maxOf(1, points / 5))
            bot.sendMessage(chat, "Игра завершена. Ты получил $points очков!", replyMarkup = KeyboardFactory.gamesMenu())
        } else {
            games.lose(uid)
            bot.sendMessage(chat, "Не повезло, в этот раз без очков.", replyMarkup = KeyboardFactory.gamesMenu())
        }
    }

    private fun playWheel(bot: Bot, chat: ChatId, uid: Long) {
        val prize = listOf(0, 5, 10, 15, 20, 30).random()
        if (prize > 0) {
            games.win(uid, prize)
            users.addRating(uid, maxOf(1, prize / 10))
            bot.sendMessage(chat, "Колесо фортуны: +$prize рейтинга!", replyMarkup = KeyboardFactory.gamesMenu())
        } else {
            games.lose(uid)
            bot.sendMessage(chat, "Колесо фортуны: пусто.", replyMarkup = KeyboardFactory.gamesMenu())
        }
    }

    private fun showGameStats(bot: Bot, chat: ChatId, uid: Long) {
        val s = games.stats(uid)
        bot.sendMessage(
            chat,
            "Статистика:\nРейтинг: ${s.rating}\nПобеды: ${s.wins}\nПоражения: ${s.losses}\nStreak: ${s.streak}\nЛучший streak: ${s.bestStreak}",
            replyMarkup = KeyboardFactory.gamesMenu()
        )
    }

    private fun showTopGames(bot: Bot, chat: ChatId) {
        val top = games.top()
        if (top.isEmpty()) {
            bot.sendMessage(chat, "Пока нет игроков в таблице.", replyMarkup = KeyboardFactory.gamesMenu())
            return
        }
        bot.sendMessage(
            chat,
            top.withIndex().joinToString("\n") { (idx, s) -> "${idx + 1}. #${s.userId} — ${s.rating}" },
            replyMarkup = KeyboardFactory.gamesMenu()
        )
    }
}