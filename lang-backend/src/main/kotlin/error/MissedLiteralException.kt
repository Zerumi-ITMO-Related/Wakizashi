package error

class MissedLiteralException(line: Int, column: Int) : CodegenException(line, column)
