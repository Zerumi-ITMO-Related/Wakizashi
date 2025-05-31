import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.util.Properties
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writer
import kotlin.system.exitProcess

// Compiler infrastructure:
// .wak -> .ast -> .sasm with stdlib -> machine_code.json -> stack-CPU

// CLI:
// java -jar waki-compiler.jar <input_file>
// Options:
// Compilation phases:
// --show-ast
// --show-assembly (also --show-sasm)
// Start compilation from:
// --from-ast <file.ast>
// --from-assembly <file.sasm>
// By default it starts from .wak file
// Stop compilation at:
// --to-ast (or just -ast)
// --to-assembly (also --to-sasm, or just -assembly/-sasm)
// By default it executes on Stack-CPU
// Export compilation artifacts:
// --export-ast <file.ast>
// --export-sasm <file.sasm>
// --export-machine <file.log>
// Computer specific:
// --with-input="1 2 3 10"

class CompilerCLI : CliktCommand(name = "waki-compiler") {
    private val file: String by argument()
        .file(mustExist = true, canBeDir = false).convert { it.readText() }

    enum class CompilationPhase(val order: Int, val binaryProcessName: String) {
        WAK(0, "lang-frontend"), AST(1, "lang-backend"), SASM(2, "assembly-translator"), COMP(3, "stack-cpu"), ;

        fun next(): CompilationPhase? = entries.find { it.order == this.order + 1 }
    }

    private val compilationStart by mutuallyExclusiveOptions(
        option("--from-ast", help = "Start from AST").flag().convert { CompilationPhase.AST },
        option("--from-assembly", "--from-sasm", help = "Start from Stack Assembly").flag()
            .convert { CompilationPhase.SASM },
    ).single().default(CompilationPhase.WAK).help("Compilation pipeline start", "Choose entry point of compiler")

    private val compilationFinish by mutuallyExclusiveOptions(
        option(
            "--to-ast", "-ast", help = "Stop after generating AST"
        ).convert { CompilationPhase.AST }, option(
            "--to-assembly", "--to-sasm", "-assembly", "-sasm", help = "Stop after generating SASM"
        ).convert { CompilationPhase.SASM }
    ).single().default(CompilationPhase.COMP).help("Compilation pipeline stop", "Choose leave point of compiler")

    private val showAst by option("--show-ast", help = "Print the AST after parsing").flag()
    private val showSasm by option("--show-assembly", "--show-sasm", help = "Print the generated stack assembly").flag()

    private val exportAst by option("--export-ast", help = "Write AST to a file").file(
        canBeDir = false, mustBeWritable = false
    )
    private val exportSasm by option("--export-sasm", help = "Write stack assembly to a file").file(
        canBeDir = false, mustBeWritable = false
    )
    private val exportMachine by option(
        "--export-machine", help = "Write machine code to a file"
    ).file(canBeDir = false, mustBeWritable = false)

    private val input: String? by option(
        "--with-input", help = "Input some data to CPU I/O"
    )

    override fun run() {
        val propFile =
            this::class.java.getResourceAsStream("/compiler.properties")
                ?: terminate("No compiler.properties file found")
        val properties = Properties().apply { load(propFile) }

        val frontendBinary = properties.getProperty("frontend-binary") ?: terminate("frontend-binary not set")
        val backendBinary = properties.getProperty("backend-binary") ?: terminate("backend-binary not set")
        val assemblyBinary = properties.getProperty("assembly-binary") ?: terminate("assembly-binary not set")
        val compBinary = properties.getProperty("comp-binary") ?: terminate("assembly-binary not set")

        val stdlib = properties.getProperty("stdlib-path") ?: terminate("unable to find standard library")

        val programTextFile = kotlin.io.path.createTempFile()
        val programMachineFile = kotlin.io.path.createTempFile()
        val stdin = kotlin.io.path.createTempFile()
        val stdout = kotlin.io.path.createTempFile()

        stdin.writer().use {
            input?.let { it1 -> it.write(it1.replace(' ', '\n').plus('\n')) }
        }

        fun getPrepareInputForPhase(compilationPhase: CompilationPhase): (String) -> String = when (compilationPhase) {
            CompilationPhase.WAK -> { output -> output }
            CompilationPhase.AST -> { it -> it }
            CompilationPhase.SASM -> { it ->
                val fullAsm = File(stdlib).readText() + it
                programTextFile.writer().use {
                    it.write(fullAsm)
                }
                fullAsm
            }

            CompilationPhase.COMP -> { it -> it }
        }

        fun getCompilerActionParameters(compilationPhase: CompilationPhase, input: String) = when (compilationPhase) {
            CompilationPhase.WAK -> ActionParameters(
                binary = frontendBinary,
                input = emptyList<String>() to input,
                startPhase = compilationStart,
                currentPhase = CompilationPhase.WAK,
                untilPhase = compilationFinish,
                generateOutput = { process -> process.inputStream.bufferedReader().readText() },
                printOutput = showAst,
                exportOutputFile = exportAst
            )

            CompilationPhase.AST -> ActionParameters(
                binary = backendBinary,
                input = emptyList<String>() to input,
                startPhase = compilationStart,
                currentPhase = CompilationPhase.AST,
                untilPhase = compilationFinish,
                generateOutput = { process -> process.inputStream.bufferedReader().readText() },
                printOutput = showSasm,
                exportOutputFile = exportSasm
            )

            CompilationPhase.SASM -> ActionParameters(
                binary = assemblyBinary,
                input = listOf(
                    programTextFile.pathString,
                    programMachineFile.pathString
                ) to input,
                startPhase = compilationStart,
                currentPhase = CompilationPhase.SASM,
                untilPhase = compilationFinish,
                generateOutput = { _ -> programMachineFile.readText() },
                printOutput = false,
                exportOutputFile = exportMachine,
            )

            CompilationPhase.COMP -> ActionParameters(
                binary = compBinary,
                input = listOf(
                    "-p",
                    programMachineFile.pathString,
                    "-i",
                    stdin.pathString,
                    "-o",
                    stdout.pathString,
                    "-l",
                    "/dev/null"
                ) to input,
                startPhase = compilationStart,
                currentPhase = CompilationPhase.COMP,
                untilPhase = compilationFinish,
                generateOutput = { _ -> stdout.readText() },
                printOutput = false,
                exportOutputFile = null
            )
        }

        fun runSequence(sequence: ActionSequence, compilationPhase: CompilationPhase): Result<ActionSequence> =
            if (compilationPhase == compilationFinish && compilationPhase != CompilationPhase.COMP) Result.success(
                sequence
            )
            else sequence.prepareNextInput { output ->
                getCompilerActionParameters(compilationPhase, getPrepareInputForPhase(compilationPhase)(output))
            }.thenRun().fold(
                onSuccess = {
                    compilationPhase.next()?.let { nextPhase ->
                        runSequence(it, nextPhase)
                    } ?: Result.success(it)
                },
                onFailure = { Result.failure(it) }
            )

        val result = runFrom(getCompilerActionParameters(compilationStart, file)).fold(
            onSuccess = {
                compilationStart.next()?.let { nextPhase -> runSequence(it, nextPhase) } ?: Result.success(it)
            },
            onFailure = { Result.failure(it) }
        ).fold(
            onSuccess = { it.collectResult() },
            onFailure = { terminate(it.message) }
        )

        println(result)
    }
}

fun main(args: Array<String>) = CompilerCLI().main(args)

fun terminate(message: String?): Nothing {
    println(message ?: "Compilation error")
    exitProcess(1)
}
