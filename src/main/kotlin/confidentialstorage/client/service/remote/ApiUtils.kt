package confidentialstorage.client.service.remote

import confidentialstorage.client.clientmodels.SessionManager
import confidentialstorage.common.HashUtils
import confidentialstorage.common.authorization.ZCapManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*

object ApiUtils {

    fun getClient(username: String = "", invocationJson: String? = null) = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
        }
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

    fun createSimpleInvocation(session: SessionManager.Session, edvId: String, action: String) =
        ZCapManager.createSimpleInvocation(
            delegationZcapJson = session.edvs[edvId]!!.rootDelegation,
            invokerDid = session.did,
            action = action
        )

    fun getDocumentChangeClient(
        edvId: String,
        documentId: String?,
        content: String,
        session: SessionManager.Session,
        action: String
    ): HttpClient {
        val contentSha = HashUtils.computeContentSha3(content)
        val invocationJson = createSimpleInvocation(session, edvId, "$action:$documentId:$contentSha")

        return getClient("", invocationJson)
    }

}
