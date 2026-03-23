package data

object RecommendationRepository {
    private val items = listOf(
        RecommendationItem(1, RecommendationType.FILM, "Интерстеллар", listOf("sci-fi", "drama"), listOf("thoughtful", "epic"), 2014, "Фильм о выборе, времени и дальнем космосе."),
        RecommendationItem(2, RecommendationType.FILM, "1+1", listOf("drama", "comedy"), listOf("uplifting", "warm"), 2011, "История дружбы и легкого, доброго юмора."),
        RecommendationItem(3, RecommendationType.FILM, "Амели", listOf("romance", "comedy"), listOf("cozy", "light"), 2001, "Тёплая история для спокойного вечера."),
        RecommendationItem(4, RecommendationType.SERIES, "Тед Лассо", listOf("comedy", "drama"), listOf("uplifting", "kind"), 2020, "Добрый сериал о людях, команде и характере."),
        RecommendationItem(5, RecommendationType.SERIES, "Очень странные дела", listOf("mystery", "sci-fi"), listOf("tense", "nostalgic"), 2016, "Атмосферная история с тайной и ретро-настроением."),
        RecommendationItem(6, RecommendationType.BOOK, "451° по Фаренгейту", listOf("classic", "dystopia"), listOf("thoughtful", "dark"), 1953, "Классика про свободу, знание и выбор."),
        RecommendationItem(7, RecommendationType.BOOK, "Гарри Поттер и философский камень", listOf("fantasy", "adventure"), listOf("cozy", "magical"), 1997, "Лёгкое входное окно в волшебный мир."),
        RecommendationItem(8, RecommendationType.GAME, "Stardew Valley", listOf("indie", "sim"), listOf("calm", "cozy"), 2016, "Спокойная игра для отдыха и маленьких побед."),
        RecommendationItem(9, RecommendationType.GAME, "Hades", listOf("action", "roguelike"), listOf("energetic", "challenging"), 2020, "Быстрый экшен с сильной атмосферой и прогрессом."),
        RecommendationItem(10, RecommendationType.FILM, "Достать ножи", listOf("mystery", "comedy"), listOf("smart", "fun"), 2019, "Легкий детектив с юмором и хорошим ритмом.")
    )

    fun all(): List<RecommendationItem> = items

    fun search(type: RecommendationType, query: String): List<RecommendationItem> {
        val q = query.trim().lowercase()
        return items.filter {
            it.type == type &&
                (
                    it.title.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.genres.any { g -> g.contains(q) } ||
                    it.moods.any { m -> m.contains(q) }
                )
        }
    }

    fun random(type: RecommendationType, query: String? = null): RecommendationItem? {
        val list = if (query.isNullOrBlank()) {
            items.filter { it.type == type }
        } else {
            val found = search(type, query)
            if (found.isNotEmpty()) found else items.filter { it.type == type }
        }
        return list.randomOrNull()
    }

    fun summarize(item: RecommendationItem): String {
        return buildString {
            append(item.title).append(" (").append(item.year).append(")").append("\n")
            append("Жанры: ").append(item.genres.joinToString(", ")).append("\n")
            append("Настроение: ").append(item.moods.joinToString(", ")).append("\n\n")
            append(item.description)
        }
    }
}
