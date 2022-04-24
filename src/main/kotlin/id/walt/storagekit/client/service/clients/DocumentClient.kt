package id.walt.storagekit.client.service.clients

import id.walt.storagekit.client.clientmodels.SessionManager.Session

interface DocumentClient {

    suspend fun sendRequest(edvId: String, id: String, content: String, sequence: Int, baseUrl: String, session: Session)
}
