package id.walt.storagekit.service

import kotlin.io.path.Path
import kotlin.io.path.writeLines

fun main() {
    Path("testfile").writeLines((1..59999).map { "This is line $it" })
}
