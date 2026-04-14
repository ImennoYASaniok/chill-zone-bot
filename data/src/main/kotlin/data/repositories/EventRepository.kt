package data

import data.models.*

class EventRepository {
    fun add(
        ownerId: Long,
        title: String,
        description: String,
        place: String,
        startsAt: String,
        maxPeople: Int,
        kind: String
    ): Long {
        return Db.insertId(
            "insert into event_items(owner_id, title, description, place, starts_at, max_people, kind) values (?, ?, ?, ?, ?, ?, ?)"
        ) { stmt ->
            stmt.setLong(1, ownerId)
            stmt.setString(2, title)
            stmt.setString(3, description)
            stmt.setString(4, place)
            stmt.setString(5, startsAt)
            stmt.setInt(6, maxPeople)
            stmt.setString(7, kind)
        }
    }

    fun upcoming(limit: Int = 10): List<EventItem> {
        return Db.query(
            "select id, owner_id, title, description, place, starts_at, max_people, kind from event_items order by id desc limit ?",
            bind = { stmt -> stmt.setInt(1, limit) },
            map = { rs ->
                EventItem(
                    id = rs.getLong("id"),
                    ownerId = rs.getLong("owner_id"),
                    title = rs.getString("title"),
                    description = rs.getString("description"),
                    place = rs.getString("place"),
                    startsAt = rs.getString("starts_at"),
                    maxPeople = rs.getInt("max_people"),
                    kind = rs.getString("kind")
                )
            }
        )
    }

    fun mine(userId: Long): List<EventItem> {
        return Db.query(
            "select id, owner_id, title, description, place, starts_at, max_people, kind from event_items where owner_id = ? order by id desc",
            bind = { stmt -> stmt.setLong(1, userId) },
            map = { rs ->
                EventItem(
                    id = rs.getLong("id"),
                    ownerId = rs.getLong("owner_id"),
                    title = rs.getString("title"),
                    description = rs.getString("description"),
                    place = rs.getString("place"),
                    startsAt = rs.getString("starts_at"),
                    maxPeople = rs.getInt("max_people"),
                    kind = rs.getString("kind")
                )
            }
        )
    }
}
