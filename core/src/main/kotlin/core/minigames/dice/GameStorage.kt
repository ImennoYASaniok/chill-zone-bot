package core.minigames.dice

import java.io.File
import kotlin.random.Random

data class UserStats(
    val userId: Long,
    var rating: Int = 0,
    var currentStreak: Int = 0,
    var maxStreak: Int = 0
)

/**
 * Мини-игры «дайс»-типа (футбол/баскет/слоты) используют простое файловое хранение.
 * Формат ratings.txt: userId|rating|currentStreak|maxStreak
 */
object GameStorage {
    private val file = File("ratings.txt").apply { if (!exists()) createNewFile() }
    private val users = mutableMapOf<Long, UserStats>()

    init {
        file.readLines().forEach { line ->
            val p = line.split("|")
            if (p.size == 4) {
                val stats = UserStats(
                    userId = p[0].toLong(),
                    rating = p[1].toInt(),
                    currentStreak = p[2].toInt(),
                    maxStreak = p[3].toInt(),
                )
                users[stats.userId] = stats
            }
        }
    }

    private fun save() {
        file.writeText(
            users.values.joinToString("\n") {
                "${it.userId}|${it.rating}|${it.currentStreak}|${it.maxStreak}"
            }
        )
    }

    fun getUser(userId: Long): UserStats = users.getOrPut(userId) { UserStats(userId) }

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
        if (user.currentStreak > user.maxStreak) user.maxStreak = user.currentStreak
        save()
        users[userId]?.rating = user.rating
        return points
    }

    fun lose(userId: Long) {
        val user = getUser(userId)
        user.currentStreak = 0
        save()
    }

    fun getTop10(): List<UserStats> {
        println(users)
        return users.values.sortedByDescending { it.rating }.take(10)
    }
}
