package confidentialstorage.server.utils

object IdentifierUtils {

    private val CHAR_POOL = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private const val EDV_ID_LENGTH = 12


    internal fun isValidEdvId(testEdvId: String) = testEdvId.all { it in CHAR_POOL } && testEdvId.length == EDV_ID_LENGTH

    internal fun generateAlphaNumeric(maxLength: Int) = (1..maxLength).map { CHAR_POOL.random() }.joinToString("")

    internal fun generateEdvIdentifier() = generateAlphaNumeric(EDV_ID_LENGTH)

    internal fun generateDocumentIdentifier() = generateAlphaNumeric(32)
}
