package confidentialstorage.client.index

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.Payload
import confidentialstorage.client.clientmodels.SessionManager.Session
import confidentialstorage.client.service.clients.CreateDocumentClient
import confidentialstorage.client.service.remote.ClientRemoteService
import confidentialstorage.common.model.chunking.Index
import confidentialstorage.common.persistence.encryption.JWEEncryption
import confidentialstorage.common.persistence.file.EncryptedHKVStore
import kotlinx.coroutines.runBlocking
import org.bitcoinj.core.Base58
import kotlin.io.path.Path

class IndexManager(private val masterKey: ByteArray) {

    private val indexes = EncryptedHKVStore("indexes", masterKey)

    fun readIndex(edvId: String) =
        Klaxon().parse<Index>(indexes.loadDocument(Path(edvId)).toString())!!

    private fun precheckIndexExists(edvId: String) {
        if (!indexes.exists(Path(edvId))) indexes.storeDocument(
            Path(edvId),
            Klaxon().toJsonString(Index(0, HashMap(), HashMap(), HashMap()))
        )
    }

    fun addToIndex(
        edvId: String,
        key: ByteArray,
        documentIds: List<String>,
        clientRemoteService: ClientRemoteService
    ) {
        precheckIndexExists(edvId)

        val index = readIndex(edvId)
        index.addFiles(Base58.encode(key), documentIds)
        updateIndex(edvId, index, clientRemoteService)
    }


    fun increaseSequence(
        edvId: String,
        documentIds: List<String>,
        clientRemoteService: ClientRemoteService
    ) {
        precheckIndexExists(edvId)

        val index = readIndex(edvId)
        documentIds.forEach {
            index.increaseSequence(it)
        }

        updateIndex(edvId, index, clientRemoteService)
    }


    fun updateIndex(edvId: String, index: Index, clientRemoteService: ClientRemoteService) {
        val indexJson = Klaxon().toJsonString(index)
        indexes.storeDocument(Path(edvId), indexJson)

        val jwe = JWEEncryption.passphraseEncrypt(Payload(indexJson), masterKey)

        runBlocking {
            clientRemoteService.sendRequest(CreateDocumentClient, edvId, "index", jwe, 0)
        }
    }

    fun retrieveIndex(edvId: String, clientRemoteService: ClientRemoteService, session: Session) {
        val jwe = clientRemoteService.sendDocumentRetrieval(session, edvId, "index")
        val indexJson = JWEEncryption.passphraseDecrypt(jwe, masterKey).toString()
        indexes.storeDocument(Path(edvId), indexJson)
    }

    fun getFromIndex(edvId: String, documentId: String): ByteArray? = Base58.decode(readIndex(edvId).getFromIndex(documentId))

    fun exists(edvId: String, documentId: String): Boolean =
        indexes.exists(Path(edvId)) && readIndex(edvId).index.contains(documentId)

    fun listIndex(edvId: String) = readIndex(edvId).index
}
