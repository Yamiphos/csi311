import AST.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.lang.Float.parseFloat;

public class Parser {

    private TranNode top;
    private TokenManager tokenManager;


    public Parser(TranNode top, List<Token> tokens) {
        this.top = top;
        this.tokenManager = new TokenManager(tokens);

    }

    // Tran = { Class | Interface }
    public TranNode Tran() throws SyntaxErrorException {

        while(!tokenManager.done()) {
            Optional<Token> option = tokenManager.peek(0);
            if (option.isPresent() && option.get().getType() == Token.TokenTypes.INTERFACE) {
                top.Interfaces.add(ParseInterface());
                RequireNewLine();

            } else if (option.isPresent() && option.get().getType() == Token.TokenTypes.CLASS) {
                top.Classes.add(ParseClasses());
                RequireNewLine();

            }
            else if(top.Classes.size()>1){
                throw new SyntaxErrorException("Only one class can exist", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            else {
                throw new SyntaxErrorException("not a class or interface", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            if(!tokenManager.done()){
                tokenManager.matchAndRemove(Token.TokenTypes.DEDENT); //bandaid fix for private test
            }
        }
        return top;
    }

    boolean RequireNewLine(){

        if(tokenManager.done()) {
            return true;
        }
        Optional option = tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);

        if(option.isPresent()) {

            while (option.isPresent()) {
                option = tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            }
            return true;
        }
        else {
            return false;
        }
    }

    InterfaceNode ParseInterface ()throws SyntaxErrorException{
        InterfaceNode Interface = new InterfaceNode();
        tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE);

        Optional<Token> Current=tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        if(Current.isPresent()){
            Interface.name = Current.get().getValue();
        }

        if(Current.isPresent()){
            if(RequireNewLine()){
                Current= tokenManager.matchAndRemove(Token.TokenTypes.INDENT);

                if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){

                    Interface.methods.add(Methodheaders());
                    RequireNewLine();

                    if(!tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
                        throw new SyntaxErrorException("Not valid tran", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                    }
                }
            }

        }
        return Interface;

    }

    MethodHeaderNode Methodheaders(){ // 2

        MethodHeaderNode Methodheader = new MethodHeaderNode();

        List<MethodHeaderNode> Methods =  new LinkedList<>();

        Optional<Token> Current=tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        if(Current.isPresent()){
            Methodheader.name =Current.get().getValue();
        }

        if(Current.isPresent()){
            Current= tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);

            if(Current.isPresent()){
                Current = tokenManager.matchAndRemove((Token.TokenTypes.RPAREN));

                if(Current.isPresent()) {
                    //Optional<Token> comma = tokenManager.peek(0);
                    //issue is probably here dont need a loop maybe
                    //comma = tokenManager.peek(0);
                    if(tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()){
                        Methodheader.returns = VariableDeclarations();
                    }
                }
                else{
                    Methodheader.parameters = VariableDeclarations();
                    Current = tokenManager.matchAndRemove((Token.TokenTypes.RPAREN));

                    if(Current.isPresent()){
                        tokenManager.matchAndRemove(Token.TokenTypes.COLON);
                        Methodheader.returns= VariableDeclarations();
                    }

                }

            }

        }

        return Methodheader;

    }

    List<VariableDeclarationNode> VariableDeclarations(){
        //end with no comma

        List<VariableDeclarationNode> Variablelist = new LinkedList<>();
        Variablelist.add(VariableDeclaration());

        Optional<Token> Current = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);

        while(Current.isPresent()){
            Variablelist.add(VariableDeclaration());
            Current = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
        }
        return Variablelist;

    }

    VariableDeclarationNode VariableDeclaration(){

        VariableDeclarationNode Variables = new VariableDeclarationNode();

        Optional<Token> Current=tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        if(Current.isPresent()){
            Variables.type = Current.get().getValue();
        }


        if(Current.isPresent()){
            Current=tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            Variables.name = Current.get().getValue();
        }
        return Variables;
    }

    ClassNode ParseClasses()throws SyntaxErrorException{
        ClassNode Class = new ClassNode();

        tokenManager.matchAndRemove(Token.TokenTypes.CLASS);

//        if(tokenManager.matchAndRemove(Token.TokenTypes.WORD).isEmpty()){
//            //throw error if no name is given for class
//        }

        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD){
            //throw error for no class name
            Class.name = tokenManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
        }
        RequireNewLine();
        //check for implements/interfaces
        // might want to peek instead of removing
        if(tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent())
        {
            do{
                Optional<Token> option=tokenManager.matchAndRemove(Token.TokenTypes.WORD);

                if(option.isEmpty()){
                    //throw exception expected interface name
                }
                Class.interfaces.add(option.get().getValue());

            }while(tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent());
        }


        if(tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()){
            //throw exception not valid
        }


        if(tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            //throw exception not valid
        }

        //optionally constructors or methods or members or a mix
        //while not a dedent loop constructors methods and members

        while(!tokenManager.done() && tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {

            if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.CONSTRUCT, Token.TokenTypes.LPAREN)) {
                Class.constructors.add(ParseConstructor());
                RequireNewLine();
            }

            //for method declarations
            Optional<Token> PrivateOrShared;
            PrivateOrShared = tokenManager.peek(0);
            //method private or shared check
            if (PrivateOrShared.get().getType() == Token.TokenTypes.PRIVATE || PrivateOrShared.get().getType() == Token.TokenTypes.SHARED) {
                Class.methods.add(ParseMethodDeclarations());
                RequireNewLine();
                PrivateOrShared = tokenManager.peek(0);
                continue;
            }
            //method header check for methods
            if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
                Class.methods.add(ParseMethodDeclarations());
                RequireNewLine();
                continue;
            }
            //members check
            if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
                Class.members.add(ParseMembers());
                RequireNewLine();
                continue;
            }

            //accessor check
            if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.INDENT, Token.TokenTypes.ACCESSOR)) {
                Class.members.add(ParseMembers());
                RequireNewLine();
                continue;
            }

            //mutator check
            if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.INDENT, Token.TokenTypes.MUTATOR)) {
                Class.members.add(ParseMembers());
                RequireNewLine();

            }

//            Optional<Token> test = tokenManager.peek(0);
//            test.get().getType();

        }


        //tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);

//        //for members
//        while(tokenManager.matchAndRemove(Token.TokenTypes.WORD).isPresent()){
//            Class.members.add(ParseMembers());
//            RequireNewLine();
//        }

        return Class;

    }


    ConstructorNode ParseConstructor()throws SyntaxErrorException{
        ConstructorNode Constructor = new ConstructorNode();

        tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT);

        tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
        boolean endofparen = false;

        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.RPAREN){
            tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
            endofparen =true;
        }

        if(endofparen == false){
            Constructor.parameters.addAll(VariableDeclarations());
        }
        RequireNewLine();

//        boolean EndOfParen = false;
//
//        while(tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent() && EndOfParen == false){
//            Constructor.parameters.addAll(VariableDeclarations());
//
//            if(tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()){
//                EndOfParen = true;
//            }
//        }
//        RequireNewLine();


        //method body stuff

        Optional<Token> Methodbodyloop = tokenManager.peek(0);

        //while(Methodbodyloop.get().getType() == Token.TokenTypes.DEDENT)
        //can loop all of this stuff after indent in a while checking to dedent

        tokenManager.matchAndRemove(Token.TokenTypes.INDENT);


        //for variable declarations
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){
            Constructor.locals.addAll(VariableDeclarations());
            RequireNewLine();
        }

        //needs to be changed for later assignments
        Optional<Token> option = tokenManager.peek(0);
        while(option.get().getType() == Token.TokenTypes.IF || option.get().getType() ==Token.TokenTypes.LOOP
                ||option.get().getType() ==Token.TokenTypes.WORD){
            Constructor.statements.add(ParseStatement());
            RequireNewLine();
            option = tokenManager.peek(0);
        }
        tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);

        return Constructor;
    }

    MemberNode ParseMembers()throws SyntaxErrorException{
        MemberNode members =  new MemberNode();
        members.declaration = VariableDeclaration();

        //for accessors and mutators only
        if(tokenManager.peek(0).get().getType()==Token.TokenTypes.INDENT){
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
        }

        if(tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent()){
            tokenManager.matchAndRemove(Token.TokenTypes.COLON);
            members.accessor = Optional.of(ParseStatements());
        }
        if (tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent()){
            tokenManager.matchAndRemove(Token.TokenTypes.COLON);
            members.mutator = Optional.of(ParseStatements());
        }

        return members;
    }

    MethodDeclarationNode ParseMethodDeclarations()throws SyntaxErrorException{
        MethodDeclarationNode MethodDec = new MethodDeclarationNode();

        if(tokenManager.peek(0).get().getType()== Token.TokenTypes.PRIVATE){
            tokenManager.matchAndRemove(Token.TokenTypes.PRIVATE);
            MethodDec.isPrivate = true;
        }

        if(tokenManager.peek(0).get().getType()== Token.TokenTypes.SHARED ){
            tokenManager.matchAndRemove(Token.TokenTypes.SHARED);
            MethodDec.isShared = true;
        }

//        if(tokenManager.peek(0).get().getType()== Token.TokenTypes.WORD){
//            MethodDec.name = tokenManager.matchAndRemove(Token.TokenTypes.WORD).toString();
//        }
        MethodHeaderNode Methodheaders =  Methodheaders();
        MethodDec.name = Methodheaders.name;
        MethodDec.parameters = Methodheaders.parameters;
        MethodDec.returns = Methodheaders.returns;

        RequireNewLine();

        //method body
        tokenManager.matchAndRemove(Token.TokenTypes.INDENT);

        //for variable declarations
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){

            //here
            while(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){
                //changed
                MethodDec.locals.add(VariableDeclaration());
                RequireNewLine();

            }

        }

        Optional<Token> option = tokenManager.peek(0);

        while(true){
            if(option.get().getType() == Token.TokenTypes.IF || option.get().getType() ==Token.TokenTypes.LOOP || option.get().getType() == Token.TokenTypes.WORD){
                MethodDec.statements.add(ParseStatement());
                RequireNewLine();
                option = tokenManager.peek(0);
            }
            else{
                break;
            }
        }


        tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);

        return MethodDec;
    }

    List<StatementNode> ParseStatements()throws SyntaxErrorException{
        List<StatementNode> Statements = new LinkedList<>();
        tokenManager.matchAndRemove(Token.TokenTypes.INDENT);

        while(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            Statements.add(ParseStatement());
            RequireNewLine();
        }
        return Statements;
    }

    StatementNode ParseStatement()throws SyntaxErrorException{


        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.IF){
            return ParseIF();
        }
        //original for later  if(tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isPresent())
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.LOOP){
            return ParseLoop();
        }
        Optional<StatementNode> s = disambiguate();
        if(s.isPresent()){
            return s.get();
        }

        throw new SyntaxErrorException("raji", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    IfNode ParseIF()throws SyntaxErrorException{
        IfNode IF =  new IfNode();
        tokenManager.matchAndRemove(Token.TokenTypes.IF);
        IF.condition = (ParseBooleanExpTerm());
        RequireNewLine();
        IF.statements = ParseStatements();

        if(tokenManager.matchAndRemove(Token.TokenTypes.ELSE).isPresent()){
            RequireNewLine();
            ParseStatements();
        }
        else{
            IF.elseStatement = Optional.empty();
        }

        return IF;

    }

    LoopNode ParseLoop()throws SyntaxErrorException{
        LoopNode Loop = new LoopNode();
        tokenManager.matchAndRemove(Token.TokenTypes.LOOP);

        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)){
            Loop.assignment = VariableReference();
            tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
        }
        tokenManager.matchAndRemove(Token.TokenTypes.LOOP);

        Loop.expression = ParseBooleanExpTerm();

        if(tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
            Loop.statements = ParseStatements();

        }
        return Loop;
    }

    ExpressionNode ParseBooleanExpTerm(){

        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.NOT){
            tokenManager.matchAndRemove((Token.TokenTypes.NOT));
            NotOpNode not = new NotOpNode();
            not.left = ParseBooleanExpTerm();
            return not;
        }
        else{
            ExpressionNode expression = ParseBooleanExpFactor();

            if (tokenManager.peek(0).get().getType() == Token.TokenTypes.AND) {
                tokenManager.matchAndRemove((Token.TokenTypes.AND));
                BooleanOpNode newBool =  new BooleanOpNode();
                newBool.op = BooleanOpNode.BooleanOperations.and;
                newBool.left = expression;
                newBool.right = ParseBooleanExpTerm();
                return newBool;
            }

            if (tokenManager.peek(0).get().getType() == Token.TokenTypes.OR) {
                tokenManager.matchAndRemove((Token.TokenTypes.OR));
                BooleanOpNode newBool =  new BooleanOpNode();
                newBool.op = BooleanOpNode.BooleanOperations.or;
                newBool.left = expression;
                newBool.right = ParseBooleanExpTerm();
                return newBool;
            }
            return expression;
        }

    }

    Optional<VariableReferenceNode> VariableReference(){
        VariableReferenceNode Variables = new VariableReferenceNode();

        Optional<Token> VR = tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        if(VR.isPresent()){
            Variables.name = VR.get().getValue();
            return Optional.of(Variables);
        }
        return Optional.empty();
    }

    ExpressionNode ParseBooleanExpFactor(){
        CompareNode Compare =  new CompareNode();

        //method call check word then period or word then left parenthesis
        // /// currently unnecessary
//        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)){
//            tokenManager.matchAndRemove(Token.TokenTypes.WORD);
//            tokenManager.matchAndRemove(Token.TokenTypes.DOT);
//            ParseMethodCallExpression();
//        }
//        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
//            tokenManager.matchAndRemove(Token.TokenTypes.WORD);
//            tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
//            ParseMethodCallExpression();
//        }

        //check for Expression, word for now
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD){

            Compare.left = ParseExpression();

            if(tokenManager.matchAndRemove(Token.TokenTypes.EQUAL).isPresent()){
                Compare.op = CompareNode.CompareOperations.eq;
            }
            else if(tokenManager.matchAndRemove(Token.TokenTypes.NOTEQUAL).isPresent()){
                Compare.op = CompareNode.CompareOperations.ne;
            }
            else if(tokenManager.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL).isPresent()){
                Compare.op = CompareNode.CompareOperations.le;
            }
            else if(tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL).isPresent()){
                Compare.op = CompareNode.CompareOperations.ge;
            }
            else if(tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHAN).isPresent()){
                Compare.op = CompareNode.CompareOperations.gt;
            }
            else if(tokenManager.matchAndRemove(Token.TokenTypes.LESSTHAN).isPresent()){
                Compare.op = CompareNode.CompareOperations.lt;
            }
            else{
                return Compare.left;
            }
            Compare.right = ParseExpression();
        }

        return Compare;
    }

    ExpressionNode ParseExpression(){
        ExpressionNode expression = term();

        while(tokenManager.peek(0).get().getType() == Token.TokenTypes.PLUS
                ||tokenManager.peek(0).get().getType() == Token.TokenTypes.MINUS){

            if(tokenManager.peek(0).get().getType() == Token.TokenTypes.PLUS){
                tokenManager.matchAndRemove(Token.TokenTypes.PLUS);
                MathOpNode Placeholder =  new MathOpNode();
                Placeholder.left = expression;
                Placeholder.right = term();
                Placeholder.op = MathOpNode.MathOperations.add;
                expression = Placeholder;

            }
            if(tokenManager.peek(0).get().getType() == Token.TokenTypes.MINUS){
                tokenManager.matchAndRemove(Token.TokenTypes.MINUS);
                MathOpNode Placeholder =  new MathOpNode();
                Placeholder.left = expression;
                Placeholder.right = term();
                Placeholder.op = MathOpNode.MathOperations.subtract;
                expression = Placeholder;
            }

        }

        return expression;

    }

    Optional<MethodCallExpressionNode> ParseMethodCallExpression(){
        MethodCallExpressionNode method = new MethodCallExpressionNode();

        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)){
            Optional<String> s = Optional.of(tokenManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue());
            method.objectName = s;
            tokenManager.matchAndRemove(Token.TokenTypes.DOT);
        }

        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
            method.methodName =  tokenManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
        }
        else{
            return Optional.empty();
        }

        List<ExpressionNode> parameterlist =  new LinkedList<>();
        if(tokenManager.peek(0).get().getType() != Token.TokenTypes.RPAREN){
            parameterlist.add(ParseExpression());
            while(tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
                parameterlist.add(ParseExpression());
            }
            tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
        }
        else{
            tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);

        }
//        while(tokenManager.peek(0).get().getType() != Token.TokenTypes.RPAREN
//        && tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
//            parameterlist.add(ParseExpression());
//        }
        method.parameters.addAll(parameterlist);

        return Optional.of(method);
    }


    Optional<StatementNode> disambiguate(){
        Optional<MethodCallExpressionNode> methodcall =  ParseMethodCallExpression();

        if(methodcall.isPresent()){
            MethodCallStatementNode ad =  new MethodCallStatementNode(methodcall.get());
            return Optional.of(ad);
        }

        Optional<VariableReferenceNode> variableReference = VariableReference();
        Optional<Token> check =tokenManager.peek(0);

        if(variableReference.isPresent()){

            if(check.get().getType() == Token.TokenTypes.COMMA){
                List<VariableReferenceNode> vrlist =  new LinkedList<>();
                vrlist.add((variableReference).get());
                while(check.get().getType() == Token.TokenTypes.COMMA){
                    tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                    vrlist.add(VariableReference().get());
                    check =tokenManager.peek(0);
                }

                if(check.get().getType() == Token.TokenTypes.ASSIGN){
                    tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
                    MethodCallStatementNode ad =  new MethodCallStatementNode(ParseMethodCallExpression().get());
                    ad.returnValues.addAll(vrlist);
                    return Optional.of(ad);
                }

            }
            else{
                tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
                AssignmentNode disCA = new AssignmentNode();

                disCA.target = variableReference.get();
                disCA.expression = ParseExpression();

                return Optional.of(disCA);
            }

        }
        return Optional.empty();
    }

    ExpressionNode term(){
        ExpressionNode expressterm = factor();

        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.TIMES){
            tokenManager.matchAndRemove(Token.TokenTypes.TIMES);
            MathOpNode Placeholder =  new MathOpNode();
            Placeholder.left = expressterm;
            Placeholder.right = term();
            Placeholder.op = MathOpNode.MathOperations.multiply;
            expressterm = Placeholder;
        }
        else if(tokenManager.peek(0).get().getType() == Token.TokenTypes.DIVIDE){
            tokenManager.matchAndRemove(Token.TokenTypes.DIVIDE);
            MathOpNode Placeholder =  new MathOpNode();
            Placeholder.left = expressterm;
            Placeholder.right = term();
            Placeholder.op = MathOpNode.MathOperations.divide;
            expressterm = Placeholder;

        }
        else if(tokenManager.peek(0).get().getType() == Token.TokenTypes.MODULO){
            tokenManager.matchAndRemove(Token.TokenTypes.MODULO);
            MathOpNode Placeholder =  new MathOpNode();
            Placeholder.left = expressterm;
            Placeholder.right = term();
            Placeholder.op = MathOpNode.MathOperations.modulo;
            expressterm = Placeholder;
        }
        return expressterm;
    }

    ExpressionNode factor(){


        if(tokenManager.peek(0).get().getType()== Token.TokenTypes.NUMBER){
            NumericLiteralNode factor = new NumericLiteralNode();
            factor.value = parseFloat(tokenManager.matchAndRemove(Token.TokenTypes.NUMBER).get().getValue());
            return factor;
        }
        else if(tokenManager.peek(0).get().getType()== Token.TokenTypes.TRUE){
            BooleanLiteralNode factor =  new BooleanLiteralNode(true);
            tokenManager.matchAndRemove(Token.TokenTypes.TRUE);
            return factor;
        }
        else if(tokenManager.peek(0).get().getType()== Token.TokenTypes.FALSE){
            BooleanLiteralNode factor =  new BooleanLiteralNode(false);
            tokenManager.matchAndRemove(Token.TokenTypes.FALSE);
            return factor;
        }
        else if(tokenManager.peek(0).get().getType()== Token.TokenTypes.QUOTEDSTRING){
            StringLiteralNode factor = new StringLiteralNode();
            factor.value = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING).get().getValue();
            return factor;
        }
        else if(tokenManager.peek(0).get().getType()== Token.TokenTypes.QUOTEDCHARACTER){
            CharLiteralNode factor = new CharLiteralNode();
            factor.value = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER).get().getValue().charAt(0);
            return factor;
        }
        else if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)
                || tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
            MethodCallExpressionNode factor = ParseMethodCallExpression().get();
            return factor;
        }
        else if(tokenManager.peek(0).get().getType()== Token.TokenTypes.WORD){
            VariableReferenceNode factor = new VariableReferenceNode();
            factor.name = tokenManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            return factor;
        }
        else if(tokenManager.peek(0).get().getType()== Token.TokenTypes.LPAREN){
            tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
            ExpressionNode factor = ParseExpression();
            tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
            return factor;
            //remove Rparen and Lparen in parse expression
        }
        //(tokenManager.peek(0).get().getType()== Token.TokenTypes.NEW)
        else{
            List<ExpressionNode> factorlist = new LinkedList<>();
            NewNode factor = new NewNode();

            tokenManager.matchAndRemove(Token.TokenTypes.NEW);

            factor.className = tokenManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();

            tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);

            if(tokenManager.peek(0).get().getType() != Token.TokenTypes.RPAREN){
                factorlist.add(ParseExpression());
            }

            while(tokenManager.peek(0).get().getType() != Token.TokenTypes.RPAREN
                    &&tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
                factorlist.add(ParseExpression());
            }
            factor.parameters = factorlist;
            tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
            return factor;
        }


    }



//    AssignmentNode ParseAssignment(){
//        AssignmentNode Assign = new AssignmentNode();
//
//        Assign.target =  VariableReference().get();
//
//        Assign.expression = ParseExpression().get();
//
//        return Assign;
//
//    }


}