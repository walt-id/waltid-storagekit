package confidentialstorage.common

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable

object Utils {
    fun Serializable.toByteArray(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(this)
        objectOutputStream.flush()
        val result = byteArrayOutputStream.toByteArray()
        byteArrayOutputStream.close()
        objectOutputStream.close()
        return result
    }
}
