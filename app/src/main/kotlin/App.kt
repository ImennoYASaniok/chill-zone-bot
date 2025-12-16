package ChillZoneBot.app.src.main.kotlin.App

import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineClass
import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import ChillZoneBot.core.src.main.kotlin.TestCore.AnswerType
import ChillZoneBot.core.src.main.kotlin.TestCore.Question
import keyboards.base.getKeyboardMenu
import keyboards.base.getInlineKeyboardMenu


import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId

import io.github.cdimascio.dotenv.dotenv

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

            callbackQuery("callback1") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Callback1!")
            }
            callbackQuery("callback2") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Callback2!")
                //update.message.replyMarkup
            }
            callbackQuery("callback3") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)
                bot.sendMessage(chatId, "Callback3!")
            }
            text {
//                "Message" -> {
//                val inlinemarkup = InlineKeyboardMarkup.create(
//                    listOf(
//                        listOf(
//                            InlineKeyboardButton.CallbackData("Bt1", "callback1")
//                        ),
//                        listOf(
//                            InlineKeyboardButton.CallbackData("Bt2", "callback2"),
//                            InlineKeyboardButton.CallbackData("Bt3", "callback3")),
//                    )
//                )
//                bot.sendMessage(chatId, text = "...", replyMarkup = inlinemarkup)
//            }
                val chatId = ChatId.fromId(message.chat.id)
                if (text=="/start") {
                    replyMenu.main(text, bot = bot, chatId = chatId)
                } else if (text == "/start1") {
                    inlineMenu.main(text, bot= bot, chatId = chatId)
                }

                if ((text == "Создать тест") and !TestsStates.creatingTest) {
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
                                // TODO("Отправить в бд")
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
                            }
                        }
                    }
                }
            }
        }
    }

    bot.startPolling()
}