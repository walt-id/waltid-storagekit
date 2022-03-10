package confidentialstorage.client.service

import confidentialstorage.client.clientmodels.SessionManager.Session
import confidentialstorage.client.index.IndexManager

class ClientIndexService(val session: Session, masterKey: ByteArray) {

    private val indexManager = IndexManager(masterKey)

    private fun documentCache(edvId: String, filter: Boolean): List<Pair<String, Int>> {
        val docIndex = indexManager.readIndex(edvId)

        val results = ArrayList<Pair<String, Int>>()

        docIndex.index.keys.run { if (filter) filterNot { it.length > 64 } else this }.forEach { docId ->
            results.add(Pair(docId, docIndex.documents[docId] ?: 0))
        }

        return results
    }

    private fun documentCache(): ArrayList<Pair<String, List<Pair<String, Int>>>> {
        val results = ArrayList<Pair<String, List<Pair<String, Int>>>>()

        session.edvs.forEach { (edvId, _) ->
            results.add(Pair(edvId, documentCache(edvId, false)))
        }

        return results
    }

    fun getTree() = documentCache()
    fun getRaw(edvId: String) = documentCache(edvId, true)
    fun getDocuments(edvId: String) = documentCache(edvId, true)
}
