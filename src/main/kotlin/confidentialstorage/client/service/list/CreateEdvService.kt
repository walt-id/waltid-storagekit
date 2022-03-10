package confidentialstorage.client.service.list

import confidentialstorage.client.clientmodels.SessionManager
import confidentialstorage.client.service.remote.ApiUtils
import confidentialstorage.common.model.edv.EdvCreatedResponse
import confidentialstorage.common.model.edv.EdvCreationRequest
import confidentialstorage.common.persistence.encryption.JWEEncryption
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.bitcoinj.core.Base58

object CreateEdvService {
    fun createEDV(
        controller: String,
        sequence: Int,
        sessionManager: SessionManager,
        sessionId: String,
        baseUrl: String
    ): EdvCreatedResponse {
        val client = ApiUtils.getClient()

        val indexKey = Base58.encode(JWEEncryption.generateDirectKey())

        return runBlocking {
            val edvResponse = client.post<EdvCreatedResponse>("$baseUrl/edvs") {
                contentType(ContentType.Application.Json)
                body = EdvCreationRequest(sequence, controller, indexKey)
            }

            sessionManager.addEdvToSession(
                sessionId,
                SessionManager.SessionEdv(edvResponse.edvId, baseUrl, edvResponse.rootDelegation, indexKey)
            )

            edvResponse
        }
    }
}
