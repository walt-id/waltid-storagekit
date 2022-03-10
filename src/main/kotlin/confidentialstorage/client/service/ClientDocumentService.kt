package confidentialstorage.client.service

import com.nimbusds.jose.Payload
import confidentialstorage.client.index.IndexManager
import confidentialstorage.client.service.remote.ClientRemoteService
import java.net.ConnectException

class ClientDocumentService(val sessionService: ClientSessionService, private val masterKey: ByteArray) {

    private val clients = HashMap<String, ClientRemoteService>()

    fun getClient(edvId: String): ClientRemoteService {
        if (!clients.containsKey(edvId)) {
            val edv = sessionService.session.edvs[edvId]!!
            clients.putIfAbsent(edvId, ClientRemoteService(edv.serverUrl, masterKey, sessionService))
        }
        return clients[edvId]!!
    }

    private val indexManager = IndexManager(masterKey)

    fun tryResolveDocument(edvId: String, docId: String): Boolean {
        val clientRemoteService = getClient(edvId)

        if (!clientRemoteService.knowsDocument(edvId, docId)) {
            indexManager.retrieveIndex(edvId, clientRemoteService, sessionService.session)

            return clientRemoteService.knowsDocument(edvId, docId)
        }
        return true
    }

    fun tryResolveDocument(docId: String): Boolean {
        forAllEdvs { edvId ->
            val clientRemoteService = getClient(edvId)
            if (clientRemoteService.knowsDocument(edvId, docId)) return true
            indexManager.retrieveIndex(edvId, clientRemoteService, sessionService.session)
            if (clientRemoteService.knowsDocument(edvId, docId)) return true
        }

        return false
    }

    fun tryResolveDocumentElseThrow(edvId: String, docId: String) {
        if (!tryResolveDocument(edvId, docId)) throw IllegalArgumentException("Could not resolve document!")
    }

    fun load(docId: String): Payload {
        forAllEdvs { edvId ->
            try {
                val clientRemoteService = getClient(edvId)

                tryResolveDocumentElseThrow(edvId, docId)
                return clientRemoteService.retrieveDocument(edvId, docId, sessionService.session)
            } catch (ignored: Exception) {
            }
        }
        throw ConnectException("Could not connect to any of the ${sessionService.session.edvs.keys} linked EDV(s).")
    }

    fun load(edvId: String, docId: String): Payload {
        val clientRemoteService = getClient(edvId)

        tryResolveDocumentElseThrow(edvId, docId)
        return clientRemoteService.retrieveDocument(edvId, docId, sessionService.session)
    }

    fun create(docId: String, content: ByteArray) {
        // TODO session management here, not in remote service?
        forAllEdvs { edvId -> create(edvId, docId, content) }
    }

    fun create(edvId: String, docId: String, content: ByteArray) {
        val clientRemoteService = getClient(edvId)

        clientRemoteService.createDocument(edvId, docId, 0, content, sessionService.session)
    }

    fun update(docId: String, content: ByteArray) = forAllEdvs { edvId -> update(edvId, docId, content) }

    fun update(edvId: String, docId: String, content: ByteArray) {
        val clientRemoteService = getClient(edvId)
        tryResolveDocumentElseThrow(edvId, docId)

        val index = clientRemoteService.indexManager.readIndex(edvId)
        val prevSequence = index.documents[docId] ?: 0

        clientRemoteService.updateDocument(edvId, docId, prevSequence.inc(), content, sessionService.session)
    }

    fun delete(docId: String, allVersions: Boolean = false) =
        forAllEdvs { edvId -> if (tryResolveDocument(edvId, docId)) delete(edvId, docId, allVersions) }

    fun delete(edvId: String, docId: String, allVersions: Boolean = false) {
        val clientRemoteService = getClient(edvId)
        clientRemoteService.deleteDocument(edvId, docId, sessionService.session, allVersions)
    }

    fun search(keyword: String): List<String> {
        val results = ArrayList<String>().toMutableSet()

        forAllEdvs { edvId ->
            val clientRemoteService = getClient(edvId)
            val res = clientRemoteService.searchDocument(sessionService.session, edvId, keyword)
            results.addAll(res.results)
        }

        return results.toList()
    }

    fun search(edvId: String, keyword: String): List<String> {
        val clientRemoteService = getClient(edvId)
        val res = clientRemoteService.searchDocument(sessionService.session, edvId, keyword)

        return res.results
    }

    private inline fun forAllEdvs(action: (edvId: String) -> Unit) =
        sessionService.session.edvs.keys.forEach { action(it) }

}
