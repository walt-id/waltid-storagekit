package confidentialstorage.common.utils

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import java.time.Instant

object JsonUtils {
    private val dateConverter = object : Converter {
        override fun canConvert(cls: Class<*>) = cls == Instant::class.java

        override fun fromJson(jv: JsonValue): Instant = Instant.parse(jv.string)

        override fun toJson(value: Any) = """ "${value as Instant}" """
    }

    fun klaxon() = Klaxon()
        .converter(dateConverter)
}
