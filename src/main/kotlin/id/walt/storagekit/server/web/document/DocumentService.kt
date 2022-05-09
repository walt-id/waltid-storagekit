package id.walt.storagekit.server.web.document

import com.beust.klaxon.Klaxon
import id.walt.storagekit.common.hashindexes.HashBasedIndex
import id.walt.storagekit.common.model.encryptedsearch.SearchDocumentReq
import id.walt.storagekit.common.model.encryptedsearch.SearchDocumentRes
import id.walt.storagekit.common.persistence.encryption.JWEEncryption
import id.walt.storagekit.common.persistence.file.DocumentStore
import id.walt.storagekit.server.utils.IdentifierUtils.generateDocumentIdentifier
import io.ipfs.multibase.Base58
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.util.*
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

object DocumentService {

    val hkvs = DocumentStore()

    @Serializable
    data class DocumentCreationResponse(val docId: String)

    fun exists(edvId: String, docId: String) = hkvs.documentExists(edvId, docId)

    fun createDocument(
        edvId: String,
        content: String,
        optionalDocId: String? = null,
        index: String
    ): DocumentCreationResponse {
        val docId = optionalDocId ?: generateDocumentIdentifier()

        //val newDoc = Document(docId, content, index)

        saveDocument(edvId, docId, content)

        return DocumentCreationResponse(docId)
    }

    fun updateDocumentContent(edvId: String, docId: String, content: String, sequence: Int) {
        check(sequence > 0) { "Cannot create a document by updating it (sequence = 0)" }
        hkvs.duplicate(edvId, docId, "$docId-${sequence - 1}")
        hkvs.storeDocument(edvId, docId, content)
    }

    private fun saveDocument(edvId: String, docId: String, document: String) =
        hkvs.storeDocument(edvId, docId, document)

    fun retrieveDocumentContent(edvId: String, docId: String): String = loadDocument(edvId, docId)//.content

    private fun loadRawDocumentJson(edvId: String, docId: String): String = hkvs.loadDocument(edvId, docId)
    private fun loadDocument(edvId: String, docId: String): String =
        /*jsonParser.parse<Document>(*/loadRawDocumentJson(edvId, docId)//)!!

    fun deleteDocument(edvId: String, docId: String): Boolean = hkvs.deleteDocument(edvId, docId)

    fun searchDocument(req: SearchDocumentReq, edvId: String): SearchDocumentRes {
        // val key = Base64.getDecoder().decode(req.indexKey)
        val indexes = Path.of("edvs", edvId, "documents")
            .listDirectoryEntries()
            .filter { it.toString().endsWith("-search-index") }
        //.map { it.readText() }
        //.map { Base64.getDecoder().decode(it) }

        /*
        println("Searching in " + Path.of("edvs", edvId, "documents")
            .listDirectoryEntries()
            .onEach { println(it.toString()) }
            .filter { it.toString().endsWith("search-index") }
            //.filter { it.endsWith("-search-index") }
        )
        */

        if (indexes.isEmpty())
            return SearchDocumentRes(emptyList())

        val resultingDocumentIds = ArrayList<String>()

        // println("Searching with ${req.indexKey}")

        indexes.forEach {
            val decryptedIndex = JWEEncryption.passphraseDecrypt(it.readText(), Base58.decode(req.indexKey)).toString()
            println(decryptedIndex)
            val hashBasedIndex = Klaxon().parse<HashBasedIndex>(decryptedIndex)!!
            if (req.keyword in hashBasedIndex.keywords)
                resultingDocumentIds.add(hashBasedIndex.documentId)
        }

        return SearchDocumentRes(resultingDocumentIds)
        //return SearchDocumentRes(emptyList()) // TODO encrypted search
    }

    //fun getDocumentDid(docId : String)
}
