#include "ast.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

// Глобальная переменная для хранения корня AST
ASTNode *ast_root = NULL;

// Инициализация списка узлов
void init_node_list(NodeList *list) {
  list->items = NULL;
  list->size = 0;
  list->capacity = 0;
}

// Добавление узла в список
void add_to_list(NodeList *list, ASTNode *node) {
  if (list->size >= list->capacity) {
    size_t new_capacity = list->capacity == 0 ? 4 : list->capacity * 2;
    ASTNode **new_items =
        (ASTNode **)realloc(list->items, new_capacity * sizeof(ASTNode *));
    if (new_items) {
      list->items = new_items;
      list->capacity = new_capacity;
    } else {
      // Обработка ошибки выделения памяти
      return;
    }
  }
  list->items[list->size++] = node;
}

// Создание строки в куче
char *strdup_custom(const char *str) {
  if (!str)
    return NULL;
  size_t len = strlen(str);
  char *new_str = (char *)malloc(len + 1);
  if (new_str) {
    strcpy(new_str, str);
  }
  return new_str;
}

// Создание узла программы
ASTNode *create_program_node() {
  ASTNode *node = (ASTNode *)malloc(sizeof(ASTNode));
  if (node) {
    node->type = NODE_PROGRAM;
    init_node_list(&node->block.children);
  }
  return node;
}

// Создание узла объявления переменной с инициализатором
ASTNode *create_variable_declaration(const char *name, const char *var_type,
                                     ASTNode *initializer) {
  ASTNode *node = (ASTNode *)malloc(sizeof(ASTNode));
  if (node) {
    node->type = NODE_VARIABLE_DECLARATION;
    node->variable.name = strdup_custom(name);
    node->variable.var_type = strdup_custom(var_type);
    node->variable.initializer = initializer;
  }
  return node;
}

// Создание узла бинарной операции
ASTNode *create_binary_operation(const char *op_type, ASTNode *left,
                                 ASTNode *right) {
  ASTNode *node = (ASTNode *)malloc(sizeof(ASTNode));
  if (node) {
    node->type = NODE_BINARY_OPERATION;
    node->binary_op.op_type = strdup_custom(op_type);
    node->binary_op.left = left;
    node->binary_op.right = right;
  }
  return node;
}

// Создание узла литерала
ASTNode *create_literal(const char * value, const char *type) {
  ASTNode *node = (ASTNode *)malloc(sizeof(ASTNode));
  if (node) {
    node->type = NODE_LITERAL;
    node->literal.string_value = strdup_custom(value);
    node->literal.type = strdup_custom(type);
  }
  return node;
}

// Создание узла идентификатора
ASTNode *create_identifier_node(const char *name) {
  ASTNode *node = (ASTNode *)malloc(sizeof(ASTNode));
  if (node) {
    node->type = NODE_IDENTIFIER;
    node->identifier.name = strdup_custom(name);
  }
  return node;
}

// Создание узла if-условия
ASTNode *create_if_node(ASTNode *condition, ASTNode *then_branch,
                        ASTNode *else_branch) {
  ASTNode *node = (ASTNode *)malloc(sizeof(ASTNode));
  if (node) {
    node->type = NODE_IF_STATEMENT;
    node->if_stmt.condition = condition;
    node->if_stmt.then_branch = then_branch;
    node->if_stmt.else_branch = else_branch;
  }
  return node;
}

// Создание узла блока
ASTNode *create_block_node() {
  ASTNode *node = (ASTNode *)malloc(sizeof(ASTNode));
  if (node) {
    node->type = NODE_BLOCK;
    init_node_list(&node->block.children);
  }
  return node;
}

ASTNode *create_function_declaration(const char *name, char **param_names,
                                     char **param_types, size_t param_count,
                                     const char *return_type, ASTNode *body) {
  // printf("Creating function declaration node for function: %s\n", name);
  ASTNode *node = malloc(sizeof(ASTNode));
  node->type = NODE_FUNCTION_DECLARATION;
  node->function_decl.name = strdup(name);
  node->function_decl.param_names = param_names;
  node->function_decl.param_types = param_types;
  node->function_decl.param_count = param_count;
  node->function_decl.return_type = strdup(return_type);
  node->function_decl.body = body;
  return node;
}

ASTNode *create_return_statement(ASTNode *value) {
  ASTNode *node = malloc(sizeof(ASTNode));
  node->type = NODE_RETURN_STATEMENT;
  node->return_stmt.value = value;
  return node;
}

ASTNode *create_function_call(const char *name, ASTNode *arg) {
    // printf("Creating function call node for function: %s\n", name);
    ASTNode *node = malloc(sizeof(ASTNode));
    node->type = NODE_FUNCTION_CALL;
    node->function_call.function_name = strdup_custom(name);

    node->function_call.arg_count = (arg != NULL) ? 1 : 0;
    node->function_call.arguments = (arg != NULL)
        ? malloc(sizeof(ASTNode *)) : NULL;

    if (arg != NULL) {
        node->function_call.arguments[0] = arg;
    }

    return node;
}

// Добавление дочернего узла
void add_child(ASTNode *parent, ASTNode *child) {
  if (parent && child &&
      (parent->type == NODE_BLOCK || parent->type == NODE_PROGRAM)) {
    add_to_list(&parent->block.children, child);
  }
}

// Освобождение памяти, выделенной под узел
void free_node(ASTNode *node) {
  if (!node)
    return;

  switch (node->type) {
  case NODE_PROGRAM:
  case NODE_BLOCK:
    for (size_t i = 0; i < node->block.children.size; i++) {
      free_node(node->block.children.items[i]);
    }
    free(node->block.children.items);
    break;

  case NODE_VARIABLE_DECLARATION:
    free(node->variable.name);
    free(node->variable.var_type);
    if (node->variable.initializer) {
      free_node(node->variable.initializer);
    }
    break;

  case NODE_BINARY_OPERATION:
    free(node->binary_op.op_type);
    free_node(node->binary_op.left);
    free_node(node->binary_op.right);
    break;

  case NODE_LITERAL:
    if (strcmp(node->literal.type, "string") == 0) {
      free(node->literal.string_value);
    }
    free(node->literal.type);
    break;

  case NODE_IDENTIFIER:
    free(node->identifier.name);
    break;

  case NODE_IF_STATEMENT:
    free_node(node->if_stmt.condition);
    free_node(node->if_stmt.then_branch);
    if (node->if_stmt.else_branch) {
      free_node(node->if_stmt.else_branch);
    }
    break;

  case NODE_FUNCTION_DECLARATION:
    break;
  case NODE_RETURN_STATEMENT:
    break;
  case NODE_FUNCTION_CALL:
    break;
  }

  free(node);
}
