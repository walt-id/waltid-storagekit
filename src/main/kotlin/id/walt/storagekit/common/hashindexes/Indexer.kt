package id.walt.storagekit.common.hashindexes

object Indexer {

    fun getKeywords(text: String, fileType: String) = text.split(" ", "\n").filterNot { it.isBlank() }

}
