package Models.FilmsModel
import kotlinx.serialization.Serializable

@Serializable
data class MovieListResponse(
    val docs: List<Movie>,
    val total: Int,
    val limit: Int,
    val page: Int,
    val pages: Int
)

// ----- Основная модель фильма -----
@Serializable
data class Movie(
    val id: Int,
    val externalId: ExternalId? = null,
    val name: String?,
    val alternativeName: String? = null,
    val enName: String? = null,
    val names: List<Name>? = null,
    val type: String,
    val typeNumber: Int,
    val year: Int? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val slogan: String? = null,
    val status: String? = null,
    val facts: List<Fact>? = null,
    val rating: Rating? = null,
    val votes: Votes? = null,
    val movieLength: Int? = null,
    val ratingMpaa: String? = null,
    val ageRating: Int? = null,
    val logo: Logo? = null,
    val poster: Poster? = null,
    val backdrop: Backdrop? = null,
    val videos: Videos? = null,
    val genres: List<Genre>? = null,
    val countries: List<Country>? = null,
    val persons: List<Person>? = null,
    val reviewInfo: ReviewInfo? = null,
    val seasonsInfo: List<SeasonsInfo>? = null,
    val budget: Budget? = null,
    val fees: Fees? = null,
    val premiere: Premiere? = null,
    val similarMovies: List<SimilarMovie>? = null,
    val sequelsAndPrequels: List<SequelsAndPrequels>? = null,
    val watchability: Watchability? = null,
    val releaseYears: List<ReleaseYear>? = null,
    val top10: Int? = null,
    val top250: Int? = null,
    val ticketsOnSale: Boolean? = null,
    val totalSeriesLength: Int? = null,
    val seriesLength: Int? = null,
    val isSeries: Boolean? = null,
    val audience: List<Audience>? = null,
    val lists: List<String>? = null,
    val networks: Networks? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null
)

// ----- Вспомогательные классы -----

@Serializable
data class ExternalId(
    val kpHD: String?,
    val imdb: String?,
    val tmdb: Int?
)

@Serializable
data class Name(
    val name: String?,
    val language: String? = null,
    val type: String? = null
)

@Serializable
data class Fact(
    val value: String,
    val type: String,
    val spoiler: Boolean?
)

@Serializable
data class Rating(
    val kp: Double?,
    val imdb: Double?,
    val tmdb: Double? = null,
    val filmCritics: Double?,
    val russianFilmCritics: Double?,
    val await: Double? = null
)

@Serializable
data class Votes(
    val kp: Double?, // Может быть строкой, как в примере
    val imdb: Int?,
    val tmdb: Int? = null,
    val filmCritics: Int?,
    val russianFilmCritics: Int?,
    val await: Int?
)

@Serializable
data class Logo(
    val url: String?
)

@Serializable
data class Poster(
    val url: String?,
    val previewUrl: String?
)

@Serializable
data class Backdrop(
    val url: String?,
    val previewUrl: String?
)

@Serializable
data class Video(
    val url: String,
    val name: String,
    val site: String,
    val size: Int?,
    val type: String
)

@Serializable
data class Videos(
    val trailers: List<Video>?
)

@Serializable
data class Genre(
    val name: String
)

@Serializable
data class Country(
    val name: String
)

@Serializable
data class Person(
    val id: Int,
    val photo: String?,
    val name: String,
    val enName: String?,
    val description: String?,
    val profession: String?,
    val enProfession: String?
)

@Serializable
data class ReviewInfo(
    val count: Int?,
    val positiveCount: Int?,
    val percentage: String?
)

@Serializable
data class SeasonsInfo(
    val number: Int?,
    val episodesCount: Int?
)

@Serializable
data class Budget(
    val value: Int?,
    val currency: String?
)

@Serializable
data class Fees(
    val world: FeeInfo?,
    val russia: FeeInfo?,
    val usa: FeeInfo?
)

@Serializable
data class FeeInfo(
    val value: Int?,
    val currency: String?
)

@Serializable
data class Premiere(
    val country: String?,
    val world: String?, // Можно преобразовать в Date, если нужно
    val russia: String?,
    val digital: String?,
    val cinema: String?,
    val bluray: String?,
    val dvd: String?
)

@Serializable
data class SimilarMovie(
    val id: Int,
    val name: String?,
    val enName: String?,
    val alternativeName: String?,
    val type: String?,
    val poster: Poster?,
    val rating: Rating?,
    val year: Int?
)

@Serializable
data class SequelsAndPrequels(
    val id: Int,
    val name: String?,
    val enName: String?,
    val alternativeName: String?,
    val type: String?,
    val poster: Poster?,
    val rating: Rating?,
    val year: Int?
)

@Serializable
data class WatchabilityItem(
    val name: String,
    val logo: Logo?,
    val url: String
)

@Serializable
data class Watchability(
    val items: List<WatchabilityItem>?
)

@Serializable
data class ReleaseYear(
    val start: Int?,
    val end: Int?
)

@Serializable
data class Audience(
    val count: Int?,
    val country: String?
)

@Serializable
data class NetworkItem(
    val name: String,
    val logo: Logo?
)

@Serializable
data class Networks(
    val items: List<NetworkItem>?
)