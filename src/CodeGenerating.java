/* CS 536: PROJECT 5 - CSX CODE GENERATOR
 * 
 * Caela Northey (cs login: caela)	905 653 2238 
 * Alan Irish    (cs login: irish)  906 591 2819
 *
 * DUE DATE: FRIDAY DEC 13, 2013
 *
 ***************************************************
 *
 *  This Visitor class generates JVM assembler code (using Jasmin's format)
 *  for CSX lite in the Printstream afile. You'll need to extend it to
 *  handle all of CSX. Note that for some AST nodes (like asgNode) code generation
 *  for CSX is more complex than that needed for CSX lite.
 *  All methods marked TODO will have to be completed by you (for full CSX)
 */

import java.io.*;

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

	// Type codes used in CSX
	public enum AdrMode { 
		global,
		local,
		stack,
		literal,
		none
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

	void branchZ(String label){
		gen("ifeq",label);
	}

	void loadI(int val){
		gen("ldc",val);
	}

	//generate a load of an int static field onto stack
	// should look like:
	//     getstatic CLASS/name I
	void loadGlobalInt(int val){
		//gen("getstatic", CLASS+"/"+,"I");
	}

	//generate a load of an int local variable onto stack
	// should look like:
	//     iload index
	void loadLocalInt(int val){
		gen("iload", val);
	}

	void storeGlobalInt(String name){
		// Generate a store into an int static field from the stack:
		// 		putstatic CLASS/name
		gen("putstatic", CLASS+"/"+name);
	}
	
	void storeLocalInt(int index){
		// Generate a store to an int local variable from the stack:
		//		istore index
		gen("istore", index);
	}

	//compute address associated w name node
	//DON'T load value addressed onto stack
	void computeAdr(nameNode n){
		if (n.subscriptVal.isNull()){ //simple unsubscribed identifier
			if(n.varName.idinfo.kind == ASTNode.Kinds.Var ||
				 n.varName.idinfo.kind == ASTNode.Kinds.ScalarParm){ //scalar
				if(n.varName.idinfo.adr == AdrModes.global){ 
					n.adr = AdrModes.global;
					n.label = n.varName.idinfo.label;
				} else { // local
					n.adr = AdrModes.local;
					n.varIndex = n.varName.idinfo.varIndex;
				}
			} else { //array
				//push ref to target to check length
				if(n.varName.idinfo.adr == AdrModes.global){
					n.label = n.varName.idinfo.label;
					loadGlobalReference(n.label, arrayTypeCode(n.varName.idinfo.type));
				} else { //local
					n.varIndex = n.varName.idinfo.varIndex;
					loadLocalReference(n.varIndex);
				}
			}
		} else { //subscripted
			//push ref first
			if(n.varName.idinfo.adr == AdrModes.global){
				n.label = n.varName.idinfo.label;
				loadGlobalReference(n.label, arrayTypeCode(n.varName.idinfo.type));
			} else { //local
				n.varIndex = n.varName.idinfo.varIndex;
				loadLocalReference(n.varIndex);
			}
			this.visit(n.subscriptVal);
		}
	}//end computeAdr()

	void binOp(String op){
		// Generate a binary operation
		// all operands are on the stack
		gen(op);
	}
	
	void storeId(identNode id){
		//TODO fix
		if(id.idinfo.kind == ASTNode.Kinds.Var || 
				id.idinfo.kind == ASTNode.Kinds.Value) {
			// id is a scalar variable
			if(id.idinfo.adr == AdrMode.global) // ident is global
				storeGlobalInt(id.idinfo.label);
			else if (id.idinfo.adr == AdrMode.local)
				storeLocalInt(id.idinfo.varIndex);
		} else {
			//Handle arrays
		}
	}

	void storeName(nameNode n){
		if (n.subscriptVal.isNull()){ //simple unsubscribed identifier
			if(n.varName.idinfo.kind == ASTNode.Kinds.Var ||
				 n.varName.idinfo.kind == ASTNode.Kinds.ScalarParm){ //scalar
				if(n.varName.idinfo.adr == AdrModes.global){ 
					storeGlobalInt(n.label);
				} else { // local
					storeLocalInt(n.varIndex);
				}
			} else { //array
				//push ref to target to check length
				switch (n.type){
					case ASTNode.Types.Integer:
						gen("invokestatic","CSXLib/checkIntArrayLength([I[I)[I");
						break;
					case ASTNode.Types.Boolean:
						gen("invokestatic","CSXLib/checkBoolArrayLength([Z[Z)[Z");
						break;
					case ASTNode.Types.Character:
						gen("invokestatic","CSXLib/checkCharArrayLength([C[C)[C");
						break;
				}
				//store source array into target var
				if(n.varName.idinfo.adr == AdrModes.global){
					n.label = n.varName.idinfo.label;
					storeGlobalReference(n.label, arrayTypeCode(n.varName.idinfo.type));
				} else { //local
					n.varIndex = n.varName.idinfo.varIndex;
					storeLocalReference(n.varIndex);
				}
			}
		} else { //subscripted
			//ref to target array, subscripted expr, and source expr
			//are already pushed; now store source val into array
			switch(n.type){
				case ASTNode.Types.Integer: //iaload
					gen("iaload");
					break;
				case ASTNode.Types.Boolean: //baload
					gen("baload");
					break;
				case ASTNode.Types.Character: //caload
					gen("caload");
					break;
			}
		}
	}//end storeName()
	
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
		System.out.println("ERROR: Invalid Type"); // TODO Shouldn't occur, remove before submitting
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
	
	String typeCode(ASTNode.Types type){
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
	
	String buildTypeCode(exprNode n){
		// TODO test for correctness
		if(isArray(n.kind))
			return arrayTypeCode(n.type);
		else return typeCode(n.type);
	}
	
	String buildTypeCode(argsNode n){
		// TODO test for correctness
		if(n.moreArgs.isNull())
			return buildTypeCode(n.argVal);
		else return buildTypeCode(n.argVal)+buildTypeCode((argsNode)n.moreArgs);
	}
	
	String buildTypeCode(String methodName, argsNodeOption args, 
														String returnCode){
		String newTypeCode = methodName;
		if(args.isNull())
			newTypeCode = newTypeCode + "()";
		else newTypeCode = newTypeCode+"(" +buildTypeCode((argsNode)args)+")";
		return newTypeCode + returnCode;
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
		n.varName.idinfo.adr = AdrMode.global;
	}
	
	void declField(constDeclNode n){
		//TODO check for correctness
		String constLabel = n.constName.idname +"$";
		declGlobalInt(constLabel, n.constValue);
		n.constName.idinfo.label = constLabel;
		n.constName.idinfo.adr = AdrMode.global;
	}
	
	void declField(arrayDeclNode n){
		//TODO check for correctness
		String arrayLabel = n.arrayName.idname + "$";
		declGlobalArray(arrayLabel, n.elementType);
		n.arrayName.idinfo.label = arrayLabel;
		n.arrayName.idinfo.adr = AdrMode.global;
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
			if(n.varName.idinfo.adr == AdrMode.none){
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
			n.varName.idinfo.adr = AdrMode.local;
			
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
	
	// 1) if source is an array, generate code to clone it and save a
	//    reference to clone in target
	// 2) if source is a string lit, generate code to convery it to a
	//    character array and save a reference in target
	// 3) if target is an indexed array, generate code to push a ref
	//    to array (using varName) then translate target.subscriptVal
	// 4) translate source
	// 5) generate code to store source's val in target
	void visit(asgNode n) {
		computeAdr(n.target);
    this.visit(n.source); //step 4

		//check if source should be cloned or converted,
		if (n.source.kind == ASTNode.Kinds.Array ||
				n.source.kind == ASTNode.Kinds.ArrayParm){ //step 1
			switch (n.type){
				case ASTNode.Types.Integer:
					gen("invokestatic"," CSXLib/cloneIntArrayLength([I)[I");
					break;
				case ASTNode.Types.Boolean:
					gen("invokestatic"," CSXLib/cloneBoolArrayLength([Z)[Z");
					break;
				case ASTNode.Types.Character:
					gen("invokestatic"," CSXLib/cloneCharArrayLength([C)[C");
					break;
				}
		}else if (n.source.kind == ASTNode.Kinds.String){ //step 2
			gen("invokestatic"," CSXLib/convertString(LJava/lang/String;)[C");
		}

		//val to store now on stack; store to LHS
		storeName(n.target); //step 5
	}
	
	// 1) translate condition (onto stack)
	// 2) generate code to conditionally branch around thenPart
	// 3) translate thenPart
	// 4) generate a jump past elsePart
	// 5) translate elsePart
	// should look like:
	//       {eval control expr onto stack top}
	//       ifeq L1    ; branch to L2 if top of stack == 0 (ie, false)
	//       {code for then part}
	//       goto  L2
	//   L1: 
	//       {code for else part}
	//   L2:
	void visit(ifThenNode n) { //No else statement in CSX lite
		String endLab;  // label that will mark end of if stmt
		String elseLab;
		this.visit(n.condition); //step 1
		elseLab = genLab();
		branchZ(elseLab); //step 2
		this.visit(n.thenPart); //step 3
		endLab = genLab();
		branch(endLab); //step 4
		defineLab(elseLab);
		this.visit(n.elsePart); //step 5
		defineLab(endPart);
	}

	// 1) translate outputValue; 
	// 2) generate call to CXSLib.printString(String) or CSXLib.printChar(char)
	//    or CSXLib.printInt(int) or Lib.printBool(bool) or Lib.PrintCharArray
	//    depending on type ot outputValue
	// 3) translate morePrints
	// NOTE: can only print int, bool, chars, char arrays, and strings
	void visit(printNode n) {
  	this.visit(n.outputValue); //step 1
		if (n.outputValue.kind == ASTNode.Kinds.Array ||
				n.outputValue.kind == ASTNode.Kinds.ArrayParm){ //step 2
				gen("invokestatic"," CSXLib/printCharArray([C)V");
		}else if (n.outputValue.kind == ASTNode.Kinds.String){
    	gen("invokestatic"," CSXLib/printString(LJava/lang/String;)V");
		}else{ 
			switch (n.outputValue.type){
				case Integer:
					gen("invokestatic"," CSXLib/printInteger(I)V");
					break;
				case Boolean:
					gen("invokestatic"," CSXLib/printBool(Z)V");
					break;
				case Character:
					gen("invokestatic"," CSXLib/printChar(C)V");
					break;
			default:
				gen("ERROR: Invalid Type"); // Shouldn't happen TODO
				break;
			}
		}
		this.visit(n.morePrints); //step 3
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
		n.adr = AdrMode.stack;
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
		n.adr = AdrMode.literal;
	}
	
	// 1) if subscriptVal is null (ie, is not an array): generate code to
	//    push val at varName's field name or local vaiable index
	// 2) Otherwise:
  //    a) generate code to push the array REFERENCE stored at
	//       varName's field name or local var index
	//    b) translate subscriptVal
	//    c) generate an iaload or baload or caload based on varName's
	//       element type
	void visit(nameNode n) {
		n.adr = AdrModes.stack;
		if(n.subscriptVal.isNull()){ //if non-subscripted
			if(n.varName.idinfo.kind == ASTNode.Kinds.Var || //if scalar var or const
				 n.varName.idinfo.kind == ASTNode.Kinds.Value ||
				 n.varName.idinfo.kind == ASTNode.Kinds.Value ){ 
				if(n.varName.idinfo.adr == AdrModes.global){ //if global, has label
					String label = n.varName.idinfo.label;
					loadGlobalInt(label);
				}else{ //else local has index
					n.varIndex = n.varName.idinfo.varIndex;
					loadLocalInt(n.varIndex);
				}
			}else{ //array or array param
				if(n.varName.idinfo.adr == AdrModes.global){ //global
					String label = n.varName.idinfo.label;
					loadGlobalReference(label);
				}else{ //else local
					n.varIndex = n.varName.idinfo.varIndex;
					loadLocalReference(n.varIndex);
				}
			}
		}else{ //is subscripted
			//push reference first
			if(n.varName.idinfo.adr == AdrModes.global){ //global
					String label = n.varName.idinfo.label;
					loadGlobalReference(label);
				}else{ //else local
					n.varIndex = n.varName.idinfo.varIndex;
					loadLocalReference(n.varIndex);
				}
			//then compute subscript val
			this.visit(n.subscriptVal);
			//then load array element onto stack
			switch(n.type){
				case ASTNode.Types.Integer: //iaload
					gen("iaload");
					break;
				case ASTNode.Types.Boolean: //baload
					gen("baload");
					break;
				case ASTNode.Types.Character: //caload
					gen("caload");
					break;
			}
		}

	}//end nameNode


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
		n.argName.idinfo.adr = AdrMode.local;
		n.argName.idinfo.varIndex = currentMethod.name.idinfo.numberOfLocals++;
	}

	void visit(arrayArgDeclNode n) {
		// TODO check for correctness
		// Label method argument with its address information
		n.argName.idinfo.adr = AdrMode.local;
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
		loadI(1);
		n.adr = AdrMode.literal;
		//n.intval = 1;
	}

	void visit(falseNode n) {
		loadI(0);
		n.adr = AdrMode.literal;
		//n.intval = 0;
	}

	void visit(constDeclNode n) {
		// TODO check for correctness
		if(currentMethod == null){ // A global const declaration
			if(n.constName.idinfo.adr == AdrMode.none){
				// First pass has no address assigned. Generate field decl
				declField(n);
			} else {
				// 2nd pass, do field initialization, if needed
				if(!isNumericLit(n.constValue)){
					// Compute constValue onto stack and store in field
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
			n.constName.idinfo.adr = AdrMode.local;
			
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
			if(n.arrayName.idinfo.adr == AdrMode.none) {
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
			n.arrayName.idinfo.adr = AdrMode.local;
			
			// Increment numberOfLocals used in this method
			currentMethod.name.idinfo.numberOfLocals++;
		}
		
		
		// Create array & store a reference to it
		loadI(n.arraySize.intval); //Push number of array elements
		allocateArray(n.elementType);
		if(n.arrayName.idinfo.adr == AdrMode.global)
			storeGlobalReference(n.arrayName.idinfo.label, 
					arrayTypeCode(n.elementType));
		else storeLocalReference(n.arrayName.idinfo.varIndex);
		
		// Allocate a field or local variable index for arrayName;
		// Generate code to allocate an array of type elementType
		// whose size is arraySize; generate code to store a reference to
		// the array in arrayName's field or local variable.
	}

	// 1) generate call to CSXLib.readInt() or Lib.readChar()...
	//    depending on type of targetVar
	// 2) generate store to targetVar
	// 3) translate moreReads
	void visit(readNode n) {
		computeAdr(n.targetVar);
		if (n.targetVar.varName.idinfo.type == ASTNode.Types.Integer){ //step 1
			gen("invokestatic"," CSXLib/readInt()I");
		}else{
			gen("invokestatic"," CSXLib/readChar()C");
		}
		storeName(n.targetVar); //step 2
		this.visit(n.moreReads); //step 3
	}

	void visit(nullReadNode n) {}


	void visit(charLitNode n) {
		loadI(n.charval);
		n.adr = AdrMode.literal;
		n.intval = n.charval;
	}

	void visit(strLitNode n) {
		gen("ldc", n.strval);
	}

	//translate argValue, then moreArgs
	void visit(argsNode n) {
		// TODO test for correctness
		//Evaluate arguments and load them onto stack
		this.visit(n.argVal);
		this.visit(n.moreArgs);
	}


	void visit(nullArgsNode n) {}

	// 1) translate operand
	// 2) generate JVM instructions cooresponding to operandCode
	// NOTE: ! can be implemented with EX-OR w 1
	void visit(unaryOpNode n) {
		this.visit(n.operand);
		loadI(1);
		gen("ixor"); //integer bitwise exclusive ors
	}

	void visit(nullStmtNode n) {}

	void visit(nullExprNode n) {}

	// 1) create assembler labesl for head and exit
	// 2) if label is non-null, store head and exit in label's
	//    sym tabel entry
	// 3) generate head label
	// 4) translate condition
	// 5) generate conditional branch to exit label
	// 6) translate loopBody
	// 7) generate jump to head
	// 8) generate exit label
	// looks like:
	//    L1: {eval control expr onto stack}
	//        ifeq l2    ; branch to L2 if top of stack == 0 (ie, false)
	//        {code for loop body}
	//    L2:
	void visit(whileNode n) {
		String top = genLab(); //step 1
		String bottom = getLab();
		if (! n.label.isNull()){ //step 2
			((identNode)n.label).idinfo.topLabel = top;
			((identNode)n.label).idinfo.bottomLabel = bottom;
		}
		defineLab(top); //step 3
		this.visit(n.condition); //step 4
		branchZ(bottom); //step 5
		this.visit(n.loopBody); //step 6
		branch(top); // step 7
		defineLab(bottom); // step 8
	}

	// 1) translate procArgs
	// 2) generate a static call to procName
	void visit(callNode n) {
		// TODO check for correctness
		// Evaluate args and push them onto the stack
		this.visit(n.args);
		// Generate call to method, using its type code
		String typeCode = buildTypeCode(n.methodName.idname, n.args,
							n.methodName.idinfo.methodReturnCode);
		//genCall(CLASS+"/"+typeCode);
		gen(CLASS+"/"+typeCode); //TODO not sure if this is correct
	}

	// 1) translate functionArgs
	// 2) generate a static call to procName
	void visit(fctCallNode n) {
		// TODO check for correctness
		// Evaluate args and push them onto the stack
		this.visit(n.methodArgs);
		// Generate call to method, using its type code
		String typeCode = buildTypeCode(n.methodName.idname, n.methodArgs,
							n.methodName.idinfo.methodReturnCode);
		//genCall(CLASS+"/"+typeCode);
		gen(CLASS+"/"+typeCode);

	}

	// 1) if returnVal is non-Null, then translate it and generate an ireturn
	// 2) otherwise generate a return
	void visit(returnNode n) {
		if(n.returnVal.isNull()){ //step 2
			gen("return");
		}else{
			this.visit(n.returnVal); //step 1
			gen("ireturn");
		}	
	}

	//generate a jump to loop exit label stored in label's sym table entry
	void visit(breakNode n) {
		branch(n.label.idinfo.bottomLabel);
	}

	//generate a jump to loop head label stored in label's sym table entry
	void visit(continueNode n) {
		branch(n.label.idinfo.topLabel);
	}

	// 1) if resultType is bool and operand is int or char, then if operand
	//    is non-zero, generate code to convert it to 1 (ie, true)
	// 2) if resultType is char and operand is an int, then generate code
	//    to extract the rightmost 7 bits of operand (ie, AND with mask)
	// NOTE:
	// in all other cases, the value of operand may be used w/out modification
	void visit(castNode n) {
		this.visit(n.operand);
		if((n.operand.type == ASTNode.Types.Integer || //step 1
				n.operand.type == ASTNode.Types.Integer)&&
        n.resultType instanceof boolTypeNode){
			loadI(0);
			genRelationalOP(sym.NOTEQ);
		} else if (n.operand.type == ASTNode.Types.Integer && //step 2
               n.resultType instanceof charTypeNode){
			loadI(127); //ie, 1111111 in binary
			gen("iand");
		}

	}
	
	// 1) if target.subscriptVal is null:
	//    a) generate code to push target.varname's value onto stack
	//    b) push integer 1 onto stack
	//    c) generate code for iadd
	//    d) store stack top into target.varName
	// 2) otherwise:
	//    a) push array ref stored in target.varName
	//    b) translate target.subscriptVal
	//    c) dubplicate top two stack values using dup2
	//    d) generate an iaload or caload
	//    e) push integer 1
	//    f) generate iadd
	//    g) generate an iastore or castore
	void visit(incrementNode n){
		if(n.target.subscriptVal.isNull()){ //un-subscripted, step 1
			this.visit(n.target); //step 1.a
			loadI(1); //step 1.b
			gen("iadd"); //step 1.c
			computeAdr(n.target);
			storeName(n.target); //step 1.d
		}else { //subscripted, step 2
			computeAdr(n.target); //step 2.a
			//this.visit(n.target); //step 2.b ??
			gen("dup2"); //one for load, one for store, step 2.c
			switch(n.target.type){ //load array element onto stack, step 2.d
				case ASTNode.Types.Integer:
					gen("iaload");
					break;
				case ASTNode.Types.Boolean:
					gen("baload");
					break;
				case ASTNode.Types.Character:
					gen("caload");
					break;
			}
			loadI(1); //step 2.e
			gen("iadd"); //step 2.f
			storeName(n.target); //step 2.g
		}
	}//end inc

	//same as incrementNode except with isub
	void visit(decrementNode n){
if(n.target.subscriptVal.isNull()){ //un-subscripted, step 1
			this.visit(n.target); //step 1.a
			loadI(1); //step 1.b
			gen("isub"); //step 1.c
			computeAdr(n.target);
			storeName(n.target); //step 1.d
		}else { //subscripted, step 2
			computeAdr(n.target); //step 2.a
			//this.visit(n.target); //step 2.b ??
			gen("dup2"); //one for load, one for store, step 2.c
			switch(n.target.type){ //load array element onto stack, step 2.d
				case ASTNode.Types.Integer:
					gen("iaload");
					break;
				case ASTNode.Types.Boolean:
					gen("baload");
					break;
				case ASTNode.Types.Character:
					gen("caload");
					break;
			}
			loadI(1); //step 2.e
			gen("isub"); //step 2.f
			storeName(n.target); //step 2.g
		}
	}//end dec

}
