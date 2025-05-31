package error

open class ASTValidationException(val line: Int, val column: Int, val description: String = "") : RuntimeException()
