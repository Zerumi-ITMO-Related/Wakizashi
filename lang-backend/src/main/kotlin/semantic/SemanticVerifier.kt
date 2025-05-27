package semantic

import ASTNode
import error.CannotInferTypeException
import error.MissedDeclarationException
import error.TypeMismatchException

fun inferType(node: ASTNode, programSymbols: SemanticContext): Result<String> {
    return when (node) {
        is ASTNode.ReturnNode -> inferType(node.value, programSymbols)
        is ASTNode.BinaryOperationNode -> inferType(node.left, programSymbols).fold(onSuccess = { typeLeft ->
            inferType(node.right, programSymbols).fold(onSuccess = { typeRight ->
                if (typeLeft != typeRight) return Result.failure(
                    CannotInferTypeException(
                        TypeMismatchException(node.line, node.column)
                    )
                )
                else Result.success(typeLeft)
            }, onFailure = { return Result.failure(it) })
        }, onFailure = { return Result.failure(it) })

        is ASTNode.FunctionCallNode -> Result.success((programSymbols.functions.find { it.name == node.name }
            ?: return Result.failure(
                CannotInferTypeException(MissedDeclarationException(node.line, node.column))
            )).returnType)

        is ASTNode.IdentNode -> Result.success((programSymbols.variables.find { it.name == node.name }
            ?: return Result.failure(
                CannotInferTypeException(MissedDeclarationException(node.line, node.column))
            )).type)

        is ASTNode.LiteralNode -> Result.success(node.valType)
        is ASTNode.ValueDeclarationNode -> Result.success(node.valType)
        is ASTNode.FunctionDeclarationNode -> Result.success(node.returnType)
        is ASTNode.BlockNode -> inferType(node.children.last(), programSymbols)

        is ASTNode.IfNode -> Result.failure(CannotInferTypeException(node.line, node.column))
        is ASTNode.ProgramNode -> Result.failure(CannotInferTypeException(node.line, node.column))
        is ASTNode.UnknownNode -> Result.failure(CannotInferTypeException(node.line, node.column))
    }
}

fun verifyFunctionAlwaysReturnValue(body: ASTNode, semanticContext: SemanticContext) : Result<Unit> {
    return Result.success(Unit)
}
