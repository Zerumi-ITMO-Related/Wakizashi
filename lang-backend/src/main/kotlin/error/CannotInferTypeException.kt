package error

class CannotInferTypeException(line: Int, column: Int) : ASTValidationException(line, column) {
    constructor(cause : ASTValidationException) : this(cause.line, cause.column) {
        this.initCause(cause)
    }
}
