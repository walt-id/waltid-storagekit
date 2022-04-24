package id.walt.storagekit.server.utils

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

object JsonUtils {
    val jsonParser = Klaxon()
        .converter(object : Converter {
            override fun canConvert(cls: Class<*>) = cls == java.nio.file.Path::class.java
            override fun toJson(value: Any): String = "\"${(value as Path).toAbsolutePath().pathString}\""
            override fun fromJson(jv: JsonValue) = Path(jv.string ?: "")

        })
}
