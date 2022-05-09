package id.walt.storagekit.client.service.remote

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.Payload
import id.walt.storagekit.client.clientmodels.SessionManager.Session
import id.walt.storagekit.client.index.IndexManager
import id.walt.storagekit.client.service.ClientSessionService
import id.walt.storagekit.client.service.clients.DocumentClient
import id.walt.storagekit.client.service.list.CreateDocumentService
import id.walt.storagekit.client.service.list.CreateEdvService
import id.walt.storagekit.client.service.remote.ApiUtils.getClient
import id.walt.storagekit.common.HashUtils.computeContentSha
import id.walt.storagekit.common.authorization.ZCapManager
import id.walt.storagekit.common.hashindexes.HashSearch
import id.walt.storagekit.common.model.chunking.EncryptedResourceStructure
import id.walt.storagekit.common.model.encryptedsearch.SearchDocumentReq
import id.walt.storagekit.common.model.encryptedsearch.SearchDocumentRes
import id.walt.storagekit.common.persistence.encryption.JWEEncryption
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash

class ClientRemoteService(
    private val baseUrl: String = "http://localhost:7000",
    masterPassphrase: ByteArray,
    private val sessionService: ClientSessionService
) {

    val indexManager = IndexManager(masterPassphrase)

    suspend fun sendRequest(documentClient: DocumentClient, edvId: String, id: String, content: String, sequence: Int) =
        documentClient.sendRequest(edvId, id, content, sequence, baseUrl, sessionService.session)

    fun createEDV(controller: String, sequence: Int) =
        CreateEdvService.createEDV(
            controller,
            sequence,
            sessionService.sessionManager,
            sessionService.session.sessionId,
            baseUrl
        )


    fun createDocument(
        edvId: String,
        documentId: String?,
        sequence: Int,
        content: ByteArray,
        session: Session
    ) = CreateDocumentService(this).createDocument(edvId, documentId, sequence, content, session, indexManager)


    fun searchDocument(session: Session, edvId: String, keyword: String): SearchDocumentRes {
        val key = session.getIndexKeyBytes(edvId)
        // println("Retrieved key $key")

        val transformedKey = Base58.encode(Sha256Hash.hashTwice(key))
        // println("Sending key $transformedKey")

        val hashedKeyword = HashSearch.calculateHash(keyword.toByteArray(), key, edvId.toByteArray())
        // println("Hashed keyword: $hashedKeyword")

        val invocationJson = ApiUtils.createSimpleInvocation(session, edvId, "SearchDocument:$hashedKeyword")
        val client = getClient("", invocationJson)

        return runBlocking {
            client.post("$baseUrl/edvs/$edvId/docs/search") {
                contentType(ContentType.Application.Json)
                setBody(SearchDocumentReq(hashedKeyword, transformedKey))
            }.body()
        }
    }

    fun knowsDocument(edvId: String, documentId: String) = indexManager.exists(edvId, documentId)

    fun sendDocumentRetrieval(session: Session, edvId: String, documentId: String): String {
        val invocationJson = ZCapManager.createSimpleInvocation(
            session.edvs[edvId]!!.rootDelegation,
            session.did,
            "RetrieveDocument:$documentId"
        )

        val client = getClient("documents", invocationJson)
        return runBlocking {
            client.get("$baseUrl/edvs/$edvId/docs/$documentId").bodyAsText()
        }
    }

    fun retrieveAndOrderChunks(
        session: Session,
        edvId: String,
        chunks: List<EncryptedResourceStructure.Chunk>,
        fileKey: ByteArray
    ): Payload {
        val arr = ArrayList<String>()

        chunks.forEach {
            val doc = sendDocumentRetrieval(session, edvId, it.id)

            check(computeContentSha(doc) == it.hashLink)

            arr.add(doc)
        }

        val decrypted = arr.map { JWEEncryption.directDecrypt(it, fileKey) }
            .flatMap { it.toBytes().toList() }.toByteArray()

        return Payload(decrypted)
    }

    fun retrieveEncryptedResourceStructure(
        edvId: String,
        documentId: String,
        session: Session
    ): EncryptedResourceStructure =
        Klaxon().parse<EncryptedResourceStructure>(sendDocumentRetrieval(session, edvId, documentId))!!

    private fun getFileKey(edvId: String, documentId: String) =
        indexManager.getFromIndex(edvId, documentId)
            ?: throw IllegalStateException("TODO: retrieve index from server")

    fun retrieveDocument(
        edvId: String,
        documentId: String,
        session: Session
    ): Payload {
        val ers = retrieveEncryptedResourceStructure(edvId, documentId, session)

        val isInlinePayload = ers.chunkCount != null

        val fileKey = getFileKey(edvId, documentId)

        return if (isInlinePayload) JWEEncryption.directDecrypt(ers.payload, fileKey)
        else retrieveAndOrderChunks(session, edvId, ers.getChunkList(fileKey), fileKey)
    }

    fun updateDocument(
        edvId: String,
        documentId: String,
        sequence: Int,
        content: ByteArray,
        session: Session
    ) = CreateDocumentService(this).changeDocument(
        edvId = edvId,
        documentId = documentId,
        sequence = sequence,
        content = content,
        session = session,
        indexManager = indexManager,
        fileKey = getFileKey(edvId, documentId),
        update = true
    )

    fun deleteSingleDocument(edvId: String, documentId: String, session: Session) {
        val invocationJson = ZCapManager.createSimpleInvocation(
            session.edvs[edvId]!!.rootDelegation,
            session.did,
            "DeleteDocument:$documentId"
        )

        val client = getClient("documents", invocationJson)

        runBlocking {
            client.delete("$baseUrl/edvs/$edvId/docs/$documentId").bodyAsText()
        }
    }

    fun deleteDocument(
        edvId: String,
        documentId: String,
        session: Session,
        allVersions: Boolean = false
    ) {
        if (!allVersions) return deleteSingleDocument(edvId, documentId, session)

        val ers = retrieveEncryptedResourceStructure(edvId, documentId, session)

        val chunks = ers.getChunkList(getFileKey(edvId, documentId))

        chunks.forEachIndexed { index, chunk ->
            deleteSingleDocument(edvId, chunk.id, session)
            println("Purging (${(100 * (index + 1)) / chunks.size}%)...")
        }
    }

    companion object {
        const val MAX_CHUNK_SIZE = 700 * 1000
    }
}
