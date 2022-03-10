package confidentialstorage.server.web.capabilities

import io.javalin.http.Context

object CapabilityController {


    fun getCapabilities(ctx: Context) {
        val res = CapabilityService.generateCapability()

        ctx.json(res)
    }
}
