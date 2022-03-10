package confidentialstorage.client.clientmodels

import com.beust.klaxon.Klaxon
import confidentialstorage.common.persistence.file.EncryptedHKVStore
import confidentialstorage.server.utils.JsonUtils.jsonParser
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
        val edvs: Map<String, SessionEdv>,
        val keyId: KeyId,
        val did: String
    ) {
        fun getIndexKeyBytes(edvId: String): ByteArray = Base58.decode(edvs[edvId]!!.indexKey)

    }

    fun addEdvToSession(sessionId: String, sessionEdv: SessionEdv) {
        val oldSession = getSession(sessionId)

        val newEdvList = oldSession.edvs.toMutableMap().apply { put(sessionEdv.edvId, sessionEdv) }

        storeSession(oldSession.copy(edvs = newEdvList))
    }

    fun sessionStoreExists() = hkvs.exists()

    fun sessionExists(sessionId: String) = when {
        !sessionStoreExists() -> false
        else -> hkvs.exists(Path(sessionId))
    }

    fun gatherCachedSessions(): List<Session> = hkvs.listDocuments()
        .map { jsonParser.parse<Session>(hkvs.loadDocumentUnresolved(it).toString())!! }

    fun getSession(sessionId: String): Session =
        jsonParser.parse<Session>(hkvs.loadDocument(Path(sessionId)).toString())!!

    fun storeSession(session: Session) {
        val serialized = Klaxon().toJsonString(session)
        //println(serialized)

        hkvs.storeDocument(Path(session.sessionId), serialized)
    }

    fun hasSessions() = hkvs.listDocuments().isNotEmpty()

}
