/* CS 536: PROJECT 4 - CSX TYPE CHECKER
 * 
 * Caela Northey (cs login: caela)	905 653 2238 
 * Alan Irish    (cs login: irish) 906 591 2819
 *
 * DUE DATE: FRIDAY NOV 22, 2013
 */

// The following methods type check  AST nodes used in CSX Lite
//  You will need to complete the methods after line 238 to type check the
//   rest of CSX
//  Note that the type checking done for CSX lite may need to be extended to
//   handle full CSX (for example binaryOpNode).

import java.util.ArrayList;

public class TypeChecking extends Visitor { 

	//	static int typeErrors =  0;     // Total number of type errors found 
	//  	public static SymbolTable st = new SymbolTable(); 	
	int typeErrors;     // Total number of type errors found 
	ASTNode.Types currReturnType; //return type of current method
	boolean mainDeclared = false; //Tracks if main has been declared
	SymbolTable st;	

	TypeChecking(){
		typeErrors = 0;
		st = new SymbolTable(); 
		currReturnType = ASTNode.Types.Void; //defalt
	}

	boolean isTypeCorrect(csxLiteNode n) {
		this.visit(n);
		return (typeErrors == 0);
	}

	boolean isTypeCorrect(classNode n) {
		this.visit(n);
		return (typeErrors == 0);
	}

	static void assertCondition(boolean assertion){  
		if (! assertion)
			throw new RuntimeException();
	}

	void typeMustBe(ASTNode.Types testType,ASTNode.Types requiredType,
			String errorMsg) {
		if ((testType != ASTNode.Types.Error) && (testType != requiredType)) {
			System.out.println(errorMsg);
			typeErrors++;
		}
	}

	void typeMustBe(ASTNode.Types testType, ASTNode.Types option1, 
			ASTNode.Types option2, String errorMsg){
		if ((testType != ASTNode.Types.Error) && !((testType == option1) || 
				(testType == option2)))
		{
			System.out.println(errorMsg);
			typeErrors++;
		}
	}

	void typesMustBeEqual(ASTNode.Types type1,ASTNode.Types type2,
			String errorMsg) {
		if ((type1 != ASTNode.Types.Error) && (type2 != ASTNode.Types.Error) &&
				(type1 != type2)) {
			System.out.println(errorMsg);
			typeErrors++;
		}
	}

	//ie, var, array, scalar param, or array param
	boolean kindIsAssignable(ASTNode.Kinds testKind){
		return (testKind == ASTNode.Kinds.Var)||
				(testKind == ASTNode.Kinds.Array)||
				(testKind == ASTNode.Kinds.ScalarParm)||
				(testKind == ASTNode.Kinds.ArrayParm);
	}

	//ie, Var, Value, ScalarParm
	boolean isScalar(ASTNode.Kinds testKind){
		return (testKind == ASTNode.Kinds.ScalarParm)||
				(testKind == ASTNode.Kinds.Value)||
				(testKind == ASTNode.Kinds.Var);
	}

	//builds an array of parmInfo objects that store type and kind
	//info for each arg in arg tree
	ArrayList<parmInfo> buildArgList(argsNodeOption firstArg){
		ArrayList<parmInfo> args = new ArrayList<parmInfo>();
		argsNodeOption currentArg = firstArg;
		while (!currentArg.isNull()){ //while args to process
			argsNode temp = (argsNode)currentArg;
			parmInfo pi = new parmInfo(temp.argVal.kind, temp.argVal.type);
			args.add(pi);
			currentArg = temp.moreArgs;
		}
		return args;
	}

	void typesMustBeComparable(ASTNode.Types type1,ASTNode.Types type2,
			String errorMsg) {
		if ((type1 == ASTNode.Types.Error) || (type2 == ASTNode.Types.Error))
			return;
		if ((type1 == ASTNode.Types.Boolean)&&(type2 == ASTNode.Types.Boolean))
			return;
		if (((type1 == ASTNode.Types.Integer) || 
				(type1 == ASTNode.Types.Character)) &&
				((type2 == ASTNode.Types.Integer) || 
						(type2 == ASTNode.Types.Character)))
			return;
		System.out.println(errorMsg);
		typeErrors++;
	}

	int countChars(String str){
		int count = 0;
		str = str.substring(1,str.length()-1);//remove quoats

		while(str.contains("\\n")){
			str = str.replace("\\n", "");
			count++;
		}
		while(str.contains("\\t")){
			str = str.replace("\\t", "");
			count++;
		}
		while(str.contains("\\")){
			str = str.replace("\\", "");
			count++;
		}
		while(str.contains("\\'")){
			str = str.replace("\\'", "");
			count++;
		}
		return count+=str.length(); //add rest of chars

	}

	String error(ASTNode n) {
		return "Error (line " + n.linenum + "): ";
	}

	static String opToString(int op) {
		switch (op) {
		case sym.PLUS:
			return(" + ");
		case sym.MINUS:
			return(" - ");
		case sym.EQ:
			return(" == ");
		case sym.NOTEQ:
			return(" != ");
		case sym.CAND:
			return(" && ");
		case sym.COR:
			return(" || ");
		case sym.GEQ:
			return(" >= ");
		case sym.GT:
			return(" > ");
		case sym.LEQ:
			return(" <= ");
		case sym.LT:
			return(" < ");
		case sym.SLASH:
			return(" / ");
		case sym.TIMES:
			return(" * ");
		default:
			assertCondition(false);
			return "";
		}
	}

	static void printOp(int op) {
		switch (op) {
		case sym.PLUS:
			System.out.print(" + ");
			break;
		case sym.MINUS:
			System.out.print(" - ");
			break;
		case sym.EQ:
			System.out.print(" == ");
			break;
		case sym.NOTEQ:
			System.out.print(" != ");
			break;
		case sym.CAND:
			System.out.print(" && ");
			break;
		case sym.COR:
			System.out.print(" || ");
			break;
		case sym.GEQ:
			System.out.print(" >= ");
			break;
		case sym.GT:
			System.out.print(" > ");
			break;
		case sym.LEQ:
			System.out.print(" <= ");
			break;
		case sym.LT:
			System.out.print(" < ");
			break;
		case sym.SLASH:
			System.out.print(" / ");
			break;
		case sym.TIMES:
			System.out.print(" * ");
			break;
		default:
			throw new Error();
		}
	}

	void visit(csxLiteNode n){
		this.visit(n.progDecls);
		this.visit(n.progStmts);
	}

	void visit(fieldDeclsNode n){
		this.visit(n.thisField);
		this.visit(n.moreFields);
	}

	void visit(nullFieldDeclsNode n){}

	void visit(stmtsNode n){
		this.visit(n.thisStmt);
		this.visit(n.moreStmts);
	}

	void visit(nullStmtsNode n){}

	void visit(varDeclNode n){
		SymbolInfo     id;
		id = (SymbolInfo) st.localLookup(n.varName.idname);

		// Check that identNode.idname is not already in the symbol table.
		if (id != null) {
			System.out.println(error(n) + id.name()+ " is already declared.");
			typeErrors++;
			n.varName.type = ASTNode.Types.Error;
		} else {
			//If the initValue is not null, handle initialization
			if(!n.initValue.isNull())
			{
				// Type check initial value expression.
				visit(n.initValue);

				// Check that the initial value's type is typeNode.type
				typesMustBeEqual(n.varType.type, ((exprNode)n.initValue).type, 
						error(n)+"Initializer must be of type "+n.varType.type);

				// Check that the initial value's kind is scalar
				// Not sure if this is the best way to do it
				try{
					assertCondition(isScalar(((exprNode)n.initValue).kind));
				} catch (RuntimeException r){
					System.out.println(error(n)+"Initial value must be scalar");
				}
				// Enter identNode.idname into symbol table with 
				// 			type = typeNode.type and kind = Variable.
				id = new SymbolInfo(n.varName.idname,
						ASTNode.Kinds.Var, n.varType.type);

				n.varName.type = n.varType.type;
				try {
					st.insert(id);
				} catch (DuplicateException d) 
				{ /* can't happen */ }
				catch (EmptySTException e) 
				{ /* can't happen */ }
				n.varName.idinfo=id;

			} else { // Declaration without initialization

				// Enter identNode.idname into symbol table with 
				// type=typeNode.type and kind = Variable.
				id = new SymbolInfo(n.varName.idname,
						ASTNode.Kinds.Var, n.varType.type);
				n.varName.type = n.varType.type;
				try {
					st.insert(id);
				} catch (DuplicateException d) 
				{ /* can't happen */ }
				catch (EmptySTException e) 
				{ /* can't happen */ }
				n.varName.idinfo=id;
			}
		}
	}

	void visit(nullTypeNode n){}

	void visit(intTypeNode n){
		//no type checking needed}
	}

	void visit(boolTypeNode n){
		//no type checking needed}
	}

	// 1) lookup identNode.idname in symbol table; error if absent
	// 2) copy symbol table's type and kind info into identNode   
	// 3) store a link to the symbol table entry in the identNode 
	void visit(identNode n){
		SymbolInfo    id;
		id =  (SymbolInfo) st.globalLookup(n.idname);
		if (id == null) {
			System.out.println(error(n) +  n.idname + " is not declared.");
			typeErrors++;
			n.type = ASTNode.Types.Error;
		} else {
			n.type = id.type; 
			n.kind = id.kind;
			n.idinfo = id; // Save ptr to correct symbol table entry
		}

	}

	// Extend nameNode's method to handle subscripts
	// 1) type check identNode
	// 2) if subscriptVal is a null node, copy identNode's type and
	//    and kind values into nameNode and return
	// 3) type check subscriptVal
	// 4) check that identNode's kind is an array
	// 5) check that subscriptVals's kind is scalar and type is int or char
	// 6) set nameNode's type to the identNode's type and the
	//    nameNode's kind to Variable
	void visit(nameNode n){
		this.visit(n.varName);    // step 1
		if(n.subscriptVal.isNull()){ // step 2
			n.type = n.varName.type;
			n.kind = n.varName.kind;
			return;
		}
		this.visit(n.subscriptVal);  // step 3
		try{
			assertCondition(n.varName.kind == ASTNode.Kinds.Array || //step 4
                      n.varName.kind == ASTNode.Kinds.ArrayParm);
		} catch (RuntimeException e) {
			typeErrors++;
			System.out.println(error(n) + "Only arrays can be subscripted.");
			return;
		}
		exprNode temp = (exprNode)n.subscriptVal;
		try{
			assertCondition(isScalar(temp.kind)); // step 5
			assertCondition((temp.type == ASTNode.Types.Integer) ||
					(temp.type == ASTNode.Types.Character));
		} catch (RuntimeException e) {
			typeErrors++;
			System.out.println(error(n) + "subscript value should be an integer or" +
					" character, but " + temp.type + " was found instead.");
		}
		n.type=n.varName.type;    // step 6
		n.kind= ASTNode.Kinds.Var;

	}

	// 1,2) type check nameNode and expression tree
	// 3) check that nameNode' kind is assignable (var, array, scalar
	//    param, or array param)
	// 4) if nameNode's kind is scalar then check expr tree's kind is also
	//    scalar and that both have same type; then return
	// 5) if nameNode and expr tree's kinds are both arrays and both have
	//    same type, check that length same; then return
	// 6) if nameNode's kind is array and type is char, and expr tree's kind
	//    is string, check that both have same length; then return
	// 7) otherwise, expr may not be assigned to name node
	// other notes: can't asign to const var
	void visit(asgNode n){

		this.visit(n.target); // step 1
		this.visit(n.source); // step 2
		try{
			assertCondition(kindIsAssignable(n.target.kind)); // step 3
		} catch (RuntimeException e) {
			typeErrors++;
			System.out.println(error(n) + "Target of kind " + n.target.kind +
					" is not assignable.");
			return;
		} 
		if(isScalar(n.target.kind)){ // step 4 
			try{
				//can't assign to a constant (aka value)
				assertCondition(n.target.kind != ASTNode.Kinds.Value);
			} catch (RuntimeException e) {
				typeErrors++;
				System.out.println(error(n) + "Cannot assign to a constant value.");
			}
			try{
				//target and source must have same type
				assertCondition(n.target.type == n.source.type);
			} catch (RuntimeException e) {
				typeErrors++;
				System.out.println(error(n)+"Both the left and right hand "+
                   "sides of an assignment must have the same type.");
			}
      return;
		}

		if((n.target.varName.kind == ASTNode.Kinds.Array) && // step 5
				(n.source.kind == ASTNode.Kinds.Array) &&
				(n.target.varName.type == n.source.type)){
			//look up target and source to get array info
			SymbolInfo id_s = (SymbolInfo)st.globalLookup(n.target.varName.idname);
			nameNode temp = (nameNode)n.source; //know is array => nameNode
			SymbolInfo id_t = (SymbolInfo)st.globalLookup(temp.varName.idname);
			try{
				assertCondition(id_s.arraySize == id_t.arraySize);
			} catch (RuntimeException e) {
				typeErrors++;
				System.out.println(error(n) + "Arrays must be same length.");
			}
			return;			
		}
		if(n.target.kind == ASTNode.Kinds.Array && // step 6
				n.target.type == ASTNode.Types.Character &&
				n.source.kind == ASTNode.Kinds.String){
			SymbolInfo id_s = (SymbolInfo)st.globalLookup(n.target.varName.idname);
			strLitNode temp2 = (strLitNode)n.source; //know is string => strLitNode
			try{
				assertCondition(id_s.arraySize == countChars(temp2.strval));
			} catch (RuntimeException e) {
				typeErrors++;
				System.out.println(error(n) + "Character array and String must have same length.");
			}
			return;
		}
		System.out.println(error(n) + "Incorrect assignment.");
	}

	// Extend ifThenNode's method to handle else parts
	void visit(ifThenNode n){
		this.visit(n.condition);
		typeMustBe(n.condition.type, ASTNode.Types.Boolean,
				error(n) + "The control expression of an if must be a bool.");
		this.visit(n.thenPart);
		this.visit(n.elsePart);
	}

	//can print int, bool and chars values + char arrays and sting lits
	void visit(printNode n){
		this.visit(n.outputValue);
		try{
			assertCondition((n.outputValue.type == ASTNode.Types.Integer ||
					n.outputValue.type == ASTNode.Types.Boolean ||
					n.outputValue.type == ASTNode.Types.Character ) ||
					(n.outputValue.type == ASTNode.Types.Character &&
					n.outputValue.kind == ASTNode.Kinds.Array ) ||
					(n.outputValue.kind == ASTNode.Kinds.String));
		} catch (RuntimeException e) {
			typeErrors++;
			System.out.println(error(n) + "Can only print Integer, Boolean, and "+
					"Character values, String, or arrays of Characters.");
		}
		this.visit(n.morePrints);
	}

	void visit(blockNode n){
		// open a new local scope for the block body
		st.openScope();
		this.visit(n.decls);
		this.visit(n.stmts);
		// close this block's local scope
		try { st.closeScope();
		}  catch (EmptySTException e) 
		{ /* can't happen */ }
	}

	void visit(binaryOpNode n){

		//Make sure the binary operator has a valid binary operator symbol
		assertCondition(n.operatorCode == sym.PLUS  ||
				n.operatorCode == sym.MINUS || 
				n.operatorCode == sym.EQ    ||
				n.operatorCode == sym.NOTEQ ||
				n.operatorCode == sym.CAND  ||
				n.operatorCode == sym.COR   ||
				n.operatorCode == sym.GEQ   ||
				n.operatorCode == sym.GT    ||
				n.operatorCode == sym.LEQ   ||
				n.operatorCode == sym.LT    ||
				n.operatorCode == sym.SLASH ||
				n.operatorCode == sym.TIMES);

		// Set binaryOpNode.kind = Value
		// This is handled in the abstract syntax tree

		// Type check left and right operands
		this.visit(n.leftOperand);
		this.visit(n.rightOperand);
		
		// Check that left and right operands are both scalar.
		try{
			assertCondition(isScalar(n.leftOperand.kind));
			assertCondition(isScalar(n.rightOperand.kind));
		} catch (RuntimeException r){
			System.out.println(error(n)+"Operands of"+opToString(n.operatorCode)
					+ "must be scalar.");
			typeErrors++;
		}
		//Check for arithmetic operation, + - / *
		if (n.operatorCode== sym.PLUS  || n.operatorCode== sym.MINUS ||
				n.operatorCode== sym.SLASH || n.operatorCode== sym.TIMES){
			// Arithmetic operators may be applied to int or char values;
			// the result is of type int.
			n.type = ASTNode.Types.Integer;

			//Check that operands have an arithmetic type (int or char)
			typeMustBe(n.leftOperand.type, ASTNode.Types.Integer, 
					ASTNode.Types.Character, error(n) + "Left operand of" + 
							opToString(n.operatorCode) + "must be arithmetic.");
			typeMustBe(n.rightOperand.type, ASTNode.Types.Integer, 
					ASTNode.Types.Character, error(n) + "Right operand of" + 
							opToString(n.operatorCode) + "must be arithmetic.");

		} //Check if relational operation, == < > != <= >=
		else if (n.operatorCode == sym.EQ  || n.operatorCode == sym.NOTEQ ||
				n.operatorCode == sym.GEQ || n.operatorCode == sym.GT  ||
				n.operatorCode == sym.LEQ || n.operatorCode == sym.LT) {
			// Relational operators may be applied only to a pair of arithmetic 
			// values or to a pair of bool values; the result is of type bool.
			n.type = ASTNode.Types.Boolean;

			String errorMsg = error(n)+"operands of"+
					opToString(n.operatorCode)+"must both be arithmetic or both"
					+ "must be boolean.";
			// Both operands must be arithmetic, or both must be booleans
			typesMustBeComparable(n.leftOperand.type, n.rightOperand.type,
					errorMsg);
		} //Check if logical operation
		else {
			//Logical operators may be applied only to bool values
			//The result is of type bool.
			n.type = ASTNode.Types.Boolean;

			//Check that left and right operands have a boolean type. 
			typeMustBe(n.leftOperand.type, ASTNode.Types.Boolean, error(n) + 
					"Left operand of" + opToString(n.operatorCode) +  
					"must be boolean.");
			typeMustBe(n.rightOperand.type, ASTNode.Types.Boolean, error(n) + 
					"Right operand of" + opToString(n.operatorCode) +  
					"must be boolean.");
		}
	}

	void visit(intLitNode n){
		n.kind = ASTNode.Kinds.Var;
		n.type = ASTNode.Types.Integer;
	}

	void visit(classNode n){
		//"The class name is external to all other scopes; 
		//it never conflicts with any other declaration."
		SymbolInfo	id;
		id = new SymbolInfo(n.className.idname, ASTNode.Kinds.VisibleLabel, 
				ASTNode.Types.Void);
		try {
			st.insert(id);
			st.openScope();
			//Type check the members of the class
			this.visit(n.members); 
			st.closeScope();
		} catch (DuplicateException e) {
			// Can't occur
		} catch (EmptySTException e) {
			// Can't occur
		}
	}


	void  visit(memberDeclsNode n){
		//Type check field declarations
		this.visit(n.fields);
		//Type check method declarations
		this.visit(n.methods);

		//Check if a main method was declared
		if(mainDeclared == false){
			System.out.println("Error: No main method was declared.");
		}
	}

	void  visit(methodDeclsNode n){
		this.visit(n.thisDecl);
		this.visit(n.moreDecls);
	}

	void visit(nullStmtNode n){}

	void visit(nullReadNode n){}

	void visit(nullPrintNode n){}

	void visit(nullExprNode n){}

	void visit(nullMethodDeclsNode n){}


	void visit(methodDeclNode n){		
		SymbolInfo     id;
		id = (SymbolInfo) st.localLookup(n.name.idname);

		//Check if main has already been declared.
		if(mainDeclared){
			System.out.println(error(n) + 
					"No method can be declared after main.");
			typeErrors++;
			n.name.type = ASTNode.Types.Error;
		}

		//If the current method is main, check it is void with zero arguments
		if(n.name.idname.equals("main")){
			mainDeclared = true;
			if(!n.args.isNull()){
				System.out.println(error(n)+"main must take zero arguments.");
				typeErrors++;
				n.name.type = ASTNode.Types.Error;
			}

			if(n.returnType.type != ASTNode.Types.Void){
				System.out.println(error(n)+"main must have return type void.");
				typeErrors++;
				n.name.type = ASTNode.Types.Error;
			}
		}

		// Check that identNode.idname is not already in the symbol table.
		if (id != null) {
			//If already in symbol table, check if overloading is possible

			//Check if name is used by a non-method
			if(id.kind != ASTNode.Kinds.Method){
				System.out.println(error(n) + n.name.idname + 
						" is already declared.");
				typeErrors++;
				n.name.type = ASTNode.Types.Error;
			}

			// Create new scope in symbol table.
			st.openScope();

			//Type check args subtree. 
			// A list of symbol table nodes is created while doing this
			this.visit(n.args);

			//If it is in the table, check for unique parameters
			if((id.kind == ASTNode.Kinds.Method) && 
					id.containsParms(st.getParms()))
			{
				System.out.println(error(n) + n.name.idname + " is already "
						+ "declared. Invalid overloading.");
				typeErrors++;
				n.name.type = ASTNode.Types.Error;
			} else if (id.kind == ASTNode.Kinds.Method) {
				//Overloading valid, add new parameters
				id.addMethodParms(st.getParms());
			}

			//Check for correct return type
			if(id.type != n.returnType.type){
				System.out.println(error(n) + n.name.idname 
						+ " must be of type " + id.type);
				n.name.type = ASTNode.Types.Error;
				typeErrors++;
			}
			currReturnType = n.returnType.type;
			//Type check the decls subtree
			this.visit(n.decls);
			//Type check the stmts
			this.visit(n.stmts);

			try {
				st.closeScope();
			} catch (EmptySTException e) {
				// Nothing to do
			}

			n.name.idinfo=id;
		} else {
			// A method declaration requires a new symbol table entry.
			//Create new entry m, with type = typeNode.type, and kind = Method
			id = new SymbolInfo(n.name.idname, ASTNode.Kinds.Method, 
					n.returnType.type);

			//Add method to symbol table
			try {
				st.insert(id);
			} catch (DuplicateException d) 
			{ /* can't happen */ }
			catch (EmptySTException e) 
			{ /* can't happen */ }

			//Create new scope in symbol table.
			st.openScope();

			//Type check args subtree
			// A list of parameters is created from arg nodes while doing this
			this.visit(n.args);

			// Add the parameters to the methods symbol info
			id.addMethodParms(st.getParms());
			currReturnType = n.returnType.type;
			//Type check the decls subtree
			this.visit(n.decls);
			//Type check the stmts
			this.visit(n.stmts);

			//Close the method's scope at the top of the symbol table 
			try { st.closeScope();
			} catch (EmptySTException e)
			{ /* can't happen */ }
			n.name.idinfo=id;

			//Add method to symbol table
			//			try {
			//			st.insert(id);
			//			} catch (DuplicateException d) 
			//			{ /* can't happen */ }
			//			catch (EmptySTException e) 
			//			{ /* can't happen */ }

		}
	}

	// only vars(including params) of type int or char may be ++/--
	void visit(incrementNode n){
		this.visit(n.target);
		try{
			assertCondition((n.target.kind == ASTNode.Kinds.Var ||
					n.target.kind == ASTNode.Kinds.ScalarParm ||
					n.target.kind == ASTNode.Kinds.ArrayParm) &&
					(n.target.type == ASTNode.Types.Character ||
					n.target.type == ASTNode.Types.Integer));
		} catch (RuntimeException e ){
			typeErrors++;
			System.out.println(error(n) + "Target of ++ can't be changed.");
		}
	}

	void visit(decrementNode n){
		this.visit(n.target);
		try{
			assertCondition((n.target.kind == ASTNode.Kinds.Var ||
					n.target.kind == ASTNode.Kinds.ScalarParm ||
					n.target.kind == ASTNode.Kinds.ArrayParm) &&
					(n.target.type == ASTNode.Types.Character ||
					n.target.type == ASTNode.Types.Integer));
		} catch (RuntimeException e) {
			typeErrors++;
			System.out.println(error(n) + "Target of -- can't be changed.");
		}
	}

	void visit(argDeclsNode n){
		this.visit(n.thisDecl);
		this.visit(n.moreDecls);
	}

	void visit(nullArgDeclsNode n){}

	void visit(valArgDeclNode n){
		SymbolInfo	id;

		// Check that identNode.idname is not already in the symbol table.
		id = (SymbolInfo) st.localLookup(n.argName.idname);
		if (id != null) {
			System.out.println(error(n) + id.name()+ " is already declared.");
			typeErrors++;
			n.argName.type = ASTNode.Types.Error;
			//Inserts the erroneous node in the param list
			st.addParm(new parmInfo(ASTNode.Kinds.ScalarParm,n.argType.type));
		} else {
			//Enter new id into the symbol table
			id = new SymbolInfo(n.argName.idname,
					ASTNode.Kinds.ScalarParm, n.argType.type);
			n.argName.type = n.argType.type;
			try {
				st.insert(id);
			} catch (DuplicateException d) 
			{ /* can't happen */ }
			catch (EmptySTException e) 
			{ /* can't happen */ }
			n.argName.idinfo=id;

			// Insert parameter into the list of parameters
			st.addParm(new parmInfo(ASTNode.Kinds.ScalarParm,n.argType.type));
		}
	}

	void visit(arrayArgDeclNode n){
		SymbolInfo	id;

		// Check that identNode.idname is not already in the symbol table.
		id = (SymbolInfo) st.localLookup(n.argName.idname);
		if (id != null) {
			System.out.println(error(n) + id.name()+ " is already declared.");
			typeErrors++;
			n.argName.type = ASTNode.Types.Error;

			// Insert erroneous parameter node into parm list
			st.addParm(new parmInfo(ASTNode.Kinds.ArrayParm, 
					n.elementType.type));
		} else {		
			// Enter parameter into symbol table
			n.argName.type = n.elementType.type;
			id = new SymbolInfo(n.argName.idname,
					ASTNode.Kinds.ArrayParm, n.argName.type);
			try {
				st.insert(id);
			} catch (DuplicateException d) 
			{ /* can't happen */ }
			catch (EmptySTException e) 
			{ /* can't happen */ }
			n.argName.idinfo=id;

			//Add to scope's list of parameters
			st.addParm(new parmInfo(ASTNode.Kinds.ArrayParm, 
					n.elementType.type));
		}
	}


	void visit(constDeclNode n){
		SymbolInfo	id;

		// Check that identNode.idname is not already in the symbol table.
		id = (SymbolInfo) st.localLookup(n.constName.idname);
		if (id != null) {
			System.out.println(error(n) + id.name()+ " is already declared.");
			typeErrors++;
			n.constName.type = ASTNode.Types.Error;
		} else {

			// Type check the const value expr.
			visit(n.constValue);	

			// 3. Check that the const value's kind is scalar (Variable, Value or ScalarParm)
			try{
				assertCondition(isScalar(n.constValue.kind));
			} catch (RuntimeException r){
				typeErrors++;
				System.out.println(error(n)+"Only scalars can be made constants");
			}
			//Enter identNode.idname into symbol table with 
			//type = constValue.type and kind = Value.
			id = new SymbolInfo(n.constName.idname,
					ASTNode.Kinds.Value, n.constValue.type);

			//The type of a constant is the type of the expression 
			//that defines the constant's value.
			n.constName.type = n.constValue.type;
			try {
				st.insert(id);
			} catch (DuplicateException d) 
			{ /* can't happen */ }
			catch (EmptySTException e) 
			{ /* can't happen */ }
			n.constName.idinfo=id;
		}	
	}

	void visit(arrayDeclNode n){
		SymbolInfo	id;

		// Check that identNode.idname is not already in the symbol table.
		id = (SymbolInfo) st.localLookup(n.arrayName.idname);
		if (id != null) {
			System.out.println(error(n) + id.name()+ " is already declared.");
			typeErrors++;
			n.arrayName.type = ASTNode.Types.Error;
		} else {

			id = new SymbolInfo(n.arrayName.idname,
					ASTNode.Kinds.Array, n.elementType.type);

			//The size of an array (in a declaration) must be greater than zero.
			if(n.arraySize.intval < 1){
				System.out.println(error(n) + n.arrayName.idname 
						+ " must have more than 0 elements.");
				typeErrors++;
				n.arrayName.type = ASTNode.Types.Error;
				id.arraySize = 1;
			} else {
				id.arraySize = n.arraySize.intval;
				n.arrayName.type = n.elementType.type;
			}

			try {
				st.insert(id);
			} catch (DuplicateException d) 
			{ /* can't happen */ }
			catch (EmptySTException e) 
			{ /* can't happen */ }
			n.arrayName.idinfo=id;
		}
		return;
	}

	void visit(charTypeNode n){
		//No type checking needed
	}

	void visit(voidTypeNode n){
		//No type checking needed
	}

	// 1) check condition (expr tree)
	// 2) check condition's type is boolean and kind is scalar
	// 3) if label is null, then type check stmtNode (loop body); return
	// 4) if there is a label (identNode):
	//		a) check lable is not already present in sym table
	//    b) if isn't enter label in sym table with kind = VisibleLabel
	//       and type = void
	//    c) type check stmtNode
	//    d) change label's kind in sym table to HiddenLabel
	void visit(whileNode n){
		this.visit(n.condition); // step 1
		try{
			assertCondition(n.condition.type == ASTNode.Types.Boolean && //step 2
					isScalar(n.condition.kind));
		} catch (RuntimeException e) {
			typeErrors++;
			System.out.println(error(n) + "Condition must be a scalar boolean.");
		}
		if(n.label.isNull()){ //step 3
			this.visit(n.loopBody);
			return;
		} else { //step 4
			identNode temp = (identNode)n.label;
			SymbolInfo id = (SymbolInfo) st.localLookup(temp.idname); // 4a
			if (id != null) {
				System.out.println(error(n)+id.name()+" label is already declared.");
				typeErrors++;
				temp.type = ASTNode.Types.Error;
			}else { // 4b
				id = new SymbolInfo(temp.idname, ASTNode.Kinds.VisibleLabel,
						ASTNode.Types.Void);
				try {
					st.insert(id);
				} catch (DuplicateException d) 
				{ System.out.println("here"); }
				catch (EmptySTException e) 
				{ System.out.println("here1"); }

			}
			this.visit(n.loopBody); // 4c
			id.kind = ASTNode.Kinds.HiddenLabel; //4d
		}
	}

	// 1) check that identNode is declared in sym table
	// 2) check that identNode's kind is VisibleLable (error if hidden)
	void visit(breakNode n){
		SymbolInfo id;
		id = (SymbolInfo) st.globalLookup(n.label.idname);
		if (id == null) {
			System.out.println(error(n) + n.label.idname + " isn't a valid label.");
			typeErrors++;
			return;
		}
		try{
			assertCondition(id.kind == ASTNode.Kinds.VisibleLabel);
		} catch (RuntimeException e) {
			typeErrors++;
			System.out.println(error(n) + "Label "+n.label.idname+" out of scope.");
		}
	}

	void visit(continueNode n){
		SymbolInfo id;
		id = (SymbolInfo) st.globalLookup(n.label.idname);
		if (id == null) {
			System.out.println(error(n) + n.label.idname + " isn't a valid label.");
			typeErrors++;
			return;
		}
		try{
			assertCondition(id.kind == ASTNode.Kinds.VisibleLabel);
		} catch (RuntimeException e) {
			typeErrors++;
			System.out.println(error(n) + "Label "+n.label.idname+" out of scope.");
		}
	}

	// 1) check that identNode.idname is declared in sym table with type
	//    void and kind = Method
	// 2) type check args subtree
	// 3) build a list of the expr nodes found in args subtree
	// 4) get list of param symbols declared for method (stored in method's
	//    symbol table entry)
	// 5) check arg list and param symbols list both have same length
	// 6) compare each arg node w corresponding param symbol:
	//    a) both same type
	//    b) variable, value, or scalarparam kind in an arg matches a 
	//       scalarparam parm; an array or arrayparam kind in an argument
	//       node matches an arrayparm param
	void visit(callNode n){

		SymbolInfo id;
		id = (SymbolInfo) st.globalLookup(n.methodName.idname); // step 1
		if (id == null) {
			System.out.println(error(n) + n.methodName.idname+"()"+ " isn't declared.");
			typeErrors++;
			return;
		}
		try{
			assertCondition(id.type == ASTNode.Types.Void && 
					id.kind == ASTNode.Kinds.Method);
		} catch (RuntimeException e){
			typeErrors++;
			System.out.println(error(n) + n.methodName.idname 
					+" requires a return value.");
			return;
		}
		this.visit(n.args); // step 2
		ArrayList<parmInfo> args = buildArgList(n.args); // step 3
		try{
			assertCondition(id.containsParms(args)); // step 4,5,6
		} catch (RuntimeException e){
			typeErrors++;
			n.methodName.type = ASTNode.Types.Error;
			if(id.parameters.size() == 0){
				System.out.println(error(n)+n.methodName.idname
						+" requires 0 parameters");
			} else if (id.parameters.size() == 1){
				if(id.parameters.get(0).size() != args.size()){
					System.out.println(error(n)+n.methodName.idname
							+" requires "+id.parameters.get(0).size()
							+" parameters");
				}
				else
				for(int i = 0; i < id.parameters.get(0).size(); i++){
					if(i == args.size())
						break;
					if(!args.get(i).isParmEqual(id.parameters.get(0).get(i)))
					{	System.out.println(error(n)+"In the call to "
									+n.methodName.idname+" parameter "
									+(i+1)+" has incorrect type.");
					}
				}
			} else{
				System.out.println(error(n)+"None of the "+id.parameters.size()
						+" definitions of "+n.methodName.idname
						+" match the parameters in this call.");
			}
		}
	}

	//only int and char values may be read
	void visit(readNode n){
		if(n.targetVar.varName.linenum != -1){ //work around from prev proj
			this.visit(n.targetVar);
			try{
				assertCondition(//n.targetVar.kind == ASTNode.Kinds.Var &&
						(n.targetVar.type == ASTNode.Types.Integer ||
						n.targetVar.type == ASTNode.Types.Character));
			} catch (RuntimeException e){
				typeErrors++;
				System.out.println(error(n) + "Only int and char values may be read.");
				return;
			}
		}//end if
		this.visit(n.moreReads);
	}

	// 1) if returnVal is null, check that currentMethod.returnType = void
	// 2) if returnVal is not null, check that returnVal's kind is scalar
	//    and its type is currentMethod.returnType
	void visit(returnNode n){
		if(n.returnVal.isNull()){ // step 1
			try{
				assertCondition(currReturnType == ASTNode.Types.Void);
			} catch (RuntimeException e){
				System.out.println(error(n) + "Missing return value of type "
						+currReturnType+".");
			}
		} else { //step 2
			this.visit(n.returnVal);
			exprNode temp = (exprNode)n.returnVal;
			try{
				assertCondition(isScalar(temp.kind) &&
						(temp.type == currReturnType));
			} catch (RuntimeException e){
				System.out.println(error(n) + "Return type mismatch; found "+
						temp.type+" but expected "+currReturnType+".");
			}
		}
	}

	void visit(argsNode n){
		this.visit(n.argVal);
		this.visit(n.moreArgs);
	}
	void visit(nullArgsNode n){}

	//can only type cast expressions of type int, char, or bool
	//to type int, char, or bool.
	void visit(castNode n){
		this.visit(n.operand);
		try{
			assertCondition(!(n.resultType instanceof voidTypeNode) &&
					(n.operand.type == ASTNode.Types.Integer ||
					n.operand.type == ASTNode.Types.Character ||
					n.operand.type == ASTNode.Types.Boolean));
		} catch (RuntimeException e){
			typeErrors++;
			System.out.println(error(n) + "Can only cast ints, chars, and bools"+
					" to int, char, or bool.");
		}
		if(n.resultType instanceof boolTypeNode){
			n.type = ASTNode.Types.Boolean;
		} else if (n.resultType instanceof charTypeNode){
			n.type = ASTNode.Types.Character;
		}else if (n.resultType instanceof intTypeNode){
			n.type = ASTNode.Types.Integer;
		}
		n.kind = n.operand.kind;
	}

	//similar to callNode:
	//only identifiers denoting functions (methods w non-void result type)
	//can be called in expressions; result type = type of function
	// 1) look up method; error if not found
	// 2) type check arg tree
	// 3) build list of type/kind tuples for args
	// 4) check if args compatable w function's parms
	void visit(fctCallNode n){
		SymbolInfo id;
		id = (SymbolInfo) st.globalLookup(n.methodName.idname); // step 1
		if (id == null) {
			System.out.println(error(n) + n.methodName.idname+"() is not "
					+ "declared.");
			typeErrors++;
			n.methodName.type = ASTNode.Types.Error;
		} else if (id.kind != ASTNode.Kinds.Method) {
			System.out.println(error(n) + n.methodName.idname+" is not a method.");
			typeErrors++;
			n.methodName.type = ASTNode.Types.Error;			
		} else {
			//Assign the fctCallNode the type and kind of the method
			n.type = id.type;
			n.kind = ASTNode.Kinds.ScalarParm;
			try{
				assertCondition(!(id.type == ASTNode.Types.Void));
			} catch (RuntimeException e){
				System.out.println(error(n) + n.methodName.idname +" is called as a procedure and must therefore return void.");
				typeErrors++;
			}
			this.visit(n.methodArgs); // step 2
			ArrayList<parmInfo> args = buildArgList(n.methodArgs); // step 3
			try{
				assertCondition(id.containsParms(args)); // step 4
			} catch (RuntimeException e){
				typeErrors++;
				if(id.parameters.size() == 0){
					System.out.println(error(n)+n.methodName.idname
												+" requires 0 parameters");
				} else if (id.parameters.size() == 1){
					if(id.parameters.get(0).size() != args.size()){
						System.out.println(error(n)+n.methodName.idname
						+" requires "+id.parameters.get(0).size()+" parameters");
					}
					else
					for(int i = 0; i < id.parameters.get(0).size(); i++){
						if(i == args.size())
							break;
						if(!args.get(i).isParmEqual(id.parameters.get(0).get(i)))
							System.out.println(error(n)+"In the call to "
								+n.methodName.idname
								+" parameter "+(i+1)+" has incorrect type.");
					}
				} else{
					System.out.println(error(n)+"None of the "+id.parameters.size()
							+" definitions of "+n.methodName.idname
							+" match the parameters in this call.");
				}
			}
		}
	}

	void visit(unaryOpNode n){
		this.visit(n.operand);
		try{
			assertCondition(n.operand.type == ASTNode.Types.Boolean);
		} catch (RuntimeException e){
			System.out.println(error(n)+"Operand of ! must be Boolean.");
		}
		n.type = ASTNode.Types.Boolean;
	}

	void visit(charLitNode n){
		n.kind = ASTNode.Kinds.Var;
		n.type = ASTNode.Types.Character;
	}

	void visit(strLitNode n){
		n.kind = ASTNode.Kinds.String;
	}

	void visit(trueNode n){
		// Type and kind assigned in abstract syntax tree
	}

	void visit(falseNode n){
		// Type and kind assigned in abstract syntax tree
	}

}
