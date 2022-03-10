package confidentialstorage.common.authorization.caveat.list

import confidentialstorage.common.authorization.ZCap
import confidentialstorage.common.authorization.caveat.Caveat
import confidentialstorage.common.authorization.caveat.CaveatMetadata

data class ValidOperationTargetsCaveat(
    val targets: List<String>
) : Caveat(type) {

    companion object : CaveatMetadata(
        type = "ValidOperationTargets"
    )

    override fun verify(zcap: ZCap) = zcap.action!!.split(":").component2() in targets

}
