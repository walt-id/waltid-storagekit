package confidentialstorage.common.authorization.caveat.list

import confidentialstorage.common.authorization.ZCap
import confidentialstorage.common.authorization.caveat.Caveat
import confidentialstorage.common.authorization.caveat.CaveatMetadata

data class ValidOperationsCaveat(
    val operations: List<String>
) : Caveat(type) {

    companion object : CaveatMetadata(
        type = "ValidOperations"
    )

    override fun verify(zcap: ZCap) = zcap.action!!.split(":").first() in operations

}
