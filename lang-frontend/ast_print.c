#include <stdio.h>
#include <string.h>
#include "ast.h"
#include "ast_print.h"

static void print_indent(int indent, FILE *output) {
  for (int i = 0; i < indent; i++) {
    fprintf(output, "  ");
  }
}

static void print_json_string(const char *s, FILE *output) {
  fputc('"', output);
  for (const char *p = s; *p; ++p) {
    if (*p == '"' || *p == '\\') {
      fputc('\\', output);
    }
    fputc(*p, output);
  }
  fputc('"', output);
}

static void visualize_ast_json(ASTNode *node, int indent, FILE *output) {
  if (!node) {
    fprintf(output, "null");
    return;
  }

  print_indent(indent, output);
  fprintf(output, "{\n");
  indent++;

  print_indent(indent, output);
  fprintf(output, "\"type\": ");
  switch (node->type) {
  case NODE_PROGRAM:
    fprintf(output, "\"PROGRAM\",\n");
    print_indent(indent, output);
    fprintf(output, "\"children\": [\n");
    for (size_t i = 0; i < node->block.children.size; i++) {
      visualize_ast_json(node->block.children.items[i], indent + 1, output);
      if (i + 1 < node->block.children.size) fprintf(output, ",\n");
      else fprintf(output, "\n");
    }
    print_indent(indent, output);
    fprintf(output, "]\n");
    break;

  case NODE_VARIABLE_DECLARATION:
    fprintf(output, "\"VAR_DECL\",\n");
    print_indent(indent, output);
    fprintf(output, "\"name\": ");
    print_json_string(node->variable.name, output);
    fprintf(output, ",\n");
    print_indent(indent, output);
    fprintf(output, "\"var_type\": ");
    print_json_string(node->variable.var_type, output);
    if (node->variable.initializer) {
      fprintf(output, ",\n");
      print_indent(indent, output);
      fprintf(output, "\"initializer\":\n");
      visualize_ast_json(node->variable.initializer, indent + 1, output);
      fprintf(output, "\n");
    } else {
      fprintf(output, "\n");
    }
    break;

  case NODE_LITERAL:
    fprintf(output, "\"LITERAL\",\n");
    print_indent(indent, output);
    fprintf(output, "\"value\": ");
    if (strcmp(node->literal.type, "int") == 0) {
      fprintf(output, "%d,\n", node->literal.int_value);
      print_indent(indent, output);
      fprintf(output, "\"value_type\": \"int\"\n");
    } else if (strcmp(node->literal.type, "string") == 0) {
      print_json_string(node->literal.string_value, output);
      fprintf(output, ",\n");
      print_indent(indent, output);
      fprintf(output, "\"value_type\": \"string\"\n");
    } else {
      fprintf(output, "null\n");
    }
    break;

  case NODE_IDENTIFIER:
    fprintf(output, "\"IDENT\",\n");
    print_indent(indent, output);
    fprintf(output, "\"name\": ");
    print_json_string(node->identifier.name, output);
    fprintf(output, "\n");
    break;

  case NODE_ASSIGNMENT:
    fprintf(output, "\"ASSIGN\",\n");
    print_indent(indent, output);
    fprintf(output, "\"target\": ");
    print_json_string(node->assignment.target, output);
    fprintf(output, ",\n");
    print_indent(indent, output);
    fprintf(output, "\"value\":\n");
    visualize_ast_json(node->assignment.value, indent + 1, output);
    break;

  case NODE_BINARY_OPERATION:
    fprintf(output, "\"BIN_OP\",\n");
    print_indent(indent, output);
    fprintf(output, "\"op\": ");
    print_json_string(node->binary_op.op_type, output);
    fprintf(output, ",\n");
    print_indent(indent, output);
    fprintf(output, "\"left\":\n");
    visualize_ast_json(node->binary_op.left, indent + 1, output);
    fprintf(output, ",\n");
    print_indent(indent, output);
    fprintf(output, "\"right\":\n");
    visualize_ast_json(node->binary_op.right, indent + 1, output);
    break;

  case NODE_IF_STATEMENT:
    fprintf(output, "\"IF\",\n");
    print_indent(indent, output);
    fprintf(output, "\"condition\":\n");
    visualize_ast_json(node->if_stmt.condition, indent + 1, output);
    fprintf(output, ",\n");
    print_indent(indent, output);
    fprintf(output, "\"then\":\n");
    visualize_ast_json(node->if_stmt.then_branch, indent + 1, output);
    if (node->if_stmt.else_branch) {
      fprintf(output, ",\n");
      print_indent(indent, output);
      fprintf(output, "\"else\":\n");
      visualize_ast_json(node->if_stmt.else_branch, indent + 1, output);
    } else {
      fprintf(output, "\n");
    }
    break;

  case NODE_RETURN_STATEMENT:
    fprintf(output, "\"RETURN\"");
    if (node->return_stmt.value) {
      fprintf(output, ",\n");
      print_indent(indent, output);
      fprintf(output, "\"value\":\n");
      visualize_ast_json(node->return_stmt.value, indent + 1, output);
    } else {
      fprintf(output, "\n");
    }
    break;

  case NODE_FUNCTION_DECLARATION:
    fprintf(output, "\"FUNC_DECL\",\n");
    print_indent(indent, output);
    fprintf(output, "\"name\": ");
    print_json_string(node->function_decl.name, output);
    fprintf(output, ",\n");
    print_indent(indent, output);
    fprintf(output, "\"return_type\": ");
    print_json_string(node->function_decl.return_type, output);
    fprintf(output, ",\n");
    print_indent(indent, output);
    fprintf(output, "\"params\": [");
    for (size_t i = 0; i < node->function_decl.param_count; i++) {
      fprintf(output, "\n");
      print_indent(indent + 1, output);
      fprintf(output, "{ \"name\": ");
      print_json_string(node->function_decl.param_names[i], output);
      fprintf(output, ", \"type\": ");
      print_json_string(node->function_decl.param_types[i], output);
      fprintf(output, " }");
      if (i + 1 < node->function_decl.param_count)
        fprintf(output, ",");
    }
    print_indent(indent, output);
    fprintf(output, "],\n");
    print_indent(indent, output);
    fprintf(output, "\"body\":\n");
    visualize_ast_json(node->function_decl.body, indent + 1, output);
    fprintf(output, "\n");
    break;

  case NODE_FUNCTION_CALL:
    fprintf(output, "\"FUNC_CALL\",\n");
    print_indent(indent, output);
    fprintf(output, "\"name\": ");
    print_json_string(node->function_call.function_name, output);
    fprintf(output, ",\n");
    print_indent(indent, output);
    fprintf(output, "\"args\": [\n");
    for (size_t i = 0; i < node->function_call.arg_count; i++) {
      visualize_ast_json(node->function_call.arguments[i], indent + 1, output);
      if (i + 1 < node->function_call.arg_count)
        fprintf(output, ",\n");
      else
        fprintf(output, "\n");
    }
    print_indent(indent, output);
    fprintf(output, "]\n");
    break;

  case NODE_BLOCK:
    fprintf(output, "\"BLOCK\",\n");
    print_indent(indent, output);
    fprintf(output, "\"children\": [\n");
    for (size_t i = 0; i < node->block.children.size; i++) {
      visualize_ast_json(node->block.children.items[i], indent + 1, output);
      if (i + 1 < node->block.children.size)
        fprintf(output, ",\n");
      else
        fprintf(output, "\n");
    }
    print_indent(indent, output);
    fprintf(output, "]\n");
    break;

  default:
    fprintf(output, "\"UNKNOWN\"\n");
    break;
  }

  indent--;
  print_indent(indent, output);
  fprintf(output, "}");
}

void visualize_ast(ASTNode *node, FILE *output) {
  visualize_ast_json(node, 0, output);
  fprintf(output, "\n");
}
