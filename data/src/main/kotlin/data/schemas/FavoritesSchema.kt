package data.schemas

object FavoritesSchema {
    // Эта схема уже включена в UserSchema как createUserFavoritesTable()
    // Но оставим для совместимости и возможного расширения
    
    fun ensure() {
        // Таблица user_favorites уже создается в UserSchema
        // Но оставим метод для возможного расширения
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
                description text,
                created_at timestamptz not null default now(),
                primary key (user_id, item_id, item_type)
            )
            """
    }

    fun getFavoritesMigrations(): List<String> {
        return listOf(
            "alter table user_favorites add column if not exists url text",
            "alter table user_favorites add column if not exists description text"
        )
    }
}