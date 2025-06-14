package semantic

import ASTNode
import ASTVisitor
import error.*
import visitAST

fun checkASTSemantic(ast: ASTNode): Result<SemanticContext> = ASTVisitor(
    visitProgramNode = ::visitProgramNode,
    visitFunctionDeclarationNode = ::visitFunctionDeclarationNode,
    visitReturnNode = ::visitReturnNode,
    visitBlockNode = ::visitBlockNode,
    visitValueDeclarationNode = ::visitValueDeclarationNode,
    visitBinaryOperationNode = ::visitBinaryOperationNode,
    visitFunctionCallNode = ::visitFunctionCallNode,
    visitIdentNode = ::visitIdentNode,
    visitIfNode = ::visitIfNode,
    visitLiteralNode = ::visitLiteralNode,
    visitUnknownNode = ::visitUnknownNode
).visitAST(ast, stdlibContext())

fun visitProgramNode(
    ast: ASTNode.ProgramNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { Result.failure(it) })
    }
}

fun visitBlockNode(
    ast: ASTNode.BlockNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { Result.failure(it) })
    }
}

fun visitFunctionDeclarationNode(
    ast: ASTNode.FunctionDeclarationNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    val name = ast.name
    val params = ast.params.map { VariableDeclaration(it.name, it.paramType) }
    val returnType = ast.returnType
    val blockReturnType = inferType(ast.body, state.withVariables(params)).fold(
        onSuccess = { it },
        onFailure = { return Result.failure(it) }
    )
    if (returnType.uppercase() != LiteralTypes.UNIT.name && returnType != blockReturnType)
        return Result.failure(WrongReturnTypeException(ast.line, ast.column))
    astVisitor.visitAST(
        ast.body,
        state.withVariables(params).withFunction(FunctionDeclaration(name, params, returnType))
    ).onFailure { return Result.failure(it) }
    return Result.success(state.withFunction(FunctionDeclaration(name, params, returnType)))
}

fun visitReturnNode(
    ast: ASTNode.ReturnNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    astVisitor.visitAST(ast.value, state).onFailure { return Result.failure(it) }
    return Result.success(state)
}

fun visitValueDeclarationNode(
    ast: ASTNode.ValueDeclarationNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    val ident = ast.name
    val valueType = inferType(ast, state).onFailure { return Result.failure(it) }
    val initType = inferType(ast.initializer, state).onFailure { return Result.failure(it) }
    astVisitor.visitAST(ast.initializer, state).onFailure { return Result.failure(it) }
    return if (valueType != initType) Result.failure(
        TypeMismatchException(
            ast.line,
            ast.column,
            valueType.getOrNull()!!, // onFailure above
            initType.getOrNull()!!, // onFailure above
        )
    )
    else Result.success(state.withVariable(VariableDeclaration(ident, ast.valType)))
}

fun visitBinaryOperationNode(
    ast: ASTNode.BinaryOperationNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    astVisitor.visitAST(ast.left, state).onFailure { return Result.failure(it) }
    astVisitor.visitAST(ast.right, state).onFailure { return Result.failure(it) }
    return Result.success(state)
}

fun visitFunctionCallNode(
    ast: ASTNode.FunctionCallNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    ast.args.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { ctx })
    }.onFailure { return Result.failure(it) }
    val callingFunction = state.functions.find { it.name == ast.name } ?: return Result.failure(
        MissedDeclarationException(
            ast.line,
            ast.column,
            ast.name,
        )
    )
    if (ast.args.size != callingFunction.params.size) return Result.failure(
        FunctionArgumentsCountMismatchException(
            ast.line,
            ast.column,
            ast.name,
            callingFunction.params.size,
            ast.args.size,
        )
    )
    ast.args.zip(callingFunction.params).map { (arg, param) ->
        val actualType = inferType(arg, state).getOrElse { return Result.failure(it) }
        val expectedType = param.type
        if (!expectedType.equals(actualType, ignoreCase = true)) return Result.failure(
            TypeMismatchException(
                arg.line,
                arg.column,
                expectedType,
                actualType
            )
        )
    }
    return Result.success(state)
}

fun visitIdentNode(
    ast: ASTNode.IdentNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    state.variables.find { it.name == ast.name } ?: return Result.failure(
        MissedDeclarationException(
            ast.line, ast.column, ast.name
        )
    )
    return Result.success(state)
}

fun visitIfNode(
    ast: ASTNode.IfNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    astVisitor.visitAST(ast.condition, state).onFailure { return Result.failure(it) }
    astVisitor.visitAST(ast.then, state).onFailure { return Result.failure(it) }
    if (ast.`else` != null) astVisitor.visitAST(ast.`else`, state).onFailure { return Result.failure(it) }
    return Result.success(state)
}

fun visitLiteralNode(
    ast: ASTNode.LiteralNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    return Result.success(state)
}

fun visitUnknownNode(
    ast: ASTNode.UnknownNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    return Result.failure(UnknownNodeInASTException(ast.line, ast.column))
}
