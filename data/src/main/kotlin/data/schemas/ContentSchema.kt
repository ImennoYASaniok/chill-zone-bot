package data.schemas

// Импортируем Db из родительского пакета
import data.Db

object ContentSchema {
    fun ensure() {
        Db.raw(createMemesTable())
        Db.raw(createMemeHistoryTable())
        Db.raw(createMemeVotesTable())
        Db.raw(createMemeFavoritesTable())
        Db.raw(createPredictionsTable())
        Db.raw(createPredictionHistoryTable())
        Db.raw(createRecommendationsTable())
    }

    fun createMemesTable(): String {
        return """
            create table if not exists memes (
                id bigserial primary key,
                file_id text not null,
                uploader_id bigint references users(user_id) on delete set null,
                caption text not null default '',
                created_at timestamptz not null default now()
            )
            """
    }

    fun createMemeHistoryTable(): String {
        return """
            create table if not exists meme_history (
                user_id bigint not null references users(user_id) on delete cascade,
                meme_id bigint not null references memes(id) on delete cascade,
                seen_at timestamptz not null default now(),
                primary key (user_id, meme_id)
            )
            """
    }

    fun createMemeVotesTable(): String {
        return """
            create table if not exists meme_votes (
                user_id bigint not null references users(user_id) on delete cascade,
                meme_id bigint not null references memes(id) on delete cascade,
                vote smallint not null,
                created_at timestamptz not null default now(),
                primary key (user_id, meme_id)
            )
            """
    }

    fun createMemeFavoritesTable(): String {
        return """
            create table if not exists meme_favorites (
                user_id bigint not null references users(user_id) on delete cascade,
                meme_id bigint not null references memes(id) on delete cascade,
                created_at timestamptz not null default now(),
                primary key (user_id, meme_id)
            )
            """
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

    fun createRecommendationsTable(): String {
        return """
            create table if not exists recommendations (
                id serial primary key,
                type text not null,
                title text not null,
                genres text not null,
                moods text not null,
                year integer not null,
                description text not null
            )
            """
    }
}