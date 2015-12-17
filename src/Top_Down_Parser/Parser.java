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
		System.out.println("Enter program and terminate with 'end'!\n");
		Lexer.lex();
		new Program();
		Code.output();
	}
}

class Program {
	Decls d;
	Stmts s;

	public Program()  {
		d = new Decls();
		s = new Stmts();
		if (Lexer.nextToken == Token.KEY_END) 
			Code.gen("return");
		return;
	}
}

class Decls {
	Idlist il;
	public Decls() {
		if (Lexer.nextToken == Token.KEY_INT) {
			Lexer.lex();  // skip over int
			il = new Idlist();
		}
	}

}

class Idlist {
	char c;
	Idlist il;
	public Idlist() {
			c = Lexer.ident;
			Code.add(c);
			Lexer.lex();
			if (Lexer.nextToken == Token.COMMA) {
				Lexer.lex();
				il = new Idlist();
		    }
			else Lexer.lex(); // skip ; 
	}
}

			
class Stmt {
	Assign a;
	Cond c;
	Loop l;
	Cmpd cd;
	public Stmt()  {
			if (Lexer.nextToken == Token.ID) {
				a = new Assign();
				Lexer.lex(); // skip over ';'
			}
			else if (Lexer.nextToken == Token.KEY_IF) {
				c = new Cond();
			}
			else if (Lexer.nextToken == Token.KEY_FOR) {
				l = new Loop();
			}
			else if (Lexer.nextToken == Token.LEFT_BRACE) {
				cd = new Cmpd();
			}
	}
} 

class Stmts {
	Stmt s;
	Stmts ss;
	public Stmts() {
		 s = new Stmt();
		 if (Lexer.nextToken == Token.ID || Lexer.nextToken == Token.KEY_IF
				|| Lexer.nextToken == Token.KEY_FOR || Lexer.nextToken == Token.LEFT_BRACE)
			 ss = new Stmts();
	}
}

class Assign {
	char c;
	Expr e;
	public Assign()  {
		c = Lexer.ident;
		Lexer.lex(); 
		Lexer.lex(); // skip over '='
		e = new Expr();
		Code.gen("istore", Code.index(c));
	}
}

interface I {public void set_rop(String op); }

class Cond implements I {
	Rexpr b;
	String rop;  
	Stmt c1;
	Stmt c2;

	public Cond()  {
		Lexer.lex(); // skip over 'if'
		Lexer.lex(); // skip over '('
		b = new Rexpr(this);
		Lexer.lex(); // skip over ')'
		int ifpoint = Code.skip(3);
		c1 = new Stmt();
		if (Lexer.nextToken == Token.KEY_ELSE) {
			Lexer.lex(); // skip over 'else'
			int elsepoint = Code.skip(3);
			Code.patch(ifpoint,rop,Code.codeptr);
			c2 = new Stmt();
			Code.patch(elsepoint,"goto",Code.codeptr);
		}
		else Code.patch(ifpoint,rop,Code.codeptr);
	}
	
	public void set_rop(String op) { rop = op; }
}

class Loop implements I {
	String rop;  
	Assign incr;
	Stmt body;
	Assign init;
	Rexpr test;
	
	public Loop()  {
		boolean is_rexpr = false;
		boolean is_incr = false;
		int incr_point1 = 0;
		int incr_point2 = 0;
		int patch_point = 0;
		Lexer.lex(); // skip over 'for'
		Lexer.lex(); // skip over '('
		if (Lexer.nextToken == Token.ID) 
			init = new Assign();  
		Lexer.lex(); // skip over ;
		int boolpoint = Code.codeptr;
		if (Lexer.nextToken != Token.SEMICOLON) 
		      {test = new Rexpr(this);
		       is_rexpr = true;
		       patch_point = Code.skip(3);
		     }
		Lexer.lex(); // skip over ;
		String[] save_incr ={};
		if (Lexer.nextToken != Token.RIGHT_PAREN) {
			is_incr = true;
			incr_point1 = Code.codeptr;
			incr = new Assign();
			incr_point2 = Code.codeptr;
			save_incr = Code.save(incr_point1,incr_point2);
			Code.resetptr(incr_point1,incr_point2);
			}
		Lexer.lex(); // skip over )
		body = new Stmt();
		if (is_incr) Code.forpatch(save_incr, incr_point2 - incr_point1);
		Code.gen("goto " + boolpoint);
		Code.skip(2);
		if (is_rexpr) Code.patch(patch_point,rop,Code.codeptr);
	}
	
	public void set_rop(String op) { rop = op; }
}

class Cmpd {
	Stmts s;
	public Cmpd()  {
		Lexer.lex(); // skip over '{'
		s = new Stmts();
		Lexer.lex(); // skip over '}'
	}
}

class Rexpr {
	Expr e1;
	Expr e2;
	String op = "";

	public Rexpr(I o)  {
		   e1 = new Expr();
		   if (Lexer.nextToken == Token.EQ_OP
				|| Lexer.nextToken == Token.GREATER_OP
				|| Lexer.nextToken == Token.LESSER_OP
	            || Lexer.nextToken == Token.NOT_EQ) {
			op = Token.toString(Lexer.nextToken);
			Lexer.lex();
			e2 = new Expr();
		}
		o.set_rop(op);
	}
}


class Expr { // expr -> term (+ | -) expr | term
	Term t;
	Expr e;
	char op;

	public Expr() {
		t = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			e = new Expr();
			Code.gen(Code.opcode(op));
		}
	}
}

class Term { // term -> factor (* | /) term | factor
	Factor f;
	Term t;
	char op;

	public Term() {
		f = new Factor();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			t = new Term();
			Code.gen(Code.opcode(op));
		}
	}
}

class Factor { // factor -> number | '(' expr ')'
	Expr e;
	char c;
	int i;

	public Factor() {
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			i = Lexer.intValue;
			if (i < 6 &&  i > -1) 
				Code.gen("iconst_" + i);
			else if (i < 128) 
					{ Code.gen("bipush " + i); 
					  Code.skip(1);
					}
				  else { Code.gen("sipush " + i);
				         Code.skip(2);
				  }
			Lexer.lex();
			break;
		case Token.ID: // id
			c = Lexer.ident;
			Code.gen("iload", Code.index(c));
			Lexer.lex();
			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();
			e = new Expr();
			Lexer.lex(); // skip over ')'
			break;
		default:
			break;
		}
	}
}

class Code {
	static char[] id = new char[25];
    public static String[] code = new String[200];
	static int codeptr = 0;
	static int idptr = 1;

	public static void add(char s) {
		id[idptr] = s;
		idptr++;
	}
	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}
	public static void gen(String s, int n) {
		if (n < 4) 
			 s = s + "_" + n;
		else s = s + " " + n;
		code[codeptr] = s;
		if (n < 4) 
			codeptr = codeptr + 1;
		else codeptr = codeptr + 2;
	}
	public static int index(char c) {
		for (int i=1; i < idptr; i++)
			if (c == id[i]) return i;
		return 0;
	}
	
	public static void output() {
		for (int i=0; i < codeptr; i++)
			if (code[i] != null && code[i] != "")
				System.out.println(i + ": " + code[i]);
	}
	public static int skip(int n) {
		codeptr=codeptr+n;
		return codeptr-n;
	}
	public static String opcode(char c) {
		switch (c) {
		case '+': return "iadd";
		case '-': return "isub";
		case '*': return "imul";
		case '/': return "idiv";
		default: return "";
		}
	}
	public static void patch(int i, String rop, int dest) {
		String instr = "";
		if (rop == "==") instr = "if_icmpne ";
		if (rop == "<") instr = "if_icmpge ";
		if (rop == ">") instr = "if_icmple ";
		if (rop == "!=") instr = "if_icmpeq ";
		if (rop == "goto") instr = "goto ";
		code[i] = instr + dest;
	}
	
	public static String[] save(int i, int j) {
		String[] a = new String[j-i];
		int k;
		for (k=i; k<j; k++)
			a[k-i] = code[k];
		return a;
	}
	
	public static void forpatch(String[] save_incr, int n) {
		int k;
		for (k=0 ; k<n ; k++)
			code[k+codeptr] = save_incr[k];
		codeptr = codeptr + n;
	}
	
	public static void resetptr(int n1, int n2) {
		codeptr = n1;
		for (int k=n1; k < n2; k++)
			code[k] = "";
	}
}
