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
                        TypeMismatchException()
                    )
                )
                else Result.success(typeLeft)
            }, onFailure = { return Result.failure(it) })
        }, onFailure = { return Result.failure(it) })

        is ASTNode.FunctionCallNode -> Result.success((programSymbols.functions.find { it.name == node.name }
            ?: return Result.failure(
                CannotInferTypeException(MissedDeclarationException())
            )).returnType)

        is ASTNode.IdentNode -> Result.success((programSymbols.variables.find { it.name == node.name }
            ?: return Result.failure(
                CannotInferTypeException(MissedDeclarationException())
            )).type)

        is ASTNode.LiteralNode -> Result.success(node.valType)
        is ASTNode.ValueDeclarationNode -> Result.success(node.valType)
        is ASTNode.FunctionDeclarationNode -> Result.success(node.returnType)

        is ASTNode.BlockNode -> Result.failure(CannotInferTypeException())
        is ASTNode.IfNode -> Result.failure(CannotInferTypeException())
        is ASTNode.ProgramNode -> Result.failure(CannotInferTypeException())
        is ASTNode.UnknownNode -> Result.failure(CannotInferTypeException())
    }
}
