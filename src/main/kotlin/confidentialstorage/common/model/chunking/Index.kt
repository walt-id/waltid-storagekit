package confidentialstorage.common.model.chunking

import confidentialstorage.common.Utils.toByteArray
import io.ipfs.multibase.Base58
import kotlinx.serialization.Serializable

@Serializable
data class IndexDocument(val id: String, val sequence: Int)

data class Index(
    var keySize: Int,
    val keys: HashMap<String, String>, // keyId -> Key
    val index: HashMap<String, String>, // document -> keyId
    val documents: HashMap<String, Int> // document -> sequence
) {
    fun addFiles(key: String, documentIds: List<String>) = documentIds.forEach { addFile(key, it) }
    fun addFile(key: String, documentId: String) {
        if (!keys.containsValue(key)) {
            keySize++

            keys[Base58.encode(keySize.toByteArray())] = key
        }

        index[documentId] = Base58.encode(keySize.toByteArray())
    }

    fun increaseSequence(documentId: String) {
        documents[documentId] = (documents[documentId] ?: 0).inc()
    }

    fun getFromIndex(documentId: String): String? {
        val keyId = index[documentId]
        /*println("keyid $keyId")
        println("key ${keys[keyId]}")*/
        return keys[keyId]
    }
}
