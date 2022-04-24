package id.walt.storagekit.server.web.edv.notifications

import com.beust.klaxon.TypeAdapter
import id.walt.storagekit.common.model.notifications.AuthReq
import id.walt.storagekit.common.model.notifications.EventMessage
import id.walt.storagekit.common.model.notifications.NotificationOperation
import id.walt.storagekit.common.model.notifications.NotificationWsRequest
import id.walt.storagekit.common.utils.JsonUtils.klaxon
import org.eclipse.jetty.websocket.api.Session
import kotlin.reflect.KClass

object NotificationService {
    @JvmInline
    value class EdvId(val id: String)

    data class NotificationSession(val edvId: EdvId, val sessionId: String, val ws: Session, var authorized: Boolean = false)

    val notificationClients = mutableMapOf<EdvId, MutableMap<String, NotificationSession>>()

    class NotificationRequestTypeAdapter : TypeAdapter<NotificationWsRequest> {
        override fun classFor(type: Any): KClass<out NotificationWsRequest> = when (type as String) {
            "auth" -> AuthReq::class
            //"request-since" -> RequestEventsSinceReq::class
            else -> throw IllegalArgumentException("Unknown notification request type: $type")
        }
    }

    fun broadcastNotifications(
        edvId: EdvId,
        documentId: String,
        sequence: Int,
        invoker: String,
        operation: NotificationOperation
    ) {
        notificationClients[edvId]?.values?.forEach {
            if (it.authorized) {
                val eventMessage = EventMessage(edvId.id, sequence, documentId, invoker, operation)

                val json = klaxon().toJsonString(eventMessage)

                it.ws.remote.sendString(json)
            }
        }
    }

    fun disconnectClient(
        edvId: EdvId,
        sessionId: String
    ) {
        notificationClients[edvId]!![sessionId]!!.ws.disconnect()
    }
}
