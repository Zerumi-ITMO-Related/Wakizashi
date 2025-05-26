import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FunctionParameter(val name: String, val paramType: String)

@Serializable
sealed class ASTNode(val type: String) {
    abstract val line: Int
    abstract val column: Int

    @Serializable
    @SerialName("PROGRAM")
    data class ProgramNode(val children: List<ASTNode>, override val line: Int, override val column: Int) :
        ASTNode("PROGRAM")

    @Serializable
    @SerialName("BLOCK")
    data class BlockNode(val children: List<ASTNode>, override val line: Int, override val column: Int) :
        ASTNode("BLOCK")

    @Serializable
    @SerialName("VAL_DECLARATION")
    data class ValueDeclarationNode(
        val name: String,
        val valType: String,
        val initializer: ASTNode,
        override val line: Int,
        override val column: Int
    ) : ASTNode("VAL_DECLARATION")

    @Serializable
    @SerialName("LITERAL")
    data class LiteralNode(val value: String, val valType: String, override val line: Int, override val column: Int) :
        ASTNode("LITERAL")

    @Serializable
    @SerialName("RETURN")
    data class ReturnNode(val value: ASTNode, override val line: Int, override val column: Int) : ASTNode("RETURN")

    @Serializable
    @SerialName("IDENT")
    data class IdentNode(val name: String, override val line: Int, override val column: Int) : ASTNode("IDENT")

    @Serializable
    @SerialName("BIN_OP")
    data class BinaryOperationNode(
        val op: String, val left: ASTNode, val right: ASTNode, override val line: Int, override val column: Int
    ) : ASTNode("BIN_OP")

    @Serializable
    @SerialName("IF")
    data class IfNode(
        val condition: ASTNode,
        val then: ASTNode,
        val `else`: ASTNode?,
        override val line: Int,
        override val column: Int
    ) : ASTNode("IF")

    @Serializable
    @SerialName("FUN_DECLARATION")
    data class FunctionDeclarationNode(
        val name: String,
        val returnType: String,
        val params: List<FunctionParameter>,
        val body: ASTNode,
        override val line: Int,
        override val column: Int
    ) : ASTNode("FUN_DECLARATION")

    @Serializable
    @SerialName("FUN_CALL")
    data class FunctionCallNode(
        val name: String, val args: List<ASTNode>, override val line: Int, override val column: Int
    ) : ASTNode("FUN_CALL")

    @Serializable
    @SerialName("UNKNOWN")
    class UnknownNode(override val line: Int, override val column: Int) : ASTNode("UNKNOWN")
}
