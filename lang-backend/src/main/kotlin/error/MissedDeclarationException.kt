package error

class MissedDeclarationException(line: Int, column: Int, symbol: String)
    : ASTValidationException(line, column, "Declaration of $symbol is not defined")
