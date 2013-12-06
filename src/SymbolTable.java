/* CS 536: PROJECT 4 - CSX TYPE CHECKER
 * 
 * Caela Northey (cs login: caela)	905 653 2238 
 * Alan Irish    (cs login: irish) 906 591 2819
 *
 * DUE DATE: FRIDAY NOV 22, 2013
 */

//
//You may use this symbol table implementation or your own (from project 1)
//
import java.util.*;
import java.io.*;
class SymbolTable {
   class Scope {
      Hashtable<String,Symb> currentScope;
  	  ArrayList<parmInfo> parameters;
      Scope next;
      Scope() {
         currentScope = new Hashtable<String,Symb>();
         parameters = new ArrayList<parmInfo>();
         next = null;
      }
      Scope(Scope scopes) {
         currentScope = new Hashtable<String,Symb>();
         parameters = new ArrayList<parmInfo>();
         next = scopes;
      }
   }

   private Scope top;

   SymbolTable() {top = new Scope();}

   public void openScope() {
      top = new Scope(top); }

   public ArrayList<parmInfo> getParms(){
	   return top.parameters;
   }
   public void addParm(parmInfo p){
	   top.parameters.add(p);
   }
   public void closeScope() throws EmptySTException {
      if (top == null)
         throw new EmptySTException();
      else top = top.next;
   }

   public void insert(Symb s)
         throws DuplicateException, EmptySTException {
      String key = (s.name().toLowerCase());
      if (top == null)
         throw new EmptySTException();
      if (localLookup(key) != null)
         throw new DuplicateException();
      else top.currentScope.put(key,s);
   }

   
   public Symb localLookup(String s) {
      String key = s.toLowerCase();
      if (top == null)
         return null;
      Symb ans =top.currentScope.get(key);
      return ans;
   }

   public Symb globalLookup(String s) {
      String key = s.toLowerCase();
      Scope top = this.top;
      while (top != null) {
         Symb ans = top.currentScope.get(key);
         if (ans != null)
            return ans;
         else top = top.next;
      }
      return null;
   }

   public String toString() {
      String ans = "";
      Scope top = this.top;
      while (top != null) {
         ans = ans +  top.currentScope.toString()+"\n";
         top = top.next;
      }
      return ans;
   }

   void dump(PrintStream ps) {
     ps.print(toString());
   }
}
