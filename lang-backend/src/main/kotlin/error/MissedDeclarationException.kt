package error

class MissedDeclarationException(line: Int, column: Int) : ASTValidationException(line, column)
