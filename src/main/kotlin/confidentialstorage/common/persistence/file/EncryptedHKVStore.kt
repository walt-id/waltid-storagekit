package confidentialstorage.common.persistence.file

import com.nimbusds.jose.Payload
import confidentialstorage.common.persistence.encryption.JWEEncryption.passphraseDecrypt
import confidentialstorage.common.persistence.encryption.JWEEncryption.passphraseEncrypt
import java.nio.file.Path
import kotlin.io.path.*


class EncryptedHKVStore(id: String, private val masterKey: ByteArray) {

    private val rootPath = Path.of(id)

    init {
        rootPath.createDirectories()
    }

    fun exists() = rootPath.exists() && rootPath.isDirectory()
    fun exists(path: Path) = exists() && path.resolveFromRoot().exists()

    private fun Path.resolveFromRoot(): Path = rootPath.resolve(this)

    private fun preconditions() {
        if (!exists()) rootPath.createDirectory()
    }

    fun loadDocument(path: Path) = loadDocumentUnresolved(path.resolveFromRoot())
    fun loadDocumentUnresolved(path: Path) = passphraseDecrypt(path.readText(), masterKey)

    fun storeDocument(path: Path, text: String) =
        path.resolveFromRoot().also { preconditions() }.writeText(passphraseEncrypt(Payload(text), masterKey))

    fun deleteDocument(path: Path) = path.resolveFromRoot().deleteIfExists()

    fun listDocuments(path: Path = emptyPath): List<Path> =
        if (exists(path)) path.resolveFromRoot().listDirectoryEntries() else emptyList()

    companion object {
        private val emptyPath = Path("")
    }
}
