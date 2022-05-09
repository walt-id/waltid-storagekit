package id.walt.storagekit.client.service

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.JWSObject
import id.walt.storagekit.client.clientmodels.SessionManager.Session
import id.walt.storagekit.client.console.ConsoleInterfaceManager
import id.walt.storagekit.client.index.IndexManager
import id.walt.storagekit.common.DataRequest
import id.walt.storagekit.common.DataResponse
import id.walt.storagekit.common.authorization.ZCapManager
import id.walt.storagekit.common.authorization.caveat.Caveat
import id.walt.services.jwt.JwtService
import id.walt.storagekit.client.service.remote.ApiUtils
import io.ipfs.multibase.Base58
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

class ClientDataRequestService(private val session: Session, masterKey: ByteArray) {

    private val indexManager = IndexManager(masterKey)

    fun verifyDataRequest(jwe: String) = JwtService.getService().verify(jwe)

    fun decodeDataRequest(requestJWS: String): DataRequest {
        val jws = JWSObject.parse(requestJWS)
        val json = jws.payload.toString()
        return Klaxon().parse<DataRequest>(json)!!
    }

    fun createDataDelegation(edvId: String, childDid: String, caveats: List<Caveat>): String {
        val edv = session.edvs[edvId]!!
        val edvDelegationJson = edv.getEdvDelegation()

        val zcap = ZCapManager.createCapabilityDelegationZCap(
            parentZCapJson = edvDelegationJson,
            ownerDid = session.did,
            childDid = childDid,
            caveats = caveats
        )

        return zcap
    }

    fun acceptDataRequest(dataRequest: DataRequest, edvId: String, dataDelegation: String) {
        val edv = session.edvs[edvId]!!
        val docId = dataRequest.preferredDataType

        val client = ApiUtils.createConfiguredClient()

        val fileKey = Base58.encode(indexManager.getFromIndex(edv.edvId, docId))
        // TODO

        val resp = DataResponse(mapOf(docId to fileKey), listOf(dataDelegation), edv.serverUrl, edv.edvId)

        ConsoleInterfaceManager.out("Transmitting acceptance respones...")

        runBlocking {
            client.post(dataRequest.responseUrl) {
                contentType(ContentType.Application.Json)
                setBody(resp)
            }
        }
    }

}
