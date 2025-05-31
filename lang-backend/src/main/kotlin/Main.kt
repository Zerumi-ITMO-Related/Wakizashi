import codegen.generateSasm
import error.ASTValidationException
import error.CodegenException
import kotlinx.serialization.json.Json
import semantic.checkASTSemantic
import kotlin.system.exitProcess

private val json = Json {
    classDiscriminator = "type"
}

fun main() {
    val ast = json.decodeFromString<ASTNode>(generateSequence { readlnOrNull() }.joinToString("\n"))
    checkASTSemantic(ast).fold(onSuccess = {
        generateSasm(ast).fold(onSuccess = { println(it) }, onFailure = {
            System.err.println("Code generation error: $it")
            if (it is CodegenException) System.err.println("On generating line ${it.line}, column: ${it.column + 1}")
            exitProcess(1)
        })
    }, onFailure = {
        System.err.println("Invalid AST: $it ${if (it.cause != null) "(cause by ${it.cause})" else ""}")
        if (it is ASTValidationException) {
            System.err.println("On line ${it.line}, column: ${it.column + 1}")
            if (it.description != "")
                System.err.println(it.description)
        }
        exitProcess(1)
    })
}
