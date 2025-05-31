package semantic

data class SemanticContext(
    val variables: List<VariableDeclaration> = emptyList(),
    val functions: List<FunctionDeclaration> = emptyList(),
) {
    fun withVariables(variables: List<VariableDeclaration>) = copy(
        variables = variables.plus(variables)
    )

    fun withVariable(variableDeclaration: VariableDeclaration) = copy(
        variables = variables.plus(variableDeclaration)
    )

    fun withFunction(functionDeclaration: FunctionDeclaration) = copy(
        functions = functions.plus(functionDeclaration)
    )
}

data class VariableDeclaration(
    val name: String, val type: String
)

data class FunctionDeclaration(
    val name: String, val params: List<VariableDeclaration>, val returnType: String
)

enum class LiteralTypes {
    INT, STRING, BOOLEAN, UNIT,
}

fun stdlibContext() = SemanticContext(
    functions = listOf(
        FunctionDeclaration(
            name = "print",
            params = listOf(VariableDeclaration("str", "String")),
            returnType = "Unit"
        ),
        FunctionDeclaration(
            name = "print_number",
            params = listOf(VariableDeclaration("num", "Int")),
            returnType = "Unit"
        ),
        FunctionDeclaration(
            name = "parse_int",
            params = listOf(VariableDeclaration("str", "String")),
            returnType = "Int"
        ),
        FunctionDeclaration(
            name = "readln",
            params = listOf(),
            returnType = "String"
        )
    )
)
