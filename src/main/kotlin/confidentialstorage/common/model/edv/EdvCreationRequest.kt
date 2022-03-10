package confidentialstorage.common.model.edv

import kotlinx.serialization.Serializable

@Serializable
data class EdvCreationRequest(
    val sequence: Int = 0, // 0
    val controller: String,
    val indexKey: String
)//, // did:example:123456789
/*val referenceId: String, // urn:uuid:abc5a436-21f9-4b4c-857d-1f5569b2600d
val keyAgreementKey: KeyAgreementKey,
val hmac: Hmac
) {
data class KeyAgreementKey(
    val id: String, // https://example.com/kms/12345
    val type: String // X25519KeyAgreementKey2019
)

data class Hmac(
    val id: String, // https://example.com/kms/67891
    val type: String // Sha256HmacKey2019
)
}*/
