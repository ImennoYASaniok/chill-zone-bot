package ChillZoneBot.app.src.main.kotlin.App

import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineButton
import ChillZoneBot.core.src.main.kotlin.InlineClass.InlineClass
import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button
import ChillZoneBot.core.src.main.kotlin.ReplyClass.ReplyClass
import ChillZoneBot.core.src.main.kotlin.TestCore.AnswerType
import ChillZoneBot.core.src.main.kotlin.TestCore.Question
import KinoPart.KinoStates
import KinoPart.searchMoviesDB
import KinoPart.searchSeriesDB
import keyboards.base.getKeyboardMenu
import keyboards.base.getInlineKeyboardMenu


import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId

import io.github.cdimascio.dotenv.dotenv
import java.sql.DriverManager
import java.sql.SQLException

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

            callbackQuery ("UNLIKEitSERIES") {

            }
            callbackQuery("LIKEDitSERIES") {

            }
            callbackQuery ("UNLIKEitMOVIES") {

            }
            callbackQuery("LIKEDitMOVIES") {

            }


            callbackQuery("dislikeITmovies") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                KinoStates.filmIndex++
                if (KinoStates.filmIndex >= KinoStates.foundFilms.size) {
                    bot.sendMessage(chatId, "К сожалению, фильмы, которые я знаю на эту тему, закончились. Возвращаю вас в главное меню.")
                    TODO("Вернуть в главное меню")
                } else {

                    val desc =
                        KinoStates.foundFilms[KinoStates.filmIndex].replace("Описание: null", "Описание: отсутствует")

                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("Нравится", "LIKEDitMOVIES"), InlineButton("Не нравится", "UNLIKEitMOVIES")
                        ), mutableListOf(
                            InlineButton("Далее", "dislikeITmovies")
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

                if (KinoStates.filmIndex== KinoStates.foundFilms.size) {
                    bot.sendMessage(chatId, "К сожалению, фильмы, которые я знаю на эту тему, закончились. Возвращаю вас в главное меню.")
                    TODO("Вернуть в главное меню")
                }
                else {

                    val desc = KinoStates.foundFilms[KinoStates.filmIndex].replace("Описание: null", "Описание: отсутствует")
                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("Нравится", "LIKEDitMOVIES"), InlineButton("Не нравится", "UNLIKEitMOVIES")
                        ), mutableListOf(
                            InlineButton("Далее", "dislikeITmovies")
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
                    bot.sendMessage(chatId, "К сожалению, сериалы, которые я знаю на эту тему, закончились. Возвращаю вас в главное меню.")
                    TODO("Вернуть в главное меню")
                } else {

                    val desc =
                        KinoStates.foundFilms[KinoStates.filmIndex].replace("Описание: null", "Описание: отсутствует")

                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("Нравится", "LIKEDitSERIES"), InlineButton("Не нравится", "UNLIKEitSERIES")
                        ), mutableListOf(
                            InlineButton("Далее", "dislikeITseries")
                        )
                    )
                    inlineMenu.update()
                    bot.sendMessage(chatId, desc, replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                }
            }


            callbackQuery("endChoosingGenresSeries") {
                val chatId = ChatId.fromId(update.callbackQuery?.message?.chat?.id ?: return@callbackQuery)

                bot.sendMessage(chatId, "Вот один из сериалов, которые подходят под ваш выбор:")

                searchSeriesDB()

                if (KinoStates.filmIndex== KinoStates.foundFilms.size) {
                    bot.sendMessage(chatId, "К сожалению, сериалы, которые я знаю на эту тему, закончились. Возвращаю вас в главное меню.")
                    TODO("Вернуть в главное меню")
                }
                else {

                    val desc = KinoStates.foundFilms[KinoStates.filmIndex].replace("Описание: null", "Описание: отсутствует")
                    inlineMenu.keyboard = mutableListOf(
                        mutableListOf(
                            InlineButton("Нравится", "LIKEDitSERIES"), InlineButton("Не нравится", "UNLIKEitSERIES")
                        ), mutableListOf(
                            InlineButton("Далее", "dislikeITseries")
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

                if (text == "Найти фильм для просмотра") {
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
                            InlineButton("для взрослых", "для взрослых"), InlineButton("короткометражка", "короткометражка")
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
                    bot.sendMessage(chatId, "Хороший выбор! Вот список жанров, которые вы можете выбрать для просмотра:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
                }

                else if (text == "Найти сериал для просмотра") {
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
                            InlineButton("для взрослых", "для взрослых"), InlineButton("короткометражка", "короткометражка")
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
                    bot.sendMessage(chatId, "Хороший выбор! Вот список жанров, которые вы можете выбрать для просмотра:", replyMarkup = inlineMenu.getKeyboardInlineMarkup())
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

                                    var insertTestQuery =
                                        "INSERT INTO tests (title, questionsAmount, author) VALUES (?, ?, ?)"
                                    var preparedTestStatement = connection.prepareStatement(insertTestQuery)

                                    preparedTestStatement.setString(1, TestsStates.currentTest.name)
                                    preparedTestStatement.setInt(2, TestsStates.currentTest.questionsAmount)
                                    preparedTestStatement.setString(3, TestsStates.currentTest.author)

                                    var rowsAffected = preparedTestStatement.executeUpdate()
                                    println("Rows affected: $rowsAffected")

                                    for (question in TestsStates.currentTest.questions) {
                                        var insertQuestionsQuery =
                                            "INSERT INTO questions (testId, title, answersAmount) VALUES (?, ?, ?)"
                                        var preparedQuestionsStatement = connection.prepareStatement(insertQuestionsQuery)

                                        preparedQuestionsStatement.setInt(1, lastId+1)
                                        preparedQuestionsStatement.setString(2, question.content)
                                        preparedQuestionsStatement.setInt(3, question.answers.size)

                                        var rows1Affected = preparedQuestionsStatement.executeUpdate()
                                        println("Rows affected: $rows1Affected")



                                        when {
                                            question.typeAnswer == AnswerType.DIGITAL -> {
                                                var insertCAnsQuery =
                                                    "INSERT INTO correctIntAnswers (questionId, ans) VALUES (?, ?)"

                                                var preparedCAnsStatement = connection.prepareStatement(insertCAnsQuery)

                                                preparedCAnsStatement.setInt(1, lastQId+1)
                                                preparedCAnsStatement.setInt(2, question.correctAnswer.toString().toInt())

                                            }
                                            question.typeAnswer == AnswerType.TEXT -> {
                                                var insertCAnsQuery =
                                                    "INSERT INTO correctTextAnswers (questionId, ans) VALUES (?, ?)"

                                                var preparedCAnsStatement = connection.prepareStatement(insertCAnsQuery)

                                                preparedCAnsStatement.setInt(1, lastQId+1)
                                                preparedCAnsStatement.setString(2, question.correctAnswer.toString())

                                            }
                                            question.typeAnswer in listOf(AnswerType.MULTIPLE_OPTIONS, AnswerType.ONE_OPTION) -> {
                                                var insertCAnsQuery =
                                                    "INSERT INTO correctChooseAnswers (questionId, ans1, ans2, ans3, ans4, ans5, ans6) VALUES (?, ?, ?, ?, ?, ?, ?)"

                                                var preparedCAnsStatement = connection.prepareStatement(insertCAnsQuery)

                                                var insertAnswersQuery =
                                                    "INSERT INTO chooseAnswers (questionId, ans1, ans2, ans3, ans4, ans5, ans6) VALUES (?, ?, ?, ?, ?, ?, ?)"
                                                var preparedAnswersStatement = connection.prepareStatement(insertAnswersQuery)


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

                                                var rows2Affected = preparedAnswersStatement.executeUpdate()
                                                println("Rows affected: $rows2Affected")

                                                var rows3Affected = preparedCAnsStatement.executeUpdate()
                                                println("Rows affected: $rows3Affected")

                                            }
                                            question.typeAnswer == AnswerType.COLOR -> {

                                            }
                                        }

                                        question.correctAnswer


//                                        preparedCAnsStatement.setInt(1, lastId+1)
//                                        preparedCAnsStatement.setString(2, question.content)
//                                        preparedCAnsStatement.setInt(3, question.answers.size)



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