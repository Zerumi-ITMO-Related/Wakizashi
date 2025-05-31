package error

class UnsupportedOperationException(line: Int, column: Int) : CodegenException(line, column)
