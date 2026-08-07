package core.routers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import core.ImageManager
import core.PendingAction
import core.Session
import core.SessionStore
import core.keyboards.KeyboardFactory
import data.models.*
import data.repositories.TestRepository
import data.repositories.UserRepository
import data.services.ModerationService

object RouterTests {
    fun startRandomTest(
            bot: Bot,
            chat: ChatId,
            uid: Long,
            tests: TestRepository,
            users: UserRepository
    ) {
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
        sendTestQuestion(bot, chat, runId, tests)
    }

    fun showUserTests(bot: Bot, chat: ChatId, tests: TestRepository) {
        val list = tests.listTests()
        if (list.isEmpty()) {
            bot.sendMessage(chat, "Пока тестов нет.", replyMarkup = KeyboardFactory.testsMenu())
            return
        }
        val my = list.take(10).joinToString("\n") { "• #${it.id} ${it.title}" }
        bot.sendMessage(
                chat,
                "Доступные тесты:\n$my",
                parseMode = ParseMode.HTML,
                replyMarkup = KeyboardFactory.testsMenu()
        )
    }

    fun handleTestAction(
            bot: Bot,
            chat: ChatId,
            uid: Long,
            text: String,
            tests: TestRepository,
            users: UserRepository,
            session: Session
    ): Boolean {
        when (text) {
            "📝 Тесты" -> {
                showUserTests(bot, chat, tests)
                return true
            }
            "Случайный тест" -> {
                startRandomTest(bot, chat, uid, tests, users)
                return true
            }
            "Мои тесты" -> {
                showUserTests(bot, chat, tests)
                return true
            }
            "Создать тест" -> {
                session.action = PendingAction.CREATE_TEST_TITLE
                session.data.clear()
                val profile = users.profile(uid)
                ImageManager.sendMessageWithImage(bot, chat, "Придумай название теста.", profile) {
                    bot.sendMessage(
                            chat,
                            "Ожидаю название:",
                            replyMarkup = KeyboardFactory.testsMenu()
                    )
                }
                return true
            }
            "⬅️ Обратно" -> {
                // Возврат в главное меню или в меню тестов
                if (session.action in
                                listOf(
                                        PendingAction.CREATE_TEST_TITLE,
                                        PendingAction.CREATE_TEST_KIND,
                                        PendingAction.CREATE_TEST_PROMPT,
                                        PendingAction.CREATE_TEST_MORE,
                                        PendingAction.PLAY_TEST
                                )
                ) {
                    session.action = PendingAction.NONE
                    session.data.clear()
                    showUserTests(bot, chat, tests)
                } else {
                    // Возврат в главное меню
                    SessionStore.clear(uid)
                    bot.sendMessage(chat, "Главное меню.", replyMarkup = KeyboardFactory.mainMenu())
                }
                return true
            }
            else -> {
                when (session.action) {
                    PendingAction.CREATE_TEST_TITLE,
                    PendingAction.CREATE_TEST_KIND,
                    PendingAction.CREATE_TEST_PROMPT,
                    PendingAction.CREATE_TEST_MORE -> {
                        val moderationResult = ModerationService.checkText(uid, text)
                        if (!moderationResult.isAllowed) {
                            if (moderationResult.shouldBan) {
                                ModerationService.banForViolation(
                                        uid,
                                        "Использование ненормативной лексики"
                                )
                                ModerationService.clearWarnings(uid)
                                bot.sendMessage(
                                        chat,
                                        "🚫 Вы были заблокированы за использование ненормативной лексики."
                                )
                                session.action = PendingAction.NONE
                                session.data.clear()
                            } else {
                                ModerationService.addWarning(uid)
                                bot.sendMessage(
                                        chat,
                                        "⚠️ <b>Предупреждение!</b>\n\nВаше сообщение содержит недопустимый контент.\n\nПовторное нарушение приведёт к автоматическому бану на 1 сутки.",
                                        parseMode = ParseMode.HTML
                                )
                            }
                            return true
                        }
                    }
                    else -> {}
                }

                when (session.action) {
                    PendingAction.CREATE_TEST_TITLE -> {
                        session.data["test_title"] = text
                        session.action = PendingAction.CREATE_TEST_KIND
                        bot.sendMessage(chat, "Какой тип теста? (single, multi, number, match)")
                        return true
                    }
                    PendingAction.CREATE_TEST_KIND -> {
                        val kind = text.lowercase()
                        if (kind !in listOf("single", "multi", "number", "match")) {
                            bot.sendMessage(
                                    chat,
                                    "Неверный тип. Используй: single, multi, number, match"
                            )
                            return true
                        }
                        session.data["test_kind"] = kind
                        session.action = PendingAction.CREATE_TEST_PROMPT
                        bot.sendMessage(chat, "Напиши вопрос:")
                        return true
                    }
                    PendingAction.CREATE_TEST_PROMPT -> {
                        session.data["test_prompt"] = text
                        session.action = PendingAction.CREATE_TEST_MORE
                        bot.sendMessage(
                                chat,
                                "Напиши варианты ответа (через |) и правильный ответ после |:\nПример: Вариант1 | Вариант2 | Вариант3 | 1"
                        )
                        return true
                    }
                    PendingAction.CREATE_TEST_MORE -> {
                        val title = session.data["test_title"] ?: ""
                        val kind = session.data["test_kind"] ?: "single"
                        val prompt = session.data["test_prompt"] ?: ""

                        val parts = text.split("|").map { it.trim() }
                        val options = parts.dropLast(1)
                        val answer = parts.last()

                        tests.createTestFull(title, prompt, kind, answer, options)
                        session.action = PendingAction.NONE
                        session.data.clear()
                        bot.sendMessage(
                                chat,
                                "✅ Тест создан!\n\n📌 $title",
                                replyMarkup = KeyboardFactory.testsMenu()
                        )
                        return true
                    }
                    else -> return false
                }
            }
        }
    }

    fun handleTestAnswer(
            bot: Bot,
            chat: ChatId,
            uid: Long,
            text: String,
            session: Session,
            tests: TestRepository,
            users: UserRepository
    ): Boolean {
        val runId = session.data["run_id"]?.toLongOrNull() ?: return false
        val testId = session.data["test_id"]?.toLongOrNull() ?: return false
        val index = session.data["index"]?.toIntOrNull() ?: 0
        val questions = tests.questions(testId)
        if (index >= questions.size) {
            session.action = PendingAction.NONE
            session.data.clear()
            bot.sendMessage(chat, "Тест уже завершён.", replyMarkup = KeyboardFactory.testsMenu())
            return true
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
            bot.sendMessage(
                    chat,
                    "Тест завершён. Результат: $newCorrect/${questions.size}",
                    replyMarkup = KeyboardFactory.testsMenu()
            )
            return true
        }

        session.data["index"] = (index + 1).toString()
        session.data["correct"] = newCorrect.toString()
        sendTestQuestion(bot, chat, runId, tests)
        return true
    }

    private fun sendTestQuestion(bot: Bot, chat: ChatId, runId: Long, tests: TestRepository) {
        val runInfo = tests.getRun(runId) ?: return
        val testId = runInfo.second
        val index = runInfo.third
        val questions = tests.questions(testId)
        if (index >= questions.size) return

        val q = questions[index]
        val header = "Вопрос ${index + 1}/${questions.size}:\n${q.prompt}"
        when (q.kind) {
            QuestionKind.SINGLE ->
                    bot.sendMessage(
                            chat,
                            header +
                                    "\n\nОтветь одним из вариантов: ${q.options.joinToString(", ")}",
                            parseMode = ParseMode.HTML,
                            replyMarkup = KeyboardFactory.testsMenu()
                    )
            QuestionKind.MULTI ->
                    bot.sendMessage(
                            chat,
                            header + "\n\nОтправь несколько вариантов через запятую.",
                            parseMode = ParseMode.HTML,
                            replyMarkup = KeyboardFactory.testsMenu()
                    )
            QuestionKind.NUMBER ->
                    bot.sendMessage(
                            chat,
                            header + "\n\nОтветь числом.",
                            parseMode = ParseMode.HTML,
                            replyMarkup = KeyboardFactory.testsMenu()
                    )
            QuestionKind.MATCH ->
                    bot.sendMessage(
                            chat,
                            header + "\n\nОтправь пары в виде левый=правый;левый2=правый2.",
                            parseMode = ParseMode.HTML,
                            replyMarkup = KeyboardFactory.testsMenu()
                    )
        }
    }

    private fun isCorrect(q: TestQuestion, answer: String): Boolean {
        val user = answer.trim().lowercase()
        return when (q.kind) {
            QuestionKind.SINGLE -> {
                val expected = q.answer.trim()
                val optionIndex = expected.toIntOrNull()
                if (optionIndex != null && optionIndex in 1..q.options.size) {
                    q.options[optionIndex - 1].trim().lowercase() == user ||
                            user == expected.lowercase()
                } else {
                    q.answer.trim().lowercase() == user
                }
            }
            QuestionKind.MULTI -> {
                val expected =
                        q.answer.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
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
                    token.substring(0, idx).trim().lowercase() to
                            token.substring(idx + 1).trim().lowercase()
                }
                .toMap()
    }
}
