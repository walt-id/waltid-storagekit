package id.walt.storagekit.server.web.capabilities

import id.walt.storagekit.server.Configuration.serverConfiguration

object CapabilityService {

    fun generateCapability(): Capability = Capability(
        context = serverConfiguration.url + "/capabilities",
        dataVaultCreationService = serverConfiguration.url + "/edvs",
        id = serverConfiguration.url,
        name = serverConfiguration.name
    )
}
