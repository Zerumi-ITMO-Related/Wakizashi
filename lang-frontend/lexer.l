%{
#include "ast.h"
#include "parser.h"
#include <stdlib.h>
#include <string.h>

int line = 1;
%}

%x COMMENT

%%

"val"           { return VAL; }
"fun"           { return FUN; }
"if"            { return IF; }
"else"          { return ELSE; }
"return"        { return RETURN; }

"Int"           { return INT; }
"Boolean"       { return BOOLEAN; }
"String"        { return STRING; }

"Unit"          { return UNIT; }

[0-9]+          { yylval.sval = strdup(yytext); return LIT_INT; }
\"[^\"]*\"      { yylval.sval = strdup(yytext); return LIT_STRING; }
"true"          { yylval.sval = strdup(yytext); return LIT_BOOLEAN; }
"false"         { yylval.sval = strdup(yytext); return LIT_BOOLEAN; }

[a-zA-Z_][a-zA-Z0-9_]* { yylval.sval = strdup(yytext); return IDENT; }


"="             { return ASSIGN; }
":"             { return COLON; }
";"             { return SEMICOLON; }
","             { return COMMA; }
"("             { return LPAREN; }
")"             { return RPAREN; }
"{"             { return LBRACE; }
"}"             { return RBRACE; }

"+"             { return PLUS; }
"-"             { return MINUS; }
"*"             { return MUL; }
"/"             { return DIV; }
"<"             { return LESS; }
">"             { return MORE; }
"=="            { return EQUAL; }
"&&"            { return AND; }
"||"            { return OR; }
"!"             { return NOT; }

[ \t\r]+        { /* пропускаем */ }

\n              { line++; yylineno++; }

.               { printf("Ошибка: неожиданный символ '%s' на строке %d\n", yytext, line); exit(1); }

%%

int yywrap() {
    return 1;
}
