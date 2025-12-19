package core.handlers.miniGames

import java.io.File
import kotlin.random.Random

// Данные одного пользователя
data class UserStats(
    val userId: Long,
    var rating: Int = 0,
    var currentStreak: Int = 0,
    var maxStreak: Int = 0
)

// Хранилище рейтингов
object GameStorage {

    private val file = File("ratings.txt").apply {
        if (!exists()) createNewFile()
    }

    private val users = mutableMapOf<Long, UserStats>()
    // начальные параметры пользователя добавлять в дб
    init {
        file.readLines().forEach {
            val p = it.split("|")
            if (p.size == 4) {
                val stats = UserStats(
                    userId = p[0].toLong(),
                    rating = p[1].toInt(),
                    currentStreak = p[2].toInt(),
                    maxStreak = p[3].toInt()
                )
                users[stats.userId] = stats
            }
        }
    }
    // функция сохранения в дб если надо сделать
    private fun save() {

        file.writeText(
            users.values.joinToString("\n") {
                "${it.userId}|${it.rating}|${it.currentStreak}|${it.maxStreak}"
            }
        )
    }

    fun getUser(userId: Long): UserStats {
        return users.getOrPut(userId) { UserStats(userId) }
    }

    // Победа — начисляем очки
    fun win(userId: Long): Int {
        val user = getUser(userId)

        val points = when (Random.nextInt(100)) {
            in 0..50 -> 5
            in 51..80 -> 10
            in 81..95 -> 15
            else -> 20
        }

        user.rating += points
        user.currentStreak++
        if (user.currentStreak > user.maxStreak) {
            user.maxStreak = user.currentStreak
        }
        // добавление серии и очков в дб, если больше максимальной серии добавить её тоже
        save()
        return points
    }

    // Проигрыш — сбрасываем серию
    fun lose(userId: Long) {
        val user = getUser(userId)
        user.currentStreak = 0
        save()
        // обнуление текущей серии в дб
    }

    fun getTop10(): List<UserStats> =
        users.values.sortedByDescending { it.rating }.take(10)
}


