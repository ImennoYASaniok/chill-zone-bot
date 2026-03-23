package data

object SeedData {
    private val recommendations = listOf(
        RecommendationItem(1, RecommendationType.FILM, "Интерстеллар", listOf("sci-fi", "drama"), listOf("thoughtful", "epic"), 2014, "Фильм о выборе, времени и дальнем космосе."),
        RecommendationItem(2, RecommendationType.FILM, "1+1", listOf("drama", "comedy"), listOf("warm", "uplifting"), 2011, "История дружбы, которая поднимает настроение."),
        RecommendationItem(3, RecommendationType.SERIES, "Тед Лассо", listOf("comedy", "drama"), listOf("uplifting", "kind"), 2020, "Лёгкий и добрый сериал про команду и характер."),
        RecommendationItem(4, RecommendationType.SERIES, "Очень странные дела", listOf("sci-fi", "mystery"), listOf("tense", "nostalgic"), 2016, "Таинственная история с атмосферой 80-х."),
        RecommendationItem(5, RecommendationType.BOOK, "451° по Фаренгейту", listOf("dystopia", "classic"), listOf("thoughtful", "dark"), 1953, "Классика, которая отлично подходит для вдумчивого вечера."),
        RecommendationItem(6, RecommendationType.BOOK, "Гарри Поттер и философский камень", listOf("fantasy", "adventure"), listOf("cozy", "magical"), 1997, "Погружение в волшебный мир для любого возраста."),
        RecommendationItem(7, RecommendationType.GAME, "Stardew Valley", listOf("sim", "indie"), listOf("calm", "cozy"), 2016, "Спокойная игра для отдыха и маленьких достижений."),
        RecommendationItem(8, RecommendationType.GAME, "Hades", listOf("roguelike", "action"), listOf("energetic", "challenging"), 2020, "Быстрый экшен с сильной атмосферой и прогрессом."),
        RecommendationItem(9, RecommendationType.FILM, "Амели", listOf("romance", "comedy"), listOf("cozy", "light"), 2001, "Тёплая история для хорошего настроения."),
        RecommendationItem(10, RecommendationType.SERIES, "Чернобыль", listOf("drama", "history"), listOf("serious", "intense"), 2019, "Короткий сильный сериал с высокой концентрацией событий.")
    )

    private val defaultsPredictions = listOf(
        "Сегодня у тебя получится то, что долго не складывалось.",
        "Неожиданная мелочь окажется очень полезной.",
        "Один разговор даст ответ, который ты давно искал.",
        "Тебя ждёт маленькая, но приятная победа."
    )

    private val defaultsTests = listOf(
        Triple("Мини-тест: логика", QuestionKind.SINGLE, "Сколько будет 7 + 5? | 10;11;12;13 | 3"),
        Triple("Мини-тест: логика", QuestionKind.SINGLE, "Продолжи последовательность: 2, 4, 8, ? | 10;12;14;16 | 4"),
        Triple("Мини-тест: настроение", QuestionKind.NUMBER, "Сколько минут нужно на короткий перерыв? | 5"),
        Triple("Мини-тест: ассоциации", QuestionKind.MATCH, "Соедини пары | кот=мяу;собака=гав")
    )

    fun ensure() {
        if (Db.single("select id from recommendations limit 1", map = { it.getInt("id") }) == null) {
            recommendations.forEach {
                Db.execute(
                    """insert into recommendations(type, title, genres, moods, year, description) values (?, ?, ?, ?, ?, ?)"""
                ) { stmt ->
                    stmt.setString(1, it.type.name)
                    stmt.setString(2, it.title)
                    stmt.setString(3, it.genres.joinToString("|"))
                    stmt.setString(4, it.moods.joinToString("|"))
                    stmt.setInt(5, it.year)
                    stmt.setString(6, it.description)
                }
            }
        }

        if (Db.single("select id from predictions limit 1", map = { it.getLong("id") }) == null) {
            defaultsPredictions.forEachIndexed { index, text ->
                val rarity = when (index) {
                    0 -> "Обычное"
                    1 -> "Редкое"
                    2 -> "Эпическое"
                    else -> "Легендарное"
                }
                Db.execute(
                    """insert into predictions(text, rarity, author_id) values (?, ?, null)"""
                ) { stmt ->
                    stmt.setString(1, text)
                    stmt.setString(2, rarity)
                }
            }
        }

        if (Db.single("select id from tests limit 1", map = { it.getLong("id") }) == null) {
            defaultsTests.forEach { (title, kind, raw) ->
                val testId = Db.insertId("insert into tests(title, author_id) values (?, null)") { stmt ->
                    stmt.setString(1, title)
                }
                val parts = raw.split("|").map { it.trim() }
                val prompt = parts[0]
                val opts = if (parts.size > 1) parts[1] else ""
                val ans = if (parts.size > 2) parts[2] else ""
                Db.execute(
                    """insert into test_questions(test_id, position, kind, prompt, options_text, answer_text) values (?, ?, ?, ?, ?, ?)"""
                ) { stmt ->
                    stmt.setLong(1, testId)
                    stmt.setInt(2, 1)
                    stmt.setString(3, kind.name)
                    stmt.setString(4, prompt)
                    stmt.setString(5, opts)
                    stmt.setString(6, ans)
                }
            }
        }
    }
}
