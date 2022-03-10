package confidentialstorage.client.service.clients

import confidentialstorage.client.clientmodels.SessionManager.Session

interface DocumentClient {

    suspend fun sendRequest(edvId: String, id: String, content: String, sequence: Int, baseUrl: String, session: Session)
}
