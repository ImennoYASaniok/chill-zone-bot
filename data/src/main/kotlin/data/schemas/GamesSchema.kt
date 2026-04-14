package data.schemas

import data.Db
import data.models.*

object GamesSchema {
    fun ensure() {
        Db.raw(createGameStatsTable())
    }

    fun createGameStatsTable(): String {
        return """
            create table if not exists game_stats (
                user_id bigint primary key references users(user_id) on delete cascade,
                rating integer not null default 0,
                wins integer not null default 0,
                losses integer not null default 0,
                streak integer not null default 0,
                best_streak integer not null default 0
            )
            """
    }
}
