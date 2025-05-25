package codegen

import ASTNode
import ASTVisitor
import visitAST

fun generateSasm(ast: ASTNode): Result<StringBuilder> = ASTVisitor(
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
).visitAST(ast, StringBuilder())

fun visitProgramNode(
    ast: ASTNode.ProgramNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting Program node")
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { ctx })
    }
}

fun visitFunctionDeclarationNode(
    ast: ASTNode.FunctionDeclarationNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting Function declaration node")
    astVisitor.visitAST(ast.body, state)
    return Result.success(state)
}

fun visitReturnNode(
    ast: ASTNode.ReturnNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting return node")
    astVisitor.visitAST(ast.value, state)
    return Result.success(state)
}

fun visitBlockNode(
    ast: ASTNode.BlockNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting block node")
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { ctx })
    }
}

fun visitValueDeclarationNode(
    ast: ASTNode.ValueDeclarationNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting value declaration node")
    astVisitor.visitAST(ast.initializer, state)
    return Result.success(state)
}

fun visitBinaryOperationNode(
    ast: ASTNode.BinaryOperationNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting value declaration node")
    astVisitor.visitAST(ast.left, state)
    astVisitor.visitAST(ast.right, state)
    return Result.success(state)
}

fun visitFunctionCallNode(
    ast: ASTNode.FunctionCallNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting function call node")
    ast.args.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { ctx })
    }
    return Result.success(state)
}

fun visitIdentNode(
    ast: ASTNode.IdentNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting identifier node")
    println(ast)
    return Result.success(state)
}

fun visitIfNode(
    ast: ASTNode.IfNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting if node")
    astVisitor.visitAST(ast.condition, state)
    astVisitor.visitAST(ast.then, state)
    if (ast.`else` != null) astVisitor.visitAST(ast.`else`, state)
    return Result.success(state)
}

fun visitLiteralNode(
    ast: ASTNode.LiteralNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visiting Literal node")
    println(ast)
    return Result.success(state)
}

fun visitUnknownNode(
    ast: ASTNode.UnknownNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    println("Visit Unknown node")
    return Result.success(state)
}
