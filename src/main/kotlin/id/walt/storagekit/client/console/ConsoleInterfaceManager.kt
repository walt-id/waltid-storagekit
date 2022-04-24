package id.walt.storagekit.client.console

import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.Parser
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

object ConsoleInterfaceManager {
    val terminal: Terminal = TerminalBuilder.builder()
        .color(true)
        .build()

    val completer: Completer? = null
    val parser: Parser? = null

    val reader: LineReader = LineReaderBuilder.builder()
        .appName("Confidential Storage")
        .terminal(terminal)
        .completer(completer)
        .parser(parser)
        .build()

    fun out(msg: String = "") = reader.printAbove(msg)

    fun boldString(msg: String): String = AttributedStringBuilder()
        .style(AttributedStyle.DEFAULT.bold())
        .append(msg)
        .style(AttributedStyle.DEFAULT)
        .toAnsi()

    fun errorColor(msg: String): String = AttributedStringBuilder()
        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
        .append(msg)
        .style(AttributedStyle.DEFAULT)
        .toAnsi()

    val boldPrompt = boldString(">")
    val boldColon = boldString(":")
}
