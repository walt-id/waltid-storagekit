package id.walt.storagekit.common.authorization

import com.beust.klaxon.Json
import com.fasterxml.jackson.annotation.JsonProperty
import id.walt.storagekit.common.authorization.caveat.Caveat
import java.time.Instant

data class ZCap(
    @JsonProperty("@context")
    @Json(name = "@context")
    val context: List<String> = listOf(/*"https://w3id.org/security/v2"*/),
    val id: String, // urn:uuid:ad86cb2c-e9db-434a-beae-71b82120a8a4,
    @Json(serializeNull = false) val action: String? = null,
    /** Either EDV-ID / Document-ID, or ZCap */
    @Json(serializeNull = false) val parentCapability: ZCap? = null, // DELEGATION
    @Json(serializeNull = false) val rootCapability: String? = null,
    //@Json(serializeNull = false) val capability: ZCap? = null, // INVOCATION
    /** did:key either of parent (edv controller) or child  (for delegation: who we are delegating to) */
    @Json(serializeNull = false) val invoker: String? = null,
    @Json(serializeNull = false) val caveat: List<Caveat>? = null,
    ///** only relevant for capabilityInvocation */
    //@Json(serializeNull = false) val capabilityChain: List<String>? = null,
    @Json(serializeNull = false) val proof: Proof? = null
) {
    data class Proof(
        val type: String, // Ed25519Signature2020
        /** capabilityInvocation / capabilityDelegation */
        val proofPurpose: String,
        val created: Instant, // 2018-02-13T21:27:09Z // TODO Check if custom en/decoder needed for Instant
        /*
         * Only exists for first initial ZCap in Chain,
         * in which case it will hold one item, the parentCapability
         */
        //val capabilityChain: List<String>? = null, // always at top
        /** Key of creator, only relevant for 2+ (in chain) */
        val creator: String? = null, // did:example:child#key1

        @Json(serializeNull = false) val verificationMethod: String? = null,

        val jws: String
    )
}
