package id.walt.storagekit.client.service

import id.walt.storagekit.client.service.remote.ClientRemoteService
import id.walt.storagekit.common.authorization.ZCapManager
import id.walt.storagekit.common.model.edv.EdvCreatedResponse
import id.walt.storagekit.common.model.notifications.AuthReq
import id.walt.storagekit.common.model.notifications.EventMessage
import id.walt.storagekit.common.utils.JsonUtils
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import okhttp3.internal.closeQuietly
import kotlin.concurrent.thread

class ClientEdvService(val sessionService: ClientSessionService, val masterKey: ByteArray) {

    fun getEdv(edvId: String) = sessionService.session.edvs[edvId] ?: throw IllegalArgumentException("Unknown EDV ID")
    val sessionDid = sessionService.session.did

    fun delegate(edvId: String, childDid: String): String =
        ZCapManager.createCapabilityDelegationZCap(getEdv(edvId).getEdvDelegation(), sessionDid, childDid)

    private val notificationsClient = HttpClient(CIO) {
        install(WebSockets) {
            // Configure WebSockets
        }
    }

    fun createEdv(providerUrl: String): EdvCreatedResponse {
        val clientRemoteService = ClientRemoteService(providerUrl, masterKey, sessionService)

        // ConsoleInterfaceManager.out("Creating new EDV at $providerUrl...")

        val creationResponse = clientRemoteService.createEDV(sessionDid, 0)

        // ConsoleInterfaceManager.out("EDV created: ${creationResponse.edvId}")

        return creationResponse
    }

    val threads = ArrayList<Thread>()
    var disconnecting = false

    fun notificationsConnect(edvId: String, callback: (event: EventMessage) -> Unit) {
        disconnecting = false
        val url = "edvs/${edvId}/notifications"

        val t = thread {
            try {

                runBlocking {
                    notificationsClient.webSocket(
                        method = HttpMethod.Get,
                        host = "localhost", // TODO
                        port = 7000, // TODO
                        path = url
                    ) {
                        val invocationJson = ZCapManager.createSimpleInvocation(
                            getEdv(edvId).getEdvDelegation(), sessionDid, "ConnectNotificationChannel:${edvId}"
                        )

                        val authReq = AuthReq(invocationJson)

                        val authReqJson = JsonUtils.klaxon().toJsonString(authReq)

                        send(authReqJson)

                        while (!disconnecting) {
                            val othersMessage = incoming.receive() as? Frame.Text
                            if (othersMessage != null) {

                                val event = JsonUtils.klaxon().parse<EventMessage>(othersMessage.readText())!!

                                callback.invoke(event)
                            }
                        }
                    }
                }
            } catch (ignored: InterruptedException) {}
        }
        threads.add(t)
    }

    fun notificationsDisconnect() {
        disconnecting = true
        threads.forEach { it.interrupt() }
        if (notificationsClient.isActive) notificationsClient.closeQuietly()
    }
}
