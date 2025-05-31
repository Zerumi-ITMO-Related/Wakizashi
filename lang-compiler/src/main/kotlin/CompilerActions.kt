import java.io.File

data class ActionParameters(
    val binary: String,
    val input: Pair<List<String>, String>,
    val startPhase: CompilerCLI.CompilationPhase,
    val currentPhase: CompilerCLI.CompilationPhase,
    val untilPhase: CompilerCLI.CompilationPhase,
    val generateOutput: (Process) -> String,
    val printOutput: Boolean,
    val exportOutputFile: File?,
)

data class ActionSequence(
    val prepareNextInput: ((output: String) -> ActionParameters) -> PreparedActionSequence,
    val collectResult: () -> String
)

data class PreparedActionSequence(
    val thenRun: () -> Result<ActionSequence>
)

fun runFrom(parameters: ActionParameters): Result<ActionSequence> {

    val process = ProcessBuilder(listOf(parameters.binary).plus(parameters.input.first))
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

    process.outputStream.bufferedWriter().use { writer ->
        writer.write(parameters.input.second)
        writer.flush()
    }

    val exitCodeFrontend = process.waitFor()
    if (exitCodeFrontend != 0)
        return Result.failure(IllegalStateException("Process ${parameters.currentPhase.binaryProcessName} exited with code $exitCodeFrontend"))

    val output = parameters.generateOutput(process)

    if (parameters.printOutput) {
        println(output)
    }

    if (parameters.exportOutputFile != null) {
        parameters.exportOutputFile.writer().use {
            it.write(output)
        }
    }

    return Result.success(ActionSequence(
        prepareNextInput = { prepareInput ->
            PreparedActionSequence(
                thenRun = {
                    runFrom(prepareInput(output))
                }
            )
        },
        collectResult = { output }
    ))
}