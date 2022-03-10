package confidentialstorage.common.hashindexes

import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash

object HashSearch {

    fun calculateHash(keyword: ByteArray, key: ByteArray, context: ByteArray): String =
        Base58.encode(Sha256Hash.hash(keyword + key + context))

}