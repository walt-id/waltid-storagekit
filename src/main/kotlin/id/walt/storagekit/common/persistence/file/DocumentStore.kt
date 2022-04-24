package id.walt.storagekit.common.persistence.file

import id.walt.storagekit.common.persistence.PersistenceMechanism
import java.nio.file.Path
import kotlin.io.path.*

class DocumentStore : PersistenceMechanism {

    private fun getDocPath(edvId: String, documentId: String) = Path.of("edvs", edvId, "documents", documentId)

    override fun loadDocument(edvId: String, documentId: String) = getDocPath(edvId, documentId).readText()

    override fun documentExists(edvId: String, documentId: String) = getDocPath(edvId, documentId).exists()

    override fun storeDocument(edvId: String, documentId: String, text: String) = getDocPath(edvId, documentId).writeText(text)

    override fun deleteDocument(edvId: String, documentId: String) = getDocPath(edvId, documentId).deleteIfExists()

    override fun duplicate(edvId: String, documentId: String, newDocumentId: String) =
        getDocPath(edvId, documentId).copyTo(getDocPath(edvId, newDocumentId))
}
