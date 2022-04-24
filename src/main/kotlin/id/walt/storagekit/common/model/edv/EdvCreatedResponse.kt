package id.walt.storagekit.common.model.edv

import kotlinx.serialization.Serializable

@Serializable
data class EdvCreatedResponse(
    val edvId: String,
    val edvDid: String,
    val rootDelegation: String
)
