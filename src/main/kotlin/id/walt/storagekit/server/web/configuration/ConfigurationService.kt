package id.walt.storagekit.server.web.configuration

import id.walt.storagekit.server.Configuration

object ConfigurationService {

    fun getConfiguration() = Configuration.serverConfiguration
}
