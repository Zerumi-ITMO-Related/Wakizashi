package error

class TypeMismatchException(line: Int, column: Int, expectedType: String, actualType: String)
    : ASTValidationException(line, column, "Expected $expectedType type, but got $actualType")
