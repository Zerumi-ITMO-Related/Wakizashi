import codegen.generateSasm
import error.ASTValidationException
import kotlinx.serialization.json.Json
import semantic.checkASTSemantic

private val json = Json {
    classDiscriminator = "type"
}

fun main() {
    val ast = json.decodeFromString<ASTNode>(generateSequence { readlnOrNull() }.joinToString("\n"))
    checkASTSemantic(ast).fold(
        onSuccess = {
            println("AST is valid")
            generateSasm(ast).fold(
                onSuccess = { println(it) },
                onFailure = { println("Code generation error: $it") }
            )
        },
        onFailure = {
            println("Invalid AST: $it (cause by ${it.cause})")
            if (it is ASTValidationException)
            println("On line ${it.line}, column: ${it.column + 1}")
        }
    )
}
