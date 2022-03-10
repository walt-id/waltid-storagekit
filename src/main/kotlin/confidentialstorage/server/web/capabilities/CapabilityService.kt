package confidentialstorage.server.web.capabilities

import confidentialstorage.server.Configuration.serverConfiguration

object CapabilityService {

    fun generateCapability(): Capability = Capability(
        context = serverConfiguration.url + "/capabilities",
        dataVaultCreationService = serverConfiguration.url + "/edvs",
        id = serverConfiguration.url,
        name = serverConfiguration.name
    )
}
