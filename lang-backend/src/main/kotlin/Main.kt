import codegen.generateSasm
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
        onFailure = { println("Invalid AST: $it (cause by ${it.cause})") }
    )
}
