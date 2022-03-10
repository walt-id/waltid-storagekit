package confidentialstorage.common.authorization.validation

import confidentialstorage.common.authorization.validation.ValidationException.*
import confidentialstorage.server.web.document.DocumentController.getDocId
import confidentialstorage.server.web.document.DocumentController.getEdvId
import confidentialstorage.server.web.document.DocumentService
import confidentialstorage.server.web.edv.EdvService
import io.javalin.http.Context
import java.util.regex.*

object RequestValidation {

    enum class ValidationScope {
        BASIC,
        BASIC_EDV,
        BASIC_EDV_DOCUMENT_CREATE,
        BASIC_EDV_DOCUMENT_UPDATE_DELETE_RETRIEVE
    }

    fun requestValidation(ctx: Context, validationScope: ValidationScope) {
        ctx.validateUrl()
        if (validationScope.ordinal >= ValidationScope.BASIC_EDV.ordinal) {
            checkSpecialCharacters(ctx.getEdvId())
            ctx.validateEdvId()
            isEdvExisting(ctx)
        }
        /*if (validationScope.ordinal >= ValidationScope.BASIC_EDV_DOCUMENT_CREATE.ordinal) {
            checkSpecialCharacters(ctx.getDocId())
            ctx.validateDocId()
            isDocumentExisting(ctx, ValidationScope.BASIC_EDV_DOCUMENT_CREATE)
        }*/
        if (validationScope.ordinal >= ValidationScope.BASIC_EDV_DOCUMENT_UPDATE_DELETE_RETRIEVE.ordinal) {
            checkSpecialCharacters(ctx.getDocId())
            ctx.validateDocId()
            isDocumentExisting(ctx, ValidationScope.BASIC_EDV_DOCUMENT_UPDATE_DELETE_RETRIEVE)
        }
    }

    private fun isEdvExisting(ctx: Context) {
        val edvId = ctx.getEdvId()
        EdvService.getEdv(edvId) ?: throw EdvNotFound("Edv with id $edvId cannot be found!")
    }

    private fun isDocumentExisting(ctx: Context, validationScope: ValidationScope) {
        val edvId = ctx.getEdvId()
        val docId = ctx.getDocId()
        if (validationScope == ValidationScope.BASIC_EDV_DOCUMENT_CREATE)
            if (DocumentService.exists(edvId, docId)) throw DocumentAlreadyExists("Document with id $docId already exists!")
        if (validationScope == ValidationScope.BASIC_EDV_DOCUMENT_UPDATE_DELETE_RETRIEVE)
            if (!DocumentService.exists(edvId, docId)) throw DocumentNotFound("Document with id $docId cannot be found!")

    }

    private fun Context.validateUrl() {
        if (url().length > 254) throw IllegalArgumentException("URL is too long! Maximum size: 254")
    }

    private fun Context.validateEdvId() {
        if (getEdvId().length > 64) throw IllegalArgumentException("Edv id is too long! Maximum size: 64")
    }

    private fun Context.validateDocId() {
        if (getDocId().length > 96) throw IllegalArgumentException("Document id is too long! Maximum size: 96")
    }

    private fun checkSpecialCharacters(input: String) {
        val p: Pattern = Pattern.compile("[^a-z0-9 -]", Pattern.CASE_INSENSITIVE)
        val m: Matcher = p.matcher(input)
        val b: Boolean = m.find()

        // if (b) throw IllegalArgumentException("Special characters are not allowed!")
    }

}
