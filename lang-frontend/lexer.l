%{
#include "ast.h"
#include "parser.h"
#include <stdlib.h>
#include <string.h>

extern YYLTYPE yylloc;

extern int yylineno;
int yycolumn = 0;

void update_location() {
    yylloc.first_line = yylineno;
    yylloc.first_column = yycolumn;
    yylloc.last_line = yylineno;
    yylloc.last_column = yycolumn + yyleng - 1;
    yycolumn += yyleng;
}
%}

%x COMMENT

%%

"val"           { update_location(); return VAL; }
"fun"           { update_location(); return FUN; }
"if"            { update_location(); return IF; }
"else"          { update_location(); return ELSE; }
"return"        { update_location(); return RETURN; }

"Int"           { update_location(); return INT; }
"Boolean"       { update_location(); return BOOLEAN; }
"String"        { update_location(); return STRING; }
"Unit"          { update_location(); return UNIT; }

[0-9]+          {
                    update_location();
                    yylval.sval = strdup(yytext);
                    return LIT_INT;
                }

\"[^\"]*\"      {
                    update_location();
                    yylval.sval = strdup(yytext);
                    return LIT_STRING;
                }

"true"|"false"  {
                    update_location();
                    yylval.sval = strdup(yytext);
                    return LIT_BOOLEAN;
                }

[a-zA-Z_][a-zA-Z0-9_]* {
                    update_location();
                    yylval.sval = strdup(yytext);
                    return IDENT;
                }

"="             { update_location(); return ASSIGN; }
":"             { update_location(); return COLON; }
";"             { update_location(); return SEMICOLON; }
","             { update_location(); return COMMA; }
"("             { update_location(); return LPAREN; }
")"             { update_location(); return RPAREN; }
"{"             { update_location(); return LBRACE; }
"}"             { update_location(); return RBRACE; }
"+"             { update_location(); return PLUS; }
"-"             { update_location(); return MINUS; }
"*"             { update_location(); return MUL; }
"/"             { update_location(); return DIV; }
"<"             { update_location(); return LESS; }
">"             { update_location(); return MORE; }
"=="            { update_location(); return EQUAL; }
"!="            { update_location(); return NOTEQUAL; }
"&&"            { update_location(); return AND; }
"||"            { update_location(); return OR; }

[ \t\r]+        {
                    int i;
                    for (i = 0; i < yyleng; i++) {
                        yycolumn += (yytext[i] == '\t') ? 4 : 1;
                    }
                }

\n              {
                    yylineno++;
                    yycolumn = 0;
                }

.               {
                    update_location();
                    fprintf(stderr, "Ошибка: неожиданный символ '%s' на строке %d, колонке %d\n", yytext, yylineno, yycolumn);
                    exit(1);
                }

%%

int yywrap() {
    return 1;
}
