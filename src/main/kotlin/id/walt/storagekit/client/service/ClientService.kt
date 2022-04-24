package id.walt.storagekit.client.service

import com.nimbusds.jose.Payload
import id.walt.storagekit.common.persistence.encryption.JWEEncryption
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.random.Random

class ClientService(
    private val masterPath: Path = Path("master")
) {

    private lateinit var _masterKey: ByteArray

    lateinit var sessionService: ClientSessionService
    lateinit var indexService: ClientIndexService
    lateinit var edvService: ClientEdvService
    lateinit var dataRequestService: ClientDataRequestService
    lateinit var documentService: ClientDocumentService

    fun masterKeyExists() = masterPath.exists()

    fun createMasterKey(newMasterKey: ByteArray) {
        masterPath.writeText(
            JWEEncryption.passphraseEncrypt(
                Payload(Random.nextBytes(Random.nextInt(12, 64))), newMasterKey
            )
        )
    }

    fun unlockWithMasterKey(masterKey: ByteArray): Result<Payload> {
        _masterKey = masterKey
        return runCatching {
            JWEEncryption.passphraseDecrypt(masterPath.readText(), masterKey)
        }
    }

    fun setupSessionService() {
        sessionService = ClientSessionService(_masterKey)
    }

    fun sessionChosen() {
        edvService = ClientEdvService(sessionService, _masterKey)
    }

    fun setup() {
        indexService = ClientIndexService(sessionService.session, _masterKey)
        edvService = ClientEdvService(sessionService, _masterKey)
        dataRequestService = ClientDataRequestService(sessionService.session, _masterKey)
        documentService = ClientDocumentService(sessionService, _masterKey)
    }
}
