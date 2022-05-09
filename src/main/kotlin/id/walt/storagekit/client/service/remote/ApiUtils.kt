package id.walt.storagekit.client.service.remote

import id.walt.storagekit.common.HashUtils
import id.walt.storagekit.common.authorization.ZCapManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiUtils {

    fun createConfiguredClient(
        block: (HttpClientConfig<CIOEngineConfig>.() -> Unit)? = null
    ): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        block?.invoke(this)
    }

    fun getClient(username: String = "", invocationJson: String? = null) = createConfiguredClient {
        //install(Logging)

        if (invocationJson != null)
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = username, password = invocationJson)
                    }

                    sendWithoutRequest { true }
                }
            }
    }

    fun createSimpleInvocation(
        session: id.walt.storagekit.client.clientmodels.SessionManager.Session,
        edvId: String,
        action: String
    ) =
        ZCapManager.createSimpleInvocation(
            delegationZcapJson = session.edvs[edvId]!!.rootDelegation,
            invokerDid = session.did,
            action = action
        )

    fun getDocumentChangeClient(
        edvId: String,
        documentId: String?,
        content: String,
        session: id.walt.storagekit.client.clientmodels.SessionManager.Session,
        action: String
    ): HttpClient {
        val contentSha = HashUtils.computeContentSha3(content)
        val invocationJson = createSimpleInvocation(session, edvId, "$action:$documentId:$contentSha")

        return getClient("", invocationJson)
    }

}
