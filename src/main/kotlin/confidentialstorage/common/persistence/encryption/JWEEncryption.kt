package confidentialstorage.common.persistence.encryption

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.jwk.ECKey
import javax.crypto.KeyGenerator


object JWEEncryption {

    private val passphraseHeader = JWEHeader(JWEAlgorithm.PBES2_HS512_A256KW, EncryptionMethod.A256GCM)

    private val directHeader = JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
    private val directKeyBitLength = EncryptionMethod.A256GCM.cekBitLength()

    private val ecdsaHeader = JWEHeader(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256GCM)

    private fun encrypt(payload: Payload, header: JWEHeader, encryptor: JWEEncrypter): String = JWEObject(header, payload).run {
        encrypt(encryptor)
        serialize()
    }

    private fun decrypt(jwe: String, decryptor: JWEDecrypter): Payload = JWEObject.parse(jwe).run {
        decrypt(decryptor)
        payload
    }

    fun passphraseEncrypt(payload: Payload, key: ByteArray): String =
        encrypt(payload, passphraseHeader, PasswordBasedEncrypter(key, 32, 50000))

    fun passphraseDecrypt(jwe: String, key: ByteArray): Payload = decrypt(jwe, PasswordBasedDecrypter(key))


    fun generateDirectKey(): ByteArray = KeyGenerator.getInstance("AES").run {
        init(directKeyBitLength)
        generateKey().encoded
    }

    fun directEncrypt(payload: Payload, key: ByteArray): String = encrypt(payload, directHeader, DirectEncrypter(key))
    fun directDecrypt(jwe: String, key: ByteArray): Payload = decrypt(jwe, DirectDecrypter(key))

    fun asymmetricEncrypt(payload: Payload, key: ECKey): String = encrypt(payload, ecdsaHeader, ECDHEncrypter(key))
    fun asymmetricDecrypt(jwe: String, key: ECKey): Payload = decrypt(jwe, ECDHDecrypter(key))

}
