package id.walt.storagekit.common.persistence

import java.nio.file.Path

interface PersistenceMechanism {
    fun loadDocument(edvId: String, documentId: String): String
    fun storeDocument(edvId: String, documentId: String, text: String)
    fun deleteDocument(edvId: String, documentId: String): Boolean
    fun documentExists(edvId: String, documentId: String): Boolean
    fun duplicate(edvId: String, documentId: String, newDocumentId: String): Path
}
