package error

class UnknownOperationException(line: Int, column: Int) : CodegenException(line, column)
