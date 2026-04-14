package data.schemas

import data.Db
import data.models.*

object MemesSchema {
    fun ensure() {
        Db.raw(createMemesTable())
        Db.raw(createMemeHistoryTable())
        Db.raw(createMemeVotesTable())
        Db.raw(createMemeFavoritesTable())
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
}
