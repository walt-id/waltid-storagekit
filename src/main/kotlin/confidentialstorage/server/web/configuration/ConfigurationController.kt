package confidentialstorage.server.web.configuration

import confidentialstorage.server.Configuration
import io.javalin.http.Context

object ConfigurationController {

    fun getConfiguration(ctx: Context) {
        ctx.json(Configuration.serverConfiguration)
    }
}
