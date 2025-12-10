package Models.BooksModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OuterResponse(
    val kind: String,
    val totalItems: Int,
    val items: List<BookResponse>
)


@Serializable
data class BookResponse(
    val kind: String,
    val id: String,
    val etag: String,
    val selfLink: String,

    @SerialName("volumeInfo")
    val volumeInfo: VolumeInfo,

    @SerialName("saleInfo")
    val saleInfo: SaleInfo? = null,

    @SerialName("accessInfo")
    val accessInfo: AccessInfo? = null
)

@Serializable
data class VolumeInfo(
    val title: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    @SerialName("industryIdentifiers")
    val industryIdentifiers: List<IndustryIdentifier>? = null,
    val pageCount: Int? = null,
    val dimensions: Dimensions? = null,
    val printType: String? = null,
    val mainCategory: String? = null,
    val categories: List<String>? = null,
    val averageRating: Double? = null,
    val ratingsCount: Int? = null,
    val contentVersion: String? = null,
    @SerialName("imageLinks")
    val imageLinks: ImageLinks? = null,
    val language: String? = null,
    val infoLink: String? = null,
    val canonicalVolumeLink: String? = null
)

@Serializable
data class IndustryIdentifier(
    val type: String? = null,
    val identifier: String? = null
)

@Serializable
data class Dimensions(
    val height: String? = null,
    val width: String? = null,
    val thickness: String? = null
)

@Serializable
data class ImageLinks(
    val smallThumbnail: String? = null,
    val thumbnail: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    @SerialName("extraLarge")
    val extraLarge: String? = null
)


@Serializable
data class SaleInfo(
    val country: String? = null,
    val saleability: String? = null,
    val isEbook: Boolean? = null,
    @SerialName("listPrice")
    val listPrice: Price? = null,
    @SerialName("retailPrice")
    val retailPrice: Price? = null,
    val buyLink: String? = null
)

@Serializable
data class Price(
    val amount: Double? = null,
    val currencyCode: String? = null
)


@Serializable
data class AccessInfo(
    val country: String? = null,
    val viewability: String? = null,
    val embeddable: Boolean? = null,
    val publicDomain: Boolean? = null,
    val textToSpeechPermission: String? = null,
    val epub: Epub? = null,
    val pdf: Pdf? = null,
    val accessViewStatus: String? = null
)

@Serializable
data class Epub(
    val isAvailable: Boolean? = null,
    val acsTokenLink: String? = null
)

@Serializable
data class Pdf(
    val isAvailable: Boolean? = null
)
