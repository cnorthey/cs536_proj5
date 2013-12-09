import java.io.*;

import ASTNode.Kinds;
import ASTNode.Types;

public class CodeGenerating extends Visitor {

	PrintStream afile;	// File to generate JVM code into 

	int cgErrors =  0;       // Total number of code generation errors 

	int numberOfLocals =  0; // Total number of local CSX-lite vars

	int labelCnt = 0;	// counter used to generate unique labels

	methodDeclNode currentMethod;

	private String CLASS;

	CodeGenerating(PrintStream f){
		afile=f;
	}

	static void assertCondition(boolean assertion){
		if (! assertion)
			throw new RuntimeException();
	}

	String error(ASTNode n) {
		return "Error (line " + n.linenum + "): ";
	}

	// generate a comment
	void  genComment(String text){
		gen("; "+text);
	}

	// generate an instruction w/ 0 operands
	void  gen(String opcode){
		afile.println("\t"+opcode);
	}

	// generate an instruction w/ 1 operand
	void  gen(String opcode, String operand){
		afile.println("\t"+opcode+"\t"+operand);
	}

	// generate an instruction w/ 1 integer operand
	void  gen(String opcode, int operand){
		afile.println("\t"+opcode+"\t"+operand);
	}


	//  generate an instruction w/ 2 operands
	void  gen(String opcode, String operand1, String operand2){
		afile.println("\t"+opcode+"\t"+ operand1+"  "+ operand2);
	}

	//  generate an instruction w/ 2 operands (String and int)
	void  gen(String opcode, String operand1, int operand2){
		afile.println("\t"+opcode+"\t"+ operand1+"  "+operand2);
	}

	//      Generate a new label of form labeln (e.g., label7 or label123)
	String   genLab(){
		return "label"+labelCnt++;
	}

	//      Place a label in generated code
	void    defineLab(String label){
		afile.println(label+":");
	}

	void branch(String label){
		gen("goto",label);
	}

	void loadI(int val){
		gen("ldc",val);
	}

	void binOp(String op){
		// Generate a binary operation
		// all operands are on the stack
		gen(op);
	}
	
	void storeGlobalInt(String name){
		// TODO
		// Generate a store into an int static field from the stack:
		// 		putstatic CLASS/name
		gen("putstatic", CLASS+"/"+name);
	}
	
	void storeLocalInt(int index){
		// TODO
		// Generate a store to an int local variable from the stack:
		//		istore index
		gen("istore", index);
	}
	
	void storeId(identNode id){
		//TODO fix
		if(id.idinfo.kind == ASTNode.Kinds.Var || 
				id.idinfo.kind == ASTNode.Kinds.Value) {
			// id is a scalar variable
			if(id.idinfo.adr == global) // ident is global
				storeGlobalInt(id.idinfo.label);
			else if (id.idinfo.adr == local)
				storeLocalInt(id.idinfo.varIndex);
		} else {
			//Handle arrays
		}
	}
	
	static Boolean isRelationalOp(int op) {
		switch (op) {
		case sym.EQ:
		case sym.NOTEQ:
		case sym.LT:
		case sym.LEQ:	
		case sym.GT:
		case sym.GEQ:
			return true;
		default:
			return false;
		}
	}
	
	void branchRelationalCompare(int tokenCode, String label){
		//TODO check for correctness
		// Generate a conditional branch to label based on tokenCode:
		gen("if_icmp"+relationCode(tokenCode), label);
	}
	
	void genRelationalOp(int operatorCode){
		//TODO check for correctness
		// Generate a code to evaluate a relational operator
		String trueLab = genLab();
		String skip = genLab();
		branchRelationalCompare(operatorCode, trueLab);
		loadI(0); // Push false;
		branch(skip);
		defineLab(trueLab);
		loadI(1); // Push true
		defineLab(skip);
	}
	
	boolean isNumericLit(exprNodeOption e){
		return 	(e instanceof intLitNode) ||
				(e instanceof charLitNode) ||
				(e instanceof trueNode) ||
				(e instanceof falseNode);
	}
	
	int getLitValue(exprNode e){
		if(e instanceof intLitNode)
			return ((intLitNode) e).intval;
		else if(e instanceof charLitNode)
			return ((charLitNode) e).charval;
		else if(e instanceof trueNode)
			return 1;
		else if(e instanceof falseNode)
			return 0;
	}
	
	void declGlobalInt(String name, exprNodeOption initValue){
		// TODO check for correctness
		if(isNumericLit(initValue)){
			// Generate a field declaration with initial value:
			int numValue = getLitValue((exprNode)initValue);
			gen(".field public static "+name+"I = "+numValue);
		} else {
			// Generate a field declaration without an initial value:
			gen(".field public static"+name+"I");
		}
	}
	
	void declGlobalArray(String name, typeNode type){
		//TODO check for correctness
		String arrayType = arrayTypeCode(type);
		
		// Generate a field declaration for an array:
		gen(".field public static "+name+" "+arrayType+"]"); // ]?
	}
	
	void allocateArray(typeNode type){
		//TODO check for correctness
		if(type instanceof intTypeNode){
			// Generate a newarray instruction for an integer array:
			gen("newarray","int");
		} else if(type instanceof charTypeNode){
			// Generate a newarray instruction for a character array:
			gen("newarray","char");
		} else if(type instanceof boolTypeNode){
			// Generate a newarray instruction for a boolean array:
			gen("newarray", "boolean");
		} else {
			System.out.println("ERROR: Invalid type"); // TODO remove before end
		}
	}
	
	String arrayTypeCode(typeNode type){
		//TODO check for correctness
		// Return array type code
		if(type instanceof intTypeNode)
			return "[I";
		else if(type instanceof charTypeNode)
			return "[C";
		else if(type instanceof boolTypeNode)
			return "[Z";
		return "ERROR: Invalid type";	//TODO remove before end
	}
	
	String typeCode(typeNode type){
		// TODO test for correctness
		// Return type code
		if(type instanceof intTypeNode)
			return "I";
		else if(type instanceof charTypeNode)
			return "C";
		else if(type instanceof boolTypeNode)
			return "Z";
		else if(type instanceof voidTypeNode)
			return "V";
		else return "ERROR: Invalid type"; //TODO remove before end
	}
	
	String typeCode(Types type){
		// TODO test for correctness
		// Return type code
		switch (type) {
		case Integer: 
			return "I";
		case Character: 
			return "C";
		case Boolean:
			return "Z";
		case Void:
			return "V";
		default: return "ERROR: Invalid type"; //TODO remove before end
		}
	}
	
	String buildTypeCode(argDeclNode n){
		// TODO check for correctness
		if(n instanceof valArgDeclNode)
			return typeCode(((valArgDeclNode) n).argType);
		else 
			return arrayTypeCode(((arrayArgDeclNode) n).elementType);
	}
	
	String buildTypeCode(argDeclsNode n){
		// TODO check for correctness
		if(n.moreDecls.isNull())
			return buildTypeCode(n.thisDecl);
		else
			return buildTypeCode(n.thisDecl) 
					+ buildTypeCode((argDeclsNode)n.moreDecls);
	}
	
	void storeGlobalReference(String name, String typeCode){
		//TODO check for correctness
		// Generate a store of a reference from the stack into a static field:
		gen("putstatic", CLASS+"/"+name, typeCode);
	}
	
	void storeLocalReference(int index){
		//TODO check for correctness
		// Generate a store of a reference from the stack into a local variable
		gen("astore", index);
	}

	// TODO Void type? no type given in spec, no return so going with void
	void declField(varDeclNode n){
		//TODO check for correctness
		String varLabel = n.varName.idname +"$"; //Append $ to avoid conflicts
		declGlobalInt(varLabel, n.initValue);
		n.varName.idinfo.label = varLabel;
		n.varName.idinfo.adr = global;
	}
	
	void declField(constDeclNode n){
		//TODO check for correctness
		String constLabel = n.constName.idname +"$";
		declGlobalInt(constLabel, n.constValue);
		n.constName.idinfo.label = constLabel;
		n.constName.idinfo.adr = global;
	}
	
	void declField(arrayDeclNode n){
		//TODO check for correctness
		String arrayLabel = n.arrayName.idname + "$";
		declGlobalArray(arrayLabel, n.elementType);
		n.arrayName.idinfo.label = arrayLabel;
		n.arrayName.idinfo.adr = global;
	}
	
	static String relationCode(int op) {
		//TODO check for correctness
		switch (op) {
		case sym.EQ:
			return "eq";	// ==
		case sym.NOTEQ:
			return "ne";	// !=
		case sym.LT:
			return "lt";	// <
		case sym.LEQ:
			return "le";	// <=
		case sym.GT:
			return "gt";	// >
		case sym.GEQ:
			return "ge";	// >=
		default:
			return "";
		}
	}
	
	static String selectOpCode(int op) {
		//TODO Double check this is correct
		switch (op) {
		case sym.PLUS:
			return("iadd");
		case sym.MINUS:
			return("isub");
		case sym.TIMES:
			return("imul");
		case sym.SLASH:
			return("idiv");
		case sym.CAND:
			return("iand");
		case sym.COR:
			return("ior");
		default:
			assertCondition(false);
			return "";
		}
	}


	//   startCodeGen translates the AST rooted by node n
	//      into JVM code which is written in afile.
	//   If no errors occur during code generation,
	//    TRUE is returned, and afile should contain a
	//    complete and correct JVM program. 
	//   Otherwise, FALSE is returned and afile need not
	//    contain a valid program.

	boolean startCodeGen(csxLiteNode n) {// For CSX Lite
		this.visit(n);
		return (cgErrors == 0);
	}

	boolean startCodeGen(classNode n) {// For CSX
		this.visit(n);
		return (cgErrors == 0);
	}

	void visit(csxLiteNode n) {
		genComment("CSX Lite program translated into Java bytecodes (Jasmin format)");
		gen(".class","public","test");
		gen(".super","java/lang/Object");
		gen(".method"," public static","main([Ljava/lang/String;)V");
		this.visit(n.progDecls);
		if (numberOfLocals >0)
			gen(".limit","locals",numberOfLocals);
		this.visit(n.progStmts);
		gen("return");
		gen(".limit","stack",10);
		gen(".end","method");
	}

	void visit(fieldDeclsNode n){
		//TODO
		// Translate thisField,
		// Then moreFields
		this.visit(n.thisField);
		this.visit(n.moreFields);
	}

	void visit(nullFieldDeclsNode n){}

	void visit(stmtsNode n){
		//System.out.println ("In stmtsNode\n");
		// Translate thisStmt
		this.visit(n.thisStmt);
		// Then moreStmts.
		this.visit(n.moreStmts);

	}

	void visit(nullStmtsNode n){}

	void visit(varDeclNode n){
		//TODO finish full implementation, check for correctness
		if(currentMethod == null){ //A global field declaration
			if(n.varName.idinfo.adr == none){
				// First pass has no address; Generate field declarations
				declField(n);
			} else {
				// Second pass; Do field initialization, if needed.
				if(!n.initValue.isNull()){
					if(!isNumericLit(n.initValue)){
						// Compute initValue onto stack; store in field
						this.visit(n.initValue);
						storeId(n.varName);
					}
				}
			}
			
		} else {
			//Handle local variable declarations
			// Give this variable and index equal to numberOfLocals, and 
			// remember index in symbol table entry
			n.varName.idinfo.varIndex = 
					currentMethod.name.idinfo.numberOfLocals;
			n.varName.idinfo.adr = local;
			
			// Increment numberOfLocals used in this method
			currentMethod.name.idinfo.numberOfLocals++;
			
			// If initValue is non-null, translate it and generate code to
			// store initValue into varName.
			if(!n.initValue.isNull()){
				this.visit(n.initValue);
				storeId(n.varName);
			}
		}
	}

	void visit(nullTypeNode n) {}

	void visit(intTypeNode n) {
		// No code generation needed
	}

	void visit(boolTypeNode n) {
		// No code generation needed
	}

	void visit(charTypeNode n) {
		// No code generation needed
	}

	void visit(voidTypeNode n) {
		// No code generation needed
	}

	void visit(asgNode n) {
		// Translate RHS (an expression)
		this.visit(n.source);

		// Value to be stored is now on the stack
		// Save it into target variable, using the variable's index
		gen("istore", n.target.varName.idinfo.varIndex);
	}

	void visit(ifThenNode n) { //No else statement in CSX lite
		String    out;  // label that will mark end of if stmt

		// translate boolean condition, pushing it onto the stack
		this.visit(n.condition);

		out = genLab();

		// generate conditional branch around then stmt
		gen("ifeq",out);

		// translate then part
		this.visit(n.thenPart);

		// generate label marking end of if stmt
		defineLab(out);
	}

	void visit(printNode n) {
		// compute value to be printed onto the stack
		this.visit(n.outputValue);

		// Call CSX library routine "printInt(int i)"
		gen("invokestatic"," CSXLib/printInt(I)V");
	}

	void visit(nullPrintNode n) {}

	void visit(blockNode n) {
		this.visit(n.decls);
		this.visit(n.stmts);
	}

	void visit(binaryOpNode n) {
		// TODO finish full implementation, check for correctness
		
		// First translate the left and right operands
		this.visit(n.leftOperand);
		this.visit(n.rightOperand);
		
		// Now the values of the operands are on the stack
		// Generate JVM instruction corresponding to operatorCode.
		// Is this a relational operator?
		if (relationCode(n.operatorCode) == ""){ // Not relational
			gen(selectOpCode(n.operatorCode));
		}else{
			//It is relational
			genRelationalOp(n.operatorCode);
		}
		n.adr = Stack;
	}


	void visit(identNode n) {
		// In CSX-lite, we don't code generate identNode directly.
		//  Instead, we do translation in parent nodes where the
		//   context of identNode is known
		// Hence no code generation actions are defined here 
		// (though you may want/need to define some in full CSX)

	}

	void visit(intLitNode n) {
		loadI(n.intval);
	}

	void visit(nameNode n) {
		// In CSX lite no arrays exist and all variable names are local variables

		// Load value of this variable onto stack using its index
		gen("iload",n.varName.idinfo.varIndex);
	}


	void visit(classNode n) {
		// TODO check for correctness
		currentMethod = null; // We're not in a method body yet
		
		 // A shared field CLASS is set to the name of the CSX class.
		CLASS = n.className.idname;
		
		// Generate beginning of class
		genComment("CSX program translated into Java bytecodes (Jasmin format)");
		gen(".class","public", CLASS);
		gen(".super","java/lang/Object");
		
		// Generate field declarations for the class, get addresses
		this.visit(n.members.fields);
		
		// Generate body of main(String[]);
		gen(".method"," public static","main([Ljava/lang/String;)V");

		// Generate non-trivial field declarations, get initial values
		this.visit(n.members.fields);
		
		
		// Generate end of class
		gen("invokestatic", CLASS+"/main()V");		
		gen("return");
		gen(".limit","stack",2);
		gen(".end","method");

		// Translate methods
		this.visit(n.members.methods);		
	}

	void visit(memberDeclsNode n) {
		// TODO is anything needed here?
		// Translate fields,
		// Then translate methods
		this.visit(n.fields);
		this.visit(n.methods);
	}


	void visit(valArgDeclNode n) {
		// TODO check for correctness
		// Label method argument with its address information
		n.argName.idinfo.adr = local;
		n.argName.idinfo.varIndex = currentMethod.name.idinfo.numberOfLocals++;
	}

	void visit(arrayArgDeclNode n) {
		// TODO check for correctness
		// Label method argument with its address information
		n.argName.idinfo.adr = local;
		n.argName.idinfo.varIndex = currentMethod.name.idinfo.numberOfLocals++;
	}

	void visit(argDeclsNode n) {
		// TODO check for correctness
		// Label each method argument with its address information
		this.visit(n.thisDecl);
		this.visit(n.moreDecls);
	}


	void visit(nullArgDeclsNode n) {}


	void visit(methodDeclsNode n) {
		// TODO check for correctness
		// Translate thisMethod,
		this.visit(n.thisDecl);
		// then moreMethods.
		this.visit(n.moreDecls);

	}

	void visit(nullMethodDeclsNode n) {}

	void visit(methodDeclNode n) {
		// TODO check for correctness
		currentMethod = n; // Now inside of a method
		n.name.idinfo.numberOfLocals = 0;
		String newTypeCode = n.name.idname;
		if(n.args.isNull()) //If there are no arguments
			newTypeCode = newTypeCode + "()";
		else newTypeCode = newTypeCode +"("
			+ buildTypeCode((argDeclsNode) n.args) + ")";
		newTypeCode = newTypeCode + typeCode(n.returnType);
		n.name.idinfo.methodReturnCode = typeCode(n.returnType);
		
		gen(".method", " public static", newTypeCode);
		
		// Assign local variable indices to args
		this.visit(n.args);
		
		// Generate code for local decls and method body
		this.visit(n.decls);
		this.visit(n.stmts);
		
		//Generate default return at end of method body
		if(n.returnType instanceof voidTypeNode)
			gen("return");
		else { // Push a default return value of 0
			loadI(0);
			gen("ireturn");
		}
		
		// Generate end of method data, with stack depth at 25
		gen(".limit", "stack", 25);
		gen(".limit", "locals", n.name.idinfo.numberOfLocals);
		gen(".end", "method");
	}

	void visit(trueNode n) {
		// TODO Auto-generated method stub

	}

	void visit(falseNode n) {
		// TODO Auto-generated method stub

	}

	void visit(constDeclNode n) {
		// TODO check for correctness
		if(currentMethod == null){ // A global const declaration
			if(n.constName.idinfo.adr == none){
				// First pass has no address assigned. Generate field decl
				declField(n);
			} else {
				// 2nd pass, do field initialization, if needed
				if(!isNumericLit(n.constValue)){
					// Comput constValue onto stack and store in field
					this.visit(n.constValue);
					storeId(n.constName);
				}
			}
		} else {
			// Process local const declaration
			// Give this constant an index equal to numberOfLocals, and
			// remember index in symbol table entry
			n.constName.idinfo.varIndex = 
					currentMethod.name.idinfo.numberOfLocals;
			n.constName.idinfo.adr = local;
			
			// Increment numberOfLocals used in this method
			currentMethod.name.idinfo.numberOfLocals++;
			
			// Compute and store const value
			this.visit(n.constValue);
			storeId(n.constName);
		}
	}

	void visit(arrayDeclNode n) {
		// TODO check for correctness, clean up
		if(currentMethod == null){ // A global array declaration
			if(n.arrayName.idinfo.adr == none) {
				//First pass, no address. Generate field declarations
				declField(n);
				return;
			}
		} else {
			// Process local array declaration
			// Give this array an index equal to numberOfLocals and
			// remember index in symbol table entry
			n.arrayName.idinfo.varIndex = 
					currentMethod.name.idinfo.numberOfLocals;
			n.arrayName.idinfo.adr = local;
			
			// Increment numberOfLocals used in this method
			currentMethod.name.idinfo.numberOfLocals++;
		}
		
		
		// Create array & store a reference to it
		loadI(n.arraySize.intval); //Push number of array elements
		allocateArray(n.elementType);
		if(n.arrayName.idinfo.adr == global)
			storeGlobalReference(n.arrayName.idinfo.label, 
					arrayTypeCode(n.elementType));
		else storeLocalReference(n.arrayName.idinfo.varIndex);
		
		// Allocate a field or local variable index for arrayName;
		// Generate code to allocate an array of type elementType
		// whose size is arraySize; generate code to store a reference to
		// the array in arrayName's field or local variable.
	}


	void visit(readNode n) {
		// TODO Auto-generated method stub

	}


	void visit(nullReadNode n) {}


	void visit(charLitNode n) {
		// TODO Auto-generated method stub

	}

	void visit(strLitNode n) {
		// TODO Auto-generated method stub

	}

	void visit(argsNode n) {
		// TODO Auto-generated method stub

	}


	void visit(nullArgsNode n) {}


	void visit(unaryOpNode n) {
		// TODO Auto-generated method stub

	}


	void visit(nullStmtNode n) {}


	void visit(nullExprNode n) {}


	void visit(whileNode n) {
		// TODO Auto-generated method stub

	}

	void visit(callNode n) {
		// TODO Auto-generated method stub

	}


	void visit(fctCallNode n) {
		// TODO Auto-generated method stub

	}


	void visit(returnNode n) {
		// TODO Auto-generated method stub

	}

	void visit(breakNode n) {
		// TODO Auto-generated method stub

	}

	void visit(continueNode n) {
		// TODO Auto-generated method stub

	}


	void visit(castNode n) {
		// TODO Auto-generated method stub

	}

	void visit(incrementNode n){
		// TODO Auto-generated method stub
	}
	void visit(decrementNode n){
		// TODO Auto-generated method stub
	}

}
