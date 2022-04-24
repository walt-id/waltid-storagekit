package id.walt.storagekit.common.model.document


import com.fasterxml.jackson.annotation.JsonProperty

data class EncryptedDocument(
    val id: String, // z19x9iFMnfo4YLsShKAvnJk4L
    val sequence: Int, // 0
    val indexed: List<Indexed>,
    val jwe: Jwe
) {
    data class Indexed(
        val hmac: Hmac,
        val sequence: Int, // 0
        val attributes: List<Any>
    ) {
        data class Hmac(
            val id: String, // did:ex:12345#key1
            val type: String // Sha256HmacKey2019
        )
    }

    data class Jwe(
        val `protected`: String, // eyJlbmMiOiJDMjBQIn0
        val recipients: List<Recipient>,
        val iv: String, // FoJ5uPIR6HDPFCtD
        val ciphertext: String, // tIupQ-9MeYLdkAc1Us0Mdlp1kZ5Dbavq0No-eJ91cF0R0hE
        val tag: String // TMRcEPc74knOIbXhLDJA_w
    ) {
        data class Recipient(
            val header: Header,
            @JsonProperty("encrypted_key")
            val encryptedKey: String // 4PQsjDGs8IE3YqgcoGfwPTuVG25MKjojx4HSZqcjfkhr0qhwqkpUUw
        ) {
            data class Header(
                val kid: String, // urn:123
                val alg: String, // ECDH-ES+A256KW
                val epk: Epk,
                val apu: String, // d7rIddZWblHmCc0mYZJw39SGteink_afiLraUb-qwgs
                val apv: String // dXJuOjEyMw
            ) {
                data class Epk(
                    val kty: String, // OKP
                    val crv: String, // X25519
                    val x: String // d7rIddZWblHmCc0mYZJw39SGteink_afiLraUb-qwgs
                )
            }
        }
    }
}
