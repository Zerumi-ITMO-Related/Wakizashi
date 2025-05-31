all: compiler

compiler: clean lexer.o parser.o main.o ast.o ast_print.o
	$(CC) -o compiler lexer.o parser.o main.o ast.o ast_print.o

lexer.c: lexer.l
	flex -o lexer.c lexer.l

parser.c parser.h: parser.y
	bison -v -d -o parser.c parser.y

lexer.o: lexer.c parser.h
	gcc -c lexer.c

parser.o: parser.c
	gcc -c parser.c

main.o: main.c
	gcc -c main.c

ast.o: ast.c
	gcc -c ast.c

ast_print.o: ast_print.c
	gcc -c ast_print.c

clean:
	rm -f *.o parser.c parser.h lexer.c compiler
