package id.walt.storagekit.common

import org.apache.commons.codec.digest.DigestUtils
import org.bitcoinj.core.Base58

object HashUtils {
    fun computeContentSha(content: String): String = Base58.encode(DigestUtils.sha256(content))
    fun computeContentSha3(content: String): String = Base58.encode(DigestUtils.sha3_384(content))
}
