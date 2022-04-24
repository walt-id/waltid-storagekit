package id.walt.storagekit.client.service

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.Payload
import id.walt.storagekit.client.clientmodels.SessionManager
import id.walt.storagekit.client.clientmodels.SessionManager.Session
import id.walt.storagekit.client.service.remote.SessionExport
import id.walt.storagekit.common.persistence.encryption.JWEEncryption
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType

class ClientSessionService(private val masterKey: ByteArray) {

    internal val sessionManager = id.walt.storagekit.client.clientmodels.SessionManager(masterKey)
    lateinit var sessionId: String

    val session: Session
    get() = sessionManager.getSession(sessionId)

    fun listCachedSessions() = sessionManager.gatherCachedSessions()
    fun sessionExists(sessionId: String) = sessionManager.sessionExists(sessionId)
    fun hasSessions() = sessionManager.sessionStoreExists() && sessionManager.hasSessions()

    fun selectSession(newSessionId: String) {
        sessionId = newSessionId
    }

    fun createSession(sessionId: String): Session {
        val keyService = KeyService.getService()

        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)


        val controllerDid = DidService.create(DidMethod.key, keyId.id)

        val session = Session(sessionId, emptyMap(), keyId, controllerDid)
        sessionManager.storeSession(session)

        return session
    }

    fun importSession(tokenJWE: String): Session {
        val decryptedTokenJson = JWEEncryption.passphraseDecrypt(tokenJWE, masterKey).toString()

        val sessionExport = Klaxon().parse<SessionExport>(decryptedTokenJson)!!

        val session = sessionExport.session

        val keyId = KeyService.getService().importKey(sessionExport.keyString)

        val did = DidService.create(DidMethod.key, keyId.id)
        check(session.did == did)

        sessionManager.storeSession(session)

        return session
    }

    fun export(sessionId: String) = export(sessionManager.getSession(sessionId))
    fun export(session: Session): String {
        val keyString = KeyService.getService().export(session.keyId.id, exportKeyType = KeyType.PRIVATE)

        val export = SessionExport(session, keyString)

        val sessionJson = Klaxon().toJsonString(export)

        return JWEEncryption.passphraseEncrypt(Payload(sessionJson), masterKey)
    }

    fun getSession(sessionId: String): Session = sessionManager.getSession(sessionId)
}
