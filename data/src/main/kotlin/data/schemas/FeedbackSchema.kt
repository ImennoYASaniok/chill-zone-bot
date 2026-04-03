package data.schemas

// Импортируем Db из родительского пакета
import data.Db

object FeedbackSchema {
    fun ensure() {
        Db.raw(createFeedbackItemsTable())
    }

    fun createFeedbackItemsTable(): String {
        return """
            create table if not exists feedback_items (
                id bigserial primary key,
                user_id bigint references users(user_id) on delete set null,
                username text not null default '',
                scope text not null,
                text text not null,
                created_at timestamptz not null default now()
            )
            """
    }
}
