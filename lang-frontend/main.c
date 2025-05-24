#include <stdio.h>

#include "ast_print.h"

int yyparse();

int main() {
    yyparse();
    visualize_ast(ast_root, stdout);
}
