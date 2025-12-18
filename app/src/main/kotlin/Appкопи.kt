package ChillZoneBot.app.src.main.kotlin.App


import BooksPart.BooksStates
import BooksPart.getInfo

import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineButton
import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineClass
import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import ChillZoneBot.core.src.main.kotlin.TestCore.AnswerType
import ChillZoneBot.core.src.main.kotlin.TestCore.Question

import KinoPart.KinoStates
import KinoPart.searchMoviesDB
import KinoPart.searchSeriesDB

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId

import io.github.cdimascio.dotenv.dotenv

import keyboards.base.getInlineKeyboardMenu
import keyboards.base.getKeyboardMenu

import java.sql.DriverManager
import java.sql.SQLException

import kotlin.math.min

fun main() {
    val dotenv = dotenv()
    val bot = bot {
        token = dotenv["BOT_TOKEN"]

        dispatch {
            val replyMenu = ReplyClass(
                keyboard = getKeyboardMenu(),
                startCommand = "/start",
                textMessage = "replyMenu"
            )

            val inlineMenu = InlineClass(
                keyboard = getInlineKeyboardMenu(),
                startCommand = "/start1",
                textMessage = "inlineMenu"
            )

            callbackQuery("ans1_toggle") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                toggleAnswer(chatId, 0, update.callbackQuery?.message?.messageId)
            }

            callbackQuery("ans2_toggle") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                toggleAnswer(chatId, 1, update.callbackQuery?.message?.messageId)
            }

            callbackQuery("ans3_toggle") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                toggleAnswer(chatId, 2, update.callbackQuery?.message?.messageId)
            }

            callbackQuery("ans4_toggle") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                toggleAnswer(chatId, 3, update.callbackQuery?.message?.messageId)
            }

            callbackQuery("ans5_toggle") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                toggleAnswer(chatId, 4, update.callbackQuery?.message?.messageId)
            }

            callbackQuery("ans6_toggle") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                toggleAnswer(chatId, 5, update.callbackQuery?.message?.messageId)
            }

            // Подтверждение выбора ответа
            callbackQuery("confirm_test_answer") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                confirmTestAnswer(chatId)
            }

            // Переход к следующему вопросу
            callbackQuery("next_question") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                goToNextQuestion(chatId)
            }

            // Завершение теста
            callbackQuery("finish_test") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                finishTest(chatId)
            }


            // Динамические callback для прохождения тестов
            // Проверяем callback data вручную
            callbackQuery {
                val callbackData = update.callbackQuery?.data ?: return@callbackQuery

                if (callbackData.startsWith("playIt_")) {
                    val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                    // Извлекаем ID теста из callback data
                    val testId = callbackData.substringAfter("playIt_").toIntOrNull()
                    println(testId)
                    if (testId != null) {
                        startTest(chatId, testId)
                    } else {
                        bot.sendMessage(chatId, "Ошибка: не удалось определить тест")
                    }
                }
            }

            callbackQuery("chooseTest") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                println(chatId)

                val chosenTest = GoingThroughTests.testsId[GoingThroughTests.currentIndex]

                val query = "SELECT id, title, author, questionsAmount FROM tests ORDER BY id;"

                val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
                val databaseUser = dotenv()["DATABASE_USER"]
                val databasePassword = dotenv()["DATABASE_PASSWORD"]
                try {
                    val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)
                    val preparedStatement = connection.prepareStatement(query)
                    preparedStatement.executeQuery().use { resultSet ->
                        GoingThroughTests.testsId.add(resultSet.getInt(1))
                        GoingThroughTests.testsTitles.add(resultSet.getString(2))
                        GoingThroughTests.testsAuthor.add(resultSet.getString(3))
                        GoingThroughTests.testsQAmount.add(resultSet.getInt(4))
                    }
                } catch (e: SQLException) {
                    e.printStackTrace()
                }

                GoingThroughTests.currentQaA = getTestQuestions(chosenTest)

                bot.sendMessage(chatId, "Вопрос №${GoingThroughTests.currentQIndex+1}:\n" +
                        GoingThroughTests.currentQaA[GoingThroughTests.currentQIndex].title
                )

                if (GoingThroughTests.currentQaA[GoingThroughTests.currentQIndex].type=="CHOOSE") {
                    var counter = 0
                    for (i in GoingThroughTests.currentQaA[GoingThroughTests.currentQIndex].chooseAnswers) {
                        if (i.isCorrect) counter++
                    }

                    GoingThroughTests.choosingAns = counter

                    // Создаем клавиатуру с toggle-кнопками
                    inlineMenu.keyboard = mutableListOf()
                    val currentQuestion = GoingThroughTests.currentQaA[GoingThroughTests.currentQIndex]

                    for (i in 0 until currentQuestion.answersAmount) {
                        val answerText = currentQuestion.chooseAnswers[i].text
                        val callbackData = "ans${i+1}_toggle"

                        if (i % 2 == 0) {
                            // Новая строка
                            inlineMenu.keyboard.add(mutableListOf(
                                InlineButton(answerText, callbackData)
                            ))
                        } else {
                            // Добавляем к последней строке
                            inlineMenu.keyboard.last().add(InlineButton(answerText, callbackData))
                        }
                    }

                    // Добавляем кнопку подтверждения
                    inlineMenu.keyboard.add(mutableListOf(
                        InlineButton("✅ Подтвердить выбор", "confirm_test_answer")
                    ))

                    inlineMenu.update()

                    bot.sendMessage(chatId, "Выберите $counter из ${GoingThroughTests.currentQaA[GoingThroughTests.currentQIndex].answersAmount} ответов:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                } else {
                    if (GoingThroughTests.currentQaA[GoingThroughTests.currentQIndex].type == "TEXT") {
                        bot.sendMessage(chatId, "Введите текстовый ответ:")
                        GoingThroughTests.waitingForAnswer = true
                    } else {
                        bot.sendMessage(chatId, "Введите числовой ответ:")
                        GoingThroughTests.waitingForAnswer = true
                    }
                }
            }

            callbackQuery("sledTest") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                bot.sendMessage(chatId, "Вот что мне удалось найти:")
                val maxIndex = min(GoingThroughTests.testsId.size, GoingThroughTests.currentIndex+5)-1
                if (GoingThroughTests.currentIndex >= maxIndex) {
                    bot.sendMessage(chatId, "К сожалению это всё что я сумел найти. Возвращаю вас в главное меню.")
                } else {
                    for (outputIndex in GoingThroughTests.currentIndex..maxIndex) {
                        inlineMenu.keyboard = mutableListOf(mutableListOf(
                            InlineButton("Пройти тест","playIt_${GoingThroughTests.testsId[outputIndex]}")
                        ))
                        inlineMenu.update()

                        bot.sendMessage(
                            chatId,
                            "Название теста: ${GoingThroughTests.testsTitles[outputIndex]}\n" +
                                    "Количество вопросов: ${GoingThroughTests.testsQAmount[outputIndex]}\n" +
                                    "Автор: ${GoingThroughTests.testsAuthor[outputIndex]}",
                            replyMarkup = inlineMenu.getKeyboardInlineMarkup()
                        )
                    }
                    GoingThroughTests.currentIndex = maxIndex + 1

                    if (GoingThroughTests.currentIndex < GoingThroughTests.testsId.size) {
                        inlineMenu.keyboard = mutableListOf(
                            mutableListOf(InlineButton("Далее", "sledTest"))
                        )
                        inlineMenu.update()
                        bot.sendMessage(chatId, "Смотреть еще тесты:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                    }
                }
            }




            text {
                val chatId = ChatId.fromId(message.chat.id)
                if (text=="/start") {
                    replyMenu.main(text, bot = bot, chatId = chatId)
                } else if (text == "/start1") {
                    inlineMenu.main(text, bot= bot, chatId = chatId)
                }

                if (text == "Пройти тест") {
                    bot.sendMessage(chatId, "Хорошая идея, вот список тестов, которые вы можете пройти:")
                    val query = "SELECT id, title, author, questionsAmount FROM tests ORDER BY id;"

                    val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
                    val databaseUser = dotenv()["DATABASE_USER"]
                    val databasePassword = dotenv()["DATABASE_PASSWORD"]
                    try {
                        val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)
                        val preparedStatement = connection.prepareStatement(query)

                        // Очищаем предыдущие данные
                        GoingThroughTests.testsId.clear()
                        GoingThroughTests.testsTitles.clear()
                        GoingThroughTests.testsAuthor.clear()
                        GoingThroughTests.testsQAmount.clear()

                        preparedStatement.executeQuery().use { resultSet ->
                            while (resultSet.next()) {
                                GoingThroughTests.testsId.add(resultSet.getInt("id"))
                                GoingThroughTests.testsTitles.add(resultSet.getString("title"))
                                GoingThroughTests.testsAuthor.add(resultSet.getString("author"))
                                GoingThroughTests.testsQAmount.add(resultSet.getInt("questionsAmount"))
                            }
                        }

                        preparedStatement.close()
                        connection.close()

                    } catch (e: SQLException) {
                        e.printStackTrace()
                    }

                    if (GoingThroughTests.testsId.isEmpty()) {
                        bot.sendMessage(chatId, "К сожалению у меня нет тестов, которые вы могли бы пройти.")
                    } else {
                        GoingThroughTests.currentIndex = 0
                        val maxIndex = min(GoingThroughTests.testsId.size, 5)-1

                        for (outputIndex in 0..maxIndex) {
                            inlineMenu.keyboard = mutableListOf(
                                mutableListOf(
                                    InlineButton("Пройти тест", "playIt_${GoingThroughTests.testsId[outputIndex]}")
                                )
                            )
                            inlineMenu.update()

                            bot.sendMessage(
                                chatId,
                                "Название теста: ${GoingThroughTests.testsTitles[outputIndex]}\n" +
                                        "Количество вопросов: ${GoingThroughTests.testsQAmount[outputIndex]}\n" +
                                        "Автор: ${GoingThroughTests.testsAuthor[outputIndex]}",
                                replyMarkup = inlineMenu.getKeyboardInlineMarkup()
                            )
                        }

                        GoingThroughTests.currentIndex = maxIndex + 1

                        if (GoingThroughTests.currentIndex < GoingThroughTests.testsId.size) {
                            inlineMenu.keyboard = mutableListOf(
                                mutableListOf(InlineButton("Далее", "sledTest"))
                            )
                            inlineMenu.update()
                            bot.sendMessage(chatId, "Смотреть еще тесты:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                        }
                    }

                }

                else if ((text == "Создать тест") and !TestsStates.creatingTest) {
                    TestsStates.creatingTest = true

                    bot.sendMessage(chatId, "Отлично, введите название вашего теста:")
                    TestsStates.waitingForName = true
                }

                else if (TestsStates.waitingForName) {
                    TestsStates.currentTest.name = text

                    bot.sendMessage(chatId, "Хорошо, теперь введите псевдоним, под которым будет опубликован тест:")
                    TestsStates.waitingForName = false
                    TestsStates.waitingForAuthor = true
                }
                else if (TestsStates.waitingForAuthor) {
                    TestsStates.currentTest.author = text

                    bot.sendMessage(chatId, "Хороший псевдоним, теперь укажите количество вопросов в тесте(число от 1 до 20):")
                    TestsStates.waitingForAuthor = false

                    TestsStates.waitingForQuestionsAmount = true
                }
                else if (TestsStates.waitingForQuestionsAmount) {

                    if (text.toIntOrNull() == null) {
                        bot.sendMessage(chatId, "Вы ввели некорректное число, введите заново. Это должно быть число от 1 до 20.")
                    } else {
                        if (text.toInt() !in 1..20) {
                            bot.sendMessage(chatId, "Вы ввели некорректное число, введите заново. Это должно быть число от 1 до 20.")
                        } else {
                            TestsStates.waitingForQuestionsAmount = false
                            TestsStates.waitingForQuestionText = true
                            TestsStates.currentTest.questionsAmount = text.toInt()
                            bot.sendMessage(chatId, "Отлично, теперь введите текст вопроса №${TestsStates.questionNomer}")
                        }
                    }
                }
                else if (TestsStates.waitingForQuestionText and (TestsStates.questionNomer < TestsStates.currentTest.questionsAmount+1)) {
                    TestsStates.currentQuestionText = text
                    replyMenu.keyboard = mutableListOf(mutableListOf(Button("Цифровой"), Button("Текстовый")), mutableListOf(Button("Выбрать вариант"), Button("Соединить цвета")), mutableListOf(Button("Выбрать несколько вариантов")))
                    replyMenu.update()
                    bot.sendMessage(chatId, "Хорошо, теперь выберите тип ответа из следующих вариантов:", replyMarkup = replyMenu.getKeyboardReplyMarkup())
                    TestsStates.waitingForQuestionText = false
                    TestsStates.waitingForQuestionType = true
                }
                else if (TestsStates.waitingForQuestionType) when (text) {
                    "Цифровой" -> {
                        TestsStates.currentAnswerType = AnswerType.DIGITAL
                        TestsStates.waitingForQuestionType = false
                        TestsStates.waitingForAnswersAmount = true
                        bot.sendMessage(chatId, "Отлично, теперь введите количество ответов на вопрос(от 1 до 6):")
                    }
                    "Текстовый" -> {
                        TestsStates.currentAnswerType = AnswerType.TEXT
                        TestsStates.waitingForQuestionType = false
                        TestsStates.waitingForAnswersAmount = true
                        bot.sendMessage(chatId, "Отлично, теперь введите количество ответов на вопрос(от 1 до 6):")
                    }
                    "Выбрать вариант" -> {
                        TestsStates.currentAnswerType = AnswerType.ONE_OPTION
                        TestsStates.waitingForQuestionType = false
                        TestsStates.waitingForAnswersAmount = true
                        bot.sendMessage(chatId, "Отлично, теперь введите количество ответов на вопрос(от 1 до 6):")
                    }
                    "Соединить цвета" -> {
                        TestsStates.currentAnswerType = AnswerType.COLOR
                        TestsStates.waitingForQuestionType = false
                        TestsStates.waitingForAnswersAmount = true
                        bot.sendMessage(chatId, "Отлично, теперь введите количество ответов на вопрос(от 1 до 6):")
                    }
                    "Выбрать несколько вариантов" -> {
                        TestsStates.currentAnswerType = AnswerType.MULTIPLE_OPTIONS
                        TestsStates.waitingForQuestionType = false
                        TestsStates.waitingForAnswersAmount = true
                        bot.sendMessage(chatId, "Отлично, теперь введите количество ответов на вопрос(от 1 до 6):")
                    }
                    else -> {
                        bot.sendMessage(chatId, "Вы ввели некорректный тип ответа. Повторите попытку.")
                    }
                }
                else if (TestsStates.waitingForAnswersAmount) {
                    if (text.toIntOrNull() == null) {
                        bot.sendMessage(chatId, "Вы ввели некорректное количество ответов. Повторите попытку.")
                    } else {
                        if (text.toInt() in 1..6) {
                            TestsStates.currentAnswersAmount = text.toInt()
                            TestsStates.waitingForAnswersAmount = false
                            TestsStates.waitingForAnswers = true
                            bot.sendMessage(chatId, "Хорошо, теперь введите сами ответы. Начните с ответа №1, введите его ниже:")
                        } else {
                            bot.sendMessage(chatId, "Вы ввели некорректное количество ответов. Это должно быть число от 1 до 6:")
                        }

                    }
                }
                else if (TestsStates.waitingForAnswers) {
                    if (TestsStates.answerNomer >= TestsStates.currentAnswersAmount) {
                        TestsStates.waitingForAnswers = false
                        TestsStates.waitingForCorrectAnswers = true
                        bot.sendMessage(chatId, "Замечательно, теперь введите правильный ответ:")
                    } else {
                        bot.sendMessage(chatId, "Хорошо, теперь введите ответ №${TestsStates.answerNomer+1}")
                        if ((TestsStates.answerNomer-1) % 2 == 0) {
                            TestsStates.currentAnswers.add(mutableListOf(Button(text)))
                        } else {
                            TestsStates.currentAnswers.last().add(Button(text))
                        }
                        TestsStates.answerNomer++
                    }
                }
                else if (TestsStates.waitingForCorrectAnswers) {
                    when (TestsStates.currentAnswerType) {
                        AnswerType.COLOR -> {
                            TestsStates.waitingForCorrectAnswers = false
                            TestsStates.questionNomer++
                            TestsStates.waitingForQuestionText = true
                            if (TestsStates.questionNomer < TestsStates.currentTest.questionsAmount+1) bot.sendMessage(chatId, "Отлично, теперь введите текст вопроса №${TestsStates.questionNomer}")

                            TestsStates.answerNomer = 1
                        }
                        AnswerType.TEXT -> {
                            TestsStates.currentCorrectAnswer = text
                            TestsStates.waitingForCorrectAnswers = false
                            TestsStates.questionNomer++
                            TestsStates.waitingForQuestionText = true
                            TestsStates.answerNomer = 1

                            TestsStates.currentTest.questions.add(Question(TestsStates.currentQuestionText, TestsStates.currentAnswerType,
                                TestsStates.currentCorrectAnswer, mutableListOf(TestsStates.currentAnswers)))
                            if (TestsStates.questionNomer < TestsStates.currentTest.questionsAmount+1) bot.sendMessage(chatId, "Отлично, теперь введите текст вопроса №${TestsStates.questionNomer}")
                            else {
                                bot.sendMessage(chatId, "Всё готово! Ваш тест был успешно опубликован!")
                                println("Название теста: ${TestsStates.currentTest.name}")
                                println("Автор: ${TestsStates.currentTest.author}")
                                println("Количество вопросов: ${TestsStates.currentTest.questionsAmount}")
                                for (i in TestsStates.currentTest.questions) {
                                    println(i.content)
                                    println(i.answers)
                                    println(i.correctAnswer)
                                }
                                try {
                                    val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
                                    val databaseUser = dotenv()["DATABASE_USER"]
                                    val databasePassword = dotenv()["DATABASE_PASSWORD"]


                                    val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

                                    // insert into movies table
                                    val getLastIdQuery = "SELECT MAX(id) FROM tests;"

                                    val prepStatement = connection.createStatement()
                                    var lastId = 0
                                    prepStatement.executeQuery(getLastIdQuery).use { resultSet ->
                                        while (resultSet.next()) {
                                            lastId = resultSet.getInt(1)
                                        }
                                    }

                                    val getLastQIdQuery = "SELECT MAX(id) FROM questions;"

                                    val prep1Statement = connection.createStatement()
                                    var lastQId = 0
                                    prep1Statement.executeQuery(getLastQIdQuery).use { resultSet ->
                                        while (resultSet.next()) {
                                            lastQId = resultSet.getInt(1)
                                        }
                                    }

                                    val insertTestQuery =
                                        "INSERT INTO tests (title, questionsAmount, author) VALUES (?, ?, ?)"
                                    val preparedTestStatement = connection.prepareStatement(insertTestQuery)

                                    preparedTestStatement.setString(1, TestsStates.currentTest.name)
                                    preparedTestStatement.setInt(2, TestsStates.currentTest.questionsAmount)
                                    preparedTestStatement.setString(3, TestsStates.currentTest.author)

                                    val rowsAffected = preparedTestStatement.executeUpdate()
                                    println("Rows affected: $rowsAffected")

                                    for (question in TestsStates.currentTest.questions) {
                                        val insertQuestionsQuery =
                                            "INSERT INTO questions (testId, title, answersAmount) VALUES (?, ?, ?)"
                                        val preparedQuestionsStatement = connection.prepareStatement(insertQuestionsQuery)

                                        preparedQuestionsStatement.setInt(1, lastId+1)
                                        preparedQuestionsStatement.setString(2, question.content)
                                        preparedQuestionsStatement.setInt(3, question.answers.size)

                                        val rows1Affected = preparedQuestionsStatement.executeUpdate()
                                        println("Rows affected: $rows1Affected")
                                        val insertCAnsQuery =
                                            "INSERT INTO correctTextAnswers (questionId, ans) VALUES (?, ?)"

                                        val preparedCAnsStatement = connection.prepareStatement(insertCAnsQuery)

                                        preparedCAnsStatement.setInt(1, lastQId+1)
                                        preparedCAnsStatement.setString(2, question.correctAnswer.toString())
                                        lastQId++
                                    }
                                } catch (e: SQLException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        AnswerType.ONE_OPTION -> {
                            TestsStates.currentCorrectAnswer = text
                            TestsStates.waitingForCorrectAnswers = false
                            TestsStates.questionNomer++
                            TestsStates.waitingForQuestionText = true
                            TestsStates.currentTest.questions.add(Question(TestsStates.currentQuestionText, TestsStates.currentAnswerType,
                                TestsStates.currentCorrectAnswer, mutableListOf(TestsStates.currentAnswers)))
                            TestsStates.answerNomer = 1
                            if (TestsStates.questionNomer < TestsStates.currentTest.questionsAmount+1) bot.sendMessage(chatId, "Отлично, теперь введите текст вопроса №${TestsStates.questionNomer}")
                            else {
                                bot.sendMessage(chatId, "Всё готово! Ваш тест был успешно опубликован!")
                                println("Название теста: ${TestsStates.currentTest.name}")
                                println("Автор: ${TestsStates.currentTest.author}")
                                println("Количество вопросов: ${TestsStates.currentTest.questionsAmount}")
                                for (i in TestsStates.currentTest.questions) {
                                    println(i.content)
                                    println(i.answers)
                                    println(i.correctAnswer)
                                }
                                try {
                                    val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
                                    val databaseUser = dotenv()["DATABASE_USER"]
                                    val databasePassword = dotenv()["DATABASE_PASSWORD"]


                                    val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

                                    // insert into movies table
                                    val getLastIdQuery = "SELECT MAX(id) FROM tests;"

                                    val prepStatement = connection.createStatement()
                                    var lastId = 0
                                    prepStatement.executeQuery(getLastIdQuery).use { resultSet ->
                                        while (resultSet.next()) {
                                            lastId = resultSet.getInt(1)
                                        }
                                    }

                                    val getLastQIdQuery = "SELECT MAX(id) FROM questions;"

                                    val prep1Statement = connection.createStatement()
                                    var lastQId = 0
                                    prep1Statement.executeQuery(getLastQIdQuery).use { resultSet ->
                                        while (resultSet.next()) {
                                            lastQId = resultSet.getInt(1)
                                        }
                                    }

                                    val insertTestQuery =
                                        "INSERT INTO tests (title, questionsAmount, author) VALUES (?, ?, ?)"
                                    val preparedTestStatement = connection.prepareStatement(insertTestQuery)

                                    preparedTestStatement.setString(1, TestsStates.currentTest.name)
                                    preparedTestStatement.setInt(2, TestsStates.currentTest.questionsAmount)
                                    preparedTestStatement.setString(3, TestsStates.currentTest.author)

                                    val rowsAffected = preparedTestStatement.executeUpdate()
                                    println("Rows affected: $rowsAffected")

                                    for (question in TestsStates.currentTest.questions) {
                                        val insertQuestionsQuery =
                                            "INSERT INTO questions (testId, title, answersAmount) VALUES (?, ?, ?)"
                                        val preparedQuestionsStatement = connection.prepareStatement(insertQuestionsQuery)

                                        preparedQuestionsStatement.setInt(1, lastId+1)
                                        preparedQuestionsStatement.setString(2, question.content)
                                        preparedQuestionsStatement.setInt(3, question.answers.size)

                                        val rows1Affected = preparedQuestionsStatement.executeUpdate()
                                        println("Rows affected: $rows1Affected")
                                        val insertCAnsQuery =
                                            "INSERT INTO correctChooseAnswers (questionId, ans1, ans2, ans3, ans4, ans5, ans6) VALUES (?, ?, ?, ?, ?, ?, ?)"

                                        val preparedCAnsStatement = connection.prepareStatement(insertCAnsQuery)

                                        val insertAnswersQuery =
                                            "INSERT INTO chooseAnswers (questionId, ans1, ans2, ans3, ans4, ans5, ans6) VALUES (?, ?, ?, ?, ?, ?, ?)"
                                        val preparedAnswersStatement = connection.prepareStatement(insertAnswersQuery)


                                        preparedCAnsStatement.setInt(1, lastQId+1)
                                        preparedAnswersStatement.setInt(1, lastQId+1)

                                        for (index in 0..<question.answers.size) {
                                            preparedCAnsStatement.setBoolean(
                                                2+index,
                                                question.answers[index].toString() == question.correctAnswer
                                            )
                                            preparedAnswersStatement.setString(
                                                2+index,
                                                question.answers[index].toString()
                                            )
                                        }

                                        for (index in question.answers.size..5) {
                                            preparedCAnsStatement.setNull(
                                                2+index,
                                                java.sql.Types.BOOLEAN
                                            )
                                            preparedAnswersStatement.setNull(
                                                2+index,
                                                java.sql.Types.VARCHAR
                                            )
                                        }

                                        val rows2Affected = preparedAnswersStatement.executeUpdate()
                                        println("Rows affected: $rows2Affected")

                                        val rows3Affected = preparedCAnsStatement.executeUpdate()
                                        println("Rows affected: $rows3Affected")
                                        lastQId++
                                    }
                                } catch (e: SQLException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        AnswerType.MULTIPLE_OPTIONS -> {
                            val preparedAns = text.split(",").filter { it.isNotBlank() }
                            preparedAns.forEach { it.replace(" ", "") }

                            if (preparedAns.size != TestsStates.currentMaxChooseOptions) {
                                bot.sendMessage(chatId, "Ошибка. Вы ввели неправильное количество нужных ответов. Повторите попытку.")
                            } else {
                                TestsStates.currentCorrectAnswer = preparedAns
                                TestsStates.waitingForCorrectAnswers = false
                                TestsStates.questionNomer++
                                TestsStates.currentTest.questions.add(Question(TestsStates.currentQuestionText, TestsStates.currentAnswerType,
                                    TestsStates.currentCorrectAnswer, mutableListOf(TestsStates.currentAnswers)))
                                TestsStates.answerNomer = 1
                                if (TestsStates.questionNomer < TestsStates.currentTest.questionsAmount+1) bot.sendMessage(chatId, "Отлично, теперь введите текст вопроса №${TestsStates.questionNomer}")
                                else {
                                    bot.sendMessage(chatId, "Всё готово! Ваш тест был успешно опубликован!")
                                    println("Название теста: ${TestsStates.currentTest.name}")
                                    println("Автор: ${TestsStates.currentTest.author}")
                                    println("Количество вопросов: ${TestsStates.currentTest.questionsAmount}")
                                    for (i in TestsStates.currentTest.questions) {
                                        println(i.content)
                                        println(i.answers)
                                        println(i.correctAnswer)
                                    }
                                    try {
                                        val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
                                        val databaseUser = dotenv()["DATABASE_USER"]
                                        val databasePassword = dotenv()["DATABASE_PASSWORD"]


                                        val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

                                        // insert into movies table
                                        val getLastIdQuery = "SELECT MAX(id) FROM tests;"

                                        val prepStatement = connection.createStatement()
                                        var lastId = 0
                                        prepStatement.executeQuery(getLastIdQuery).use { resultSet ->
                                            while (resultSet.next()) {
                                                lastId = resultSet.getInt(1)
                                            }
                                        }

                                        val getLastQIdQuery = "SELECT MAX(id) FROM questions;"

                                        val prep1Statement = connection.createStatement()
                                        var lastQId = 0
                                        prep1Statement.executeQuery(getLastQIdQuery).use { resultSet ->
                                            while (resultSet.next()) {
                                                lastQId = resultSet.getInt(1)
                                            }
                                        }

                                        val insertTestQuery =
                                            "INSERT INTO tests (title, questionsAmount, author) VALUES (?, ?, ?)"
                                        val preparedTestStatement = connection.prepareStatement(insertTestQuery)

                                        preparedTestStatement.setString(1, TestsStates.currentTest.name)
                                        preparedTestStatement.setInt(2, TestsStates.currentTest.questionsAmount)
                                        preparedTestStatement.setString(3, TestsStates.currentTest.author)

                                        val rowsAffected = preparedTestStatement.executeUpdate()
                                        println("Rows affected: $rowsAffected")

                                        for (question in TestsStates.currentTest.questions) {
                                            val insertQuestionsQuery =
                                                "INSERT INTO questions (testId, title, answersAmount) VALUES (?, ?, ?)"
                                            val preparedQuestionsStatement = connection.prepareStatement(insertQuestionsQuery)

                                            preparedQuestionsStatement.setInt(1, lastId+1)
                                            preparedQuestionsStatement.setString(2, question.content)
                                            preparedQuestionsStatement.setInt(3, question.answers.size)

                                            val rows1Affected = preparedQuestionsStatement.executeUpdate()
                                            println("Rows affected: $rows1Affected")

                                            val insertCAnsQuery =
                                                "INSERT INTO correctChooseAnswers (questionId, ans1, ans2, ans3, ans4, ans5, ans6) VALUES (?, ?, ?, ?, ?, ?, ?)"

                                            val preparedCAnsStatement = connection.prepareStatement(insertCAnsQuery)

                                            val insertAnswersQuery =
                                                "INSERT INTO chooseAnswers (questionId, ans1, ans2, ans3, ans4, ans5, ans6) VALUES (?, ?, ?, ?, ?, ?, ?)"
                                            val preparedAnswersStatement = connection.prepareStatement(insertAnswersQuery)


                                            preparedCAnsStatement.setInt(1, lastQId+1)
                                            preparedAnswersStatement.setInt(1, lastQId+1)

                                            for (index in 0..<question.answers.size) {
                                                preparedCAnsStatement.setBoolean(
                                                    2+index,
                                                    question.answers[index].toString() == question.correctAnswer
                                                )
                                                preparedAnswersStatement.setString(
                                                    2+index,
                                                    question.answers[index].toString()
                                                )
                                            }

                                            for (index in question.answers.size..5) {
                                                preparedCAnsStatement.setNull(
                                                    2+index,
                                                    java.sql.Types.BOOLEAN
                                                )
                                                preparedAnswersStatement.setNull(
                                                    2+index,
                                                    java.sql.Types.VARCHAR
                                                )
                                            }

                                            val rows2Affected = preparedAnswersStatement.executeUpdate()
                                            println("Rows affected: $rows2Affected")

                                            val rows3Affected = preparedCAnsStatement.executeUpdate()
                                            println("Rows affected: $rows3Affected")
                                            lastQId++
                                        }
                                    } catch (e: SQLException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        AnswerType.DIGITAL -> {
                            if (text.toIntOrNull() == null) {
                                bot.sendMessage(chatId, "Повторите попытку. Введенное число должно быть целым без всяких знаков")
                            } else {
                                TestsStates.currentCorrectAnswer = text.toInt()
                                TestsStates.questionNomer++
                                TestsStates.waitingForCorrectAnswers = false
                                TestsStates.currentTest.questions.add(Question(TestsStates.currentQuestionText, TestsStates.currentAnswerType,
                                    TestsStates.currentCorrectAnswer, mutableListOf(TestsStates.currentAnswers)))
                                TestsStates.waitingForQuestionText = true
                                TestsStates.answerNomer = 1
                                println(TestsStates.questionNomer)
                                println(TestsStates.currentTest.questionsAmount)
                                if (TestsStates.questionNomer < TestsStates.currentTest.questionsAmount+1) bot.sendMessage(chatId, "Отлично, теперь введите текст вопроса №${TestsStates.questionNomer}")
                                else {
                                    bot.sendMessage(chatId, "Всё готово! Ваш тест был успешно опубликован!")
                                    println("Название теста: ${TestsStates.currentTest.name}")
                                    println("Автор: ${TestsStates.currentTest.author}")
                                    println("Количество вопросов: ${TestsStates.currentTest.questionsAmount}")
                                    for (i in TestsStates.currentTest.questions) {
                                        println(i.content)
                                        println(i.answers)
                                        println(i.correctAnswer)
                                    }
                                    try {
                                        val databaseUrl = dotenv()["DATABASE_URL"] // "jdbc:postgresql://localhost:5432/dbname"
                                        val databaseUser = dotenv()["DATABASE_USER"]
                                        val databasePassword = dotenv()["DATABASE_PASSWORD"]


                                        val connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)

                                        // insert into movies table
                                        val getLastIdQuery = "SELECT MAX(id) FROM tests;"

                                        val prepStatement = connection.createStatement()
                                        var lastId = 0
                                        prepStatement.executeQuery(getLastIdQuery).use { resultSet ->
                                            while (resultSet.next()) {
                                                lastId = resultSet.getInt(1)
                                            }
                                        }

                                        val getLastQIdQuery = "SELECT MAX(id) FROM questions;"

                                        val prep1Statement = connection.createStatement()
                                        var lastQId = 0
                                        prep1Statement.executeQuery(getLastQIdQuery).use { resultSet ->
                                            while (resultSet.next()) {
                                                lastQId = resultSet.getInt(1)
                                            }
                                        }

                                        val insertTestQuery =
                                            "INSERT INTO tests (title, questionsAmount, author) VALUES (?, ?, ?)"
                                        val preparedTestStatement = connection.prepareStatement(insertTestQuery)

                                        preparedTestStatement.setString(1, TestsStates.currentTest.name)
                                        preparedTestStatement.setInt(2, TestsStates.currentTest.questionsAmount)
                                        preparedTestStatement.setString(3, TestsStates.currentTest.author)

                                        val rowsAffected = preparedTestStatement.executeUpdate()
                                        println("Rows affected: $rowsAffected")

                                        for (question in TestsStates.currentTest.questions) {
                                            val insertQuestionsQuery =
                                                "INSERT INTO questions (testId, title, answersAmount) VALUES (?, ?, ?)"
                                            val preparedQuestionsStatement = connection.prepareStatement(insertQuestionsQuery)

                                            preparedQuestionsStatement.setInt(1, lastId+1)
                                            preparedQuestionsStatement.setString(2, question.content)
                                            preparedQuestionsStatement.setInt(3, question.answers.size)

                                            val rows1Affected = preparedQuestionsStatement.executeUpdate()
                                            println("Rows affected: $rows1Affected")
                                            val insertCAnsQuery =
                                                "INSERT INTO correctIntAnswers (questionId, ans) VALUES (?, ?)"

                                            val preparedCAnsStatement = connection.prepareStatement(insertCAnsQuery)

                                            preparedCAnsStatement.setInt(1, lastQId+1)
                                            preparedCAnsStatement.setInt(2, question.correctAnswer.toString().toInt())
                                            lastQId++
                                        }
                                    } catch (e: SQLException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
                else if (GoingThroughTests.waitingForAnswer) {
                    handleTextAnswer(chatId, text)
                }
            }
        }
    }

    bot.startPolling()
}

fun startTest(chatId: ChatId, testId: Int) {
    // Получаем вопросы теста
    GoingThroughTests.currentQaA = getTestQuestions(testId)

    if (GoingThroughTests.currentQaA.isEmpty()) {
        bot.sendMessage(chatId, "Ошибка: вопросы теста не найдены")
        return
    }

    // Сбрасываем состояние
    GoingThroughTests.currentQIndex = 0
    GoingThroughTests.waitingForAnswer = false
    GoingThroughTests.selectedAnswers.clear()
    GoingThroughTests.userAnswers.clear()
    GoingThroughTests.correctAnswersCount = 0

    // Отправляем первый вопрос
    sendTestQuestion(chatId, GoingThroughTests.currentQIndex)
}

// Функция для отправки вопроса теста
fun sendTestQuestion(chatId: ChatId, questionIndex: Int) {
    if (questionIndex >= GoingThroughTests.currentQaA.size) {
        finishTest(chatId)
        return
    }

    val question = GoingThroughTests.currentQaA[questionIndex]
    val questionNumber = questionIndex + 1
    val totalQuestions = GoingThroughTests.currentQaA.size

    println(question.type)

    when (question.type) {
        "CHOOSE" -> {
            // Считаем количество правильных ответов для этого вопроса
            val correctCount = question.chooseAnswers.count { it.isCorrect }
            GoingThroughTests.choosingAns = correctCount

            // Создаем клавиатуру с toggle-кнопками
            val keyboard = mutableListOf<MutableList<InlineButton>>()

            for (i in 0 until question.answersAmount) {
                val answerText = question.chooseAnswers.getOrNull(i)?.text ?: "Вариант ${i+1}"
                val callbackData = "ans${i+1}_toggle"

                // Проверяем, выбран ли уже этот ответ
                val isSelected = GoingThroughTests.selectedAnswers.contains(i)
                val buttonText = if (isSelected) "✅ $answerText" else answerText

                if (i % 2 == 0) {
                    // Новая строка
                    keyboard.add(mutableListOf(InlineButton(buttonText, callbackData)))
                } else {
                    // Добавляем к последней строке
                    keyboard.last().add(InlineButton(buttonText, callbackData))
                }
            }

            // Кнопка подтверждения выбора
            keyboard.add(mutableListOf(InlineButton("✅ Подтвердить выбор", "confirm_test_answer")))

            bot.sendMessage(
                chatId,
                "📝 Вопрос $questionNumber/$totalQuestions:\n\n${question.title}\n\n" +
                        "Выберите $correctCount из ${question.answersAmount} ответов:",
                replyMarkup = InlineClass(keyboard, "", "").getKeyboardInlineMarkup()
            )
        }

        "INT", "TEXT" -> {
            println("TEXT")
            GoingThroughTests.waitingForAnswer = true
            val answerType = if (question.type == "TEXT") "текстовый" else "числовой"

            bot.sendMessage(
                chatId,
                "📝 Вопрос $questionNumber/$totalQuestions:\n\n${question.title}\n\n" +
                        "Введите $answerType ответ:"
            )
        }

        else -> {
            bot.sendMessage(chatId, "Неизвестный тип вопроса")
        }
    }
}

// Функция для переключения выбора ответа (toggle)
fun toggleAnswer(chatId: ChatId, answerIndex: Int, messageId: Long?) {
    val question = GoingThroughTests.currentQaA[GoingThroughTests.currentQIndex]

    if (answerIndex < question.answersAmount) {
        if (GoingThroughTests.selectedAnswers.contains(answerIndex)) {
            // Убираем выбор
            GoingThroughTests.selectedAnswers.remove(answerIndex)
        } else {
            // Добавляем выбор, но проверяем лимит
            val correctCount = question.chooseAnswers.count { it.isCorrect }
            if (GoingThroughTests.selectedAnswers.size < correctCount) {
                GoingThroughTests.selectedAnswers.add(answerIndex)
            } else {
                // Можно показать сообщение о превышении лимита
                return
            }
        }

        // Обновляем клавиатуру
        updateAnswerKeyboard(chatId, messageId, question)
    }
}

// Функция для обновления клавиатуры с ответами
fun updateAnswerKeyboard(chatId: ChatId, messageId: Long?, question: QuestionWithAnswers) {
    val keyboard = mutableListOf<MutableList<InlineButton>>()

    for (i in 0 until question.answersAmount) {
        val answerText = question.chooseAnswers.getOrNull(i)?.text ?: "Вариант ${i+1}"
        val callbackData = "ans${i+1}_toggle"

        val isSelected = GoingThroughTests.selectedAnswers.contains(i)
        val buttonText = if (isSelected) "✅ $answerText" else answerText

        if (i % 2 == 0) {
            keyboard.add(mutableListOf(InlineButton(buttonText, callbackData)))
        } else {
            keyboard.last().add(InlineButton(buttonText, callbackData))
        }
    }

    // Кнопка подтверждения
    val confirmButtonText = if (GoingThroughTests.selectedAnswers.size == GoingThroughTests.choosingAns) {
        "✅ Подтвердить выбор (${GoingThroughTests.selectedAnswers.size}/$GoingThroughTests.choosingAns)"
    } else {
        "Подтвердить выбор (${GoingThroughTests.selectedAnswers.size}/$GoingThroughTests.choosingAns)"
    }

    keyboard.add(mutableListOf(InlineButton(confirmButtonText, "confirm_test_answer")))

    // Обновляем сообщение с клавиатурой
    if (messageId != null) {
        bot.editMessageReplyMarkup(
            chatId = chatId,
            messageId = messageId,
            replyMarkup = InlineClass(keyboard, "", "").getKeyboardInlineMarkup()
        )
    }
}

// Функция для подтверждения выбора ответа
fun confirmTestAnswer(chatId: ChatId) {
    val question = GoingThroughTests.currentQaA[GoingThroughTests.currentQIndex]
    val correctCount = question.chooseAnswers.count { it.isCorrect }

    // Проверяем, что выбрано правильное количество ответов
    if (GoingThroughTests.selectedAnswers.size != correctCount) {
        bot.sendMessage(
            chatId,
            "⚠️ Нужно выбрать ровно $correctCount ответов. Вы выбрали ${GoingThroughTests.selectedAnswers.size}."
        )
        return
    }

    // Проверяем правильность выбора
    var isCorrect = true
    for (i in 0 until question.answersAmount) {
        val shouldBeSelected = question.chooseAnswers[i].isCorrect
        val isSelected = GoingThroughTests.selectedAnswers.contains(i)

        if (shouldBeSelected != isSelected) {
            isCorrect = false
            break
        }
    }

    // Сохраняем ответ пользователя
    GoingThroughTests.userAnswers.add(
        UserAnswer(
            questionId = question.id,
            selectedIndices = GoingThroughTests.selectedAnswers.toList(),
            isCorrect = isCorrect
        )
    )

    if (isCorrect) {
        GoingThroughTests.correctAnswersCount++
    }

    // Переходим к следующему вопросу
    GoingThroughTests.currentQIndex++
    GoingThroughTests.selectedAnswers.clear()

    if (GoingThroughTests.currentQIndex < GoingThroughTests.currentQaA.size) {
        sendTestQuestion(chatId, GoingThroughTests.currentQIndex)
    } else {
        finishTest(chatId)
    }
}

// Функция для обработки текстовых ответов
fun handleTextAnswer(chatId: ChatId, answerText: String) {
    if (GoingThroughTests.currentQIndex >= GoingThroughTests.currentQaA.size) {
        bot.sendMessage(chatId, "Ошибка: нет текущего вопроса")
        GoingThroughTests.waitingForAnswer = false
        return
    }

    val question = GoingThroughTests.currentQaA[GoingThroughTests.currentQIndex]
    var isCorrect = false

    when (question.type) {
        "INT" -> {
            // Обработка числового ответа
            val userAnswer = answerText.trim().toIntOrNull()

            if (userAnswer != null) {
                // Получаем правильные числовые ответы
                val intAnswers = question.answers
                if (intAnswers is List<*>) {
                    // Проверяем, есть ли среди правильных ответов введенное число
                    val correctAnswers = intAnswers.filterIsInstance<IntAnswer>()
                    isCorrect = correctAnswers.any { it.value == userAnswer }

                    // Альтернативно, если answers хранятся как List<Any>
                    val correctNumericAnswers = mutableListOf<Int>()
                    for (answer in intAnswers) {
                        when (answer) {
                            is IntAnswer -> correctNumericAnswers.add(answer.value)
                            is Int -> correctNumericAnswers.add(answer)
                            is String -> answer.toIntOrNull()?.let { correctNumericAnswers.add(it) }
                        }
                    }
                    isCorrect = correctNumericAnswers.contains(userAnswer)
                }
            } else {
                bot.sendMessage(chatId, "Пожалуйста, введите целое число.")
                return
            }
        }

        "TEXT" -> {
            // Обработка текстового ответа
            val userAnswer = answerText.trim().lowercase()

            // Получаем правильные текстовые ответы
            val textAnswers = question.answers
            if (textAnswers is List<*>) {
                // Проверяем, есть ли среди правильных ответов введенный текст
                val correctAnswers = textAnswers.filterIsInstance<TextAnswer>()
                isCorrect = correctAnswers.any {
                    it.value.trim().lowercase() == userAnswer
                }

                // Альтернативно, если answers хранятся как List<Any>
                val correctTextResponses = mutableListOf<String>()
                for (answer in textAnswers) {
                    when (answer) {
                        is TextAnswer -> correctTextResponses.add(answer.value.trim().lowercase())
                        is String -> correctTextResponses.add(answer.trim().lowercase())
                    }
                }
                isCorrect = correctTextResponses.contains(userAnswer)
            }
        }

        else -> {
            bot.sendMessage(chatId, "Неизвестный тип вопроса")
            GoingThroughTests.waitingForAnswer = false
            return
        }
    }

    // Сохраняем ответ пользователя
    GoingThroughTests.userAnswers.add(
        UserAnswer(
            questionId = question.id,
            answerText = answerText,
            isCorrect = isCorrect
        )
    )

    // Увеличиваем счетчик правильных ответов
    if (isCorrect) {
        GoingThroughTests.correctAnswersCount++
        bot.sendMessage(chatId, "✅ Правильно!")
    } else {
        // Показываем правильный ответ
        val correctAnswer = when (question.type) {
            "INT" -> {
                val intAnswers = question.answers.filterIsInstance<IntAnswer>()
                intAnswers.joinToString(", ") { it.value.toString() }
            }
            "TEXT" -> {
                val textAnswers = question.answers.filterIsInstance<TextAnswer>()
                textAnswers.joinToString(", ") { it.value }
            }
            else -> "Неизвестно"
        }
        bot.sendMessage(chatId, "❌ Неправильно. Правильный ответ: $correctAnswer")
    }

    // Сбрасываем флаг ожидания ответа
    GoingThroughTests.waitingForAnswer = false

    // Переходим к следующему вопросу
    GoingThroughTests.currentQIndex++

    if (GoingThroughTests.currentQIndex < GoingThroughTests.currentQaA.size) {
        // Даем небольшую паузу перед следующим вопросом
        Thread.sleep(1000)
        sendTestQuestion(chatId, GoingThroughTests.currentQIndex)
    } else {
        // Тест завершен
        finishTest(chatId)
    }
}

// Функция для завершения теста
fun finishTest(chatId: ChatId) {
    val totalQuestions = GoingThroughTests.currentQaA.size
    val correctAnswers = GoingThroughTests.correctAnswersCount
    val percentage = if (totalQuestions > 0) {
        (correctAnswers.toDouble() / totalQuestions.toDouble() * 100).toInt()
    } else {
        0
    }

    val resultMessage = """
        🎉 Тест завершен!
        
        📊 Результаты:
        Правильных ответов: $correctAnswers из $totalQuestions
        Процент правильных: $percentage%
        
        ${getResultMessage(percentage)}
        
        Спасибо за прохождение теста! 🎓
    """.trimIndent()

    bot.sendMessage(chatId, resultMessage)

    // Сбрасываем состояние
    GoingThroughTests.reset()
}

// Функция для получения сообщения о результате
fun getResultMessage(percentage: Int): String {
    return when {
        percentage >= 90 -> "🏆 Отличный результат! Вы настоящий эксперт!"
        percentage >= 70 -> "👍 Хороший результат! Вы хорошо разбираетесь в теме."
        percentage >= 50 -> "💪 Неплохо! Есть что повторить, но в целом неплохо."
        else -> "📚 Есть над чем поработать! Рекомендую изучить материал еще раз."
    }
}

// Функция для перехода к следующему вопросу (если нужно)
fun goToNextQuestion(chatId: ChatId) {
    GoingThroughTests.currentQIndex++
    sendTestQuestion(chatId, GoingThroughTests.currentQIndex)
}

// Класс для хранения ответов пользователя
data class UserAnswer(
    val questionId: Int,
    val selectedIndices: List<Int> = emptyList(),
    val answerText: String = "",
    val isCorrect: Boolean = false
)

lateinit var bot: com.github.kotlintelegrambot.Bot