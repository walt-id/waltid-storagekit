package id.walt.storagekit.client.service.list

import id.walt.storagekit.client.service.remote.ApiUtils
import id.walt.storagekit.common.model.edv.EdvCreatedResponse
import id.walt.storagekit.common.model.edv.EdvCreationRequest
import id.walt.storagekit.common.persistence.encryption.JWEEncryption
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.bitcoinj.core.Base58

object CreateEdvService {
    fun createEDV(
        controller: String,
        sequence: Int,
        sessionManager: id.walt.storagekit.client.clientmodels.SessionManager,
        sessionId: String,
        baseUrl: String
    ): EdvCreatedResponse {
        val client = ApiUtils.getClient()

        val indexKey = Base58.encode(JWEEncryption.generateDirectKey())

        return runBlocking {
            val edvResponse = client.post("$baseUrl/edvs") {
                contentType(ContentType.Application.Json)
                setBody(EdvCreationRequest(sequence, controller, indexKey))
            }.body<EdvCreatedResponse>()

            sessionManager.addEdvToSession(
                sessionId,
                id.walt.storagekit.client.clientmodels.SessionManager.SessionEdv(
                    edvId = edvResponse.edvId,
                    serverUrl = baseUrl,
                    rootDelegation = edvResponse.rootDelegation,
                    indexKey = indexKey
                )
            )

            edvResponse
        }
    }
}
