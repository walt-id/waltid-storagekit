package confidentialstorage.server

import com.beust.klaxon.Klaxon
import confidentialstorage.client.console.ConsoleInterfaceManager
import confidentialstorage.common.authorization.validation.ValidationException
import confidentialstorage.server.Configuration.serverConfiguration
import confidentialstorage.server.web.capabilities.CapabilityController
import confidentialstorage.server.web.configuration.ConfigurationController
import confidentialstorage.server.web.document.DocumentController
import confidentialstorage.server.web.edv.EdvController
import confidentialstorage.server.web.edv.notifications.NotificationController
import id.walt.common.prettyPrint
import id.walt.servicematrix.ServiceMatrix
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.plugin.openapi.dsl.documented
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class Webserver {

    private val javalin: Javalin = Javalin.create {
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
    println(ConsoleInterfaceManager.boldString("[Confidential Storage]"))
    println("Remote Node")
    println("Burgmann & Pavel & Zeiler")
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
