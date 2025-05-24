all: compiler

compiler: clean lexer.o parser.o main.o
	$(CC) -o compiler lexer.o parser.o main.o

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

clean:
	rm -f *.o parser.c parser.h lexer.c compiler
