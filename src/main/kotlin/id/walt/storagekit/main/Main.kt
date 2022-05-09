package id.walt.storagekit.main

import id.walt.storagekit.server.main as serverMain
import id.walt.storagekit.client.console.main as clientMain
import id.walt.storagekit.service.main as serviceMain

fun printHelp(extraMsg: String? = null) = println(
    """
    |runner args: [client, server, service]
    |client: simple CLI client
    |server: start a Storage Kit compliant backend instance
    |service: simple example service
    ${if (extraMsg != null) "|$extraMsg" else ""}
    """.trimMargin()
)

fun main(args: Array<String>) {
    val newArgs = args.drop(1).toTypedArray()

    when {
        args.isEmpty() -> printHelp("no argument supplied.")

        else -> when (args.first()) {
            "client" -> clientMain(newArgs)
            "server" -> serverMain(newArgs)
            "service" -> serviceMain()

            else -> printHelp("unknown argument: ${args.first()}")
        }
    }
}
