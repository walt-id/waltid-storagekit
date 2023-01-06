package id.walt.storagekit.common.authorization

import com.beust.klaxon.*
import id.walt.storagekit.common.authorization.caveat.Caveat
import id.walt.storagekit.common.authorization.caveat.list.ValidOperationTargetsCaveat
import id.walt.storagekit.common.authorization.caveat.list.ValidOperationsCaveat
import id.walt.storagekit.common.authorization.caveat.list.ValidUntilCaveat
import id.walt.storagekit.common.utils.JsonUtils.klaxon
import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDObject
import id.walt.crypto.LdSigner
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.services.keystore.KeyStoreService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import java.net.URI
import java.util.*

object ZCapManager {

    private val keyStore: KeyStoreService
        get() = ContextManager.keyStore

    private fun generateUrnUUID() = "urn:uuid:${UUID.randomUUID()}"

    private fun sign(json: String, config: ProofConfig): String {
        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(json)
        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val key = keyStore.load(config.issuerDid)

        val signer = LdSigner.Ed25519Signature2018(key.keyId)

        signer.creator = URI.create(config.issuerDid)
        signer.created = Date() // Use the current date
        //signer.domain = config.domain ?: TrustedIssuerClient.domain
        //signer.nonce = config.nonce ?: EssifServer.nonce
        config.issuerVerificationMethod?.let { signer.verificationMethod = URI.create(config.issuerVerificationMethod) }
        signer.proofPurpose = config.proofPurpose

        signer.sign(jsonLdObject)

        // Fix: this hack is needed as, signature-ld encodes type-field as array, which is not correct
        // return correctProofStructure(proof, jsonCred)
        return jsonLdObject.toJson(true)
    }

    fun verify(json: String, issuerDid: String): Boolean {
        //println("Verify for $issuerDid")
        //println(json.prettyPrint())

        val resolvedDid = DidService.resolve(issuerDid)
        if (keyStore.getKeyId(resolvedDid.id) == null) {
            println("importing key...")
            DidService.importDidAndKeys(resolvedDid.id)
        }

        val publicKey = keyStore.load(resolvedDid.id)

        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true

        val jsonLdObject = JsonLDObject.fromJson(json)
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val verifier = id.walt.crypto.LdVerifier.Ed25519Signature2018(publicKey)

        return verifier.verify(jsonLdObject)
    }

    /*
    ZCap1: Delegation to owner (by provider?)
    ZCap2: Delegation to other (by owner)
    ZCap3: Invocation by other (by other)
    */

    /**
     * capabilityDelegation to owner/provider, by the provider
     * @param edvId: ID used in the URL
     * @param controllerDid: The controller that gets permissions delegated to by the provider
     * @param edvDid: The DID of the EDV (root-of-trust, trusted by remote because it's essentially metadata)
     */
    fun createInitialEdvDelegationZCap(rootObject: String, controllerDid: String, edvDid: String): String {
        val zcapId = generateUrnUUID()

        val zCap = ZCap(
            id = zcapId,
            rootCapability = rootObject,
            invoker = controllerDid,
            //capabilityChain = listOf(edvUrl)
        )

        val zCapJson = klaxon().toJsonString(zCap)

        val conf = ProofConfig(
            issuerDid = edvDid,
            proofPurpose = "capabilityDelegation",
            proofType = ProofType.LD_PROOF,
            domain = null,
            nonce = null,
            issuerVerificationMethod = getDidVerification(controllerDid)
        )

        return sign(zCapJson, conf)
    }

    /**
     * capabilityDelegation to a child (from an owner/controller)
     * @param parentZCapJson Signed parentCapability ZCap (that grants the owner/controller the permissions)
     * @param childDid The DID we are delegating permissions to
     */
    fun createCapabilityDelegationZCap(
        parentZCapJson: String,
        ownerDid: String,
        childDid: String,
        caveats: List<Caveat>? = null
    ): String {
        val zcapId = generateUrnUUID()

        val parentZCap = klaxon().parse<ZCap>(parentZCapJson)!!

        val zCap = ZCap(
            id = zcapId,
            parentCapability = parentZCap,
            invoker = childDid,
            caveat = caveats
        )

        val zCapJson = klaxon().toJsonString(zCap)

        val conf = ProofConfig(
            issuerDid = ownerDid, // FIXME parent.Invoker?
            proofPurpose = "capabilityDelegation",
            proofType = ProofType.LD_PROOF,
            domain = null,
            nonce = null,
            //issuerVerificationMethod = DidService.getAuthenticationMethods(invokerDid)!![0]
            //issuerVerificationMethod = DidService.getAuthenticationMethods(childDid)!![0] // FIXME
            issuerVerificationMethod = getDidVerification(childDid) // FIXME thought up
        )

        return sign(zCapJson, conf)
    }

    /**
     * Owner itself invokes
     * @param action: The action, e.g. CreateDocument:DocumentID:SHA256ofContent
     */
    fun createSimpleInvocation(delegationZcapJson: String, invokerDid: String, action: String): String {
        val zcapId = generateUrnUUID()

        val delegatedZCap = klaxon().parse<ZCap>(delegationZcapJson)!!

        val zCap = ZCap(
            id = zcapId,
            action = action,
            parentCapability = delegatedZCap,
            invoker = invokerDid
        )

        val zCapJson = klaxon().toJsonString(zCap)

        val conf = ProofConfig(
            issuerDid = invokerDid,
            proofPurpose = "capabilityInvocation",
            proofType = ProofType.LD_PROOF,
            domain = null,
            nonce = null,
            issuerVerificationMethod = getDidVerification(invokerDid)
        )

        return sign(zCapJson, conf)
    }

    fun getDidVerification(did: String): String {
        //if (!DidService.listDids().contains(did))
        //    DidService.importDid(did)
        /*val didObj = */ DidService.loadOrResolveAnyDid(did)

        return DidService.getAuthenticationMethods(did)!!.first().id
    }

    fun rawSignatureChainVerification(
        invocationJson: String,
        did: String,
        edvDid: String,
        rootObject: String
    ): LinkedHashMap<ZCap, Boolean> {
        val chain = LinkedHashMap<ZCap, Boolean>()

        //println(invocationJson)
        var currentZCap = klaxon().parse<ZCap>(invocationJson)!!
        var currentDid = did

        var counter = 0
        while (currentZCap.rootCapability == null) {
            println("Verifying zcap $counter for $currentDid")
            counter++
            if (counter > 50) throw StackOverflowError("Too many ZCaps in chain!")

            val currentZCapJson = klaxon().toJsonString(currentZCap)

            chain[currentZCap] = verify(currentZCapJson, currentDid)

            currentZCap = currentZCap.parentCapability!!
            currentDid = currentZCap.proof!!.creator!!
        }

        println("Verifying root zcap")
        chain[currentZCap] = verify(klaxon().toJsonString(currentZCap), edvDid) && currentZCap.rootCapability == rootObject

        return chain
    }

    fun chainVerification(invocationJson: String, did: String, edvDid: String, rootObject: String): Boolean {
        val chain = rawSignatureChainVerification(invocationJson, did, edvDid, rootObject)
        println("Raw signature down, pointer matching:")

        val reverseChain = chain.map { it.key }.reversed()

        val collectedCaveats = ArrayList<Caveat>()

        var index = 0
        reverseChain.forEach { zcap ->
            println("= $index ${zcap.proof!!.proofPurpose}")
            if (zcap.rootCapability != null && zcap.rootCapability == rootObject) {
                println("EDV BASE START")
                println("EDV DID $edvDid - ROOT OF TRUST")
            } else {
                val askingKeys = DidService.getAuthenticationMethods(zcap.proof.creator!!)!!.first().id
                val allowedKeys = reverseChain[index - 1].proof!!.verificationMethod
                val grantedBy = zcap.parentCapability!!.proof!!.creator

                println("Asking keys:  $askingKeys")
                println("Allowed keys: $allowedKeys")
                println("Granted by:   ${grantedBy}${if (grantedBy == edvDid) " [ROOT OF TRUST]" else ""}")

                check(askingKeys == allowedKeys)

                if (zcap.caveat != null) {
                    println("Collected new caveat/s: ${zcap.caveat}")
                    collectedCaveats.addAll(zcap.caveat)
                }
            }

            if (index + 1 < reverseChain.size) //
                check(zcap.proof.proofPurpose == "capabilityDelegation")

            //println(klaxon().toJsonString(zcap).prettyPrint())


            index++
        }

        if (chain.keys.first().proof!!.proofPurpose == "capabilityInvocation") {
            val invocation = chain.keys.first()

            collectedCaveats.forEachIndexed { index2, caveat ->
                val caveatCheck = caveat.verify(invocation)

                println("Caveat check ${index2 + 1}/${collectedCaveats.size}: ${caveat.type} - $caveatCheck")
                check(caveatCheck)
            }
        }

        return true
    }

    private val blackList = HashMap<String, Boolean>() // TODO Cache on file system

    fun isIdBlackListed(id: String): Boolean = blackList.getOrDefault(id, false)
    fun blackListId(id: String) = blackList.set(id, true)

    /**
     * Delegated invocation (child invokes action)
     * @param chain Capability chain (with the previous delegations)
     * @param invokerDid Child DID
     * @param action The action, e.g. CreateDocument:DocumentID:SHA256ofContent
     */
    fun createDelegatedInvocation(parentZCapJson: String, invokerDid: String, action: String): String {
        val zcapId = generateUrnUUID()

        val zCap = ZCap(
            id = zcapId,
            action = action,
            //capabilityChain = chain,
            invoker = invokerDid,
            parentCapability = klaxon().parse<ZCap>(parentZCapJson)!! // TODO
        )

        val zCapJson = klaxon().toJsonString(zCap)

        val conf = ProofConfig(
            issuerDid = invokerDid,
            proofPurpose = "capabilityInvocation",
            proofType = ProofType.LD_PROOF,
            domain = null,
            nonce = null,
            issuerVerificationMethod = DidService.getAuthenticationMethods(invokerDid)!!.first().id
        )

        return sign(zCapJson, conf)
    }
}

fun main() {
    ServiceMatrix("service-matrix.properties")

    println("1. [Creating EDV]")

    val controllerDid = DidService.create(DidMethod.key)

    println("OwnerDid $controllerDid")
    //println("Owner sends EdvCreationRequest with controller $ownerDid")
    //println()

    val edvDid = DidService.create(DidMethod.key)

    println("EDV is getting generated, EDV-DID $edvDid")

    //println("1. [EDV delegation from provider to owner]")

    val json = ZCapManager.createInitialEdvDelegationZCap("http://localhost:7000/edvs/EDV1ABC", controllerDid, edvDid)
    ZCapManager.chainVerification(json, controllerDid, edvDid, "http://localhost:7000/edvs/EDV1ABC")
    //println(ZCapManager.verifySimpleInvocation(json, ownerDid, edvDid, "http://localhost:7000/edvs/EDV1ABC"))

    //println("ZCap: ")
    //println(json)

    //println(ZCapManager.verify(json, edvDid))

    println("2. [EDV invocation from owner]")

    val invocationJson = ZCapManager.createSimpleInvocation(json, controllerDid, "CreateDocument:DocumentID:SHA256ofContent")
    ZCapManager.chainVerification(invocationJson, controllerDid, edvDid, "http://localhost:7000/edvs/EDV1ABC")
    //println(ZCapManager.verifySimpleInvocation(invocationJson, ownerDid, edvDid, "http://localhost:7000/edvs/EDV1ABC"))

    println("3. [EDV delegation from owner to child]")

    println("Creating delegation:")

    val childDid = DidService.create(DidMethod.key)
    println("ChildDid $childDid")

    val caveats = listOf(
        ValidOperationsCaveat(listOf("CreateDocument")),
        ValidOperationTargetsCaveat(listOf("DocumentID")),
        ValidUntilCaveat(System.currentTimeMillis() + 5000)
    )

    val json2 = ZCapManager.createCapabilityDelegationZCap(json, controllerDid, childDid, caveats)
    //println(json2)
    ZCapManager.chainVerification(json2, controllerDid, edvDid, "http://localhost:7000/edvs/EDV1ABC")
    //ZCapManager.verifySimpleInvocation(json2, ownerDid, edvDid, "http://localhost:7000/edvs/EDV1ABC")
    //println(json2)

    println("4. [EDV invocation from child]")

    //val delegatedInvocation =
    //  ZCapManager.createDelegatedInvocation(json2, childDid, "CreateDocument:DocumentID:SHA256ofContent")
    //println(delegatedInvocation)

    //println(delegatedInvocation)

    //ZCapManager.chainVerification(delegatedInvocation, childDid, edvDid, "http://localhost:7000/edvs/EDV1ABC")
    //println(ZCapManager.verifySimpleInvocation(delegatedInvocation, childDid, edvDid, "http://localhost:7000/edvs/EDV1ABC"))

    val childDid2 = DidService.create(DidMethod.key)
    val json3 = ZCapManager.createCapabilityDelegationZCap(json2, childDid, childDid2)

    val delegatedInvocationJson =
        ZCapManager.createDelegatedInvocation(json3, childDid2, "CreateDocument:DocumentID:SHA256ofContent")
    println(delegatedInvocationJson)
    println(
        "\nOVERALL RESULT: " + ZCapManager.chainVerification(
            delegatedInvocationJson,
            childDid2,
            edvDid,
            "http://localhost:7000/edvs/EDV1ABC"
        )
    )
}
