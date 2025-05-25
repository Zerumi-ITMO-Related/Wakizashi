package semantic

sealed class SemanticContext {
    data class ProgramContext(val variables : String) : SemanticContext()
    data class LiteralContext(val value: String, val valType: String) : SemanticContext()
}

enum class LiteralTypes {
    INT,
    STRING,
    BOOLEAN,
    UNIT,
}