package Interpreter;

import AST.*;

import java.util.*;

public class Interpreter {
    private TranNode top;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        this.top = top;

        // Create the "console" class
        ClassNode console = new ClassNode();
        console.name = "console";
        console.methods = new ArrayList<>();
        console.constructors = new LinkedList<>();
        console.interfaces = new ArrayList<>();
        console.members = new ArrayList<>();

        // Create the ConsoleWrite method
        ConsoleWrite consoleWrite = new ConsoleWrite();
        consoleWrite.name = "write";
        consoleWrite.locals = new ArrayList<>();
        consoleWrite.returns = new LinkedList<>();
        consoleWrite.parameters = new ArrayList<>();
        consoleWrite.isPrivate = false; // It's shared
        consoleWrite.isShared = true;
        consoleWrite.statements = new ArrayList<>();
        consoleWrite.isVariadic = true;
        consoleWrite.console = new LinkedList<String>();

        // Add the method to the console class
        console.methods.add(consoleWrite);

        // Add the console class to the top node
        top.Classes.add(console);
    }


    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() throws Exception {
        List<InterpreterDataType> c = new LinkedList<>();

        for(int i=0; i<top.Classes.size(); i++){

            for(int x=0; x<top.Classes.get(i).methods.size(); x++){

                if(top.Classes.get(i).methods.get(x).isShared && top.Classes.get(i).methods.get(x).name.equals("start")
                &&!top.Classes.get(i).methods.get(x).isPrivate && top.Classes.get(i).methods.get(x).parameters.isEmpty()){

                    Optional<ObjectIDT> o = Optional.of(new ObjectIDT(top.Classes.get(i)));
                    interpretMethodCall(o,top.Classes.get(i).methods.get(x),c );

                    //System.out.println("---------end of start method");
                }
            }
        }

    }

    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) throws Exception{
        List<InterpreterDataType> result = getParameters(object, locals, mc);

        //no object name, compare method names in object class's methods interpretMethodCall on that
        if(mc.objectName.isEmpty()){

            for(int i= 0; i<object.get().astNode.methods.size();i++){
                if(mc.methodName.equals(object.get().astNode.methods.get(i).name)){
                    return interpretMethodCall(object,object.get().astNode.methods.get(i),result);
                }
            }

        }
        //if it has a name, check the names of the classes in top,
        // if name matches and the method isshared interpret method call
        if(mc.objectName.isPresent()){

            for(int i=0; i<top.Classes.size();i++){

                if(mc.objectName.get().equals(top.Classes.get(i).name) ){

                    for(int x=0; x<top.Classes.get(i).methods.size();x++){
                        if(top.Classes.get(i).methods.get(x).name.equals(mc.methodName) && top.Classes.get(i).methods.get(x).isShared){
                            return interpretMethodCall(object,top.Classes.get(i).methods.get(x),result);
                        }

                    }

                }

            }

            //if it has a name check locals hashmap for matching name
            if(locals.containsKey(mc.objectName.get())){
                ReferenceIDT LC = (ReferenceIDT) locals.get(mc.objectName.get());

                for(int i=0; i<LC.refersTo.get().astNode.methods.size();i++){
                    if(mc.methodName.equals(LC.refersTo.get().astNode.methods.get(i).name)){
                        return interpretMethodCall(LC.refersTo,LC.refersTo.get().astNode.methods.get(i),result);
                    }
                }

            }
            // or check the hashmap of members of the object
            if(object.get().members.containsKey(mc.objectName.get())){
                ReferenceIDT LC = (ReferenceIDT) object.get().members.get(mc.objectName.get());

                for(int x = 0; x <object.get().members.size(); x++){
                    if(LC.refersTo.get().astNode.methods.get(x).name.equals(mc.methodName)){
                        return interpretMethodCall(object,LC.refersTo.get().astNode.methods.get(x),result);

                    }

                }
            }


        }

        return result;
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall (Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) throws Exception{
        var retVal = new LinkedList<InterpreterDataType>();

        //hashmap for locals
        HashMap<String, InterpreterDataType> lhm;
        lhm = new HashMap<>();

        //if m is built in, if name matches class name, console, and it is shared
        if(m instanceof BuiltInMethodDeclarationNode){
            return ((BuiltInMethodDeclarationNode) m).Execute(values);
        }

        //loop through m's locals and call instantiate on them to get type, then put them into a string, IDT, hashmap,
        //pass to InterpretStatementBock
        //do loop for parameters and return values, parameters, name is their name,
        // and their type is values, return values is like locals
        //if values != amount of m throw
        for(int i =0; i<m.locals.size(); i++){
            lhm.put(m.locals.get(i).name,instantiate(m.locals.get(i).type));
        }
//        if(object.isPresent()){
//            for(int i=0; i<object.get().astNode.members.size(); i++){
//                lhm.put(object.get().astNode.members.get(i).declaration.name,instantiate(object.get().astNode.members.get(i).declaration.type));
//            }
//        }
        for(int x=0; x<m.parameters.size(); x++){
            lhm.put(m.parameters.get(x).name,values.get(x));
        }
        for (int c=0; c<m.returns.size();c++){
            lhm.put(m.returns.get(c).name,instantiate(m.returns.get(c).type));
        }
        if(m.parameters.size() != values.size()){
            throw new Exception("m did not match the size of passed in values");
        }
        else{
            interpretStatementBlock(object,m.statements,lhm);
            //go through m's return values and put them in retval
            for(int i=0; i<m.returns.size();i++){
                retVal.add(lhm.get(m.returns.get(i).name));
            }
        }

        return retVal;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, NewNode mc, ObjectIDT newOne) throws Exception{

        List<InterpreterDataType> lidt = getParameters(callerObj,locals,mc);

        if(getClassByName(mc.className).isEmpty()){
            throw new Exception("class not found, findconstructor and run it");
        }
        for(int i=0; i<newOne.astNode.constructors.size();i++){
            if(doesConstructorMatch(newOne.astNode.constructors.get(0),mc,lidt)){

                interpretConstructorCall(newOne,newOne.astNode.constructors.get(i),lidt);
                System.out.println(newOne.members.get("x"));
                System.out.println(newOne.members.get("y"));
            }
        }
    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) throws Exception{

        HashMap<String, InterpreterDataType> lhm;
        lhm = new HashMap<>();

        for(int i=0; i<c.locals.size(); i++){
            lhm.put(c.locals.get(i).name,instantiate(c.locals.get(i).type));
        }

        for(int i=0; i<object.astNode.members.size(); i++){
            object.members.put(object.astNode.members.get(i).declaration.name,instantiate(object.astNode.members.get(i).declaration.type));
            //lhm.put(object.astNode.members.get(i).declaration.name,instantiate(object.astNode.members.get(i).declaration.type));
        }
        if(c.parameters.size() != values.size()){
            throw new Exception("c's parameters did not match the size of values.");
        }
        for(int x=0; x<c.parameters.size(); x++){
            lhm.put(c.parameters.get(x).name,values.get(x));
        }
        interpretStatementBlock(Optional.of(object),c.statements,lhm);
        //update object members with new lhm value
//       System.out.println("x is:"+lhm.get("x"));
//        System.out.println("Y is:"+ lhm.get("x"));
//        object.astNode.members.get(0).declaration.
//        object.members.put("x", lhm.get("x"));
//        object.members.put("y", lhm.get("y"));

    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
     * Blocks, by definition, do ever statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) throws Exception{
        for(int i=0; i< statements.size(); i++){
            //Assignment node
            if(statements.get(i) instanceof AssignmentNode){ //issue seems to be here but also could be because of new node
                InterpreterDataType FV = findVariable(((AssignmentNode) statements.get(i)).target.name,locals,object);
                FV.Assign(evaluate(locals,object, ((AssignmentNode) statements.get(i)).expression));

            }
            //methodcall statement node
            if(statements.get(i) instanceof MethodCallStatementNode){
                List<InterpreterDataType> MC =findMethodForMethodCallAndRunIt(object,locals,(MethodCallStatementNode)statements.get(i));
                for(int x = 0; x <MC.size(); x++){
                    InterpreterDataType FV =findVariable(((MethodCallStatementNode) statements.get(i)).returnValues.get(x).name,locals,object);
                    FV.Assign(MC.get(x));

                }

            }
            //loops
            if(statements.get(i) instanceof LoopNode){
//                //setup, loop over iterator
//                //will fail if not found
//                List<InterpreterDataType> MC =findMethodForMethodCallAndRunIt(object,locals,(MethodCallStatementNode)statements.get(i));
//                getMethodFromObject(object.get(),(MethodCallStatementNode) statements.get(i),MC);


                ExpressionNode express = (ExpressionNode) statements.get(i);
                InterpreterDataType Loop =  evaluate(locals,object,express);

                //boolean loop
                if(Loop instanceof BooleanIDT){
                    while(((BooleanIDT) Loop).Value){
                        interpretStatementBlock(object,statements,locals);
                    }
                }
                // iterative

            }

            //if
            if(statements.get(i) instanceof IfNode){

                ExpressionNode express = (ExpressionNode) statements.get(i);
                InterpreterDataType condition =  evaluate(locals,object,express);

                if(condition instanceof BooleanIDT){
                    //if it true
                    if(((BooleanIDT) condition).Value){
                        interpretStatementBlock(object,statements,locals);
                    }

                    //if not true and else node
                    if(!((BooleanIDT) condition).Value && ((IfNode) statements.get(i)).elseStatement.isPresent()){
                        interpretStatementBlock(object, statements, locals);
                    }

                }


            }
        }
    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call doMethodCall() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) throws Exception{
        //basic data types bool, char, number, string
        if(expression instanceof BooleanLiteralNode){
            return new BooleanIDT(((BooleanLiteralNode) expression).value);
        }
        else if(expression instanceof CharLiteralNode){
            return new CharIDT(((CharLiteralNode) expression).value);
        }
        else if(expression instanceof NumericLiteralNode){
            return new NumberIDT(((NumericLiteralNode) expression).value);
        }
        else if(expression instanceof StringLiteralNode){
            return new StringIDT(((StringLiteralNode) expression).value);
        }
        //boolean OP node
        if(expression instanceof BooleanOpNode){
            //evaluating left and right throw exception if not booleanIDT
            BooleanIDT left = (BooleanIDT) evaluate(locals,object, ((BooleanOpNode) expression).left);
            BooleanIDT right = (BooleanIDT) evaluate(locals,object, ((BooleanOpNode) expression).right);
            // and/or on results
            if(((BooleanOpNode) expression).op == BooleanOpNode.BooleanOperations.and){
                return new BooleanIDT(left.Value && right.Value);
            }
            if (((BooleanOpNode) expression).op == BooleanOpNode.BooleanOperations.or){
                return new BooleanIDT(left.Value || right.Value);
            }
        }
        //compare node
        if(expression instanceof CompareNode){ //handle new node
            //evaluating left and right throw error if not numberIDT
            NumberIDT left = (NumberIDT) evaluate(locals,object,((CompareNode) expression).left);
            NumberIDT right = (NumberIDT) evaluate(locals,object,((CompareNode) expression).right);

            //lt, le, gt, ge, eq, ne
            if(((CompareNode) expression).op== CompareNode.CompareOperations.lt){
                return new BooleanIDT(left.Value < right.Value);
            }
            else if(((CompareNode) expression).op== CompareNode.CompareOperations.le){
                return new BooleanIDT(left.Value <= right.Value);
            }
            else if(((CompareNode) expression).op== CompareNode.CompareOperations.gt){
                return new BooleanIDT(left.Value > right.Value);
            }
            else if(((CompareNode) expression).op== CompareNode.CompareOperations.ge){
                return new BooleanIDT(left.Value >= right.Value);
            }
            else if(((CompareNode) expression).op== CompareNode.CompareOperations.eq){
                return new BooleanIDT(left.Value == right.Value);
            }
            else if(((CompareNode) expression).op== CompareNode.CompareOperations.ne){
                return new BooleanIDT(left.Value != right.Value);
            }

        }
        // math op node
        if(expression instanceof MathOpNode){
            //evaluating left and right
            InterpreterDataType left =  evaluate(locals,object,((MathOpNode) expression).left);
            InterpreterDataType right =  evaluate(locals,object,((MathOpNode) expression).right);

            //number and  number
            if(left instanceof NumberIDT && right instanceof NumberIDT){
                //add, subtract, multiply, divide, modulo
                if(((MathOpNode) expression).op == MathOpNode.MathOperations.add){
                    return new NumberIDT(((NumberIDT) left).Value + ((NumberIDT) right).Value);
                }
                else if(((MathOpNode) expression).op == MathOpNode.MathOperations.subtract){
                    return new NumberIDT(((NumberIDT) left).Value - ((NumberIDT) right).Value);
                }
                else if(((MathOpNode) expression).op == MathOpNode.MathOperations.multiply){
                    return new NumberIDT(((NumberIDT) left).Value * ((NumberIDT) right).Value);
                }
                else if(((MathOpNode) expression).op == MathOpNode.MathOperations.divide){
                    return new NumberIDT(((NumberIDT) left).Value / ((NumberIDT) right).Value);
                }
                else if(((MathOpNode) expression).op == MathOpNode.MathOperations.modulo){
                    return new NumberIDT(((NumberIDT) left).Value % ((NumberIDT) right).Value);
                }
            }

            //string and string
            if(left instanceof StringIDT && right instanceof StringIDT){
                return new StringIDT(((StringIDT) left).Value + ((StringIDT) right).Value);
            }

            //number and string, turn number into string and add it to string
            if(left instanceof NumberIDT && right instanceof StringIDT){
              return new StringIDT(String.valueOf(((NumberIDT) left).Value) + ((StringIDT) right).Value);

            }
            else if(left instanceof StringIDT && right instanceof  NumberIDT){
                return new StringIDT(String.valueOf(((StringIDT) left).Value) + ((NumberIDT) right).Value);
            }
        }

        //method call expression
        if(expression instanceof MethodCallStatementNode){
             return findMethodForMethodCallAndRunIt(object,locals,(MethodCallStatementNode) expression).get(0);

        }
        // variable reference node
        if(expression instanceof VariableReferenceNode){
            return  findVariable(((VariableReferenceNode) expression).name,locals,object);
        }
        if(expression instanceof NewNode){

            for (int x=0; x<top.Classes.size(); x++){
                if(top.Classes.get(x).name.equals(((NewNode) expression).className)){
                     ObjectIDT o = new ObjectIDT(top.Classes.get(x));
                     System.out.println("test");
                     findConstructorAndRunIt(object,locals,(NewNode) expression,o);
                     return o;


//                     ObjectIDT i = (ObjectIDT) evaluate(locals,object,((NewNode) expression).parameters.getFirst());
//                     return i;



//                    interpretConstructorCall(object.get(),top.Classes.get(x).constructors.getFirst(),left);
//                    return ;
//                    left = (InterpreterDataType) top.Classes.get(x).constructors.getFirst();
//                    return left;
                }
            }
        }

        System.out.println("evaluate exception");
        throw new IllegalArgumentException();
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this methoc call?
     * We double check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {

        //variadic
        if(m instanceof BuiltInMethodDeclarationNode && !((BuiltInMethodDeclarationNode) m).isVariadic){
            return true;
        }

        //checking if m and mc are equal in name and size
        if(!Objects.equals(m.name,mc.methodName) && m.parameters.size() == mc.parameters.size()){
            return false;
        }

        //checking if m and also mc is equal to size of parameters
        if(m.parameters.size() != parameters.size()){
            return false;
        }

        //checking for matching
        for(int i=0; i< parameters.size(); i++){
            if(!typeMatchToIDT(m.parameters.get(i).name, parameters.get(i))){
                return false;
            }
        }

        return true;
        //d
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, NewNode mc, List<InterpreterDataType> parameters) {
        //size checking
        if(c.parameters.size() != mc.parameters.size()){
            return false;
        }
        if(c.parameters.size() != parameters.size()){
            return false;
        }

        //checking for name
        for(int i=0; i< parameters.size(); i++){
            if(typeMatchToIDT(c.parameters.get(i).type,parameters.get(i))){
                return false;
            }
        }
        return true;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) throws Exception{
        List<InterpreterDataType> paraList = new ArrayList<>();

        for(int i=0; i<mc.parameters.size();i++){
            paraList.add(evaluate(locals,object,mc.parameters.get(i)));
        }
        return paraList;
        //c
        //d
    }

    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, NewNode mc) throws Exception{
        List<InterpreterDataType> paraList = new ArrayList<>();

        for(int i=0; i<mc.parameters.size();i++){
            paraList.add(evaluate(locals,object,mc.parameters.get(i)));
        }
        return paraList;
        //c
        //d
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {

        //if instance of idt is x-idt type, && type =="x" return true
        //simple data types
        if(idt instanceof StringIDT && Objects.equals(type, "string")){
            return true;
        }
        else if(idt instanceof NumberIDT && Objects.equals(type, "number")){
            return true;
        }
        else if(idt instanceof BooleanIDT && Objects.equals(type, "boolean")){
            return true;
        }
        else if(idt instanceof CharIDT && Objects.equals(type, "char")){
            return true;
        }

        //object stuff
        if(idt instanceof ObjectIDT){
            //object class name matching
            if(Objects.equals(type, ((ObjectIDT) idt).astNode.name)){
                return true;
            }
            //object class interface matching
            for(int i=0; i<((ObjectIDT) idt).astNode.interfaces.size(); i++) {
                if (Objects.equals(type, ((ObjectIDT) idt).astNode.interfaces.get(i))) {
                        return true;
                }
            }
        }

        //reference stuff
        if (idt instanceof ReferenceIDT) {
            // Check for object type and valid reference
            if(((ReferenceIDT) idt).refersTo.isPresent()){
                return Objects.equals(type, ((ReferenceIDT) idt).refersTo.get().astNode.name);
            }
        }
        throw new RuntimeException("Unable to resolve type " + type);
        //c
        //d
    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        //call does match on each method return the method if true else throw exception.
        // mc and parameters are passed to does match but are not changed

        for(int i=0; i<object.astNode.methods.size();i++){
            if(doesMatch(object.astNode.methods.get(i),mc,parameters)){
                return object.astNode.methods.get(i);
            }
        }

        throw new RuntimeException("Unable to resolve method call " + mc);
        //c
        //d
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {

        for(int x=0; x<top.Classes.size(); x++){
            if(Objects.equals(top.Classes.get(x).name,name)){
                return Optional.of(top.Classes.get(x));
            }
        }

        return Optional.empty();
        //c
        //d
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {
        if(locals.containsKey(name)){
            return locals.get(name);
        }
        else if(object.isPresent() && object.get().members.containsKey(name)){
            return object.get().members.get(name);
        }
        else{
            throw new RuntimeException("Unable to find variable " + name);
        }
        //c
        //d
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {

        switch(type){
            case "string":
                StringIDT s =new StringIDT("");
                return s;
            case "number":
                NumberIDT n =new NumberIDT(0);
                return n;
            case "boolean":
                BooleanIDT b =new BooleanIDT(false);
                return b;
            case "character":
                CharIDT c = new CharIDT(' ');
                return c;
            default:
                return new ReferenceIDT();
        }

    }
    //d
}


