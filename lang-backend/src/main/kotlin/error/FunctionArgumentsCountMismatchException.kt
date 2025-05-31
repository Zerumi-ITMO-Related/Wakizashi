package error

class FunctionArgumentsCountMismatchException(line: Int, column: Int, function: String, expected: Int, actual: Int) :
    ASTValidationException(line, column, "During function call: $function | Expected: $expected arguments, actual: $actual")
