package id.walt.storagekit.server.web.edv

import id.walt.storagekit.common.authorization.ZCapManager
import id.walt.storagekit.common.model.edv.EdvCreationRequest
import id.walt.storagekit.server.Configuration.serverConfiguration
import id.walt.storagekit.server.services.DataStorageService
import id.walt.storagekit.server.utils.IdentifierUtils.generateAlphaNumeric
import id.walt.model.DidMethod
import id.walt.services.did.DidService
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

@Serializable
class StoredDataVault(
    val edvId: String,
    val sequence: Int = 0,
    val controller: String,

    val baseStorage: Path = DataStorageService.dataStoragePath,
    val vaultDir: Path = baseStorage.resolve(edvId),
    val metadataFile: Path = vaultDir.resolve("metadata.json"),
    val documentsDir: Path = vaultDir.resolve("documents"),

    val administrationToken: String = generateAlphaNumeric(64),
    val edvDid: String = DidService.create(DidMethod.key),
    val initialZCapDelegation: String = ZCapManager.createInitialEdvDelegationZCap(
        rootObject = "${serverConfiguration.url}/edvs/$edvId",
        controllerDid = controller,
        edvDid = edvDid
    )
) {

    constructor(id: String, req: EdvCreationRequest) : this(id, req.sequence, req.controller)

    fun create() {
        DataStorageService.createAndStoreDataVault(this)
    }

    fun listDocuments() = documentsDir.listDirectoryEntries().map { it.nameWithoutExtension }

    fun hasDocument(id: String) = documentsDir.resolve(id).exists()
    fun getDocument(id: String) = documentsDir.resolve(id).readText()

}
