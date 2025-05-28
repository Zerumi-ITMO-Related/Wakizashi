import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.util.Properties
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writer

class CompilerCLI : CliktCommand() {
    private val file: String by argument().file(mustExist = true, canBeDir = false).convert { it.readText() }

    override fun run() {
        val propFile = this::class.java.getResourceAsStream("/compiler.properties")
            ?: error("No compiler.properties file found")
        val properties = Properties().apply { load(propFile) }

        val frontendBinary = properties.getProperty("frontend-binary") ?: error("frontend-binary not set")
        val backendBinary = properties.getProperty("backend-binary") ?: error("backend-binary not set")
        val assemblyBinary = properties.getProperty("assembly-binary") ?: error("assembly-binary not set")
        val compBinary = properties.getProperty("comp-binary") ?: error("assembly-binary not set")
        val stdlib = properties.getProperty("stdlib-path") ?: error("unable to find standard library")

        val frontendProcess = ProcessBuilder(frontendBinary)
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

        val assembly = backendProcess.inputStream.bufferedReader().readText()

        val exitCodeBackend = backendProcess.waitFor()
        if (exitCodeBackend != 0) error("Backend exited with code $exitCodeBackend")

        val programText = File(stdlib).readText().plus(assembly)
        val programTextFile = kotlin.io.path.createTempFile()
        programTextFile.writer().use {
            it.write(programText)
        }

        val programMachineFile = kotlin.io.path.createTempFile()

        val assemblyProcess = ProcessBuilder(assemblyBinary, programTextFile.pathString, programMachineFile.pathString)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        val exitCodeAssembly = assemblyProcess.waitFor()
        if (exitCodeAssembly != 0) error("Assembly exited with code $exitCodeAssembly")

        val stdin = kotlin.io.path.createTempFile()
        val stdout = kotlin.io.path.createTempFile()

        val compProcess = ProcessBuilder(
            compBinary,
            "-p",
            programMachineFile.pathString,
            "-i",
            stdin.pathString,
            "-o",
            stdout.pathString,
            "-l",
            "/dev/null"
        )
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        val exitCodeComp = compProcess.waitFor()
        if (exitCodeComp != 0) error("Comp exited with code $exitCodeComp")

        println(stdout.readText())
    }
}

fun main(args: Array<String>) = CompilerCLI().main(args)
