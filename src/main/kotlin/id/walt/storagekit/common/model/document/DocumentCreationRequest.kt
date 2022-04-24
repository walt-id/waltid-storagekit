package id.walt.storagekit.common.model.document


import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
data class DocumentCreationRequest(
    val id: String? = null, // urn:uuid:94684128-c42c-4b28-adb0-aec77bf76044
    val sequence: Int, // 0
    /*@JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Json(serializeNull = false)*/
    val content: String,
    val index: String
)

data class SpecDocumentCreationRequest(
    val id: String, // urn:uuid:94684128-c42c-4b28-adb0-aec77bf76044
    val sequence: Int, // 0
    val jwe: Jwe
) {
    data class Jwe(
        val `protected`: String, // eyJlbmMiOiJDMjBQIn0
        val recipients: List<Recipient>,
        val iv: String, // i8Nins2vTI3PlrYW
        val ciphertext: String, // Cb-963UCXblINT8F6MDHzMJN9EAhK3I
        val tag: String // pfZO0JulJcrc3trOZy8rjA
    ) {
        data class Recipient(
            val header: Header,
            @JsonProperty("encrypted_key")
            val encryptedKey: String // OR1vdCNvf_B68mfUxFQVT-vyXVrBembuiM40mAAjDC1-Qu5iArDbug
        ) {
            data class Header(
                val alg: String, // A256KW
                val kid: String // https://example.com/kms/zSDn2MzzbxmX
            )
        }
    }
}
