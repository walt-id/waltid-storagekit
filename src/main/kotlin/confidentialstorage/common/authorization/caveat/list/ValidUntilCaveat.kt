package confidentialstorage.common.authorization.caveat.list

import confidentialstorage.common.authorization.ZCap
import confidentialstorage.common.authorization.caveat.Caveat
import confidentialstorage.common.authorization.caveat.CaveatMetadata

data class ValidUntilCaveat(
    val timestamp: Long
) : Caveat(type) {

    companion object : CaveatMetadata(
        type = "ValidUntil"
    )

    override fun verify(zcap: ZCap) = System.currentTimeMillis() < timestamp

}
