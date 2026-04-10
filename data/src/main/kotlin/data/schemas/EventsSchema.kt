package data.schemas

import data.Db

object EventsSchema {
    fun ensure() {
        Db.raw(createEventItemsTable())
        Db.raw(createEventParticipantsTable())
    }

    fun createEventItemsTable(): String {
        return """
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
            """
    }

    fun createEventParticipantsTable(): String {
        return """
            create table if not exists event_participants (
                event_id bigint not null references event_items(id) on delete cascade,
                user_id bigint not null references users(user_id) on delete cascade,
                joined_at timestamptz not null default now(),
                primary key (event_id, user_id)
            )
            """
    }
}
