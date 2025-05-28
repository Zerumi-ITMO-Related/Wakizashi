package codegen

data class CodegenContext(
    val literals: List<LiteralDeclaration> = emptyList(),
    val references: List<ReferenceDeclaration> = emptyList(),
    val functions: List<FunctionDeclaration> = emptyList(),
) {
    fun withLiteral(literalDeclaration: LiteralDeclaration) =
        copy(literals = literals.plus(literalDeclaration))

    fun withReference(referenceDeclaration: ReferenceDeclaration) =
        copy(references = references.plus(referenceDeclaration))

    fun withFunction(functionDeclaration: FunctionDeclaration) =
        copy(functions = functions.plus(functionDeclaration))

    fun withReferences(references: List<ReferenceDeclaration>) =
        copy(references = this.references.plus(references))
}

data class FunctionDeclaration(
    val label: String,
    val assembly: List<String>
)

data class ReferenceDeclaration(
    val label: String
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

fun stdlibContext() = CodegenContext(
    functions = listOf(
        FunctionDeclaration(
            "print",
            listOf(
                "lit print_str",
                "jump"
            )
        )
    )
)
