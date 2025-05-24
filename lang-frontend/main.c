#include <stdio.h>

#include "ast_print.h"

int yyparse();

int main() {
    yyparse();

    printf("#AST:\n");
    visualize_ast(ast_root, stdout);
    printf("\n");
}
