package id.walt.storagekit.client.service.clients

import id.walt.storagekit.client.clientmodels.SessionManager
import id.walt.storagekit.client.service.remote.ApiUtils
import id.walt.storagekit.common.model.document.DocumentCreationRequest
import id.walt.storagekit.server.web.document.DocumentService
import io.ktor.client.request.*
import io.ktor.http.*

object CreateDocumentClient : DocumentClient {
    override suspend fun sendRequest(
        edvId: String,
        id: String,
        content: String,
        sequence: Int,
        baseUrl: String,
        session: id.walt.storagekit.client.clientmodels.SessionManager.Session
    ) {
        val client = ApiUtils.getDocumentChangeClient(edvId, id, content, session, "CreateDocument")

        client.post<DocumentService.DocumentCreationResponse>("$baseUrl/edvs/$edvId/docs") {
            contentType(ContentType.Application.Json)
            body = DocumentCreationRequest(id, sequence, content, ""/*index*/) // TODO index
        }
    }
}
