#ifndef AST_H
#define AST_H

#include <stddef.h>

// Типы узлов AST
typedef enum {
  NODE_PROGRAM = 1,
  NODE_VARIABLE_DECLARATION,
  NODE_FUNCTION_DECLARATION,
  NODE_FUNCTION_CALL,
  NODE_BINARY_OPERATION,
  NODE_LITERAL,
  NODE_IDENTIFIER,
  NODE_IF_STATEMENT,
  NODE_RETURN_STATEMENT,
  NODE_BLOCK,
} NodeType;

// Предварительное объявление структуры для рекурсивного использования
typedef struct ASTNode ASTNode;

typedef struct {
  ASTNode **items;
  size_t size;
  size_t capacity;
} NodeList;

// Структура для узла AST
struct ASTNode {
  NodeType type;
  int line; // номер строки в исходном коде
  int column; // номер столбца в исходном коде
  union {
    // Для NODE_VARIABLE_DECLARATION
    struct {
      char *name;
      char *var_type;
      ASTNode *initializer;
    } variable;

    // Для NODE_BINARY_OPERATION
    struct {
      char *op_type;
      ASTNode *left;
      ASTNode *right;
    } binary_op;

    // Для NODE_LITERAL
    struct {
      union {
        int int_value;
        float float_value;
        char *string_value;
      };
      char *type;
    } literal;

    // Для NODE_IDENTIFIER
    struct {
      char *name;
    } identifier;

    // Для NODE_IF_STATEMENT
    struct {
      ASTNode *condition;
      ASTNode *then_branch;
      ASTNode *else_branch; // может быть NULL
    } if_stmt;

    // Для NODE_BLOCK и NODE_PROGRAM
    struct {
      NodeList children;
    } block;

    // Для NODE_FUNCTION_DECLARATION
    struct {
      char *name;
      char **param_names; // массив имён параметров
      char **param_types; // массив типов параметров
      size_t param_count;
      char *return_type;
      ASTNode *body; // тело функции
    } function_decl;

    // Для NODE_FUNCTION_CALL
    struct {
      char *function_name;
      ASTNode **arguments;
      size_t arg_count;
    } function_call;

    // Для NODE_RETURN_STATEMENT
    struct {
      ASTNode *value; // может быть NULL
    } return_stmt;
  };
};

// Функции для создания узлов
ASTNode *create_program_node(int line, int column);

ASTNode *create_variable_declaration(const char *name, const char *var_type,
                                     ASTNode *initializer, int line, int column);

ASTNode *create_binary_operation(const char *op_type, ASTNode *left,
                                 ASTNode *right, int line, int column);

ASTNode *create_literal(const char *value, const char *type, int line, int column);

ASTNode *create_identifier_node(const char *name, int line, int column);

ASTNode *create_assignment_node(const char *target, ASTNode *value, int line, int column);

ASTNode *create_if_node(ASTNode *condition, ASTNode *then_branch,
                        ASTNode *else_branch, int line, int column);

ASTNode *create_while_node(ASTNode *condition, ASTNode *body, int line, int column);

ASTNode *create_round_node(const char *variable, ASTNode *start, ASTNode *end,
                           ASTNode *step, ASTNode *body, int line, int column);

ASTNode *create_block_node(int line, int column);

ASTNode *create_function_declaration(const char *name, char **param_names,
                                     char **param_types, size_t param_count,
                                     const char *return_type, ASTNode *body, int line, int column);

ASTNode *create_return_statement(ASTNode *value, int line, int column);

ASTNode *create_function_call(const char *name, ASTNode *arg, int line, int column);

// Функции для работы со списком узлов
void add_child(ASTNode *parent, ASTNode *child);

void free_node(ASTNode *node);

// Глобальная переменная для хранения корня AST
extern ASTNode *ast_root;

#endif /* AST_H */
