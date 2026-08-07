package data.schemas

import data.Db
import data.models.*

object ProfileSchema {
    fun ensure() {
        Db.raw(createUserTable())
        Db.raw(createBannedUsersTable())
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
                account_type text not null default 'user',
                created_at timestamptz not null default now(),
                updated_at timestamptz not null default now()
            )
            """
    }

    fun createBannedUsersTable(): String {
        return """
            create table if not exists banned_users (
                user_id bigint primary key references users(user_id) on delete cascade,
                banned_at timestamptz not null default now(),
                reason text,
                ban_expires_at timestamptz
            )
            """
    }

    fun getMigrations(): List<String> {
        return listOf(
            "alter table users add column if not exists hide_username boolean not null default false",
            "alter table users add column if not exists account_type text not null default 'user'",
            "alter table banned_users add column if not exists ban_expires_at timestamptz"
        )
    }
}
