import confidentialstorage.client.service.ClientService
import confidentialstorage.common.persistence.encryption.JWEEncryption
import confidentialstorage.server.Webserver
import id.walt.servicematrix.ServiceMatrix
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlin.concurrent.thread

class FullTest : StringSpec({
    ".session-store data edvs indexes logs salt .stored-edvs sessions master".split(" ").forEach {
        File(it).deleteRecursively()
    }
    ServiceMatrix("service-matrix.properties")

    "Start webserver" {
        thread {
            Webserver().start()
        }
    }
    Thread.sleep(700)
    val tdocId = "doc1"
    val tdocId2 = "doc2"
    val tcontent = "this is some text"
    val tcontent1_2 = "this is some new text"
    val tcontent2 = "this is also some text"
    val tmasterkey = "123456".toByteArray()

    val clientService = ClientService()
    "Setup session" {
        clientService.run {
            createMasterKey(tmasterkey)
            unlockWithMasterKey(tmasterkey)
            setupSessionService()
        }
    }
    "Create session" {
        clientService.run {
            val newSess = sessionService.createSession("sess01")

            sessionService.selectSession(newSess.sessionId)

            setup()
        }
    }

    var edvId = ""
    "Create EDV" {
        clientService.run {
            val res = edvService.createEdv("http://localhost:7000")
            edvId = res.edvId
            println("Created EDV $edvId")
        }
    }

    "Create document" {
        clientService.run {
            documentService.create(tdocId, tcontent.toByteArray())
        }
    }

    "Load document" {
        clientService.run {
            val result = documentService.load(edvId, "doc1").toString()
            result shouldContain tcontent
            println(result)
        }
    }

    "Enable notification channel" {
        clientService.run {
            edvService.notificationsConnect(edvId) { event ->
                println("Received notification from EDV $edvId: Document ${event.documentId} was ${event.operation.name} by ${event.invoker}.")
            }
        }
    }

    "Create document2" {
        clientService.run {
            documentService.create(tdocId2, tcontent2.toByteArray())
        }
    }

    "Update document" {
        clientService.run {
            documentService.update(tdocId, tcontent1_2.toByteArray())
        }
    }

    "Disconnect notificationchannel" {
        clientService.run {
            edvService.notificationsDisconnect()
        }
    }

    "Delete documents" {
        clientService.run {
            documentService.delete(tdocId)
            documentService.delete(tdocId2)
        }
    }

    "Recreate document" {
        clientService.run {
            documentService.create("doc1", "new content in this document".toByteArray())
        }
    }

    "Search document" {
        clientService.run {
            val results = documentService.search(edvId, "content")
            results.forEach { println(it) }
            results shouldHaveAtLeastSize 1
        }
    }

    "GetDocuments" {
        clientService.run {
            val results = indexService.getDocuments(edvId)
            results.forEach { println(it) }
            results shouldHaveAtLeastSize 1
        }
    }

    "GetTree" {
        clientService.run {
            val results = indexService.getTree()
            results.forEach { println(it) }
        }
    }

    "GetRaw" {
        clientService.run {
            val results = indexService.getRaw(edvId)

            results.forEach { println(it) }
            results shouldHaveAtLeastSize 1
        }
    }


    "Export" {
        clientService.run {
            val jwe = sessionService.export(sessionService.sessionId)
            val decrypt = JWEEncryption.passphraseDecrypt(jwe, tmasterkey)
            println(decrypt)
        }
    }
})

