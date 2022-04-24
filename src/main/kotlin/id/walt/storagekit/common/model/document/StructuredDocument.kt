package id.walt.storagekit.common.model.document

import id.walt.model.DidMethod
import id.walt.services.did.DidService
import kotlinx.serialization.Serializable

@Serializable
data class StructuredDocument(
    val docId: String, // urn:uuid:94684128-c42c-4b28-adb0-aec77bf76044
    val meta: Meta,
    val content: Content,
    val authorizationToken: String,
    val docDid: String = DidService.create(DidMethod.key)
) {
    @Serializable
    data class Meta(
        val created: String // 2019-06-18
    )

    @Serializable
    data class Content(
        val message: String // Hello World!
    )
}
