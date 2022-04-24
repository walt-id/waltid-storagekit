package id.walt.storagekit.client.clientmodels

import com.beust.klaxon.Klaxon
import id.walt.storagekit.common.persistence.file.EncryptedHKVStore
import id.walt.storagekit.server.utils.JsonUtils.jsonParser
import id.walt.crypto.KeyId
import org.bitcoinj.core.Base58
import kotlin.io.path.Path

class SessionManager(masterPassphrase: ByteArray) {

    val hkvs = EncryptedHKVStore("sessions", masterPassphrase)

    data class SessionDocument(
        val id: String,
    )

    data class SessionEdv(
        val edvId: String,
        val serverUrl: String,
        val rootDelegation: String,
        val indexKey: String
    ) {
        fun getEdvDelegation() = rootDelegation
    }

    data class Session(
        val sessionId: String,
        val edvs: Map<String, id.walt.storagekit.client.clientmodels.SessionManager.SessionEdv>,
        val keyId: KeyId,
        val did: String
    ) {
        fun getIndexKeyBytes(edvId: String): ByteArray = Base58.decode(edvs[edvId]!!.indexKey)

    }

    fun addEdvToSession(sessionId: String, sessionEdv: id.walt.storagekit.client.clientmodels.SessionManager.SessionEdv) {
        val oldSession = getSession(sessionId)

        val newEdvList = oldSession.edvs.toMutableMap().apply { put(sessionEdv.edvId, sessionEdv) }

        storeSession(oldSession.copy(edvs = newEdvList))
    }

    fun sessionStoreExists() = hkvs.exists()

    fun sessionExists(sessionId: String) = when {
        !sessionStoreExists() -> false
        else -> hkvs.exists(Path(sessionId))
    }

    fun gatherCachedSessions(): List<id.walt.storagekit.client.clientmodels.SessionManager.Session> = hkvs.listDocuments()
        .map { jsonParser.parse<id.walt.storagekit.client.clientmodels.SessionManager.Session>(hkvs.loadDocumentUnresolved(it).toString())!! }

    fun getSession(sessionId: String): id.walt.storagekit.client.clientmodels.SessionManager.Session =
        jsonParser.parse<id.walt.storagekit.client.clientmodels.SessionManager.Session>(hkvs.loadDocument(Path(sessionId)).toString())!!

    fun storeSession(session: id.walt.storagekit.client.clientmodels.SessionManager.Session) {
        val serialized = Klaxon().toJsonString(session)
        //println(serialized)

        hkvs.storeDocument(Path(session.sessionId), serialized)
    }

    fun hasSessions() = hkvs.listDocuments().isNotEmpty()

}
