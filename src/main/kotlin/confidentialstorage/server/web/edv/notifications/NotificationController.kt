package confidentialstorage.server.web.edv.notifications

import confidentialstorage.common.authorization.ZCap
import confidentialstorage.common.authorization.ZCapManager
import confidentialstorage.common.model.notifications.AuthReq
import confidentialstorage.common.model.notifications.NotificationWsRequest
import confidentialstorage.common.utils.JsonUtils.klaxon
import confidentialstorage.server.Configuration.serverConfiguration
import confidentialstorage.server.web.edv.EdvService
import confidentialstorage.server.web.edv.notifications.NotificationManager.EdvId
import confidentialstorage.server.web.edv.notifications.NotificationManager.NotificationSession
import confidentialstorage.server.web.edv.notifications.NotificationManager.disconnectClient
import confidentialstorage.server.web.edv.notifications.NotificationManager.notificationClients
import io.javalin.websocket.WsConfig
import java.util.function.*

class NotificationController : Consumer<WsConfig> {

    override fun accept(ws: WsConfig) {

        ws.onConnect {
            val edvId = EdvId(it.pathParam("edv-id"))

            println("Notification client accepted at ${it.session.remoteAddress}")

            if (!notificationClients.containsKey(edvId))
                notificationClients[edvId] = mutableMapOf()

            val notificationSession = NotificationSession(edvId, it.sessionId, it.session)

            notificationClients[edvId]?.put(it.sessionId, notificationSession)
        }

        ws.onMessage {
            val edvId = EdvId(it.pathParam("edv-id"))

            val receivedJson = it.message()

            when (val wsReq = klaxon().parse<NotificationWsRequest>(receivedJson)!!) {
                is AuthReq -> {
                    val invocationJson = wsReq.invocationJson

                    val did = klaxon().parse<ZCap>(invocationJson)!!.invoker!!

                    val success = ZCapManager.chainVerification(
                        invocationJson,
                        did,
                        EdvService.getEdvDid(edvId.id),
                        "${serverConfiguration.url}/edvs/${edvId.id}"
                    )

                    //val authRes = AuthRes(success)
                    //it.send(authRes).get()

                    if (success) {
                        notificationClients[edvId]!![it.sessionId]!!.authorized = true
                    } else {
                        disconnectClient(edvId, it.sessionId)
                    }
                }
            }
        }

        ws.onClose {
            val edvId = EdvId(it.pathParam("edv-id"))

            println("Notification client closed at ${it.session.remoteAddress}")

            notificationClients[edvId]?.remove(it.sessionId)
        }
        ws.onError {
            val edvId = it.pathParam("edv-id")

            println("Error for $edvId/${it.sessionId}")
        }
    }
}
