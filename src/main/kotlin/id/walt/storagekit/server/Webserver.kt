package id.walt.storagekit.server

import com.beust.klaxon.Klaxon
import id.walt.storagekit.client.console.ConsoleInterfaceManager
import id.walt.storagekit.common.authorization.validation.ValidationException
import id.walt.storagekit.server.Configuration.serverConfiguration
import id.walt.storagekit.server.web.capabilities.CapabilityController
import id.walt.storagekit.server.web.configuration.ConfigurationController
import id.walt.storagekit.server.web.document.DocumentController
import id.walt.storagekit.server.web.edv.EdvController
import id.walt.storagekit.server.web.edv.notifications.NotificationController
import id.walt.common.prettyPrint
import id.walt.servicematrix.ServiceMatrix
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.utils.OpenApiVersionUtil
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class Webserver {

    private val javalin: Javalin = Javalin.create {
        OpenApiVersionUtil.logWarnings = false
        it.apply {
            registerPlugin(RouteOverviewPlugin("/api-routes"))
            registerPlugin(Configuration.openApiPlugin)

            requestLogger { ctx, executionTimeMs ->
                println("[WWW] ${ctx.method()} ${ctx.path()} (${ctx.status()})")
            }
            //enableDevLogging()
            enableCorsForAllOrigins()
        }
    }.routes {
        path("/edvs") {
            post("", documented(EdvController.createEdvDocs(), EdvController::createEdv))

            path("/{edv-id}") {

                ws("/notifications", NotificationController())

                path("/docs") {
                    post("", documented(DocumentController.createDocumentDocs(), DocumentController::createDocument))
                    post("/search", documented(DocumentController.searchDocumentDocs(), DocumentController::searchDocument))

                    path("/{doc-id}") {
                        get("", documented(DocumentController.getDocumentDocs(), DocumentController::getDocument))
                        patch("", documented(DocumentController.updateDocumentDocs(), DocumentController::updateDocument))
                        delete("", documented(DocumentController.deleteDocumentDocs(), DocumentController::deleteDocument))
                    }
                }
            }
        }
        get("capabilities", CapabilityController::getCapabilities)
        get("configuration", ConfigurationController::getConfiguration)
    }.exception(ValidationException.DocumentAlreadyExists::class.java) { e, ctx ->
        ctx.status(400)
        ctx.result(e.message!!)
    }.exception(ValidationException.DocumentNotFound::class.java) { e, ctx ->
        ctx.status(404)
        ctx.result(e.message!!)
    }

    fun start() {
        javalin.start(serverConfiguration.port)
    }
}

fun main(args: Array<String>) {
    println(ConsoleInterfaceManager.boldString("[walt.id Storage Kit]"))
    println("Remote Node")

    ServiceMatrix("service-matrix.properties")

    if (args.isNotEmpty()) {
        val path = args.first()
        println("Loading config \"$path\"...")

        val config = Klaxon().parse<ServerConfiguration>(Path(path).readText())!!

        serverConfiguration = config
    } else {
        println("Generating default config...")
        val json = Klaxon().toJsonString(ServerConfiguration()).prettyPrint()
        Path("config.json").writeText(json)
    }

    Webserver().start()
}
