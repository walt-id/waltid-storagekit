package confidentialstorage.client.service.clients

import confidentialstorage.client.clientmodels.SessionManager
import confidentialstorage.client.service.remote.ApiUtils
import confidentialstorage.common.model.document.DocumentCreationRequest
import io.ktor.client.request.*
import io.ktor.http.*

object UpdateDocumentClient : DocumentClient {
    override suspend fun sendRequest(
        edvId: String,
        id: String,
        content: String,
        sequence: Int,
        baseUrl: String,
        session: SessionManager.Session
    ) {
        val client = ApiUtils.getDocumentChangeClient(edvId, id, content, session, "UpdateDocument")

        client.patch<Unit>("$baseUrl/edvs/$edvId/docs/$id") {
            contentType(ContentType.Application.Json)
            body = DocumentCreationRequest(id, sequence, content, ""/*index*/) // TODO index
        }
    }
}
