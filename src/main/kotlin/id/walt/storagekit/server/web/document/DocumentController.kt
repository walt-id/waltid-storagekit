package id.walt.storagekit.server.web.document

import id.walt.storagekit.common.HashUtils.computeContentSha3
import id.walt.storagekit.common.authorization.ZCap
import id.walt.storagekit.common.authorization.ZCapManager
import id.walt.storagekit.common.authorization.validation.RequestValidation
import id.walt.storagekit.common.authorization.validation.RequestValidation.requestValidation
import id.walt.storagekit.common.model.document.DocumentCreationRequest
import id.walt.storagekit.common.model.encryptedsearch.SearchDocumentReq
import id.walt.storagekit.common.model.encryptedsearch.SearchDocumentRes
import id.walt.storagekit.common.model.notifications.NotificationOperation
import id.walt.storagekit.common.utils.JsonUtils.klaxon
import id.walt.storagekit.server.Configuration.serverConfiguration
import id.walt.storagekit.server.utils.IdentifierUtils.isValidEdvId
import id.walt.storagekit.server.web.document.DocumentService.DocumentCreationResponse
import id.walt.storagekit.server.web.edv.EdvService
import id.walt.storagekit.server.web.edv.notifications.NotificationService
import id.walt.storagekit.server.web.edv.notifications.NotificationService.EdvId
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document
import io.swagger.v3.oas.models.security.SecurityRequirement
import java.time.Instant

object DocumentController {

    fun Context.err(code: Int, message: String): Boolean {
        this.status(code).result(message)
        return false
    }

    private fun Context.createValidate() =
        requestValidation(this, RequestValidation.ValidationScope.BASIC_EDV_DOCUMENT_CREATE)

    private fun Context.updateValidate() =
        requestValidation(this, RequestValidation.ValidationScope.BASIC_EDV_DOCUMENT_UPDATE_DELETE_RETRIEVE)

    fun Context.getEdvId() = pathParam("edv-id")
    fun Context.getDocId() = pathParam("doc-id")

    private fun Context.getZCap() = klaxon().parse<ZCap>(basicAuthCredentials().password)!!
    private fun Context.getInvokerDid(): String = this.getZCap().invoker!!

    private fun Context.isAuthorizedForEdv(action: String): Boolean {
        if (!this.basicAuthCredentialsExist())
            return err(401, "No EDV authorization was added to request.")

        val edvId = getEdvId()

        if (!isValidEdvId(edvId))
            return err(400, "Invalid edv id syntax.")

        EdvService.getEdv(edvId) ?: return err(404, "No such data vault by id ($edvId) found.")

        val pw = basicAuthCredentials().password

        if (pw.length == 64) {
            // Token
            return if (!EdvService.isCorrectAdministrationToken(edvId, pw))
                err(403, "Invalid token for edv ($edvId)")
            else true
        }

        // ZCap

        val edvDid = EdvService.getEdvDid(edvId)
        val rootObject = "${serverConfiguration.url}/edvs/$edvId"

        println(pw)
        val invokerDid = this.getInvokerDid()

        if (!ZCapManager.chainVerification(pw, invokerDid, edvDid, rootObject))
            return err(403, "ZCap could not be verified!")

        val zCap = this.getZCap()

        if (zCap.proof!!.proofPurpose != "capabilityInvocation")
            return err(403, "ZCap is not a invocation!")

        if (zCap.proof.created.epochSecond < (Instant.now().epochSecond - 5))
            return err(403, "ZCap usage window exceeded!")

        if (ZCapManager.isIdBlackListed(zCap.id))
            return err(403, "ZCaps cannot be reused! (nonce not unique)")

        if (zCap.action != action)
            return err(403, "ZCap contains invalid action! (${zCap.action} != $action)")

        ZCapManager.blackListId(zCap.id)
        return true
    }

    private fun Context.isAuthorizedForDocument(): Boolean {
        if (!this.basicAuthCredentialsExist())
            return err(401, "No document authorization was added to request.")

        val edvId = getEdvId()

        if (!isValidEdvId(edvId))
            return err(400, "Invalid edv id syntax")

        EdvService.getEdv(edvId) ?: return err(404, "No such data vault by id ($edvId) found.")

        val docId = getDocId()

        // TODO

        return true
    }

    fun createDocument(ctx: Context) {

        ctx.createValidate()

        val req = ctx.bodyAsClass<DocumentCreationRequest>()

        val contentSha = computeContentSha3(req.content)

        println("Checking for action \"CreateDocument:${req.id}:$contentSha\"...")
        if (!ctx.isAuthorizedForEdv("CreateDocument:${req.id}:$contentSha")) return

        val res = DocumentService.createDocument(ctx.getEdvId(), req.content, req.id, req.index)

        ctx.json(res)

        NotificationService.broadcastNotifications(
            EdvId(ctx.getEdvId()),
            res.docId,
            req.sequence,
            ctx.getInvokerDid(),
            NotificationOperation.CREATE
        )
    }

    fun createDocumentDocs() = document().operation {
        it.operationId("createDocument").summary("Create document in EDV.")
        it.addSecurityItem(SecurityRequirement().addList("http")).addTagsItem("Documents")
    }.body<DocumentCreationRequest>().result<DocumentCreationResponse>("200")

    fun getDocument(ctx: Context) {
        ctx.updateValidate()

        if (!ctx.isAuthorizedForEdv("RetrieveDocument:${ctx.getDocId()}") /*|| !ctx.isAuthorizedForDocument()*/) return

        try {
            ctx.result(DocumentService.retrieveDocumentContent(ctx.getEdvId(), ctx.getDocId()))
            NotificationService.broadcastNotifications(
                EdvId(ctx.getEdvId()),
                ctx.getDocId(),
                -1,
                ctx.getInvokerDid(),
                NotificationOperation.GET
            )
        } catch (e: java.nio.file.NoSuchFileException) {
            ctx.status(404).result("Not found")
        }
    }

    fun getDocumentDocs() = document().operation {
        it.operationId("getDocument").summary("Retrieve document in EDV.")
        it.addSecurityItem(SecurityRequirement().addList("http")).addTagsItem("Documents")
    }.result<String>("200")

    fun updateDocument(ctx: Context) {
        ctx.updateValidate()

        val req = ctx.bodyAsClass<DocumentCreationRequest>()

        val contentSha = computeContentSha3(req.content)

        println("Checking for action \"UpdateDocument:${req.id}:$contentSha\"...")
        if (!ctx.isAuthorizedForEdv("UpdateDocument:${req.id}:$contentSha")) return

        //if (!ctx.isAuthorizedForDocument()) return

        DocumentService.updateDocumentContent(ctx.getEdvId(), ctx.getDocId(), req.content, req.sequence)


        NotificationService.broadcastNotifications(
            EdvId(ctx.getEdvId()),
            req.id!!,
            req.sequence,
            ctx.getInvokerDid(),
            NotificationOperation.UPDATE
        )

        ctx.status(201)
    }

    fun updateDocumentDocs() = document().operation {
        it.operationId("updateDocument").summary("Update document in EDV.")
        it.addSecurityItem(SecurityRequirement().addList("http")).addTagsItem("Documents")
    }.body<DocumentCreationRequest>()
        .result<Int>("201")

    fun deleteDocument(ctx: Context) {
        ctx.updateValidate()

        println("Checking for action \"DeleteDocument:${ctx.getDocId()}\"...")
        if (!ctx.isAuthorizedForEdv("DeleteDocument:${ctx.getDocId()}")) return

        // if (!ctx.isAuthorizedForDocument()) return

        val success = DocumentService.deleteDocument(ctx.getEdvId(), ctx.getDocId())

        ctx.status(if (success) 201 else 404)

        if (success) {
            NotificationService.broadcastNotifications(
                EdvId(ctx.getEdvId()),
                ctx.getDocId(),
                -1,
                ctx.getInvokerDid(),
                NotificationOperation.DELETE
            )
        }
    }

    fun deleteDocumentDocs() = document().operation {
        it.operationId("deleteDocument").summary("Delete document in EDV.")
        it.addSecurityItem(SecurityRequirement().addList("http")).addTagsItem("Documents")
    }
        .result<Int>("201") { it.description("Successfully deleted document from EDV.") }
        .result<Int>("404") { it.description("Document was not found in EDV.") }


    fun searchDocument(ctx: Context) {
        // ctx.updateValidate()
        val req = ctx.bodyAsClass<SearchDocumentReq>()

        println("Checking for action \"SearchDocument:${req.keyword}\"...")
        if (!ctx.isAuthorizedForEdv("SearchDocument:${req.keyword}")) return

        ctx.json(DocumentService.searchDocument(req, ctx.getEdvId()))

        /*NotificationService.broadcastNotifications(
            EdvId(ctx.getEdvId()),
            ctx.getDocId(),
            -1,
            ctx.getInvokerDid(),
            NotificationOperation.SEARCH
        )*/
    }

    fun searchDocumentDocs() = document().operation {
        it.operationId("searchDocument").summary("Perform an encrypted search")
        it.addSecurityItem(SecurityRequirement().addList("http")).addTagsItem("Documents")
    }.body<SearchDocumentReq>()
        .result<SearchDocumentRes>("200") { it.description("Encrypted search result list") }
}
