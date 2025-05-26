package error

open class ASTValidationException(val line: Int, val column: Int) : RuntimeException()
