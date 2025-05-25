import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.types.file
import java.util.Properties

class CompilerCLI : CliktCommand() {
    private val file: String by argument().file(mustExist = true, canBeDir = false).convert { it.readText() }

    override fun run() {
        // echo("Compiling file: $file")
        val propFile = this::class.java.getResourceAsStream("/compiler.properties")
            ?: error("No compiler.properties file found")
        val properties = Properties().apply { load(propFile) }

        val frontendBinary = properties.getProperty("frontend-binary") ?: error("frontend-binary not set")
        val backendBinary = properties.getProperty("backend-binary") ?: error("backend-binary not set")

        val frontendProcess = ProcessBuilder(frontendBinary, file)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        frontendProcess.outputStream.bufferedWriter().use { writer ->
            writer.write(file)
            writer.flush()
        }

        val astJson = frontendProcess.inputStream.bufferedReader().readText()
        val exitCodeFrontend = frontendProcess.waitFor()
        if (exitCodeFrontend != 0) error("Frontend exited with code $exitCodeFrontend")

        val backendProcess = ProcessBuilder(backendBinary)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        backendProcess.outputStream.bufferedWriter().use { it.write(astJson); it.flush() }

        backendProcess.inputStream.bufferedReader().forEachLine { println(it) }

        val exitCodeBackend = backendProcess.waitFor()
        if (exitCodeBackend != 0) error("Backend exited with code $exitCodeBackend")
    }
}

fun main(args: Array<String>) = CompilerCLI().main(args)
