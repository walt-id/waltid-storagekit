package confidentialstorage.ssikitext

import confidentialstorage.common.Utils.toByteArray
import confidentialstorage.common.persistence.file.EncryptedHKVStore
import id.walt.crypto.*
import id.walt.servicematrix.ServiceConfiguration
import id.walt.services.keystore.KeyStoreService
import id.walt.services.keystore.KeyType
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.name

class EncryptedKeyStore(configurationPath: String) : KeyStoreService() {

    data class EncryptionConfiguration(val encryptionAtRestKey: String) : ServiceConfiguration

    override val configuration: EncryptionConfiguration = fromConfiguration(configurationPath)

    //private val log = KotlinLogging.logger {}
    val hkvs = EncryptedHKVStore("keystore", configuration.encryptionAtRestKey.toByteArray())

    //TODO: get key format from config
    private val KEY_FORMAT = KeyFormat.PEM
    private val KEYS_ROOT = Path("keys")
    private val ALIAS_ROOT = Path("alias")

    override fun listKeys(): List<Key> = hkvs.listDocuments(KEYS_ROOT)
        .filter { k -> k.name == "meta" }
        .map {
            load(it.parent!!.name)
        }

    override fun load(alias: String, keyType: KeyType): Key {
        //log.debug { "Loading key \"${alias}\"." }

        val keyId = getKeyId(alias) ?: alias

        val metaData = loadKey(keyId, "meta").decodeToString()
        val algorithm = metaData.substringBefore(delimiter = ";")
        val provider = metaData.substringAfter(delimiter = ";")

        val publicPart = loadKey(keyId, "enc-pubkey").decodeToString()
        val privatePart = if (keyType == KeyType.PRIVATE) loadKey(keyId, "enc-privkey").decodeToString() else null


        return buildKey(keyId, algorithm, provider, publicPart, privatePart, KEY_FORMAT)
    }

    override fun addAlias(keyId: KeyId, alias: String) {
        hkvs.storeDocument(Path(ALIAS_ROOT.name, alias), keyId.id)

        val aliasListPath = Path(KEYS_ROOT.name, keyId.id, "aliases")

        if (!hkvs.exists(aliasListPath)) hkvs.storeDocument(aliasListPath, "")

        val aliases = hkvs.loadDocument(aliasListPath).toString()
            .split("\n").plus(alias)
        hkvs.storeDocument(Path(KEYS_ROOT.name, keyId.id, "aliases"), aliases.joinToString("\n"))
    }

    override fun store(key: Key) {
        //println("D Storing key ${key.keyId}")
        addAlias(key.keyId, key.keyId.id)
        storeKeyMetaData(key)
        storePublicKey(key)
        storePrivateKeyWhenExisting(key)
    }

    override fun getKeyId(alias: String) =
        runCatching { hkvs.loadDocument(Path(ALIAS_ROOT.name, alias)).toString() }.getOrNull()

    override fun delete(alias: String) {
        val keyId = getKeyId(alias)
        if (keyId.isNullOrEmpty())
            return
        val aliases = hkvs.loadDocument(Path(KEYS_ROOT.name, keyId, "aliases")).toString()
        aliases.split("\n").forEach { a -> hkvs.deleteDocument(Path(ALIAS_ROOT.name, a)) }
        hkvs.deleteDocument(Path(KEYS_ROOT.name, keyId))
    }

    private fun storePublicKey(key: Key) =
        saveKeyData(
            key = key,
            suffix = "enc-pubkey",
            data = when (KEY_FORMAT) {
                KeyFormat.PEM -> key.getPublicKey().toPEM()
                else -> key.getPublicKey().toBase64()
            }.encodeToByteArray()
        )

    private fun storePrivateKeyWhenExisting(key: Key) {
        if (key.keyPair != null && key.keyPair!!.private != null) {
            saveKeyData(
                key = key,
                suffix = "enc-privkey",
                data = when (KEY_FORMAT) {
                    KeyFormat.PEM -> key.keyPair!!.private.toPEM()
                    else -> key.keyPair!!.private.toBase64()
                }.encodeToByteArray()
            )
        }
    }

    private fun storeKeyMetaData(key: Key) {
        saveKeyData(key, "meta", (key.algorithm.name + ";" + key.cryptoProvider.name).encodeToByteArray())
    }

    private fun saveKeyData(key: Key, suffix: String, data: ByteArray): Unit =
        hkvs.storeDocument(
            path = Path(KEYS_ROOT.name, key.keyId.id, suffix),
            text = Base64.getEncoder().encodeToString(data)
        )

    private fun loadKey(keyId: String, suffix: String): ByteArray =
        Base64.getDecoder().decode(hkvs.loadDocument(Path(KEYS_ROOT.name, keyId, suffix)).toBytes())
}
