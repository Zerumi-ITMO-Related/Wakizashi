import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.default
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

// Compiler infrastructure:
// .wak -> .ast -> .sasm with stdlib -> .json -> stack-CPU

// CLI:
// java -jar waki-compiler.jar <input_file>
// Options:
// Compilation phases:
// --show-ast
// --show-assembly (also --show-sasm)
// --show-journal
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
// --export-journal <file.log>

class CompilerCLI : CliktCommand(name = "waki-compiler") {
    private val file: String by argument()
        .file(mustExist = true, canBeDir = false).convert { it.readText() }
        .default(File("/Users/zerumi/gitClone/Wakizashi/lang-frontend/input/test0.wak").readText())

    enum class CompilationPhase(val order: Int, val binaryProcessName: String) {
        WAK(0, "lang-frontend"), AST(1, "lang-backend"), SASM(2, "assembly-translator"), COMP(3, "stack-cpu"), ;

        fun next(): CompilationPhase? = entries.find { it.order == this.order + 1 }
    }

    private val compilationStart by mutuallyExclusiveOptions(
        option("--from-ast").flag().convert { CompilationPhase.AST },
        option("--from-assembly", "--from-sasm").flag().convert { CompilationPhase.SASM },
    ).single().default(CompilationPhase.WAK).help("Compilation pipeline", "Choose entry point of compiler")

    private val compilationFinish by mutuallyExclusiveOptions(
        option(
            "--to-ast", "-ast", help = "Stop after generating AST"
        ).convert { CompilationPhase.AST }, option(
            "--to-assembly", "--to-sasm", "-assembly", "-sasm", help = "Stop after generating SASM"
        ).convert { CompilationPhase.SASM }
    ).single().default(CompilationPhase.COMP)

    private val showAst by option("--show-ast", help = "Print the AST after parsing").flag()
    private val showSasm by option("--show-assembly", "--show-sasm", help = "Print the generated stack assembly").flag()
    private val showJournal by option("--show-journal", help = "Show semantic journal/log").flag()

    private val exportAst by option("--export-ast", help = "Write AST to a file").file(
        canBeDir = false, mustBeWritable = false
    )
    private val exportSasm by option("--export-sasm", help = "Write stack assembly to a file").file(
        canBeDir = false, mustBeWritable = false
    )
    private val exportJournal by option(
        "--export-journal", help = "Write semantic log to a file"
    ).file(canBeDir = false, mustBeWritable = false)

    override fun run() {
        val propFile =
            this::class.java.getResourceAsStream("/compiler.properties") ?: error("No compiler.properties file found")
        val properties = Properties().apply { load(propFile) }

        val frontendBinary = properties.getProperty("frontend-binary") ?: error("frontend-binary not set")
        val backendBinary = properties.getProperty("backend-binary") ?: error("backend-binary not set")
        val assemblyBinary = properties.getProperty("assembly-binary") ?: error("assembly-binary not set")
        val compBinary = properties.getProperty("comp-binary") ?: error("assembly-binary not set")

        val stdlib = properties.getProperty("stdlib-path") ?: error("unable to find standard library")

        val programTextFile = kotlin.io.path.createTempFile()
        val programMachineFile = kotlin.io.path.createTempFile()
        val stdin = kotlin.io.path.createTempFile()
        val stdout = kotlin.io.path.createTempFile()

        fun getInputProcessor(compilationPhase: CompilationPhase): (String) -> String = when (compilationPhase) {
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
                generateOutput = { _ -> "" },
                printOutput = false,
                exportOutputFile = null,
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
                printOutput = showJournal,
                exportOutputFile = exportJournal
            )
        }

        fun runSequence(sequence: ActionSequence, compilationPhase: CompilationPhase): Result<ActionSequence> =
            sequence.prepareNextInput { output ->
                getCompilerActionParameters(compilationPhase, getInputProcessor(compilationPhase)(output))
            }.thenRun().fold(
                onSuccess = {
                    compilationPhase.next()?.let { nextPhase -> runSequence(it, nextPhase) } ?: Result.success(it)
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
            onFailure = { error(it) }
        )

        println(result)
    }
}

fun main(args: Array<String>) = CompilerCLI().main(args)
