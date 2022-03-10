package confidentialstorage.common.authorization.caveat.adapter

import com.beust.klaxon.TypeAdapter
import confidentialstorage.common.authorization.caveat.Caveat
import confidentialstorage.common.authorization.caveat.CaveatTypeRegistry
import kotlin.reflect.KClass

class CaveatTypeAdapter : TypeAdapter<Caveat> {
    @Suppress("UNCHECKED_CAST")
    override fun classFor(type: Any): KClass<out Caveat> = CaveatTypeRegistry.getType(type as String)
        ?: throw IllegalArgumentException("CaveatTypeAdapter: Unknown caveat type: $type")
}
