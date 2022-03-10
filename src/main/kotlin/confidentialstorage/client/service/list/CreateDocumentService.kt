package confidentialstorage.client.service.list

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.Payload
import confidentialstorage.client.clientmodels.SessionManager.Session
import confidentialstorage.client.index.Chunk
import confidentialstorage.client.index.IndexManager
import confidentialstorage.client.service.clients.CreateDocumentClient
import confidentialstorage.client.service.clients.UpdateDocumentClient
import confidentialstorage.client.service.remote.ClientRemoteService
import confidentialstorage.common.HashUtils
import confidentialstorage.common.hashindexes.HashBasedIndex
import confidentialstorage.common.hashindexes.HashSearch
import confidentialstorage.common.hashindexes.Indexer
import confidentialstorage.common.model.chunking.EncryptedResourceStructure
import confidentialstorage.common.persistence.encryption.JWEEncryption
import confidentialstorage.server.utils.IdentifierUtils
import io.ktor.client.features.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bitcoinj.core.Sha256Hash

class CreateDocumentService(private val clientRemoteService: ClientRemoteService) {

    private fun mapDocumentChunks(content: ByteArray): ArrayList<Pair<Int, Int>> {
        var chunkMapped = 0
        val chunkSizeMappings = ArrayList<Pair<Int, Int>>()

        while (chunkMapped < content.size) {
            val nextRange = chunkMapped + Integer.min(content.size - chunkMapped, ClientRemoteService.MAX_CHUNK_SIZE)

            chunkSizeMappings.add(Pair(chunkMapped, nextRange))

            chunkMapped = nextRange
        }
        return chunkSizeMappings
    }

    private suspend fun uploadChunks(
        edvId: String,
        chunkSizeMappings: ArrayList<Pair<Int, Int>>,
        chunks: ArrayList<Chunk>,
        content: ByteArray,
        sequence: Int,
        session: Session,
        fileKey: ByteArray
    ) {
        coroutineScope {
            chunkSizeMappings.forEachIndexed { counter, chunkMapping ->
                val data = JWEEncryption.directEncrypt(
                    Payload(content.copyOfRange(chunkMapping.first, chunkMapping.second)), fileKey
                )
                val id = HashUtils.computeContentSha3(data)

                val chunk = Chunk(id, data)
                chunks.add(chunk)

                launch {
                    clientRemoteService.sendRequest(CreateDocumentClient, edvId, chunk.id, chunk.data, sequence)
                }

                val i = counter + 1
                val chunkCount = chunkSizeMappings.size
                val remaining = chunkCount - i

                println("Chunk $i/$chunkCount transmitted. ${if (remaining > 0) "$remaining remaining..." else "Done!"}")
            }
        }
    }

    private fun encryptToJweWithChunkHashlinks(chunks: ArrayList<Chunk>, fileKey: ByteArray): String {
        println("Generating chunk hashlinks...")
        val arr: Array<String> = chunks.map { it.id + "?hl=" + HashUtils.computeContentSha(it.data) }.toTypedArray()
        val json = Klaxon().toJsonString(arr)
        val jwe = JWEEncryption.directEncrypt(Payload(json), fileKey)

        return jwe
    }

    private fun createEncryptedResourceStructure(
        documentId: String?, payload: String, inlinePayload: Boolean, chunkCount: Int
    ): Pair<EncryptedResourceStructure, String> {
        val encryptedResourceStructure = EncryptedResourceStructure(
            id = documentId ?: IdentifierUtils.generateAlphaNumeric(64),
            payload = payload,
            chunkCount = if (!inlinePayload) null else chunkCount
        )

        return Pair(encryptedResourceStructure, Klaxon().toJsonString(encryptedResourceStructure))
    }

    private suspend fun uploadEncryptedResourceStructure(
        edvId: String,
        documentId: String?,
        payload: String,
        inlinePayload: Boolean,
        chunkCount: Int,
        sequence: Int,
        update: Boolean
    ): EncryptedResourceStructure {
        println("Uploading EncryptedResourceStructure...")

        val (ers, ersJson) = createEncryptedResourceStructure(documentId, payload, inlinePayload, chunkCount)

        clientRemoteService.sendRequest(
            documentClient = if (update) UpdateDocumentClient else CreateDocumentClient,
            edvId = edvId,
            id = ers.id,
            content = ersJson,
            sequence = sequence
        )

        return ers
    }

    private fun addToIndex(
        indexManager: IndexManager,
        edvId: String,
        fileKey: ByteArray,
        chunks: ArrayList<Chunk>,
        ers: EncryptedResourceStructure
    ) {
        indexManager.addToIndex(edvId, fileKey, chunks.map { it.id } + ers.id, clientRemoteService)
    }

    private fun increaseSequence(
        indexManager: IndexManager,
        edvId: String,
        docIds: List<String>,
    ) {
        indexManager.increaseSequence(edvId, docIds, clientRemoteService)
    }

    suspend fun createEncryptedIndex(
        session: Session, edvId: String, content: ByteArray, ers: EncryptedResourceStructure, sequence: Int, update: Boolean
    ) {
        val encryptedSearchKey = session.getIndexKeyBytes(edvId)

        val keywords = Indexer.getKeywords(content.decodeToString(), "unknown")
        val hashedKeywords = keywords.map {
            HashSearch.calculateHash(
                it.toByteArray(), encryptedSearchKey,
                edvId.toByteArray()
            )
        }

        val hashBasedIndex = HashBasedIndex(ers.id, hashedKeywords)
        val json = Klaxon().toJsonString(hashBasedIndex)

        val isEncryptedIndexOversize = json.length > 1000000

        if (isEncryptedIndexOversize) {
            println("Encrypted Search will be disabled for extremely large encrypted index.")
        } else {
            val jwe = JWEEncryption.passphraseEncrypt(
                Payload(json), Sha256Hash.hashTwice(encryptedSearchKey)
            )

            try {
                println("Uploading encrypted search index...")


                clientRemoteService.sendRequest(
                    documentClient = if (update) UpdateDocumentClient else CreateDocumentClient,
                    edvId = edvId,
                    id = ers.id + "-search-index",
                    content = jwe,
                    sequence = sequence
                )
            } catch (e: ClientRequestException) {
                println("Encrypted Search will be disabled for extremely large encrypted index.")
            }
        }
    }

    fun changeDocument(
        edvId: String,
        documentId: String?,
        sequence: Int,
        content: ByteArray,
        session: Session,
        indexManager: IndexManager,
        fileKey: ByteArray,
        update: Boolean // otherwise create
    ) {
        println("Mapping chunks...")
        val chunkSizeMappings = mapDocumentChunks(content)

        println("(Content size ${content.size}, chunks needed: ${chunkSizeMappings.size})")

        runBlocking {
            val chunks = ArrayList<Chunk>()
            val inlinePayload = chunkSizeMappings.size == 1

            val ersPayload: String = if (inlinePayload) {
                // Inline payload
                JWEEncryption.directEncrypt(Payload(content), fileKey)
            } else {
                uploadChunks(edvId, chunkSizeMappings, chunks, content, sequence, session, fileKey)
                // check if chunks are returned         ^^^

                println("All uploads are finished, continuing...")
                encryptToJweWithChunkHashlinks(chunks, fileKey)
            }

            val ers = uploadEncryptedResourceStructure(
                edvId = edvId,
                documentId = documentId,
                payload = ersPayload,
                inlinePayload = inlinePayload,
                chunkCount = chunkSizeMappings.size,
                sequence = sequence,
                update = update
            )
            addToIndex(indexManager, edvId, fileKey, chunks, ers)
            if (update)
                increaseSequence(indexManager, edvId, listOf(documentId!!))

            // Hash-based Index
            println("Done. Creating encrypted search index...")
            createEncryptedIndex(session, edvId, content, ers, sequence, update)
        }
    }

    fun createDocument(
        edvId: String,
        documentId: String?,
        sequence: Int,
        content: ByteArray,
        session: Session,
        indexManager: IndexManager,
    ) {
        println("Generating file key...")
        val fileKey = JWEEncryption.generateDirectKey()

        changeDocument(edvId, documentId, sequence, content, session, indexManager, fileKey, false)

        println("All done. File \"$documentId\" is now stored to EDV \"$edvId\"!")
    }
}
