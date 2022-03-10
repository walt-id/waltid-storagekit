package confidentialstorage.common.model.chunking

import com.beust.klaxon.Klaxon
import confidentialstorage.common.persistence.encryption.JWEEncryption
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedResourceStructure(
    val id: String,
    // val index: List<String>, // seperated into its own file to
    // save space for ResourceStructure retrievals (mostly search isn't needed)
    val chunkCount: Int? = null,
    val sequence: Int = 0,
    val payload: String
) {
    data class Chunk(val id: String, val hashLink: String)

    fun getChunkList(fileKey: ByteArray): List<Chunk> {
        val chunksJson = JWEEncryption.directDecrypt(payload, fileKey).toString()
        val parsedChunks = Klaxon().parseArray<String>(chunksJson)!!
            .map { it.split("?hl=") }
            .map { Chunk(it[0], it[1]) }

        return parsedChunks
    }
}

