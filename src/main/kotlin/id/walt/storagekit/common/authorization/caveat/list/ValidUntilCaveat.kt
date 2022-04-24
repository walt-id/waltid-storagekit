package id.walt.storagekit.common.authorization.caveat.list

import id.walt.storagekit.common.authorization.ZCap
import id.walt.storagekit.common.authorization.caveat.Caveat
import id.walt.storagekit.common.authorization.caveat.CaveatMetadata

data class ValidUntilCaveat(
    val timestamp: Long
) : Caveat(type) {

    companion object : CaveatMetadata(
        type = "ValidUntil"
    )

    override fun verify(zcap: ZCap) = System.currentTimeMillis() < timestamp

}
