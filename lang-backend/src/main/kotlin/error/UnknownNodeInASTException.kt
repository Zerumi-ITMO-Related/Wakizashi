package error

class UnknownNodeInASTException(line: Int, column: Int) : ASTValidationException(line, column)
