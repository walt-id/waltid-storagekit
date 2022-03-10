# Confidential Storage

The **Confidential Storage** by **walt.id** is a secure data storage solution, allowing you to interface with
**Encrypted Data Vaults** easily.

The system is scoped into:

- **Confidential Storage Server (remote node)**: Hosting EDVs
- **Confidential Storage Client (local)**: Interfacing with remote EDVs
- **Service wrapper**: Easily access data of your clients directly in their EDVs

The library is written in **Kotlin/Java** and can be directly integrated as Maven/Gradle dependency.  
soon: Alternatively the library or the additional Docker container can be run as RESTful webservice.

## Functionality

The Confidential Storage functions are in the scope of:

(special features are highlighted)

### Layer 1

- 1.1 Server request validation
- 1.2 Encrypted data persistence
- 1.3 Global configuration (capability discovery)
- **1.4 Enforcement of authorization policies** with *ZCap-LD*
- **1.5 Encrypted data chunking**
- 1.5 Resource structures
- 1.6 Encrypted Resource structures

### Layer 2:

- **Encrypted search**
- **Versioning and replication**
- **sharing with other entities**

### Layer 3:

- **Notifications**
- Vault-wide integrity protection

(cmp. Confidential Storage specifications from the Identity Foundation)

## Examples

### General examples

```kotlin
val clientService = ClientService().run {
    // Setup client
    createMasterKey(tmasterkey)
    unlockWithMasterKey(tmasterkey)
    setupSessionService()

    // Create session
    val newSess = sessionService.createSession("sess01")
    sessionService.selectSession(newSess.sessionId)

    // Setup
    setup()

    // Create EDV
    edvService.createEdv("http://localhost:7000")

    // Create document
    documentService.create("doc1", "this is a document".toByteArray())

    // Load document
    val loadedDocument = documentService.load("doc1").toString()
    println(loadedDocument)

    // Enable notification handler
    edvService.notificationsConnect(edvId) { event ->
        println("Received notification from EDV $edvId: Document ${event.documentId} was ${event.operation.name} by ${event.invoker}.")
    }

    // Update document
    documentService.update("doc1", "new content".toByteArray())

    // Encrypted search
    val results = documentService.search("content")
    results.forEach { println(it) }

    // Delete document
    documentService.delete("doc1")

    // Export session
    val exportJWE = sessionService.export(sessionService.sessionId)
}
```

### Handle DataRequests

```kotlin
val req = reader.readLine("Enter data request: ")

// Verify request
println("Verifying request...")
val verified = clientService.dataRequestService.verifyDataRequest(req)

if (!verified) {
    out("SIGNATURE VERIFICATION FAILED!")
    return
}
out("Signature successfully verified.")

// Decode request
val dataRequest = clientService.dataRequestService.decodeDataRequest(req)

out("[${dataRequest.context}]:")
out("DID ${dataRequest.did} requests a ${dataRequest.preferredDataType}.")
out()
out("Do you want to accept at ${dataRequest.responseUrl}?")
val input = reader.readLine("Accept request (y/n)$boldColon ")

if (input != "y") {
    out("Data request disposed.")
    return
}

// Accept request
val docId = dataRequest.preferredDataType
val childDid = dataRequest.did

val caveats = listOf(ValidOperationsCaveat(listOf("RetrieveDocument")), ValidOperationTargetsCaveat(listOf(docId)))

val delegation = clientService.dataRequestService.createDataDelegation(edvId, childDid, caveats)

out("Delegated permissions for EDV $edvId from owner ${getSession().did} to child $childDid!")

clientService.dataRequestService.acceptDataRequest(dataRequest, edvId, delegation)

out("Data request accepted!")
```
