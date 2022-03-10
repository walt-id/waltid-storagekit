package confidentialstorage.server.web.edv

import confidentialstorage.common.model.edv.EdvCreatedResponse
import confidentialstorage.common.model.edv.EdvCreationRequest
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document

object EdvController {

    private const val EDV_URL = "/encrypted-data-vaults/"

    fun createEdv(ctx: Context) {
        val req = ctx.bodyAsClass<EdvCreationRequest>()

        val createdResponse = EdvService.createEdv(req)

        val createdEdvUrl = EDV_URL + createdResponse.edvId
        ctx.redirect(createdEdvUrl, 200)

        ctx.json(createdResponse)
    }

    fun createEdvDocs() = document().operation {
        it.summary("Create an Encrypted Data Vault").operationId("createEdv")
            .addTagsItem("Encrypted Data Vaults")
    }.body<EdvCreationRequest>().result<EdvCreatedResponse>("200") {
        it.description("Created; EDV URL is set in 'location' header")
    }.result<Int>("400") {
        it.description("Data vault creation failed")
    }.result<Int>("409") {
        it.description("A duplicate data vault exists")
    }
}
