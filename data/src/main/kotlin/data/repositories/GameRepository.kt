package data

class GameRepository {
    fun stats(userId: Long): GameStats {
        return Db.single(
            "select user_id, rating, wins, losses, streak, best_streak from game_stats where user_id = ?",
            bind = { stmt -> stmt.setLong(1, userId) },
            map = { rs ->
                GameStats(
                    userId = rs.getLong("user_id"),
                    rating = rs.getInt("rating"),
                    wins = rs.getInt("wins"),
                    losses = rs.getInt("losses"),
                    streak = rs.getInt("streak"),
                    bestStreak = rs.getInt("best_streak")
                )
            }
        ) ?: GameStats(userId, 0, 0, 0, 0, 0)
    }

    fun win(userId: Long, points: Int): GameStats {
        Db.execute(
            """
            insert into game_stats(user_id, rating, wins, losses, streak, best_streak)
            values (?, ?, 1, 0, 1, 1)
            on conflict (user_id) do update set
                rating = game_stats.rating + excluded.rating,
                wins = game_stats.wins + 1,
                streak = game_stats.streak + 1,
                best_streak = greatest(game_stats.best_streak, game_stats.streak + 1)
            """
        ) { stmt ->
            stmt.setLong(1, userId)
            stmt.setInt(2, points)
        }
        return stats(userId)
    }

    fun lose(userId: Long): GameStats {
        Db.execute(
            """
            insert into game_stats(user_id, rating, wins, losses, streak, best_streak)
            values (?, 0, 0, 1, 0, 0)
            on conflict (user_id) do update set
                losses = game_stats.losses + 1,
                streak = 0
            """
        ) { stmt ->
            stmt.setLong(1, userId)
        }
        return stats(userId)
    }

    fun top(limit: Int = 10): List<GameStats> {
        return Db.query(
            "select user_id, rating, wins, losses, streak, best_streak from game_stats order by rating desc, wins desc limit ?",
            bind = { stmt -> stmt.setInt(1, limit) },
            map = { rs ->
                GameStats(
                    userId = rs.getLong("user_id"),
                    rating = rs.getInt("rating"),
                    wins = rs.getInt("wins"),
                    losses = rs.getInt("losses"),
                    streak = rs.getInt("streak"),
                    bestStreak = rs.getInt("best_streak")
                )
            }
        )
    }
}
