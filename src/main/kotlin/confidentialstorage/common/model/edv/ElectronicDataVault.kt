package confidentialstorage.common.model.edv

data class ElectronicDataVault(
    val id: String, // https://example.com/edvs/z4sRgBJJLnYy
    val sequence: Int = 0, // 0
    val controller: String, // did:example:123456789
    //@Json(serializeNull = false) val invoker: String? = null, // did:example:123456789
    //@Json(serializeNull = false) val delegator: String? = null, // did:example:123456789
    //@Json(serializeNull = false) val referenceId: String? = null, // my-primary-data-vault
    /*val keyAgreementKey: KeyAgreementKey,
    val hmac: Hmac*/
) {
    /*
    data class KeyAgreementKey(
        val id: String, // https://example.com/kms/12345
        val type: String // X25519KeyAgreementKey2019
    )

    data class Hmac(
        val id: String, // https://example.com/kms/67891
        val type: String // Sha256HmacKey2019
    )*/
}
