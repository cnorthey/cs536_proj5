/* CS 536: PROJECT 5 - CSX CODE GENERATOR
 * 
 * Caela Northey (cs login: caela)	905 653 2238 
 * Alan Irish    (cs login: irish)  906 591 2819
 *
 * DUE DATE: FRIDAY DEC 13, 2013
 *
 ***************************************************
 *  class used to hold information associated w/
 *  Symbs (which are stored in SymbolTables)
 *  Update to handle arrays and methods
 * 
 ****************************************************/

import java.util.ArrayList;

class SymbolInfo extends Symb {
	public ASTNode.Kinds kind;
	public ASTNode.Types type;
	public ArrayList<ArrayList<parmInfo>> parameters;	
	int arraySize; //Used by arrays
	private exprNode[] elements;   //used by arrays

	//P5: used for code generating: global, local, literal, stack, none
	public CodeGenerating.AdrMode adr; 
	public String label; 		//for global field names
	public int varIndex; 		//index of local variables
	public int intVal; 			//val of int, char or bool lit
	public String strVal; 	//val of String lit
	public String topLabel; //for while loops
	public String bottomLabel; // for while loops

	public SymbolInfo(String id, ASTNode.Kinds k, ASTNode.Types t){    
		super(id);
		kind = k; type = t;
		parameters = new ArrayList<ArrayList<parmInfo>>();
		arraySize = 0;
		adr = CodeGenerating.AdrMode.none;
		label = null;
		varIndex = 0;
		intVal = 0;
		strVal = null;
		topLabel = null;
		bottomLabel = null;
	};
	
	public void setArraysize(int size){
		arraySize = size;
	}

	public void addMethodParms(ArrayList<parmInfo> parms){
		parameters.add(parms);
	}

	public void addElement(int index, exprNode e){
		elements[index] = e;
	}
	
	public exprNode getElement(int index){
		return elements[index];
	}
	
	//This function compares a given list of parameters to see if they
	//match any of the accepted lists of parameters. This function can be used
	//to detect invalid overloading, and can be used to detect a proper
	//function call.
	public boolean containsParms(ArrayList<parmInfo> parms)
	{
		boolean duplicate = false;
		// For every set of parameters
		for(int i = 0; i < parameters.size(); i++)
		{
			//Check the length of parameters first
			if(parms.size() == parameters.get(i).size())
			{
				if(parms.size() == 0)
					return true; //Handles empty parameters
				
				duplicate = true;
				//For every parameter in the lists of parameters
				for(int j = 0; j < parameters.get(i).size(); j++)
				{
					//Check if parameters match
					duplicate = parms.get(j).isParmEqual(parameters.get(i).get(j));
					//If different parms, then check the next list of parms
					if(duplicate == false)
						break;					
				}
				if(duplicate)
					return true; //A duplicate list has been found
			}
		}
		return false; //No matching parameters have been found
	}


	// public SymbolInfo(String id, int k, int t){
	//	super(id);
	//	kind = new Kinds(k); type = new Types(t);};
	public String toString(){
		return "("+name()+": kind=" + kind+ ", type="+  type+")";};

}
