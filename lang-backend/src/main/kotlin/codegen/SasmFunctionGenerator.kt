package codegen

import ASTNode
import error.MissedLiteralException
import error.UnknownNodeInASTException
import error.UnknownOperationException
import error.UnsupportedOperationException
import java.util.*

fun generateFunctionBody(
    node: ASTNode,
    codegenState: CodegenContext,
    currentFunction: ASTNode.FunctionDeclarationNode,
    state: List<String> = emptyList()
): Result<List<String>> {
    return when (node) {
        is ASTNode.BlockNode -> {
            return node.children.fold(Result.success(state)) { ctx, child ->
                ctx.fold(
                    onSuccess = { generateFunctionBody(child, codegenState, currentFunction, it) },
                    onFailure = { Result.failure(it) })
            }
        }

        is ASTNode.BinaryOperationNode -> {
            val left = generateFunctionBody(node.left, codegenState, currentFunction).fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) }
            )
            val right = generateFunctionBody(node.right, codegenState, currentFunction).fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) }
            )
            val op = when (node.op) {
                "+" -> "lit add"
                "-" -> "lit sub"
                "*" -> "lit mul"
                "/" -> "lit div"
                "==" -> "lit equals"
                "!=" -> "lit not_equals"
                ">" -> "lit more"
                "<" -> "lit less"
                "&&" -> "lit and"
                "||" -> "lit or"
                else -> return Result.failure(UnknownOperationException(node.line, node.column))
            }
            Result.success(state.plus(listOf(left, right).flatten().plus(op).plus("call")))
        }

        is ASTNode.FunctionCallNode -> {
            val args = node.args.reversed().fold(Result.success(emptyList<String>())) { ctx, child ->
                ctx.fold(
                    onSuccess = { generateFunctionBody(child, codegenState, currentFunction, it) },
                    onFailure = { Result.failure(it) })
            }.fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) }
            )
            val functionDeclaration = "lit ${node.name}"
            val call = "call"
            Result.success(state.plus(args.plus(functionDeclaration).plus(call)))
        }

        is ASTNode.IdentNode -> Result.success(state.plus(listOf("lit ${node.name}", "load")))
        is ASTNode.IfNode -> {
            val thenBlock = generateFunctionBody(node.then, codegenState, currentFunction).fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) }
            )
            val elseBlock = node.`else`?.let {
                generateFunctionBody(it, codegenState, currentFunction).fold(
                    onSuccess = { it },
                    onFailure = { return Result.failure(it) }
                )
            } ?: emptyList()
            val condition = generateFunctionBody(node.condition, codegenState, currentFunction).fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) }
            )

            val uuid = UUID.randomUUID()
            val elseLabel = "else-$uuid"
            val continueLabel = "continue-$uuid"

            Result.success(
                state.plus(
                    listOf("lit $elseLabel")
                        .asSequence()
                        .plus(condition)
                        .plus(
                            listOf(
                                "jz",
                            )
                        )
                        .plus(thenBlock)
                        .plus(
                            listOf(
                                "lit $continueLabel",
                                "jump",
                                "$elseLabel:"
                            )
                        )
                        .plus(elseBlock)
                        .plus(
                            listOf(
                                "$continueLabel:",
                                "nop"
                            )
                        )
                        .toList()
                )
            )
        }

        is ASTNode.ReturnNode -> {
            val initReturn = generateFunctionBody(node.value, codegenState, currentFunction).fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) }
            )
            Result.success(state.plus(initReturn).plus("ret"))
        }

        is ASTNode.LiteralNode -> {
            val literal = codegenState.literals.find { it.value == node.value } ?: return Result.failure(
                MissedLiteralException(
                    node.line,
                    node.column
                )
            )
            Result.success(state.plus(listOf("lit ${literal.label}", "load")))
        }

        is ASTNode.ValueDeclarationNode -> {
            val identifier = "lit init_${node.name}"
            Result.success(state.plus(listOf(identifier, "call")))
        }

        is ASTNode.UnknownNode -> Result.failure(UnknownNodeInASTException(node.line, node.column))

        is ASTNode.ProgramNode -> Result.failure(UnsupportedOperationException(node.line, node.column))
        is ASTNode.FunctionDeclarationNode -> Result.failure(UnsupportedOperationException(node.line, node.column))
    }
}
