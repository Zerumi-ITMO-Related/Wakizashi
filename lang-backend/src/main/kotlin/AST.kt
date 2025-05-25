import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FunctionParameter(val name: String, val paramType: String)

@Serializable
sealed class ASTNode(val type: String) {
    @Serializable
    @SerialName("PROGRAM")
    data class ProgramNode(val children : List<ASTNode>) : ASTNode("PROGRAM")
    @Serializable
    @SerialName("BLOCK")
    data class BlockNode(val children: List<ASTNode>) : ASTNode("BLOCK")
    @Serializable
    @SerialName("VAL_DECLARATION")
    data class ValueDeclarationNode(val name: String, val valType: String, val initializer: ASTNode) : ASTNode("VAL_DECLARATION")
    @Serializable
    @SerialName("LITERAL")
    data class LiteralNode(val value: String, val valType: String) : ASTNode("LITERAL")
    @Serializable
    @SerialName("RETURN")
    data class ReturnNode(val value: ASTNode) : ASTNode("RETURN")
    @Serializable
    @SerialName("IDENT")
    data class IdentNode(val name: String) : ASTNode("IDENT")
    @Serializable
    @SerialName("BIN_OP")
    data class BinaryOperationNode(val op: String, val left: ASTNode, val right: ASTNode) : ASTNode("BIN_OP")
    @Serializable
    @SerialName("IF")
    data class IfNode(val condition: ASTNode, val then: ASTNode, val `else`: ASTNode?) : ASTNode("IF")
    @Serializable
    @SerialName("FUN_DECLARATION")
    data class FunctionDeclarationNode(val name: String, val returnType: String, val params: List<FunctionParameter>, val body: ASTNode) : ASTNode("FUN_DECLARATION")
    @Serializable
    @SerialName("FUN_CALL")
    data class FunctionCallNode(val name: String, val args: List<ASTNode>) : ASTNode("FUN_CALL")
    @Serializable
    @SerialName("UNKNOWN")
    class UnknownNode : ASTNode("UNKNOWN")
}
