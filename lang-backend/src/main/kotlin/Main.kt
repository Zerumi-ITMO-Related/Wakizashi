import kotlinx.serialization.json.Json
import semantic.checkASTSemantic

private val json = Json {
    classDiscriminator = "type"
}

fun main() {
    println("Compiler")
    val ast = json.decodeFromString<ASTNode>(generateSequence { readlnOrNull() }.joinToString("\n"))
    val isValidAst = checkASTSemantic(ast)
    println(isValidAst)
}
