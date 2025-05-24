%{
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern int yylex();
void yyerror(const char *s);

typedef struct {
    char* sval;
    int   ival;
} yylval_t;

#define YYSTYPE yylval_t
#define YYERROR_VERBOSE 1

extern int yylineno;
extern char* yytext;
%}

%union {
    char* sval;
    int ival;
}

%token <sval> IDENT
%token <ival> LIT_INT
%token <sval> LIT_STRING
%token VAL FUN RETURN
%token UNIT INT BOOLEAN STRING
%token IF ELSE WHILE BREAK
%token ASSIGN COLON SEMICOLON COMMA
%token LPAREN RPAREN LBRACE RBRACE LESS MORE EQUAL
%token PLUS MINUS MUL DIV

%type <sval> expression
%type <sval> param param_list

%%

program
    : statements
;

statements
    : /* empty */
    | statement statements
;

statement
    : declaration_statement
    | if_statement
    | while_statement
    | break_statement SEMICOLON
    | function_call SEMICOLON
    | retutn_statement SEMICOLON
;

function_call
    : IDENT LPAREN expression RPAREN
;

retutn_statement
    : RETURN
    | RETURN expression
;

declaration_statement
    : VAL IDENT COLON type ASSIGN expression SEMICOLON
    | FUN IDENT LPAREN param_list RPAREN COLON type block
;
    
if_statement
    : IF LPAREN expression RPAREN block
    | IF LPAREN expression RPAREN block ELSE block
;

/* while(bool) { ... } */
while_statement
    : WHILE LPAREN expression RPAREN block
;

/* break */
break_statement
    : BREAK
;

/* 2 + 2 */
/* factorial(a) */
expression
    : expression PLUS expression
    | expression MINUS expression
    | expression MUL expression
    | expression DIV expression
    | expression LESS expression
    | expression MORE expression
    | expression EQUAL expression
    | LPAREN expression RPAREN
    | function_call
    | LIT_INT
    | LIT_STRING
    | IDENT
;


/* a: Int, b: String */
param_list
    : /* nothing */
    | param
    | param_list COMMA param
;

/* a: Int */
param
    : IDENT COLON type
;

/* { ... } */
block
    : LBRACE statements RBRACE
;

/* Int */
type
    : INT
    | BOOLEAN
    | STRING
    | UNIT
;

%%

void yyerror(const char *s) {
    fprintf(stderr, "Syntax error at line %d: %s near '%s'\n", yylineno, s, yytext);
}
