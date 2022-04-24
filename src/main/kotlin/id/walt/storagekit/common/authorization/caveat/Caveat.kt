package id.walt.storagekit.common.authorization.caveat

import com.beust.klaxon.TypeFor
import id.walt.storagekit.common.authorization.ZCap
import id.walt.storagekit.common.authorization.caveat.adapter.CaveatTypeAdapter

@TypeFor(field = "type", adapter = CaveatTypeAdapter::class)
abstract class Caveat(val type: String) {
    abstract fun verify(zcap: ZCap): Boolean
}

