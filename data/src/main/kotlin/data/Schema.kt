package data

object Schema {
    fun ensure() {
        val statements = listOf(
            """
            create table if not exists users (
                user_id bigint primary key,
                username text not null default '',
                display_name text not null default '',
                bio text not null default '',
                hidden boolean not null default false,
                show_media boolean not null default true,
                rating integer not null default 0,
                created_at timestamptz not null default now(),
                updated_at timestamptz not null default now()
            )
            """,
            """
            create table if not exists memes (
                id bigserial primary key,
                file_id text not null,
                uploader_id bigint references users(user_id) on delete set null,
                caption text not null default '',
                created_at timestamptz not null default now()
            )
            """,
            """
            create table if not exists meme_history (
                user_id bigint not null references users(user_id) on delete cascade,
                meme_id bigint not null references memes(id) on delete cascade,
                seen_at timestamptz not null default now(),
                primary key (user_id, meme_id)
            )
            """,
            """
            create table if not exists meme_votes (
                user_id bigint not null references users(user_id) on delete cascade,
                meme_id bigint not null references memes(id) on delete cascade,
                vote smallint not null,
                created_at timestamptz not null default now(),
                primary key (user_id, meme_id)
            )
            """,
            """
            create table if not exists meme_favorites (
                user_id bigint not null references users(user_id) on delete cascade,
                meme_id bigint not null references memes(id) on delete cascade,
                created_at timestamptz not null default now(),
                primary key (user_id, meme_id)
            )
            """,
            """
            create table if not exists predictions (
                id bigserial primary key,
                text text not null,
                rarity text not null,
                author_id bigint references users(user_id) on delete set null,
                created_at timestamptz not null default now()
            )
            """,
            """
            create table if not exists prediction_history (
                user_id bigint not null references users(user_id) on delete cascade,
                prediction_id bigint not null references predictions(id) on delete cascade,
                claimed_at timestamptz not null default now(),
                primary key (user_id, prediction_id)
            )
            """,
            """
            create table if not exists game_stats (
                user_id bigint primary key references users(user_id) on delete cascade,
                rating integer not null default 0,
                wins integer not null default 0,
                losses integer not null default 0,
                streak integer not null default 0,
                best_streak integer not null default 0
            )
            """,
            """
            create table if not exists recommendations (
                id serial primary key,
                type text not null,
                title text not null,
                genres text not null,
                moods text not null,
                year integer not null,
                description text not null
            )
            """,
            """
            create table if not exists tests (
                id bigserial primary key,
                title text not null,
                author_id bigint references users(user_id) on delete set null,
                created_at timestamptz not null default now()
            )
            """,
            """
            create table if not exists test_questions (
                id bigserial primary key,
                test_id bigint not null references tests(id) on delete cascade,
                position integer not null,
                kind text not null,
                prompt text not null,
                options_text text not null default '',
                answer_text text not null default ''
            )
            """,
            """
            create table if not exists test_runs (
                id bigserial primary key,
                user_id bigint not null references users(user_id) on delete cascade,
                test_id bigint not null references tests(id) on delete cascade,
                current_index integer not null default 0,
                correct integer not null default 0,
                finished boolean not null default false,
                started_at timestamptz not null default now(),
                finished_at timestamptz
            )
            """,
            """
            create table if not exists test_answers (
                id bigserial primary key,
                run_id bigint not null references test_runs(id) on delete cascade,
                question_id bigint not null references test_questions(id) on delete cascade,
                answer_text text not null,
                correct boolean not null,
                created_at timestamptz not null default now()
            )
            """,
            """
            create table if not exists event_items (
                id bigserial primary key,
                owner_id bigint not null references users(user_id) on delete cascade,
                title text not null,
                description text not null,
                place text not null,
                starts_at text not null,
                max_people integer not null,
                kind text not null,
                created_at timestamptz not null default now()
            )
            """,
            """
            create table if not exists event_participants (
                event_id bigint not null references event_items(id) on delete cascade,
                user_id bigint not null references users(user_id) on delete cascade,
                joined_at timestamptz not null default now(),
                primary key (event_id, user_id)
            )
            """,
            """
            create table if not exists feedback_items (
                id bigserial primary key,
                user_id bigint references users(user_id) on delete set null,
                username text not null default '',
                scope text not null,
                text text not null,
                created_at timestamptz not null default now()
            )
            """
        )

        statements.forEach(Db::raw)
    }
}
