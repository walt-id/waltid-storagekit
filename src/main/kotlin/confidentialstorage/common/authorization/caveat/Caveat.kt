package confidentialstorage.common.authorization.caveat

import com.beust.klaxon.TypeFor
import confidentialstorage.common.authorization.ZCap
import confidentialstorage.common.authorization.caveat.adapter.CaveatTypeAdapter

@TypeFor(field = "type", adapter = CaveatTypeAdapter::class)
abstract class Caveat(val type: String) {
    abstract fun verify(zcap: ZCap): Boolean
}

