package id.walt.storagekit.server

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.securityScheme
import io.javalin.plugin.openapi.InitialConfigurationCreator
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfiguration(
    val host: String = "http://localhost",
    val port: Int = 7000,
    val name: String = "Storage Kit",
    val description: String = "Description of this walt.id Storage Kit instance",
    val maxChunkSize: Int = 1_000_000,
    val contact: ContactInformation = ContactInformation(
        name = "Client: SSI Fabric GmbH",
        url = "https://walt.id",
        email = "contact@walt.id"
    )
) {
    @Serializable
    data class ContactInformation(
        var name: String,
        val url: String,
        val email: String
    )

    val url by lazy { "$host:$port" }
}

object Configuration {

    var serverConfiguration = ServerConfiguration()

    val openApiPlugin = OpenApiPlugin(
        OpenApiOptions(
            InitialConfigurationCreator {
                OpenAPI().apply {
                    info {
                        title = serverConfiguration.name
                        description = serverConfiguration.description
                        contact = Contact().apply {
                            name = serverConfiguration.contact.name
                            url = serverConfiguration.contact.url
                            email = serverConfiguration.contact.email
                        }
                        version = "1.0"
                    }
                    servers = listOf(
                        //Server().description("Walt.ID").url("https://storage-kit.walt.id"),
                        Server().description("Local testing server").url(serverConfiguration.url),
                    )
                    externalDocs {
                        description = "Storage Kit Docs"
                        url = "https://docs.walt.id/"
                    }

                    components {
                        /*
                securityScheme {
                    name = "bearerAuth"
                    type = SecurityScheme.Type.HTTP
                    scheme = "bearer"
                    `in` = SecurityScheme.In.HEADER
                    description = "HTTP Bearer Token authentication"
                    bearerFormat = "JWT"
                }
                */
                        securityScheme {
                            name = "basicAuth"
                            type = SecurityScheme.Type.HTTP
                            scheme = "basic"
                            `in` = SecurityScheme.In.HEADER
                            description = "HTTP Basic Auth"
                        }
                    }
                }
            },
        ).apply {
            path("/api-documentation")
            swagger(SwaggerOptions("/swagger").title(serverConfiguration.name))
            reDoc(ReDocOptions("/redoc").title(serverConfiguration.name))
        },
    )
}
