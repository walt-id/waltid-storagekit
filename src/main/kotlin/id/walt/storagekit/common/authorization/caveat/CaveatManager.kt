package id.walt.storagekit.common.authorization.caveat

import com.beust.klaxon.Klaxon
import kotlin.reflect.KClass

object CaveatManager {
    private val klaxon = Klaxon()

    fun getCaveat(json: String): Caveat {
        return klaxon.parse<Caveat>(json)!!
    }

    fun register(metadata: CaveatMetadata, vc: KClass<out Caveat>) = CaveatTypeRegistry.register(metadata, vc)
    inline fun <reified T : Caveat> register(metadata: CaveatMetadata) = register(metadata, T::class)
}
