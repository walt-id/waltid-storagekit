package id.walt.storagekit.server.web.capabilities


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Capability(
    @SerialName("@context")
    var context: String, // https://w3id.org/encrypted-data-vaults/v1
    @SerialName("dataVaultCreationService")
    var dataVaultCreationService: String, // https://example.com/data-vaults
    @SerialName("id")
    var id: String, // https://example.com/
    @SerialName("name")
    var name: String // Example Website
)
