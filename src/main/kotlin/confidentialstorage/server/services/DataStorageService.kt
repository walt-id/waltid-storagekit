package confidentialstorage.server.services

import com.beust.klaxon.Klaxon
import confidentialstorage.server.web.edv.StoredDataVault
import kotlin.io.path.*

object DataStorageService {

    private val klaxon = Klaxon()
    val dataStoragePath = Path("edvs")

    private fun initFiles(dataVault: StoredDataVault) {
        dataVault.run {
            vaultDir.createDirectories()
            metadataFile.createFile()
            documentsDir.createDirectory()
        }
    }

    fun createAndStoreDataVault(dataVault: StoredDataVault) {
        initFiles(dataVault)

        val json = klaxon.toJsonString(dataVault)

        dataVault.metadataFile.writeText(json)
    }
}
