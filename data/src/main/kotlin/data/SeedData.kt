package data

import data.models.*
import data.processes.ImageAvatarProcess
import java.time.LocalDateTime
import java.sql.Timestamp
import java.io.File

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

    // Моки пользователей для тестирования админ панели и других функций
    private val mockUsers = listOf(
        // Обычный пользователь
        UserProfile(
            userId = 1001L,
            username = "testuser1",
            displayName = "Тестовый Пользователь",
            bio = "Люблю фильмы и игры",
            hidden = false,
            showMedia = true,
            rating = 150,
            hideUsername = false,
            isBanned = false,
            createdAt = LocalDateTime.now().minusDays(30),
            isAdmin = false
        ),
        // Админ
        UserProfile(
            userId = 1002L,
            username = "admin_test",
            displayName = "Админ Тест",
            bio = "Администратор бота",
            hidden = false,
            showMedia = true,
            rating = 500,
            hideUsername = false,
            isBanned = false,
            createdAt = LocalDateTime.now().minusDays(60),
            isAdmin = true
        ),
        // Забаненный пользователь
        UserProfile(
            userId = 1003L,
            username = "banned_user",
            displayName = "Забаненный Пользователь",
            bio = "Нарушил правила",
            hidden = false,
            showMedia = true,
            rating = 50,
            hideUsername = false,
            isBanned = true,
            createdAt = LocalDateTime.now().minusDays(90),
            bannedAt = "2024-01-15 10:30",
            reason = "Спам",
            isAdmin = false
        ),
        // Пользователь со скрытым профилем
        UserProfile(
            userId = 1004L,
            username = "hidden_user",
            displayName = "Скрытый Пользователь",
            bio = "Приватный профиль",
            hidden = true,
            showMedia = false,
            rating = 200,
            hideUsername = true,
            isBanned = false,
            createdAt = LocalDateTime.now().minusDays(45),
            isAdmin = false
        ),
        // Пользователь с высоким рейтингом
        UserProfile(
            userId = 1005L,
            username = "top_user",
            displayName = "Топ Пользователь",
            bio = "Активный участник",
            hidden = false,
            showMedia = true,
            rating = 1000,
            hideUsername = false,
            isBanned = false,
            createdAt = LocalDateTime.now().minusDays(120),
            isAdmin = false
        ),
        // Пользователь без username
        UserProfile(
            userId = 1006L,
            username = "",
            displayName = "Пользователь Без Username",
            bio = "Нет юзернейма",
            hidden = false,
            showMedia = true,
            rating = 100,
            hideUsername = false,
            isBanned = false,
            createdAt = LocalDateTime.now().minusDays(20),
            isAdmin = false
        ),
        // Новый пользователь
        UserProfile(
            userId = 1007L,
            username = "newbie",
            displayName = "Новичок",
            bio = "Только зарегистрировался",
            hidden = false,
            showMedia = true,
            rating = 10,
            hideUsername = false,
            isBanned = false,
            createdAt = LocalDateTime.now().minusDays(1),
            isAdmin = false
        )
    )

    fun ensure() {
        // Получение путей к аватаркам мок пользователей
        val mockAvatarPaths = mutableMapOf<Long, String>()
        try {
            val resource = javaClass.classLoader.getResource("mocks/avatars")
            if (resource != null) {
                val resourceUrl = resource.toString()

                // Если ресурс находится внутри JAR (в Docker), пропускаем обработку
                if (!resourceUrl.contains(".jar!")) {
                    var avatarPath = resource.path
                    // Обработка Windows путей (file:/C:/...)
                    if (avatarPath.startsWith("/") && avatarPath.length > 2 && avatarPath[2] == ':') {
                        avatarPath = avatarPath.substring(1)
                    }

                    val avatarDir = File(avatarPath)
                    if (avatarDir.exists() && avatarDir.isDirectory) {
                        println("🖼️ Обработка аватарок в: $avatarPath")
                        ImageAvatarProcess.processAvatarDirectory(avatarPath)

                        // Собираем пути к обработанным аватаркам
                        avatarDir.listFiles()?.forEach { file ->
                            if (file.isFile && file.name.endsWith(".jpg", ignoreCase = true)) {
                                val userId = when (file.name) {
                                    "avatar_1.jpg" -> 1001L
                                    "avatar_2.jpg" -> 1002L
                                    "avatar_3.jpg" -> 1003L
                                    else -> null
                                }
                                if (userId != null) {
                                    mockAvatarPaths[userId] = file.absolutePath
                                    println("✅ Аватарка для пользователя $userId: ${file.name}")
                                }
                            }
                        }
                    } else {
                        println("ℹ️ Директория с мок аватарками не найдена: $avatarPath")
                    }
                } else {
                    println("ℹ️ Ресурсы мок аватарок находятся внутри JAR архива (нормально для production)")
                }
            }
        } catch (e: Exception) {
            println("⚠️ Ошибка при обработке мок аватарок: ${e.message}")
        }
        
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

        // Вставка мок-пользователей для тестирования
        mockUsers.forEach { user ->
            // Проверяем, существует ли пользователь
            val existingUser = Db.single(
                "select user_id from users where user_id = ?",
                bind = { stmt -> stmt.setLong(1, user.userId) },
                map = { it.getLong("user_id") }
            )

            if (existingUser == null) {
                // Используем путь к локальному файлу как avatarFileId для мок пользователей
                val avatarFileId = mockAvatarPaths[user.userId] ?: user.avatarFileId

                // Вставляем пользователя
                Db.execute(
                    """insert into users(user_id, username, display_name, bio, hidden, show_media, rating, hide_username, created_at, avatar_file_id)
                       values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
                ) { stmt ->
                    stmt.setLong(1, user.userId)
                    stmt.setString(2, user.username)
                    stmt.setString(3, user.displayName)
                    stmt.setString(4, user.bio)
                    stmt.setBoolean(5, user.hidden)
                    stmt.setBoolean(6, user.showMedia)
                    stmt.setInt(7, user.rating)
                    stmt.setBoolean(8, user.hideUsername)
                    stmt.setTimestamp(9, user.createdAt?.let { Timestamp.valueOf(it) })
                    stmt.setString(10, avatarFileId)
                }

                // Если пользователь забанен, вставляем запись в banned_users
                if (user.isBanned) {
                    Db.execute(
                        """insert into banned_users(user_id, banned_at, reason) values (?, ?, ?)"""
                    ) { stmt ->
                        stmt.setLong(1, user.userId)
                        stmt.setTimestamp(2, user.bannedAt?.let { Timestamp.valueOf(it.replace(" ", "T")) })
                        stmt.setString(3, user.reason)
                    }
                }
            }
        }
    }
}
