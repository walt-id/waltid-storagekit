package id.walt.storagekit.common.authorization.caveat.list

import id.walt.storagekit.common.authorization.ZCap
import id.walt.storagekit.common.authorization.caveat.Caveat
import id.walt.storagekit.common.authorization.caveat.CaveatMetadata

data class ValidOperationTargetsCaveat(
    val targets: List<String>
) : Caveat(type) {

    companion object : CaveatMetadata(
        type = "ValidOperationTargets"
    )

    override fun verify(zcap: ZCap) = zcap.action!!.split(":").component2() in targets

}
