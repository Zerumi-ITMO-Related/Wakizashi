package codegen

import ASTNode
import ASTVisitor
import error.UnknownNodeInASTException
import org.jetbrains.annotations.Contract
import visitAST

fun generateSasm(ast: ASTNode) =
    generateCodegenContextFromAST(ast).fold(
        onSuccess = { Result.success(generateSasmFromContext(it)) },
        onFailure = { Result.failure(it) }
    )

fun generateSasmFromContext(context: CodegenContext) = buildString {
    context.references.forEach { reference ->
        this.appendLine("${reference.label}:")
        this.appendLine("word 0")
    }
    context.literals.forEach { literal ->
        this.appendLine("${literal.label}:")
        this.appendLine(literal.literals.joinToString("\n") { "word $it" })
    }
    context.functions.forEach {
        this.appendLine("${it.label}:")
        this.appendLine(it.assembly.joinToString("\n"))
    }

    // and generate entry point
    this.appendLine("start:")
    this.appendLine("lit main")
    this.appendLine("call")
    this.appendLine("halt")
}

fun generateCodegenContextFromAST(ast: ASTNode): Result<CodegenContext> = ASTVisitor(
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
    ast: ASTNode.ProgramNode, state: CodegenContext, astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { Result.failure(it) })
    }
}

fun visitBlockNode(
    ast: ASTNode.BlockNode, state: CodegenContext, astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    return ast.children.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { Result.failure(it) })
    }
}

fun visitBinaryOperationNode(
    ast: ASTNode.BinaryOperationNode,
    state: CodegenContext,
    astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    return astVisitor.visitAST(ast.left, state).fold(
        onSuccess = { updatedState ->
            astVisitor.visitAST(ast.right, updatedState).fold(
                onSuccess = { Result.success(it) },
                onFailure = { return Result.failure(it) }
            )
        },
        onFailure = { return Result.failure(it) }
    )
}

fun visitFunctionDeclarationNode(
    ast: ASTNode.FunctionDeclarationNode,
    state: CodegenContext,
    astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    val stateWithLiterals = astVisitor.visitAST(ast.body, state).fold(
        onSuccess = { it },
        onFailure = { return Result.failure(it) }
    ).withReferences(ast.params.map { ReferenceDeclaration(it.name) })
    val paramInit = ast.params.map {
        listOf(
            "lit ${it.name}",
            "store"
        )
    }.flatten()
    val functionDeclaration = FunctionDeclaration(
        ast.name,
        generateFunctionBody(ast.body, stateWithLiterals).fold(
            onSuccess = { paramInit.plus(it) },
            onFailure = { return Result.failure(it) }
        )
    )
    return Result.success(stateWithLiterals.withFunction(functionDeclaration))
}

fun visitReturnNode(
    ast: ASTNode.ReturnNode,
    state: CodegenContext,
    astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    return astVisitor.visitAST(ast.value, state).fold(
        onSuccess = { Result.success(it) },
        onFailure = { return Result.failure(it) }
    )
}

fun visitFunctionCallNode(
    ast: ASTNode.FunctionCallNode,
    state: CodegenContext,
    astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    return ast.args.fold(Result.success(state)) { ctx, child ->
        ctx.fold(onSuccess = { astVisitor.visitAST(child, it) }, onFailure = { Result.failure(it) })
    }
}

fun visitValueDeclarationNode(
    ast: ASTNode.ValueDeclarationNode,
    state: CodegenContext,
    astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    val stateWithLiterals = astVisitor.visitAST(ast.initializer, state).fold(
        onSuccess = { it },
        onFailure = { return Result.failure(it) }
    )
    val reference = ReferenceDeclaration(
        label = ast.name
    )
    val initFunction =
        FunctionDeclaration("init_${ast.name}", generateFunctionBody(ast.initializer, stateWithLiterals).fold(
            onSuccess = { it.plus(listOf("lit ${ast.name}", "store", "ret")) },
            onFailure = { return Result.failure(it) }
        ))
    return Result.success(stateWithLiterals.withFunction(initFunction).withReference(reference))
}

fun visitIfNode(
    ast: ASTNode.IfNode,
    state: CodegenContext,
    astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    return astVisitor.visitAST(ast.condition, state).fold(
        onSuccess = { updatedState ->
            astVisitor.visitAST(ast.then, updatedState).fold(
                onSuccess = { updatedState2 ->
                    ast.`else`?.let { astVisitor.visitAST(it, updatedState2) } ?: Result.success(updatedState2)
                },
                onFailure = { Result.failure(it) }
            )
        },
        onFailure = { Result.failure(it) }
    )
}

fun visitLiteralNode(
    ast: ASTNode.LiteralNode,
    state: CodegenContext,
    astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    val wordList = generateLiterals(ast.value, LiteralTypes.valueOf(ast.valType.uppercase()))
    val literal = LiteralDeclaration("lit-${state.literals.size}", ast.value, wordList)
    return Result.success(state.withLiteral(literal))
}

fun visitIdentNode(
    ast: ASTNode.IdentNode,
    state: CodegenContext,
    astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    return Result.success(state)
}

@Contract("_, _, _ -> fail")
fun visitUnknownNode(
    ast: ASTNode.UnknownNode, state: CodegenContext, astVisitor: ASTVisitor<CodegenContext>
): Result<CodegenContext> {
    return Result.failure(UnknownNodeInASTException(ast.line, ast.column))
}
