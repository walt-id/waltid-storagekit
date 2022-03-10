package confidentialstorage.common.authorization.validation

interface ValidationException {

    class EdvNotFound(message: String) : Exception(message)
    class DocumentNotFound(message: String) : Exception(message)
    class DocumentAlreadyExists(message: String) : Exception(message)

}
