package id.walt.storagekit.common.authorization.caveat.list

import id.walt.storagekit.common.authorization.ZCap
import id.walt.storagekit.common.authorization.caveat.Caveat
import id.walt.storagekit.common.authorization.caveat.CaveatMetadata

data class ValidOperationsCaveat(
    val operations: List<String>
) : Caveat(type) {

    companion object : CaveatMetadata(
        type = "ValidOperations"
    )

    override fun verify(zcap: ZCap) = zcap.action!!.split(":").first() in operations

}
