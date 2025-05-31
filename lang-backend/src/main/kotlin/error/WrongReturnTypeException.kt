package error

class WrongReturnTypeException(line: Int, column: Int) : CodegenException(line, column)
