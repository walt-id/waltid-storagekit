package confidentialstorage.common.authorization.caveat

import kotlin.reflect.KClass

object CaveatTypeRegistry {

    class TypeRegistration(
        val caveat: KClass<out Caveat>,
        val metadata: CaveatMetadata,
    )

    private val registry = HashMap<String, TypeRegistration>()

    fun remove(type: String) = registry.remove(type)

    fun contains(type: String) = getRegistration(type) != null

    init {
        Defaults.loadDefaultCaveats()
    }

    private fun registerDefinition(
        type: String,
        registration: TypeRegistration
    ) {
        if (contains(type)) {
            throw CaveatTypeAlreadyRegisteredException(registration)
        }

        registry[type] = registration
    }

    fun register(metadata: CaveatMetadata, caveat: KClass<out Caveat>) {
        registerDefinition(metadata.type, TypeRegistration(caveat, metadata))
    }

    private fun getRegistration(type: String) = registry[type]

    fun getType(type: String): KClass<out Caveat>? = getRegistration(type)?.caveat
    fun getMetadata(type: String): CaveatMetadata = getRegistration(type)!!.metadata

}
