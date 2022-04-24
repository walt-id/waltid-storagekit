package id.walt.storagekit.server.web.edv

import id.walt.storagekit.common.model.edv.EdvCreatedResponse
import id.walt.storagekit.common.model.edv.EdvCreationRequest
import id.walt.storagekit.server.services.DataStorageService
import id.walt.storagekit.server.utils.IdentifierUtils.generateEdvIdentifier
import id.walt.storagekit.server.utils.JsonUtils
import kotlin.io.path.notExists
import kotlin.io.path.readText

object EdvService {

    fun createEdv(req: EdvCreationRequest): EdvCreatedResponse {
        val id = generateEdvIdentifier()

        val newDataVault = StoredDataVault(id, req)

        newDataVault.create()

        return EdvCreatedResponse(newDataVault.edvId, newDataVault.administrationToken, newDataVault.initialZCapDelegation)
    }

    fun getEdv(id: String): StoredDataVault? {
        val edvMetadata = DataStorageService.dataStoragePath.resolve(id).resolve("metadata.json")
        if (edvMetadata.notExists())
            return null

        return JsonUtils.jsonParser.parse<StoredDataVault>(edvMetadata.readText())
    }

    fun isCorrectAdministrationToken(edvId: String, administrationToken: String) =
        getEdv(edvId)!!.administrationToken == administrationToken

    fun getEdvDid(edvId: String) = getEdv(edvId)!!.edvDid
}
