package codegen

data class CodegenContext(
    val literals: List<LiteralDeclaration> = emptyList(),
    val functions: List<FunctionDeclaration> = emptyList()
) {
    fun withLiteral(literalDeclaration: LiteralDeclaration) =
        copy(literals = literals.plus(literalDeclaration))

    fun withFunction(functionDeclaration: FunctionDeclaration) =
        copy(functions = functions.plus(functionDeclaration))
}

data class FunctionDeclaration(
    val label: String,
    val assembly: List<String>
)

data class LiteralDeclaration(
    val label: String,
    val value: String,
    val literals: List<Int>,
)

enum class LiteralTypes {
    INT,
    STRING,
    BOOLEAN,
    UNIT
}
