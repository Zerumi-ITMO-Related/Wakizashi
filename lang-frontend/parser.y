%locations

%{
#include "ast.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern int yylex();
void yyerror(const char *s);

#define YYERROR_VERBOSE 1

extern int yylineno;
extern char* yytext;

extern ASTNode* ast_root;
%}

%union {
    char* sval;
    ASTNode* node;

    struct {
        char **names;
        char **types;
        size_t count;
    } params;
}

%token <sval> IDENT
%token <sval> LIT_INT LIT_STRING
%token <sval> LIT_BOOLEAN UNIT

%token VAL FUN RETURN
%token INT BOOLEAN STRING
%token IF ELSE
%token COLON SEMICOLON COMMA
%token LPAREN RPAREN LBRACE RBRACE LESS MORE EQUAL NOTEQUAL
%token ASSIGN PLUS MINUS MUL DIV AND OR

%type <sval> type 
%type <params> param param_list

%type <node> program statements statement declaration_statement
%type <node> expression if_statement function_call return_statement
%type <node> expression_list block

%right '='
%left '+' '-'
%left '*' '/' '%'
%left '.'

%start program

%%

program
    : statements { ast_root = $1; }
;

statements
    : statement statements
    {
        ASTNode *program;
        if ($1->type == NODE_PROGRAM) {
            program = $1;
        } else {
            program = create_program_node(@1.first_line, @1.first_column);
            add_child(program, $1);
        }
        if ($2) {
            for (size_t i = 0; i < $2->block.children.size; ++i) {
                add_child(program, $2->block.children.items[i]);
            }
        }
        $$ = program;
    }
    | statement
    {
        ASTNode *program = create_program_node(@1.first_line, @1.first_column);
        add_child(program, $1);
        $$ = program;
    }
;


/* fun main() : Unit { ... } */
statement
    : declaration_statement         { $$ = $1; }
    | if_statement                  { $$ = $1; }
    | function_call SEMICOLON       { $$ = $1; }
    | return_statement SEMICOLON    { $$ = $1; }
;

/* factorial(2 + 2) */
function_call
    : IDENT LPAREN expression_list RPAREN
    {
        $$ = create_function_call($1, $3->function_call.arguments, $3->function_call.arg_count, @1.first_line, @1.first_column);
        free($3);
    }
;

/* factorial(2 + 2, 3) */
expression_list
    : expression
    {
        $$ = create_expression_list($1);
    }
    | expression_list COMMA expression
    {
        $$ = append_expression_list($1, $3);
    }
    | /* nothing */
    {
        $$ = create_empty_expression_list();
    }
;

/* return 2 + 2 */
return_statement
    : RETURN expression        { $$ = create_return_statement($2, @1.first_line, @1.first_column); }
;

/* val a: Int = 4 */
declaration_statement
    : VAL IDENT COLON type ASSIGN expression SEMICOLON {
        $$ = create_variable_declaration($2, $4, $6, @1.first_line, @1.first_column);
    }
    | FUN IDENT LPAREN param_list RPAREN COLON type block 
    {
            $$ = create_function_declaration(
                $2,
                $4.names,
                $4.types,
                $4.count,
                $7,
                $8,
                @1.first_line,
                @1.first_column
            );
        }
;

/* if (bool) { ... } else { ... } */
if_statement
    : IF LPAREN expression RPAREN block
    { 
        $$ = create_if_node($3, $5, NULL, @1.first_line, @1.first_column);
    }
    | IF LPAREN expression RPAREN block ELSE block
    { 
        $$ = create_if_node($3, $5, $7, @1.first_line, @1.first_column);
    }
;

/* 2 + 2 */
expression
    : expression PLUS expression 
    {
        $$ = create_binary_operation("+", $1, $3, @2.first_line, @2.first_column);
    }
    | expression MINUS expression
    {
        $$ = create_binary_operation("-", $1, $3, @2.first_line, @2.first_column);
    }
    | expression MUL expression
    {
        $$ = create_binary_operation("*", $1, $3, @2.first_line, @2.first_column);
    }
    | expression DIV expression
    {
        $$ = create_binary_operation("/", $1, $3, @2.first_line, @2.first_column);
    }
    | expression LESS expression
    {
        $$ = create_binary_operation("<", $1, $3, @2.first_line, @2.first_column);
    }
    | expression MORE expression
    {
        $$ = create_binary_operation(">", $1, $3, @2.first_line, @2.first_column);
    }
    | expression EQUAL expression
    {
        $$ = create_binary_operation("==", $1, $3, @2.first_line, @2.first_column);
    }
    | expression NOTEQUAL expression
    {
        $$ = create_binary_operation("!=", $1, $3, @2.first_line, @2.first_column);
    }
    | expression AND expression
    {
        $$ = create_binary_operation("&&", $1, $3, @2.first_line, @2.first_column);
    }
    | expression OR expression
    {
        $$ = create_binary_operation("||", $1, $3, @2.first_line, @2.first_column);
    }
    | LPAREN expression RPAREN
    {
        $$ = $2; // просто возвращаем выражение внутри скобок
    }
    | function_call
    {
        $$ = $1; // возвращаем вызов функции
    }
    | LIT_INT
    {
        $$ = create_literal($1, "Int", @1.first_line, @1.first_column);
    }
    | LIT_STRING
    {
        $$ = create_literal($1, "String", @1.first_line, @1.first_column);
    }
    | LIT_BOOLEAN
    {
        $$ = create_literal($1, "Boolean", @1.first_line, @1.first_column);
    }
    | UNIT
    {
        $$ = create_literal($1, "Unit", @1.first_line, @1.first_column); // создаем узел для Unit
    }
    | IDENT
    {
        $$ = create_identifier_node($1, @1.first_line, @1.first_column); // создаем узел переменной
    }
;


/* a: Int, b: String */
param_list
    : // пустой список
    {
        $$.names = NULL;
        $$.types = NULL;
        $$.count = 0;
    }
    | param
    {
        $$.names = malloc(sizeof(char*));
        $$.types = malloc(sizeof(char*));
        $$.names[0] = $1.names[0];
        $$.types[0] = $1.types[0];
        $$.count = 1;
    }
    | param_list COMMA param
    {
        size_t n = $1.count + 1;
        $$.names = realloc($1.names, sizeof(char*) * n);
        $$.types = realloc($1.types, sizeof(char*) * n);
        $$.names[n - 1] = $3.names[0];
        $$.types[n - 1] = $3.types[0];
        $$.count = n;
    }
;

/* a: Int */
param
    : IDENT COLON type
    {
        $$.names = malloc(sizeof(char*));
        $$.types = malloc(sizeof(char*));
        $$.names[0] = strdup($1);
        $$.types[0] = strdup($3);
        $$.count = 1;
    }
;

/* { ... } */
block
    : LBRACE statements RBRACE
    {
        $$ = create_block_node(@1.first_line, @1.first_column);
        if ($2 && ($2->type == NODE_PROGRAM || $2->type == NODE_BLOCK)) {
            NodeList* children = &$2->block.children;
            for (size_t i = 0; i < children->size; i++) {
                add_child($$, children->items[i]);
            }
            children->size = 0;
            free_node($2);
        }
    }
;

/* Int */
type
    : INT     { $$ = strdup("Int"); }
    | BOOLEAN { $$ = strdup("Boolean"); }
    | STRING  { $$ = strdup("String"); }
    | UNIT    { $$ = strdup("Unit"); }
;

%%

// Получение корня AST
ASTNode* get_ast_root() {
    return ast_root;
}

void yyerror(const char *s) {
    fprintf(stderr, "Syntax error at line %d: %s near '%s'\n", yylineno, s, yytext);
}
