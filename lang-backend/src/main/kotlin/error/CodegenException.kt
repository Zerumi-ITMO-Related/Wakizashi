package error

open class CodegenException(val line: Int, val column: Int, val description: String = "") : RuntimeException()
