data class ASTVisitor<T>(
    val visitProgramNode: (ASTNode.ProgramNode, T, ASTVisitor<T>) -> Result<T>,
    val visitFunctionDeclarationNode: (ASTNode.FunctionDeclarationNode, T, ASTVisitor<T>) -> Result<T>,
    val visitReturnNode: (ASTNode.ReturnNode, T, ASTVisitor<T>) -> Result<T>,
    val visitBlockNode: (ASTNode.BlockNode, T, ASTVisitor<T>) -> Result<T>,
    val visitValueDeclarationNode: (ASTNode.ValueDeclarationNode, T, ASTVisitor<T>) -> Result<T>,
    val visitBinaryOperationNode: (ASTNode.BinaryOperationNode, T, ASTVisitor<T>) -> Result<T>,
    val visitFunctionCallNode: (ASTNode.FunctionCallNode, T, ASTVisitor<T>) -> Result<T>,
    val visitIdentNode: (ASTNode.IdentNode, T, ASTVisitor<T>) -> Result<T>,
    val visitIfNode: (ASTNode.IfNode, T, ASTVisitor<T>) -> Result<T>,
    val visitLiteralNode: (ASTNode.LiteralNode, T, ASTVisitor<T>) -> Result<T>,
    val visitUnknownNode: (ASTNode.UnknownNode, T, ASTVisitor<T>) -> Result<T>
)

fun<T> ASTVisitor<T>.visitAST(ast: ASTNode, state: T): Result<T> {
    return when (ast) {
        is ASTNode.ProgramNode -> visitProgramNode(ast, state, this)
        is ASTNode.FunctionDeclarationNode -> visitFunctionDeclarationNode(ast, state, this)
        is ASTNode.ReturnNode -> visitReturnNode(ast, state, this)
        is ASTNode.BlockNode -> visitBlockNode(ast, state, this)
        is ASTNode.ValueDeclarationNode -> visitValueDeclarationNode(ast, state, this)
        is ASTNode.BinaryOperationNode -> visitBinaryOperationNode(ast, state, this)
        is ASTNode.FunctionCallNode -> visitFunctionCallNode(ast, state, this)
        is ASTNode.IdentNode -> visitIdentNode(ast, state, this)
        is ASTNode.IfNode -> visitIfNode(ast, state, this)
        is ASTNode.LiteralNode -> visitLiteralNode(ast, state, this)
        is ASTNode.UnknownNode -> visitUnknownNode(ast, state, this)
    }
}
