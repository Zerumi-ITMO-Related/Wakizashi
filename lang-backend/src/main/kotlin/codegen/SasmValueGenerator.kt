package codegen

import ASTNode

/*
val b: Int = 14 ->

outside function:
lit-1:
    word 14
b:
    word 0
init_b:
    lit lit-1
    load
    lit b
    store
    ret

inside function:
    lit init_b
    call
*/

/*
val b: Int = factorial(3);

outside function:
lit-1:
    word 3
lit-2:
    word 1
    word 2
    word 3
b:
    word 0
init_b:
    lit lit-1
    lit factorial
    call
    lit b
    store
    ret

inside function:
    lit init_b
    call
 */

fun generateLiterals(literal: String, type: LiteralTypes): List<Int> {
    return when (type) {
        LiteralTypes.INT -> listOf(literal.toInt())
        LiteralTypes.STRING -> literal.map { it.code }
        LiteralTypes.BOOLEAN -> if (literal.equals("true", ignoreCase = true)) listOf(1) else listOf(0)
        LiteralTypes.UNIT -> listOf(0)
    }
}
