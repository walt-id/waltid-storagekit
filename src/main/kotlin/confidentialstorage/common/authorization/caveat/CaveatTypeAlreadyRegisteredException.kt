package confidentialstorage.common.authorization.caveat

class CaveatTypeAlreadyRegisteredException(registration: CaveatTypeRegistry.TypeRegistration) :
    IllegalArgumentException("Type \"${registration.caveat.simpleName!!}\" [${registration.metadata.type}] already exists in the registry.")
