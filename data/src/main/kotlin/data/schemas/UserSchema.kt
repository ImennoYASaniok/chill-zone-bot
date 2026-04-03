package data.schemas

// Импортируем Db из родительского пакета
import data.Db

object UserSchema {
    fun ensure() {
        Db.raw(createUserTable())
        Db.raw(createGameStatsTable())
        Db.raw(createUserFavoritesTable())
    }

    fun createUserTable(): String {
        return """
            create table if not exists users (
                user_id bigint primary key,
                username text not null default '',
                display_name text not null default '',
                bio text not null default '',
                hidden boolean not null default false,
                show_media boolean not null default true,
                rating integer not null default 0,
                hide_username boolean not null default false,
                created_at timestamptz not null default now(),
                updated_at timestamptz not null default now()
            )
            """
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

    fun createUserFavoritesTable(): String {
        return """
            create table if not exists user_favorites (
                user_id bigint not null references users(user_id) on delete cascade,
                item_id integer not null,
                item_type text not null,
                title text not null,
                year integer not null,
                poster_url text,
                source text not null,
                url text,
                created_at timestamptz not null default now(),
                primary key (user_id, item_id, item_type)
            )
            """
    }

    fun getMigrations(): List<String> {
        return listOf(
            "alter table users add column if not exists hide_username boolean not null default false",
            "alter table user_favorites add column if not exists url text"
        )
    }
}