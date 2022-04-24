package id.walt.storagekit.common

import kotlinx.serialization.Serializable

@Serializable
data class DataRequest(
    val context: String,
    val preferredDataType: String,
    val did: String,
    val responseUrl: String
)

@Serializable
data class DataResponse(
    val filesIndex: Map<String, String>,
    val capabilities: List<String>,
    val baseUrl: String,
    val edvId: String
)
