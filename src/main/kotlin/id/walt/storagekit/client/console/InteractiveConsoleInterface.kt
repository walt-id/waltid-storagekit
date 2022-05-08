package id.walt.storagekit.client.console

import id.walt.storagekit.client.clientmodels.SessionManager.Session
import id.walt.storagekit.client.clientmodels.SessionManager.SessionEdv
import id.walt.storagekit.client.console.ConsoleInterfaceManager.boldColon
import id.walt.storagekit.client.console.ConsoleInterfaceManager.boldPrompt
import id.walt.storagekit.client.console.ConsoleInterfaceManager.boldString
import id.walt.storagekit.client.console.ConsoleInterfaceManager.errorColor
import id.walt.storagekit.client.console.ConsoleInterfaceManager.out
import id.walt.storagekit.client.console.ConsoleInterfaceManager.reader
import id.walt.storagekit.client.service.ClientService
import id.walt.storagekit.common.authorization.caveat.list.ValidOperationTargetsCaveat
import id.walt.storagekit.common.authorization.caveat.list.ValidOperationsCaveat
import id.walt.servicematrix.ServiceMatrix
import org.jline.builtins.Commands
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.notExists
import kotlin.io.path.readBytes
import kotlin.system.exitProcess

object CommandLineInterface {

    val clientService = ClientService()

    private var selectedSessionId: String? = null

    private fun getSession() = clientService.sessionService.session

    private fun readSessionSelection(): String {
        val cachedSessions = clientService.sessionService.listCachedSessions()

        out()
        out("Cached sessions:")
        cachedSessions.forEachIndexed { index, session ->
            out("- ${index + 1}. ${session.sessionId}")
        }
        out("Type 'add' to add additional sessions using the session wizard.")
        out()

        when (val readSession = reader.readLine("Enter session identifier or index${boldColon} ")) {
            "add" -> {
                addSessionInput()
                return readSessionSelection()
            }
            "" -> return readSessionSelection()
            else -> {
                val readSessionSelected = try {
                    if (
                        readSession.toIntOrNull() != null
                        && readSession.toInt().toString() == readSession
                    ) cachedSessions[readSession.toInt() - 1].sessionId else readSession
                } catch (e: Exception) {
                    return readSessionSelection()
                }

                if (!clientService.sessionService.sessionExists(readSessionSelected)) {
                    out(errorColor("No session available with session id: $readSessionSelected"))
                    out()
                    return readSessionSelection()
                }

                return readSessionSelected
            }
        }

    }

    private fun askSelectSession() {
        out(boldString("[Select session]"))

        clientService.setupSessionService()

        if (clientService.sessionService.hasSessions()) {
            val sessionId = readSessionSelection()
            out()
            out("Selecting session $sessionId...")
            clientService.sessionService.selectSession(sessionId)
            selectedSessionId = sessionId
        } else {
            out()
            out("No cached sessions were found.")
            out("Adding a new session...")
            out()
            val session = addSessionInput()
            selectedSessionId = session.sessionId
        }
        out()
    }

    private fun importSession(): Session {
        out(boldString("[Import session]"))
        out()

        val tokenJWE = reader.readLine("Enter session token$boldColon ")

        val session = clientService.sessionService.importSession(tokenJWE)

        out("Session imported!")

        return session
    }

    private fun addSessionInput(): Session {
        out(boldString("[Add session]"))
        out()
        out("Welcome to the session setup wizard!")
        out("Please follow the instructions below:")
        out()
        out("Enter new session identifier or enter \"import\" to import an exiting session.")

        var sessionId = ""
        while (sessionId.isBlank()) sessionId = reader.readLine("Enter session identifier$boldColon ").trim()

        out()

        if (sessionId == "import") {
            val session = importSession()
            clientService.setupSessionService()
            clientService.sessionService.selectSession(session.sessionId)
            clientService.sessionChosen()
            return session
        }

        val session = clientService.sessionService.createSession(sessionId)
        clientService.sessionService.selectSession(sessionId)
        clientService.sessionChosen()

        out()
        out("Creating first EDV for session:")

        createEdv()

        out("Session \"$sessionId\" was stored to local session saves.")

        return session
    }

    private fun createEdv() {
        val providerUrl = reader.readLine("Enter provider url [${"http://localhost:7000"}]$boldColon ")
            .ifBlank { "http://localhost:7000" }

        val res = clientService.edvService.createEdv(providerUrl)


        out("EDV created: ${res.edvId}")
    }

    private fun printHelp() = out(
        """
                |Document options (document, doc, d):
                |  - document load   - Load a document from this EDV, cache it locally (gets updated automatically on notification)
                |  - document create - Create (sequence 0) a new document in this EDV and publish it to to EDV peers (e.g. backups)
                |  - document update - Recreate, resequence, and publish update to EDV peers (e.g. backups)
                |  - document delete - Unlink document from EDV, optionally remove all backups at replica nodes (notify peers)
                |  - document purge  - Purge document in all versions from EDV, optionally remove all backups at replica nodes (notify peers)
                |  - document cache  - Show cached documents
                |  - document search - Start remote-run encrypted-search operation
                |
                |Index options (index, i):
                |  - index tree      - Displays the whole index tree (all EDVs, all documents)
                |  - index show      - Displays the index of the selected EDV
                |  - index documents - Filters the index of the selected EDV to base documents
                |
                |EDV options (edv, e):
                |  - edv delegate - Delegate certain permissions to another keypair
                |  - edv add      - Create a new EDV to add to the session
                |  
                |  - Notification options:
                |    - edv notifications connect    - Connect to the notification channel for this EDV
                |    - edv notifications disconnect - Disconnect from the notification channel for this EDV
                |
                |Data request options (datarequest, dr):
                |  - datarequest - Accept a data request
                |
                |Session options (session, s):
                |  - session info   - Display stored information about the current session
                |  - session export - Exports the current session to use on another device, or as a backup
                |  - session switch - Switch to another session
                |
                |Client options:
                |  - help/?    - Show this help
                |  - history   - Show command history
                |  - exit/quit - Quit the Storage Kit Interactive Console Interface
                
            """.trimMargin()
    )

    private fun documentLoad() {
        val docId = reader.readLine("Document identifier$boldColon ")

        if (!clientService.documentService.tryResolveDocument(docId)) {
            out("A document with document id \"$docId\" is neither known in any local index cache nor in any remote index for any of the available EDVs!")
            return
        }

        val retrievedDocument = clientService.documentService.load(docId)

        out("Retrieved document \"$docId\" (below):")
        out(retrievedDocument.toString())
    }

    private fun documentIdInput(): String = reader.readLine("Document identifier$boldColon ")

    private fun contentPathInput(): ByteArray {
        var contentPath: Path? = null
        while (contentPath == null || contentPath.notExists()) {
            contentPath = Path(reader.readLine("Path/to/content.file$boldColon "))
        }

        return contentPath.readBytes()
    }

    private fun documentCreate() {
        val docId = documentIdInput()
        val content = contentPathInput()

        clientService.documentService.create(docId, content)
        out("Successfully created document!")
        out()
    }

    private fun documentUpdate() {
        val docId = documentIdInput()

        if (!clientService.documentService.tryResolveDocument(docId)) {
            out("No document with id \"$docId\" could be resolved.")
            return
        }

        val content = contentPathInput()

        clientService.documentService.update(docId, content)
        out("Successfully updated document: $docId")
    }

    private fun documentDelete(allVersions: Boolean) {
        val docId = documentIdInput()

        if (!clientService.documentService.tryResolveDocument(docId)) {
            out("No document with id \"$docId\" could be resolved.")
            return
        }

        clientService.documentService.delete(docId, allVersions)
        out("Successfully deleted document: $docId")
    }

    private fun documentCache(edvId: String) {
        val documents = clientService.indexService.getDocuments(edvId)

        documents.forEachIndexed { index, doc ->
            out("  - ${index + 1}. ${doc.first}${if ((doc.second) > 0) " (${doc.second + 1} versions)" else ""}")
        }
    }

    private fun documentCache() {
        var edvIndex = 1
        getSession().edvs.forEach { (edvId, _) ->
            out("${edvIndex++}. EDV: $edvId")
            documentCache(edvId)
        }
    }

    private fun documentSearch() {
        val keyword = reader.readLine("Enter keyword$boldColon ")!!

        val results = clientService.documentService.search(keyword)

        out("Searchable Symmetric Encryption done.")

        if (results.isEmpty()) {
            out("Keyword \"$keyword\" was not found in any encrypted index.")
        } else {
            out("Keyword \"$keyword\" was found ${results.size} time(s):")
            results.forEachIndexed { index, docId ->
                out("- ${index + 1}. $docId")
            }
        }
    }

    private fun notificationsConnect() {
        val edvId = getSession().edvs.keys.first()
        clientService.edvService.notificationsConnect(edvId) { event ->
            out("Received notification from EDV $edvId: Document ${event.documentId} was ${event.operation.name} by ${event.invoker}.")
        }
    }

    private fun notificationsDisconnect() {
        clientService.edvService.notificationsDisconnect()
    }

    private fun delegatePermissions(edv: SessionEdv) {
        val childDid = reader.readLine("Enter delegate DID$boldColon ")
        val json = clientService.edvService.delegate(edv.edvId, childDid)

        out("Delegated permissions for EDV \"${edv.edvId}\" from owner \"${getSession().did} to child \"$childDid\"!")
        out(json)
    }

    private fun dataRequest() {
        val req = reader.readLine("Enter data request$boldColon ")


        // TODO DID has to be imported before the request can be verified

        /*
        val resolvedDid = DidService.resolve(dataRequest.did)
        if (KeyStoreService.getService().getKeyId(resolvedDid.id) == null) {
            println("Importing DID...")
            DidService.importDid(resolvedDid.id)
        }*/
        ;
        try {
            println("Verifying request...")
            val verified = clientService.dataRequestService.verifyDataRequest(req)

            if (!verified) {
                out("SIGNATURE VERIFICATION FAILED!")
                return
            }

            out("Signature successfully verified.")
        } catch (ignored: Exception) {
            // TODO: return, error logger
        }

        out()

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

        val edvId = getSession().edvs.keys.first() // Todo
        val docId = dataRequest.preferredDataType
        val childDid = dataRequest.did

        val caveats =
            listOf(ValidOperationsCaveat(listOf("RetrieveDocument")), ValidOperationTargetsCaveat(listOf(docId)))

        val delegation = clientService.dataRequestService.createDataDelegation(edvId, childDid, caveats)

        out("Delegated permissions for EDV \"${edvId}\" from owner \"${getSession().did}\" to child \"$childDid\"!")

        clientService.dataRequestService.acceptDataRequest(dataRequest, edvId, delegation)

        out("Data request accepted!")
    }


    private fun sessionExport() {
        out(boldString("[Session export]"))
        out()
        out("Exporting session \"$selectedSessionId\"...")

        val jwe = clientService.sessionService.export(selectedSessionId!!)

        out("")
        out(boldString("Session token for \"$selectedSessionId\":") + " $jwe")
        out("This session token is encrypted with your master passphrase.")
        out("The same master passphrase has to be used to decrypt the session token in your new client.")
    }

    private fun sessionInfo() {
        out(boldString("[Session info]"))
        out()

        val session = getSession()

        out("${boldString("ID:")}     ${session.sessionId}")
        out("${boldString("DID:")}    ${session.did}")
        out("${boldString("Key ID:")} ${session.keyId}")
        out()
        out(boldString("Linked EDVs:"))
        session.edvs.values.forEachIndexed { index, edv ->
            out("${index + 1}: ${edv.edvId} @ ${edv.serverUrl}")
        }
    }

    private fun parseRepl(argv: Array<String>) {
        val line = reader.readLine("$boldPrompt $selectedSessionId $boldPrompt ")

        val words = reader.parser.parse(line, 0).words()

        if (getSession().edvs.values.isEmpty()) {
            out("Notice: You don't have any EDV linked to your session.")
        }

        val edv = getSession().edvs.values.first()

        try {
            when (val primary = words[0]) {
                "document", "doc", "d" -> {
                    when (val secondary = words[1]) {
                        "load" -> documentLoad()
                        "create" -> documentCreate()
                        "update" -> documentUpdate()
                        "purge" -> documentDelete(true)
                        "delete" -> documentDelete(false)
                        "cache" -> documentCache()
                        "search" -> documentSearch()
                        else -> out("Unknown secondary command: $secondary")
                    }
                }
                "index", "i" -> {
                    when (val secondary = words[1]) {
                        "tree" -> documentCache()
                        "show" -> documentCache(edv.edvId)
                        "documents", "docs" -> documentCache(edv.edvId)
                        else -> out("Unknown secondary command: $secondary")
                    }
                }
                "edv", "e" -> {
                    when (val secondary = words[1]) {
                        "delegate" -> delegatePermissions(edv)
                        "notifications" -> {
                            when (val tertiary = words[2]) {
                                "connect" -> notificationsConnect()
                                "disconnect" -> notificationsDisconnect()
                                else -> out("Unknown tertiary command: $tertiary")
                            }
                        }
                        "add" -> createEdv()
                        "info" -> edvInfo()
                        else -> out("Unknown secondary command: $secondary")
                    }
                }
                "datarequest", "dr" -> {
                    dataRequest()
                }
                "session", "s" -> {
                    when (val secondary = words[1]) {
                        "info" -> sessionInfo()
                        "export" -> sessionExport()
                        "switch" -> askSelectSession()
                        else -> out("Unknown secondary command: $secondary")
                    }
                }
                "history" -> Commands.history(reader, System.out, System.err, Path("."), argv)
                "help" -> printHelp()
                "?" -> printHelp()
                "exit" -> quitInterface()
                "quit" -> quitInterface()
                "" -> return
                else -> {
                    out("Unknown primary command: $primary")
                    out("View \"help\" for help.")
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            out("Invalid argument length for command (${e.message})!")
            out("View help with \"help\".")
        } catch (e: Exception) {
            out("An error occurred: ${e.message ?: e.localizedMessage}")
            // e.printStackTrace()
        }
    }

    private fun edvInfo() {
        clientService.sessionService.session.edvs.forEach {
            out("EDV ${it.key}:")
            out("  ${it.value.serverUrl}")
        }
    }

    private fun quitInterface() {
        notificationsDisconnect()
        exitProcess(0)
    }

    private fun repl(argv: Array<String>) {
        out(boldString("[Session ${selectedSessionId}]"))
        out()

        while (true) {
            parseRepl(argv)
        }
    }

    fun start(args: Array<String>) {
        out(boldString("[walt.id Storage Kit]"))
        out("Interactive CLI")

        ServiceMatrix("service-matrix.properties")

        masterPassphrase()

        askSelectSession()

        clientService.setup()

        repl(args)
    }

    private fun newMasterPassphrase() {
        out(boldString("[Master passphrase setup]"))
        out()
        out("You have not setup a master passphrase.")
        out("A new one will be setup.")
        out()
        val masterPassphrase = enterMasterPassphrase("Please enter your desired master passphrase$boldColon ")
        clientService.createMasterKey(masterPassphrase)
        clientService.unlockWithMasterKey(masterPassphrase)
        out()
    }

    private fun masterPassphrase() =
        if (clientService.masterKeyExists()) unlockMasterPassphrase() else newMasterPassphrase()

    private fun unlockMasterPassphraseSingle() {
        out()
        out("Please unlock your client store with your master passphrase.")
        out()
        val masterKey = enterMasterPassphrase("Please enter your master passphrase$boldColon ")
        out()

        clientService.unlockWithMasterKey(masterKey)
            .onSuccess {
                out("Master passphrase correct. Master unlock succeeded.")
                out()
            }.onFailure {
                out("Master passphrase incorrect. Master unlock failed.")
                out("Please try again:")
                unlockMasterPassphraseSingle()
            }
    }

    private fun unlockMasterPassphrase() {
        out(boldString("[Master unlock]"))
        unlockMasterPassphraseSingle()
    }

    private fun enterMasterPassphrase(prompt: String): ByteArray =
        (System.console()?.readPassword(prompt)?.toString()?.toByteArray() ?: reader.readLine(prompt, '*').toByteArray())
}

fun main(args: Array<String>) {
    CommandLineInterface.start(args)
}

