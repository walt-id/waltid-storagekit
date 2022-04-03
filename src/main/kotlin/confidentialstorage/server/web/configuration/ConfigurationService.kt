package confidentialstorage.server.web.configuration

import confidentialstorage.server.Configuration

object ConfigurationService {

    fun getConfiguration() = Configuration.serverConfiguration
}
