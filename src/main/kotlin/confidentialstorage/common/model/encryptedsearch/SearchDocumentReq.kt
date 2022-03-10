package confidentialstorage.common.model.encryptedsearch

import kotlinx.serialization.Serializable

@Serializable
data class SearchDocumentReq(
    val keyword: String,
    val indexKey: String
)
