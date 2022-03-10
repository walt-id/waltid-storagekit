package confidentialstorage.common.model.chunking

import kotlinx.serialization.Serializable

@Serializable
data class ResourceStructure(
    val id: String,
    val meta: Meta,
    val content: List<String> // if size=1, the entire payload, else hashlinks to individual chunks
) {
    @Serializable
    data class Meta(
        val contentType: String // MIME type
    )
}
