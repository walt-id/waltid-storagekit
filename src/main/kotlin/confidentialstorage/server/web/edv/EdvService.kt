package confidentialstorage.server.web.edv

import confidentialstorage.common.model.edv.EdvCreatedResponse
import confidentialstorage.common.model.edv.EdvCreationRequest
import confidentialstorage.server.services.DataStorageService
import confidentialstorage.server.utils.IdentifierUtils.generateEdvIdentifier
import confidentialstorage.server.utils.JsonUtils
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
