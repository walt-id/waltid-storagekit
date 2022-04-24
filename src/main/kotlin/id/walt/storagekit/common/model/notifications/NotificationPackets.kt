package id.walt.storagekit.common.model.notifications

import com.beust.klaxon.TypeFor
import id.walt.storagekit.server.web.edv.notifications.NotificationService
import kotlinx.serialization.Serializable

@TypeFor(field = "type", adapter = NotificationService.NotificationRequestTypeAdapter::class)
open class NotificationWsRequest(open val type: String)

data class AuthReq(val invocationJson: String) : NotificationWsRequest("auth")
//data class RequestEventsSinceReq(val sequence: Int) : NotificationWsRequest()

data class AuthRes(val authorized: Boolean)

enum class NotificationOperation {
    CREATE,
    UPDATE,
    DELETE,
    SEARCH,
    GET
}

@Serializable
data class EventMessage(
    val edvId: String,
    val sequence: Int,
    val documentId: String,
    val invoker: String,
    val operation: NotificationOperation
)

