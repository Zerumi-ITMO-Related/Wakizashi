package codegen

import ASTNode
import ASTVisitor
import error.UnknownNodeInASTException
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
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { ctx })
    }
}

fun visitBlockNode(
    ast: ASTNode.BlockNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { ctx })
    }
}

fun visitUnknownNode(
    ast: ASTNode.UnknownNode, state: StringBuilder, astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    return Result.failure(UnknownNodeInASTException(ast.line, ast.column))
}

fun visitLiteralNode(
    ast: ASTNode.LiteralNode,
    state: StringBuilder,
    astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    state.appendLine("lit ${ast.value}")
    return Result.success(state)
}

fun visitBinaryOperationNode(
    ast: ASTNode.BinaryOperationNode,
    state: StringBuilder,
    astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    astVisitor.visitAST(ast.left, state)
    astVisitor.visitAST(ast.right, state)
    val op = when (ast.op) {
        "+" -> "add"
        "-" -> "sub"
        "*" -> "mul"
        "/" -> "div"
        else -> return Result.failure(IllegalArgumentException("Unknown operator ${ast.op}"))
    }
    state.appendLine(op)
    return Result.success(state)
}

fun visitFunctionDeclarationNode(
    ast: ASTNode.FunctionDeclarationNode,
    state: StringBuilder,
    astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    state.appendLine("${ast.name}:")
    return astVisitor.visitAST(ast.body, state)
}

fun visitReturnNode(
    ast: ASTNode.ReturnNode,
    state: StringBuilder,
    astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    astVisitor.visitAST(ast.value, state)
    state.appendLine("ret")
    return Result.success(state)
}

fun visitFunctionCallNode(
    ast: ASTNode.FunctionCallNode,
    state: StringBuilder,
    astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    for (arg in ast.args) {
        astVisitor.visitAST(arg, state)
    }
    state.appendLine("call ${ast.name}")
    return Result.success(state)
}

fun visitIdentNode(
    ast: ASTNode.IdentNode,
    state: StringBuilder,
    astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    state.appendLine("load ${ast.name}")
    return Result.success(state)
}

fun visitValueDeclarationNode(
    ast: ASTNode.ValueDeclarationNode,
    state: StringBuilder,
    astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    astVisitor.visitAST(ast.initializer, state)
    state.appendLine("store ${ast.name}")
    return Result.success(state)
}

fun visitIfNode(
    ast: ASTNode.IfNode,
    state: StringBuilder,
    astVisitor: ASTVisitor<StringBuilder>
): Result<StringBuilder> {
    val elseLabel = "else_${ast.hashCode()}"
    val endLabel = "endif_${ast.hashCode()}"

    astVisitor.visitAST(ast.condition, state)
    state.appendLine("jz $elseLabel")
    astVisitor.visitAST(ast.then, state)
    state.appendLine("jmp $endLabel")
    state.appendLine("$elseLabel:")
    ast.`else`?.let { astVisitor.visitAST(it, state) }
    state.appendLine("$endLabel:")

    return Result.success(state)
}

