<div align="center">
 <h1>Storage Kit</h1>
 <span>by </span><a href="https://walt.id">walt.id</a>
 <p>Enhance your app with zero trust storage and privacy-preserving data sharing<p>
 <a href="https://walt.id/community">
<img src="https://img.shields.io/badge/Join-The Community-blue.svg?style=flat" alt="Join community!" />
</a>
<a href="https://twitter.com/intent/follow?screen_name=walt_id">
<img src="https://img.shields.io/twitter/follow/walt_id.svg?label=Follow%20@walt_id" alt="Follow @walt_id" />
</a>
</div>

## Getting Started

- [CLI | Command Line Interface](https://docs.walt.id/v/storage-kit/getting-started/cli-command-line-interface) - Try out the functions of the Storage Kit locally.
- [REST Api](https://docs.walt.id/v/storage-kit/getting-started/rest-apis) - Use the functions of the Storage Kit via an REST api.
- [Maven/Gradle Dependency](https://docs.walt.id/v/storage-kit/getting-started/dependency-jvm) - Use the functions of the Storage Kit directly in a Kotlin/Java project.

Checkout the [Official Documentation](https://docs.walt.id/v/storage-kit/storage-kit/ssi-kit-or-basics), to dive deeper into the architecture and configuration options available.

## What is the Storage Kit?
Written in Kotlin and based on the [DIF specification](https://identity.foundation/confidential-storage/) the **Storage Kit** is a secure data confidential storage solution, allowing you to interface with **Encrypted Data Vaults** easily.

The system is scoped into:

- **Storage Kit Server (_Provider_)**: Hosting EDVs
- **Storage Kit Client (_Client_)**: Interfacing with remote EDVs
- **Service wrapper (_Service_)**: Easily access data of your clients directly in their EDVs


soon: Alternatively the library or the additional Docker container can be run as RESTful webservice.

### Functionality

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

## Join the community

* Connect and get the latest updates: [Discord](https://discord.gg/zUnxncExF5) | [Newsletter](https://walt.id/newsletter) | [YouTube](https://www.youtube.com/channel/UCXfOzrv3PIvmur_CmwwmdLA) | [Twitter](https://mobile.twitter.com/walt_id)
* Get help, request features and report bugs: [GitHub Discussions](https://github.com/walt-id/.github/discussions)

## License

Licensed under the [Apache License, Version 2.0](https://github.com/walt-id/waltid-storage-kit/blob/master/LICENSE)
