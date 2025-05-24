#include "ast_print.h"
#include "ast.h"
#include <stdio.h>
#include <string.h>

// Вспомогательная функция для отображения отступов
static void print_indent(int indent, FILE *output) {
  for (int i = 0; i < indent; i++) {
    fprintf(output, "  ");
  }
}

// Рекурсивная функция для визуализации AST с отступами
static void visualize_ast_with_indent(ASTNode *node, int indent, FILE *output) {
  if (!node) {
    print_indent(indent, output);
    fprintf(output, "NULL\n");
    return;
  }

  switch (node->type) {
  case NODE_PROGRAM:
    print_indent(indent, output);
    fprintf(output, "PROGRAM\n");
    for (size_t i = 0; i < node->block.children.size; i++) {
      visualize_ast_with_indent(node->block.children.items[i], indent + 1,
                                output);
    }
    break;

  case NODE_VARIABLE_DECLARATION:
    print_indent(indent, output);
    fprintf(output, "VAR_DECL: %s (type: %s)\n",
            node->variable.name, node->variable.var_type);
    if (node->variable.initializer) {
      print_indent(indent + 1, output);
      fprintf(output, "INIT:\n");
      visualize_ast_with_indent(node->variable.initializer, indent + 2, output);
    }
    break;

  case NODE_BINARY_OPERATION:
    print_indent(indent, output);
    fprintf(output, "BIN_OP: %s\n", node->binary_op.op_type);
    print_indent(indent + 1, output);
    fprintf(output, "LEFT:\n");
    visualize_ast_with_indent(node->binary_op.left, indent + 2, output);
    print_indent(indent + 1, output);
    fprintf(output, "RIGHT:\n");
    visualize_ast_with_indent(node->binary_op.right, indent + 2, output);
    break;

  case NODE_LITERAL:
    print_indent(indent, output);
    if (strcmp(node->literal.type, "int") == 0) {
      fprintf(output, "LITERAL: %d (type: int)\n", node->literal.int_value);
    } else if (strcmp(node->literal.type, "string") == 0) {
      fprintf(output, "LITERAL: \"%s\" (type: string)\n",
              node->literal.string_value);
    } else {
      fprintf(output, "LITERAL: (unknown type)\n");
    }
    break;

  case NODE_IDENTIFIER:
    print_indent(indent, output);
    fprintf(output, "IDENT: %s\n", node->identifier.name);
    break;

  case NODE_ASSIGNMENT:
    print_indent(indent, output);
    fprintf(output, "ASSIGN: %s\n", node->assignment.target);
    print_indent(indent + 1, output);
    fprintf(output, "VALUE:\n");
    visualize_ast_with_indent(node->assignment.value, indent + 2, output);
    break;

  case NODE_IF_STATEMENT:
    print_indent(indent, output);
    fprintf(output, "IF\n");
    print_indent(indent + 1, output);
    fprintf(output, "CONDITION:\n");
    visualize_ast_with_indent(node->if_stmt.condition, indent + 2, output);
    print_indent(indent + 1, output);
    fprintf(output, "THEN:\n");
    visualize_ast_with_indent(node->if_stmt.then_branch, indent + 2, output);
    if (node->if_stmt.else_branch) {
      print_indent(indent + 1, output);
      fprintf(output, "ELSE:\n");
      visualize_ast_with_indent(node->if_stmt.else_branch, indent + 2, output);
    }
    break;

  case NODE_BLOCK:
    print_indent(indent, output);
    fprintf(output, "BLOCK\n");
    for (size_t i = 0; i < node->block.children.size; i++) {
      visualize_ast_with_indent(node->block.children.items[i], indent + 1,
                                output);
    }
    break;

  case NODE_FUNCTION_DECLARATION:
    print_indent(indent, output);
    fprintf(output, "FUNC_DECL: %s (return type: %s)\n",
            node->function_decl.name, node->function_decl.return_type);
    print_indent(indent + 1, output);
    fprintf(output, "PARAMS:\n");
    for (size_t i = 0; i < node->function_decl.param_count; i++) {
      print_indent(indent + 2, output);
      fprintf(output, "%s: %s\n", node->function_decl.param_names[i],
              node->function_decl.param_types[i]);
    }
    if (node->function_decl.body) {
      print_indent(indent + 1, output);
      fprintf(output, "BODY:\n");
      visualize_ast_with_indent(node->function_decl.body, indent + 2, output);
    }
    break;

  case NODE_RETURN_STATEMENT:
    print_indent(indent, output);
    fprintf(output, "RETURN\n");
    if (node->return_stmt.value) {
      print_indent(indent + 1, output);
      fprintf(output, "VALUE:\n");
      visualize_ast_with_indent(node->return_stmt.value, indent + 2, output);
    } else {
      print_indent(indent + 1, output);
      fprintf(output, "NO VALUE\n");
    }
    break;

  case NODE_FUNCTION_CALL:
    print_indent(indent, output);
    fprintf(output, "FUNC_CALL: %s\n", node->function_call.function_name);
    if (node->function_call.arg_count > 0) {
      print_indent(indent + 1, output);
      fprintf(output, "ARGUMENTS:\n");
      for (size_t i = 0; i < node->function_call.arg_count; i++) {
        visualize_ast_with_indent(node->function_call.arguments[i], indent + 2,
                                  output);
      }
    } else {
      print_indent(indent + 1, output);
      fprintf(output, "NO ARGUMENTS\n");
    }
    break;

  default:
    print_indent(indent, output);
    fprintf(output, "UNKNOWN NODE TYPE: %d\n", node->type);
    break;
  }
}

void visualize_ast(ASTNode *node, FILE *output) {
  visualize_ast_with_indent(node, 0, output);
}

int save_ast_to_file(ASTNode *node, const char *filename) {
  FILE *file = fopen(filename, "w");
  if (!file) {
    return -1;
  }

  visualize_ast(node, file);
  fclose(file);
  return 0;
}
