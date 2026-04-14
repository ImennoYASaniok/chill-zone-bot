package data.models

data class EventItem(
    val id: Long,
    val ownerId: Long,
    val title: String,
    val description: String,
    val place: String,
    val startsAt: String,
    val maxPeople: Int,
    val kind: String
)
