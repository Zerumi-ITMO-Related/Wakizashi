package semantic

import ASTNode
import ASTVisitor
import error.TypeMismatchException
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
).visitAST(ast, SemanticContext.ProgramContext(""))

fun visitProgramNode(
    ast: ASTNode.ProgramNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting Program node")
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { ctx })
    }
}

fun visitFunctionDeclarationNode(
    ast: ASTNode.FunctionDeclarationNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting Function declaration node")
    val bodyContext = astVisitor.visitAST(ast.body, state)
    return Result.success(state)
}

fun visitReturnNode(
    ast: ASTNode.ReturnNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting return node")
    astVisitor.visitAST(ast.value, state)
    return Result.success(state)
}

fun visitBlockNode(
    ast: ASTNode.BlockNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting block node")
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { ctx })
    }
}

fun visitValueDeclarationNode(
    ast: ASTNode.ValueDeclarationNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting value declaration node")
    // value declaration should be only binop or literal or function call
    val value = astVisitor.visitAST(ast.initializer, state)
    return Result.success(state)
}

fun visitBinaryOperationNode(
    ast: ASTNode.BinaryOperationNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting value declaration node")
    astVisitor.visitAST(ast.left, state)
    astVisitor.visitAST(ast.right, state)
    return Result.success(state)
}

fun visitFunctionCallNode(
    ast: ASTNode.FunctionCallNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting function call node")
    ast.args.fold(Result.success(state)) { ctx, child ->
        ctx.fold(
            onSuccess = { astVisitor.visitAST(child, it) },
            onFailure = { ctx }
        )
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
    astVisitor.visitAST(ast.condition, state)
    astVisitor.visitAST(ast.then, state)
    if (ast.`else` != null) astVisitor.visitAST(ast.`else`, state)
    return Result.success(state)
}

fun visitLiteralNode(
    ast: ASTNode.LiteralNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visiting Literal node")
    // infer type
    val value = ast.value
    val literalType = when {
        Regex("[0-9]+").matches(value) -> LiteralTypes.INT
        Regex("\"[^\"]*\"").matches(value) -> LiteralTypes.STRING
        Regex("true|false").matches(value) -> LiteralTypes.BOOLEAN
        Regex("Unit").matches(value) -> LiteralTypes.UNIT
        else -> return Result.failure(TypeMismatchException())
    }
    val requestedType = LiteralTypes.valueOf(ast.valType.uppercase())

    println("Expected Type: $requestedType, actual: $literalType")

    return if (requestedType == literalType) Result.success(state) else Result.failure(TypeMismatchException())
}

fun visitUnknownNode(
    ast: ASTNode.UnknownNode, state: SemanticContext, astVisitor: ASTVisitor<SemanticContext>
): Result<SemanticContext> {
    println("Visit Unknown node")
    return Result.success(state)
}
