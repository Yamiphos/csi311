import AST.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserTests {
    private TranNode LexAndParse(String input, int tokenCount) throws Exception {
        var l = new Lexer(input);
        var tokens = l.Lex();
        Assertions.assertEquals(tokenCount, tokens.size());
        var tran = new TranNode();
        var p = new Parser(tran, tokens);
        p.Tran();
        return tran;
    }

    @Test
    public void testInterface() throws Exception {
        var t = LexAndParse("interface someName\r\n\tupdateClock()\r\n\tsquare() : number s", 15);
        Assertions.assertEquals(1, t.Interfaces.size());
        Assertions.assertEquals(2, t.Interfaces.getFirst().methods.size());
    }



    @Test
    public void testClassWithOneMethod() throws Exception {
        var t = LexAndParse("class Tran\r\n\thelloWorld()\r\n\t\tx = 1 + 1", 16);
        Assertions.assertEquals(1, t.Classes.size());
        Assertions.assertEquals(1, t.Classes.getFirst().methods.size());
        Assertions.assertEquals(1, t.Classes.getFirst().methods.getFirst().statements.size());
    }

    @Test
    public void testClassWithMultipleMembers() throws Exception {
        var t = LexAndParse("class Tran\n" +
                "\tnumber w\n" +
                "\tstring x\n" +
                "\tboolean y\n" +
                "\tcharacter z", 16);
        Assertions.assertEquals(1, t.Classes.size());
        Assertions.assertEquals(4, t.Classes.getFirst().members.size());
        var m = t.Classes.getFirst().members;
        Assertions.assertEquals("number", m.getFirst().declaration.type);
        Assertions.assertEquals("w", m.getFirst().declaration.name);
        Assertions.assertEquals("string", m.get(1).declaration.type);
        Assertions.assertEquals("x", m.get(1).declaration.name);
        Assertions.assertEquals("boolean", m.get(2).declaration.type);
        Assertions.assertEquals("y", m.get(2).declaration.name);
        Assertions.assertEquals("character", m.get(3).declaration.type);
        Assertions.assertEquals("z", m.get(3).declaration.name);
    }

    @Test
    public void testClassWithMethodsAndMembers() throws Exception {
        var t = LexAndParse("class Tran\n" +
                        "\tnumber w\n" +
                        "\tstring x\n" +
                        "\tboolean y\n" +
                        "\tcharacter z\n" +
                        "\thelloWorld()\n" +
                        "\t\tx = 1 + 1"
                , 28);
        Assertions.assertEquals(1, t.Classes.size());
        var m = t.Classes.getFirst().members;
        Assertions.assertEquals(4, t.Classes.getFirst().members.size()); // scramble test order to break the "duplicate code" warning
        Assertions.assertEquals("boolean", m.get(2).declaration.type);
        Assertions.assertEquals("y", m.get(2).declaration.name);
        Assertions.assertEquals("character", m.get(3).declaration.type);
        Assertions.assertEquals("z", m.get(3).declaration.name);
        Assertions.assertEquals("string", m.get(1).declaration.type);
        Assertions.assertEquals("x", m.get(1).declaration.name);
        Assertions.assertEquals("number", m.getFirst().declaration.type);
        Assertions.assertEquals("w", m.getFirst().declaration.name);

        Assertions.assertEquals(1, t.Classes.getFirst().methods.size());
        Assertions.assertEquals(1, t.Classes.getFirst().methods.getFirst().statements.size());
    }

    @Test
    public void testClassIf() throws Exception {
        var t = LexAndParse("class Tran\n" +
                        "\thelloWorld()\n" +
                        "\t\tif n>100\n" +
                        "\t\t\tkeepGoing = false"
                , 21);
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(1, myMethod.statements.size());
        Assertions.assertEquals("AST.IfNode", myMethod.statements.getFirst().getClass().getName());
        Assertions.assertTrue(((IfNode)(myMethod.statements.getFirst())).elseStatement.isEmpty());
    }

    @Test
    public void testClassIfElse() throws Exception {
        var t = LexAndParse("class Tran\n" +
                        "\thelloWorld()\n" +
                        "\t\tif n>100\n" +
                        "\t\t\tkeepGoing = false\n" +
                        "\t\telse\n" +
                        "\t\t\tkeepGoing = true\n"
                , 30);
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(1, myMethod.statements.size());
        Assertions.assertEquals("AST.IfNode", myMethod.statements.getFirst().getClass().getName());
        Assertions.assertTrue(((IfNode)(myMethod.statements.getFirst())).elseStatement.isPresent());
        Assertions.assertEquals(1,((IfNode)(myMethod.statements.getFirst())).elseStatement.orElseThrow().statements.size());
    }

    @Test
    public void testLoopVariable() throws Exception {
        var t = LexAndParse("class Tran\n" +
                        "\thelloWorld()\n" +
                        "\t\tloop n\n" +
                        "\t\t\tkeepGoing = false\n"
                , 20);
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(1, myMethod.statements.size());
        Assertions.assertInstanceOf(LoopNode.class, myMethod.statements.getFirst());
    }

    @Test
    public void testLoopCondition() throws Exception {
        var t = LexAndParse("class Tran\n" +
                        "\thelloWorld()\n" +
                        "\t\tloop n<100\n" +
                        "\t\t\tkeepGoing = false\n"
                , 22);
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(1, myMethod.statements.size());
        Assertions.assertInstanceOf(LoopNode.class, myMethod.statements.getFirst());
        Assertions.assertInstanceOf(CompareNode.class, ((LoopNode) myMethod.statements.getFirst()).expression);
    }

    @Test
    public void testLoopConditionWithVariable() throws Exception {
        var t = LexAndParse("class Tran\n" +
                        "\thelloWorld()\n" +
                        "\t\tloop c = n<100\n" +
                        "\t\t\tkeepGoing = false\n"
                , 24);
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(1, myMethod.statements.size());
        Assertions.assertInstanceOf(LoopNode.class, myMethod.statements.getFirst());
        Assertions.assertInstanceOf(CompareNode.class, ((LoopNode) myMethod.statements.getFirst()).expression);
        Assertions.assertTrue(((LoopNode) myMethod.statements.getFirst()).assignment.isPresent());
    }

    @Test
    public void testMethodCallWithMulitpleVariables() throws Exception {
        var t = LexAndParse("class Tran\n" +
                        "\thelloWorld()\n" +
                        "\t\ta,b,c,d,e = doSomething()\n"
                , 25);
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(1, myMethod.statements.size());
        var firstStatement = myMethod.statements.getFirst();
        Assertions.assertInstanceOf(MethodCallStatementNode.class, firstStatement);
        Assertions.assertEquals(5,((MethodCallStatementNode) firstStatement).returnValues.size());
    }

    private Token createToken(Token.TokenTypes type, int line, int column, String value) {
        return new Token(type, line, column, value);
    }

    private Token createToken(Token.TokenTypes type, int line, int column) {
        return new Token(type, line, column);
    }

    @Test
    public void testGetCurrentColumnNumber() {
        Token token1 = createToken(Token.TokenTypes.WORD, 1, 5, "hello");
        TokenManager tokenManager = new TokenManager(new LinkedList<>(Arrays.asList(token1)));

        // Check if the current column is returned correctly
        assertEquals(5, tokenManager.getCurrentColumnNumber(), "The current column should be 5");
    }

    //private tests

    @Test
    public void termsTest() throws Exception {
        Lexer l = new Lexer("class Tran\n" +
                "\thelloWorld()\n" +
                "\t\thi(z)\n"+
                "\t\tresult = 5 * 10\n");
        var rev = l.Lex();
        TranNode TN = new TranNode();
        Parser p = new Parser(TN, rev);
        p.Tran();
        var firstStatement = TN.Classes.get(0).methods.get(0).statements.get(1);
        Assertions.assertInstanceOf(AssignmentNode.class, firstStatement);
        MathOpNode expression = (MathOpNode) ((AssignmentNode) firstStatement).expression;
        Assertions.assertEquals("multiply", expression.op.name());
        Assertions.assertEquals(5.0, ((NumericLiteralNode) expression.left).value);
        Assertions.assertEquals(10.0, ((NumericLiteralNode) expression.right).value);
    }







}
