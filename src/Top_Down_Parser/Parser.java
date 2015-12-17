package Top_Down_Parser;

import java.lang.Integer;
import java.util.Stack;

/*
~~~~~~~~~Object Oriented TOP-DOWN PARSER AND JAVA BYTE-CODE GENERATOR~~~~~~~~~
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
								SUBMITTED BY:
								=============
Name: Twinkle Asthana								Name: Agradeep Khanra
UB ID Name: twinklea								UB ID Name: agradeep			
UB Person No.: 50169071								Person No.: 50169196			
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~TINY PL GRAMMAR~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Grammar for TinyPL:
program -> decls stmts end
decls -> int idlist ';'
idlist -> id [',' idlist ]
stmts -> stmt [ stmts ]
stmt -> assign ';'| cmpd | cond | loop
assign -> id '=' expr
cmpd -> '{' stmts '}'
cond -> if '(' rexp ')' stmt [ else stmt ]
loop -> for '(' [assign] ';' [rexp] ';' [assign] ')' stmt
rexp -> expr ('<' | '>' | '==' | '!= ') expr
expr -> term [ ('+' | '-') expr ]
term -> factor [ ('*' | '/') term ]
factor -> int_lit | id | '(' expr ')'
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
*/

//Parser Driver Class
public class Parser {
	public static void main(String[] args)  {
		System.out.println("Enter the Tiny PL program and terminate with 'end'!\n");
		Lexer.lex();
		@SuppressWarnings("unused")
		Program prog = new Program();
		Code.output();
	}
}


class Program {
	//program -> decls stmts end
	
	Decls decls;		
	Stmts stmts;		
	public Program(){
		decls = new Decls();
		Lexer.lex();
		stmts = new Stmts(Token.KEY_END);
		Code.gen(1, "return", 1);
	}
}
class Decls {
	//decls -> int idlist ';'
	
	Idlist idlist;
	int nextToken = 1;
	public Decls(){
		idlist = new Idlist();
	}
}
class Idlist {
	//idlist -> id [',' idlist ]

	Idlist idlist;
	static char[] id = new char[100];
	static int x;
	static boolean breakExec = false;
	
	public Idlist() {
		outerloop:
		while( breakExec != true) {
			Lexer.lex();
			if( Lexer.nextToken == Token.ID ) {
				if(Lexer.ch == ';'){
					id[x] = Lexer.ident;
					x = x + 1;
					breakExec = true;
					break outerloop;
				}
				
				id[x] = Lexer.ident;
				x = x + 1;
				idlist = new Idlist();
			}
		}
	} 
}

class Stmt {
	//stmt -> assign ';'| cmpd | cond | loop
	Assign assign;
	Loop loop;
	Cond cond;
	Cmpd cmpd1;
	
	//Data Members
	static char curr;
	static int indexCurr;
	int elsePtr, intByte;
	int gotoptr, intStart;
	public static boolean forInd = false;
	
	public Stmt() { 
		if(Lexer.nextToken == 0)
			Lexer.lex();
		switch (Lexer.nextToken) {
			
			case Token.ID:    
				curr = Lexer.ident;
				if(indexCurr > 0)
					indexCurr = 0;
				
				for(char ch : Idlist.id) {
					if(ch != Lexer.ident) {
						indexCurr = indexCurr + 1;
					} else {
						break;
					}
				}
				Lexer.lex();

			case Token.ASSIGN_OP: // =   
				assign = new Assign(1);
				curr = '\0'; indexCurr = 0;
				break;  
 
			case Token.KEY_FOR: // for 
				forInd = true;
				gotoptr = Code.bytes;
				loop = new Loop();    //Loop call
				
				if(Loop.intCounter > 0){
					intByte = Loop.forBytes[Loop.intCounter - 1];
					Code.gen( 1, "goto  " + intByte , 1);
					Loop.intCounter--;
				}

				Code.bytes = Code.bytes + 2;
				if(!Rexpr.codePtr.isEmpty()){
					int ifPtr = Rexpr.codePtr.pop();
					Code.code[ifPtr] = Code.code[ ifPtr ] + Code.bytes;
				}
				break;  
				
			case Token.KEY_IF: // if
				cond = new Cond();
				break;
			
			case Token.LEFT_BRACE:
				cmpd1 = new Cmpd();
				
			default:
				break;
		} 
	} 
} 

class Stmts {
	//stmts -> stmt [ stmts ]
	Stmt stmt;
	Stmts stmts;
	
	public Stmts(int cond) {
		stmt = new Stmt();
		if( Lexer.nextToken!=Token.KEY_END && Lexer.nextToken!=Token.ID) {
			Lexer.lex();
		}
		if ( Lexer.nextToken != cond && Lexer.nextToken!=Token.KEY_END ) {
			stmts = new Stmts( cond );
		}
	}
}

class Assign {
	//assign -> id '=' expr
	Expr expr;
	
	//Data Members
	static int index, curr;
	public Assign(int flag){
		if(flag == 1){			
			Lexer.lex();
			if(Lexer.nextToken == Token.ID && Stmt.forInd == true){
				Stmt.curr = Lexer.ident;
				if(Stmt.indexCurr > 0)
					Stmt.indexCurr = 0;
				for(char ch : Idlist.id) {
					if(ch != Lexer.ident) {
						Stmt.indexCurr = Stmt.indexCurr + 1;
					} else {
						break;
					}
				}
			}
			expr = new Expr(1);
			int bytesRequired = 1;
			index = Stmt.indexCurr+1;
			if(index > 3)
			{
				bytesRequired = 2;
			}
			Code.gen(bytesRequired, "istore_" + index, 1);
		}
		else{
			Lexer.lex();
			expr = new Expr(2);
			int bytesRequired = 1;
			index = Stmt.indexCurr+1;
			if(index > 3)
			{
				bytesRequired = 2;
			}
			Code.gen(bytesRequired, "istore_" + index, 2);
		}
	}
}

class Cond {
	//cond -> if '(' rexp ')' stmt [ else stmt ]
	Rexpr rexpr;
	Stmt stmt,stmt1;

	//Data Members
	int updateConn;
	
	public Cond() {
		Lexer.lex();
		rexpr = new Rexpr();
		
		Lexer.lex();
		stmt = new Stmt();
		
		if( Lexer.nextToken != Token.KEY_END ) {
			Lexer.lex();
		}
		
		if(!Rexpr.codePtr.isEmpty()) {
			int elsePtr = Rexpr.codePtr.pop();
			if(Lexer.nextToken == Token.KEY_END){
				updateConn = Code.bytes;
			}
			else if(Lexer.nextToken == Token.RIGHT_BRACE){
				updateConn = Code.bytes;
			}
			else{
				updateConn = Code.bytes + 3;
			}
			
			Code.code[elsePtr] = Code.code[elsePtr] + updateConn;
		}
		
		if(Lexer.nextToken==Token.KEY_ELSE){
			int elsePtr = Code.codeptr;
			Code.gen(1, "goto ", 1);
			Code.bytes = Code.bytes + 2;
			Lexer.lex();
			stmt1 = new Stmt();
			Code.code[elsePtr] = Code.code[elsePtr] + Code.bytes;
		}
	}
}

class Loop {
	//loop -> for '(' [assign] ';' [rexp] ';' [assign] ')' stmt
	Rexpr rexpr;
	Assign assign1, assign2;
	Stmt stmt;
	
	//Data Members
	public static int forBytes[] = new int[10];
	int bytesRequired, index, tempByte, newByte, intColonPosition, intUnderscorePosition, intRank;
	public static int intIdent = 0, intCounter;
	String tempString;
	public static boolean firstExp = false, blnFirstExpEmpty = false;
	
	public Loop(){
		Lexer.lex();
		if(Lexer.ch != ' '){
			firstExp = true;
			assign1 = new Assign(1);
		}
		
		firstExp = false;
		
		Lexer.lex();
		if(Lexer.nextToken == Token.ID){
			int i = 0;
			for(char ch : Idlist.id) {
				if(ch == '\0')
					break;
				if(ch == Lexer.ident) {bytesRequired = 1;
					index = (i+1);
					if(index > 3)
					{
						bytesRequired = 2;
					}
					for(int z = 0; z < Code.codeptr; z++)
						if(Code.code[z].toLowerCase().contains(("istore_" + index).toLowerCase())  || Stmt.forInd == false){
							Code.gen(bytesRequired, "iload_" + index, 1);
							forBytes[intCounter] = Code.bytes - bytesRequired;
							intCounter++;
							break;
						}
				} else {
					i = i + 1;
				}
			}
		}
		else
			blnFirstExpEmpty = true;
		rexpr = new Rexpr();
		Lexer.lex();
		
		intIdent++;
		if(Lexer.nextToken == Token.ID){
			int i = 0;
			for(char ch : Idlist.id) {
				if(ch == '\0')
					break;
				if(ch == Lexer.ident) {bytesRequired = 1;
					index = (i+1);
					if(index > 3)
					{
						bytesRequired = 2;
					}
					for(int z = 0; z < Code.codeptr; z++)
						if(Code.code[z].toLowerCase().contains(("istore_" + index).toLowerCase()) || Stmt.forInd == false){
							Code.gen(bytesRequired, "iload_" + index, 2);
							break;
						}
				} else {
					i = i + 1;
				}
			}
		}
		if(Lexer.nextToken != Token.RIGHT_PAREN)
			assign2 = new Assign(2);
		
		Lexer.lex();
		stmt = new Stmt();
		
		for(int y = 0; y < Code.tempptr; y++){
			newByte = Code.bytes;
			intUnderscorePosition = Code.temp[y].indexOf('_');
			intRank = Integer.parseInt(Code.temp[y].substring(0, intUnderscorePosition));
			intColonPosition = Code.temp[y].indexOf(':');
			tempByte = Integer.parseInt(Code.temp[y].substring(intUnderscorePosition+1, intColonPosition));
			tempString = Code.temp[y].substring(intColonPosition);
			if(intRank == intIdent){
				Code.code[Code.codeptr] = Code.bytes + tempString;
				Code.codeptr++;
				Code.bytes = newByte + tempByte - y;
			}
		}
		intIdent--;
	}
}

class Cmpd {
	//cmpd -> '{' stmts '}'
	Stmts stmts;
	
	public Cmpd(){	
		Lexer.lex();
		stmts = new Stmts(Token.RIGHT_BRACE);
	}
}

class Rexpr {
	//rexp -> expr ('<' | '>' | '==' | '!= ') expr
	Expr e1;
	Expr e2;
	
	//Data Members
	static Stack<Integer> codePtr = new Stack<Integer>();
	static String codeGen;
	
	public Rexpr(){
		Lexer.lex();
		if(Lexer.nextToken == 0)
			Lexer.lex();
		e1 = new Expr(1);
		switch (Lexer.nextToken) {
		case Token.EQ_OP:     
			codeGen = "if_icmpne ";
			break;
		case Token.GREATER_OP:     
			codeGen = "if_icmple ";
			break;	  
		case Token.LESSER_OP:     
			codeGen = "if_icmpge ";
			break;  
		case Token.NOT_EQ:     
			codeGen = "if_icmpeq ";
			break; 
		} 
		Lexer.lex();
		
		e2 = new Expr(1);
		codePtr.push(Code.codeptr);
		Code.gen(1, codeGen, 1);
		Code.bytes = Code.bytes + 2;
	}
}
class Expr {
	//expr -> term [ ('+' | '-') expr ]
	Term t;
	Expr e;
	
	//Data Members
	char op;
	
	public Expr(int flag) {
		if(flag == 1){
			t = new Term(1);
			if(Lexer.nextToken == Token.ASSIGN_OP)
				Lexer.lex();
			if(Lexer.nextToken == Token.ID)
				Lexer.lex();
			if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
				op = Lexer.nextChar;
				Lexer.lex();
				e = new Expr(1);
				Code.gen(1,Code.opcode(op), 1);
			}
		}
		else{
			t = new Term(2);
			if(Lexer.nextToken == Token.ASSIGN_OP)
				Lexer.lex();
			if(Lexer.nextToken == Token.ID)
				Lexer.lex();
				if(Stmt.indexCurr > 0)
					Stmt.indexCurr = 0;
				for(char ch : Idlist.id) {
					if(ch != Lexer.ident) {
						Stmt.indexCurr = Stmt.indexCurr + 1;
					} else {
						break;
					}
				}
			if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
				op = Lexer.nextChar;
				Lexer.lex();
				e = new Expr(2);
				Code.gen(1,Code.opcode(op), 2);
			}
		}
	}
}

class Term {
	//term -> factor [ ('*' | '/') term ]
	Factor f;
	Term t;
	
	//Data Members
	char op;
	int i;
	
	public Term(int flag) {
			if(flag == 1){
				f = new Factor(1);
				if(Lexer.nextToken == Token.ASSIGN_OP)
					Lexer.lex();
				if(Lexer.nextToken == Token.ID){
					if(Stmt.forInd && Loop.firstExp){
						int i = 0;
						for(char ch : Idlist.id) {
							if(ch == '\0')
								break;
							if(ch == Lexer.ident) {int bytesRequired = 1;
								int index = (i+1);
								if(index > 3)
								{
									bytesRequired = 2;
								}
								for(int z = 0; z < Code.codeptr; z++)
									if(Code.code[z].contains(("istore_" + index)) || Stmt.forInd == false){
										Code.gen(bytesRequired, "iload_" + index, 1);
										break;
									}
							} else {
								i = i + 1;
							}
						}
					}
					Lexer.lex();
				}
				if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
					op = Lexer.nextChar;
					Lexer.lex();
					t = new Term(1);
					Code.gen(1,Code.opcode(op), 1);
				}
				if(Lexer.nextToken == Token.INT_LIT){
					i = Lexer.intValue;
					Lexer.lex();
					if( i <=5 ) {
						Code.gen(1,"iconst_" + i, 1);
					} else if( i >= 6 && i<128 ){
						Code.gen(2,"bipush  " + i, 1);
					} else {
						Code.gen(3,"sipush  " + i, 1);
					}
				}
				
			}
			else{
				f = new Factor(2);
				if(Lexer.nextToken == Token.ASSIGN_OP)
					Lexer.lex();
				if(Lexer.nextToken == Token.ID)
					Lexer.lex();
				if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
					op = Lexer.nextChar;
					Lexer.lex();
					t = new Term(2);
					Code.gen(1,Code.opcode(op), 2);
				}
				if(Lexer.nextToken == Token.INT_LIT){
					i = Lexer.intValue;
					Lexer.lex();
					if( i <=5 ) {
						Code.gen(1,"iconst_" + i, 2);
					} else if( i >= 6 && i<128 ){
						Code.gen(2,"bipush  " + i, 2);
					} else {
						Code.gen(3,"sipush  " + i, 2);
					}
				}
			}
	}
}
class Factor {
	//factor -> int_lit | id | '(' expr ')'
	Expr e;
	int i;
	
	public Factor(int flag) {
		
		if(flag == 1){
			switch (Lexer.nextToken) {
			case Token.INT_LIT: // number
				i = Lexer.intValue;
				Lexer.lex();
				if( i <=5 ) {
					Code.gen(1,"iconst_" + i, 1);
				} else if( i >= 6 && i<128 ){
					Code.gen(2,"bipush  " + i, 1);
				} else {
					Code.gen(3,"sipush  " + i, 1);
				}
				break;
			case Token.ID:
				int i = 0;
				for(char ch : Idlist.id) {
					if(ch == '\0')
						break;
					if(ch == Lexer.ident) {int bytesRequired = 1;
						int index = (i+1);
						if(index > 3)
						{
							bytesRequired = 2;
						}
						if(Code.codeptr == 0)
							Code.gen(bytesRequired, "iload_" + index, 1);
						else{
							for(int z = 0; z < Code.codeptr; z++)
								if(Code.code[z].contains(("istore_" + index))  || Stmt.forInd == false){
									Code.gen(bytesRequired, "iload_" + index, 1);
									if(Loop.blnFirstExpEmpty){
										Loop.forBytes[Loop.intCounter] = Code.bytes - bytesRequired;
										Loop.intCounter++;
										Loop.blnFirstExpEmpty = false;
									}
									break;
								}
						}
					} else {
						i = i + 1;
					}
				}
				Lexer.lex();
				break;
			case Token.LEFT_PAREN: // '('
				Lexer.lex();
				e = new Expr(1);
				Lexer.lex(); // skip ')'
				break;
			default:
				break;
			}
		}
		else{
			switch (Lexer.nextToken) {
			case Token.INT_LIT: // Integer Literals
				i = Lexer.intValue;
				Lexer.lex();
				if( i <=5 ) {
					Code.gen(1,"iconst_" + i, 2);
				} else if( i >= 6 && i<128 ){
					Code.gen(2,"bipush  " + i, 2);
				} else {
					Code.gen(3,"sipush  " + i, 2);
				}
				break;
			case Token.ID:
				int i = 0;
				for(char ch : Idlist.id) {
					if(ch == '\0')
						break;
					if(ch == Lexer.ident) {int bytesRequired = 1;
						int index = (i+1);
						if(index > 3)
						{
							bytesRequired = 2;
						}
						for(int z = 0; z < Code.codeptr; z++)
							if(Code.code[z].contains(("istore_" + index))  || Stmt.forInd == false){
								Code.gen(bytesRequired, "iload_" + index, 2);
								break;
							}
					} else {
						i = i + 1;
					}
				}
				Lexer.lex();
				break;
			case Token.LEFT_PAREN: // '('
				Lexer.lex();
				e = new Expr(1);
				Lexer.lex(); // skip ')'
				break;
			default:
				break;
			}
		}
	}
}

//Class Code to generate Java Byte Codes
class Code {
	
	//Data Members
	public static String[] code = new String[100];
	public static int codeptr = 0;
	
	public static String[] temp = new String[100];
	public static int tempptr = 0;
	
	public static int bytes = 0, bytes2 = 1;
	
	public static void gen(int byt, String s, int flag) {
		if(flag == 1){
			code[codeptr] = bytes + ": " + s;
			codeptr++;
			bytes = bytes + byt;
		}
		else{
			temp[tempptr] = Loop.intIdent + "_" + bytes2 + ": " + s;
			tempptr++;
			bytes2 = bytes2 + byt;
		}
	}
	public static String opcode(char op) {
		switch(op) {
			case '/':  return "idiv";
			case '*':  return "imul";
			case '+' : return "iadd";
			case '-':  return "isub";
			default: return "";
		}
	}
	public static void output() {
		
		System.out.println("\nJava Byte Codes are :\n");
		
		for (int i=0; i<codeptr; i++)
			System.out.println(code[i]);
	}
}