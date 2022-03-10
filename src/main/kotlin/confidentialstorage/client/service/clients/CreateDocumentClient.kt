package confidentialstorage.client.service.clients

import confidentialstorage.client.clientmodels.SessionManager
import confidentialstorage.client.service.remote.ApiUtils
import confidentialstorage.common.model.document.DocumentCreationRequest
import confidentialstorage.server.web.document.DocumentService
import io.ktor.client.request.*
import io.ktor.http.*

object CreateDocumentClient : DocumentClient {
    override suspend fun sendRequest(
        edvId: String,
        id: String,
        content: String,
        sequence: Int,
        baseUrl: String,
        session: SessionManager.Session
    ) {
        val client = ApiUtils.getDocumentChangeClient(edvId, id, content, session, "CreateDocument")

        client.post<DocumentService.DocumentCreationResponse>("$baseUrl/edvs/$edvId/docs") {
            contentType(ContentType.Application.Json)
            body = DocumentCreationRequest(id, sequence, content, ""/*index*/) // TODO index
        }
    }
}
