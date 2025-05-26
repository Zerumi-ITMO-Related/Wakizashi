package error

class TypeMismatchException(line: Int, column: Int) : ASTValidationException(line, column)
