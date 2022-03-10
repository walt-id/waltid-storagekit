package confidentialstorage.service

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.Payload
import confidentialstorage.client.service.remote.ApiUtils
import confidentialstorage.common.DataRequest
import confidentialstorage.common.DataResponse
import confidentialstorage.common.HashUtils
import confidentialstorage.common.authorization.ZCapManager
import confidentialstorage.common.model.chunking.EncryptedResourceStructure
import confidentialstorage.common.persistence.encryption.JWEEncryption
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.post
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.bitcoinj.core.Base58
import java.util.*

fun sendDocumentRetrieval(edvId: String, documentId: String, baseUrl: String, zcap: String, did: String): String {
    val invocationJson = ZCapManager.createDelegatedInvocation(
        parentZCapJson = zcap,
        invokerDid = did,
        action = "RetrieveDocument:$documentId"
    )

    val client = ApiUtils.getClient("documents", invocationJson)
    return runBlocking {
        client.get<String>("$baseUrl/edvs/$edvId/docs/$documentId")
    }
}

fun retrieveAndOrderChunks(
    edvId: String,
    payload: Payload,
    fileKey: ByteArray,
    baseUrl: String,
    zcap: String,
    did: String
): Payload {
    val chunkList = Klaxon().parseArray<String>(payload.toString())!!
        .map { it.split("?hl=") }
        .map { Pair(it[0], it[1]) }

    val arr = ArrayList<String>()

    chunkList.forEach {
        val doc = sendDocumentRetrieval(edvId, it.first, baseUrl, zcap, did)

        check(HashUtils.computeContentSha(doc) == it.second)

        arr.add(doc)
    }

    val decrypted = arr.map { JWEEncryption.directDecrypt(it, fileKey) }
        .flatMap { it.toBytes().toList() }.toByteArray()

    return Payload(decrypted)
}

fun retrieveDocument(
    edvId: String,
    documentId: String,
    fileKey: ByteArray,
    baseUrl: String,
    zcap: String,
    did: String
): Payload {
    val resultText = sendDocumentRetrieval(edvId, documentId, baseUrl, zcap, did)

    val ers = Klaxon().parse<EncryptedResourceStructure>(resultText)!!

    val inlinePayload = ers.chunkCount != null

    val ersJWE = ers.payload

    val payload = JWEEncryption.directDecrypt(ersJWE, fileKey)

    return if (inlinePayload) payload else retrieveAndOrderChunks(edvId, payload, fileKey, baseUrl, zcap, did)
}

fun main() {
    ServiceMatrix("service-matrix.properties")

    val requests = HashMap<String, DataRequest>()

    println("[Service example]")
    println()

    println("Setting up service...")

    val keyService = KeyService.getService()
    println("Creating key pair...")
    val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
    println()
    println("Key pair created: $keyId")

    val pem = keyService.toPem(keyId.id, KeyType.PUBLIC)
    println("New public key:")
    println(pem)

    println()
    println("Creating service DID...")

    val did = DidService.create(DidMethod.key, keyId.id)
    println("New service DID: $did")

    Javalin.create {
        it.enableDevLogging()
    }.routes {
        post("/accept-request/{id}") {
            val resp = it.bodyAsClass<DataResponse>()
            println("Our request was accepted, querying EDV...")
            //println(resp)

            val baseUrl = resp.baseUrl
            val edvId = resp.edvId
            val docId = resp.filesIndex.keys.first()
            val zcap = resp.capabilities.first()
            val fileKey = resp.filesIndex.values.first()

            val doc = retrieveDocument(edvId, docId, Base58.decode(fileKey), baseUrl, zcap, did)

            println(doc.toString())
        }
    }.start(8000)


    println()
    println("-------------")

    println("Generating data request...")
    val reqId = UUID.randomUUID().toString()

    val req = DataRequest("Job application", "EuropassCredential", did, "http://localhost:8000/accept-request/$reqId")
    requests[reqId] = req

    println("Data request $reqId:")
    //println(req)

    val json = Klaxon().toJsonString(req)

    val signed = JwtService.getService().sign(keyId.id, json)

    // println(JwtService.getService().verify(signed))

    println("JWS: $signed")

}
