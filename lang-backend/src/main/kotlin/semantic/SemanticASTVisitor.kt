package semantic

import ASTNode
import ASTVisitor
import error.TypeMismatchException
import error.UnknownNodeInASTException
import error.WrongReturnTypeException
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
    println("Visiting Program node")
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { Result.failure(it) })
    }
}

fun visitBlockNode(
    ast: ASTNode.BlockNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting block node")
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { Result.failure(it) })
    }
}

fun visitFunctionDeclarationNode(
    ast: ASTNode.FunctionDeclarationNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting Function declaration node")
    val name = ast.name
    val params = ast.params.map { VariableDeclaration(it.name, it.paramType) }
    val returnType = ast.returnType
    val blockReturnType = inferType(ast.body, state.withVariables(params)).fold(
        onSuccess = { it },
        onFailure = { return Result.failure(it) }
    )
    if (returnType.uppercase() != LiteralTypes.UNIT.name && returnType != blockReturnType)
        return Result.failure(WrongReturnTypeException(ast.line, ast.column))
    astVisitor.visitAST(ast.body, state.withVariables(params)).onFailure { return Result.failure(it) }
    return Result.success(state.withFunction(FunctionDeclaration(name, params, returnType)))
}

fun visitReturnNode(
    ast: ASTNode.ReturnNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting return node")
    astVisitor.visitAST(ast.value, state).onFailure { return Result.failure(it) }
    return Result.success(state)
}

fun visitValueDeclarationNode(
    ast: ASTNode.ValueDeclarationNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting value declaration node")
    val ident = ast.name
    val valueType = inferType(ast, state).onFailure { return Result.failure(it) }
    val initType = inferType(ast.initializer, state).onFailure { return Result.failure(it) }
    astVisitor.visitAST(ast.initializer, state).onFailure { return Result.failure(it) }
    return if (valueType != initType) Result.failure(TypeMismatchException(ast.line, ast.column))
    else Result.success(state.withVariable(VariableDeclaration(ident, ast.valType)))
}

fun visitBinaryOperationNode(
    ast: ASTNode.BinaryOperationNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting value declaration node")
    astVisitor.visitAST(ast.left, state).onFailure { return Result.failure(it) }
    astVisitor.visitAST(ast.right, state).onFailure { return Result.failure(it) }
    return Result.success(state)
}

fun visitFunctionCallNode(
    ast: ASTNode.FunctionCallNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting function call node")
    ast.args.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { ctx })
    }
    return Result.success(state)
}

fun visitIdentNode(
    ast: ASTNode.IdentNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting identifier node")
    println(ast)
    return Result.success(state)
}

fun visitIfNode(
    ast: ASTNode.IfNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting if node")
    astVisitor.visitAST(ast.condition, state).onFailure { return Result.failure(it) }
    astVisitor.visitAST(ast.then, state).onFailure { return Result.failure(it) }
    if (ast.`else` != null) astVisitor.visitAST(ast.`else`, state).onFailure { return Result.failure(it) }
    return Result.success(state)
}

fun visitLiteralNode(
    ast: ASTNode.LiteralNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting Literal node")
    return Result.success(state)
}

fun visitUnknownNode(
    ast: ASTNode.UnknownNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visit Unknown node")
    return Result.failure(UnknownNodeInASTException(ast.line, ast.column))
}
