package data.schemas

import data.Db

object PredictionsSchema {
    fun ensure() {
        Db.raw(createPredictionsTable())
        Db.raw(createPredictionHistoryTable())
    }

    fun createPredictionsTable(): String {
        return """
            create table if not exists predictions (
                id bigserial primary key,
                text text not null,
                rarity text not null,
                author_id bigint references users(user_id) on delete set null,
                created_at timestamptz not null default now()
            )
            """
    }

    fun createPredictionHistoryTable(): String {
        return """
            create table if not exists prediction_history (
                user_id bigint not null references users(user_id) on delete cascade,
                prediction_id bigint not null references predictions(id) on delete cascade,
                claimed_at timestamptz not null default now(),
                primary key (user_id, prediction_id)
            )
            """
    }
}
