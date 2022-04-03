package confidentialstorage.server.web.configuration

import io.javalin.http.Context

object ConfigurationController {

    fun getConfiguration(ctx: Context) {
        ctx.json(ConfigurationService.getConfiguration())
    }
}
