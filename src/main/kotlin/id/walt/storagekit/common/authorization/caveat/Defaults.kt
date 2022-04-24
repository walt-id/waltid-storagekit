package id.walt.storagekit.common.authorization.caveat

import id.walt.storagekit.common.authorization.caveat.list.ValidOperationTargetsCaveat
import id.walt.storagekit.common.authorization.caveat.list.ValidOperationsCaveat
import id.walt.storagekit.common.authorization.caveat.list.ValidUntilCaveat

object Defaults {

    private val defaults = lazy {
        CaveatManager.register<ValidOperationsCaveat>(ValidOperationsCaveat)
        CaveatManager.register<ValidOperationTargetsCaveat>(ValidOperationTargetsCaveat)
        CaveatManager.register<ValidUntilCaveat>(ValidUntilCaveat)
    }

    fun loadDefaultCaveats() {
        // Register default types
        if (!defaults.isInitialized()) {
            defaults.value
        }
    }
}
