import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Lexer1Tests {

    @Test
    public void SimpleLexerTest() {
        var l = new Lexer("ab cd ef gh");
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());
            Assertions.assertEquals("ab", res.get(0).getValue());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals("gh", res.get(3).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.WORD, result.getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MultilineLexerTest() {
        var l = new Lexer("ab cd ef gh\nasdjkdsajkl\ndsajkdsa asdjksald dsajhkl \n");
        try {
            var res = l.Lex();
            Assertions.assertEquals(11, res.size());
            Assertions.assertEquals("ab", res.get(0).getValue());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals("gh", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(4).getType());
            Assertions.assertEquals("asdjkdsajkl", res.get(5).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(6).getType());
            Assertions.assertEquals("dsajkdsa", res.get(7).getValue());
            Assertions.assertEquals("asdjksald", res.get(8).getValue());
            Assertions.assertEquals("dsajhkl", res.get(9).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(10).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void NotEqualsTest() {
        var l = new Lexer(")");
        try {
            var res = l.Lex();
            Assertions.assertEquals(1, res.size());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(0).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void IndentTest() {
        var l = new Lexer(
                "loop keepGoing\n" +
                    "    if n >= 15\n" +
	                "        keepGoing = false\n"
                 );
        try {
            var res = l.Lex();
            Assertions.assertEquals(16, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void TwoCharacterTest() {
        var l = new Lexer(">= > <= < = == !=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(7, res.size());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHANEQUAL, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHAN, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHANEQUAL, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHAN, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.ASSIGN, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(6).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MixedTest() {
        var l = new Lexer("word 1.2 : ( )");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("word", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(1).getType());
            Assertions.assertEquals("1.2", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(4).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }


    @Test //own test for numbers
    public void NumberTest() {
        var l = new Lexer("-.2");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());
            Assertions.assertEquals(Token.TokenTypes.MINUS, res.get(0).getType());
            Assertions.assertEquals(".2", res.get(1).getValue());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }

    }

    @Test //own test for blank space at start of a number
    public void Blankspace() {
        var l = new Lexer(" 12 13");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());
            Assertions.assertEquals("12", res.get(0).getValue());
            Assertions.assertEquals("13", res.get(1).getValue());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test //own test for blankspace at start of text and periods.
    public void Blankspace2() {
        var l = new Lexer(" I am quick. Really quick.");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals("I", res.get(0).getValue());
            Assertions.assertEquals("am", res.get(1).getValue());
            Assertions.assertEquals("quick", res.get(2).getValue());
            Assertions.assertEquals("Really", res.get(3).getValue());
            Assertions.assertEquals("quick", res.get(4).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test //own test for Punctuation
    public void Punctuation() {
        var l = new Lexer("== <=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());
            Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHANEQUAL, res.get(1).getType());


        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test //own test for !
    public void ExclamationPTtest() {
        var l = new Lexer("I!");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());
            Assertions.assertEquals("I", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NOT, res.get(1).getType());


        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test //leave alone
    public void dotAtEnd() {
        var l = new Lexer("I.");
        try {
            var res = l.Lex();
            Assertions.assertEquals(1, res.size());
            Assertions.assertEquals("I", res.get(0).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void IndentTest1() {
        var l = new Lexer(
                "loop keepGoing\n" +
                        "    if n >= 15\n" +
                        "        keepGoing = false\n" +
                        "loop keepGoing\n" +
                        "    if n >= 15\n" +
                        "        keepGoing = false\n"
        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(32, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void KeyWordLexerTest() {
        var l = new Lexer("class interface something accessor: mutator: if else loop && || shared construct new private implements true false !");
        try {
            var res = l.Lex();

            // Let's check if we've got the right number of tokens!
            Assertions.assertEquals(20, res.size());

            // Checking all the individual tokens!
            Assertions.assertEquals(Token.TokenTypes.CLASS, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.INTERFACE, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(2).getType());
            Assertions.assertEquals("something", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.ACCESSOR, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.MUTATOR, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(6).getType());
            Assertions.assertEquals(Token.TokenTypes.IF, res.get(7).getType());
            Assertions.assertEquals(Token.TokenTypes.ELSE, res.get(8).getType());
            Assertions.assertEquals(Token.TokenTypes.LOOP, res.get(9).getType());
            Assertions.assertEquals(Token.TokenTypes.AND, res.get(10).getType());
            Assertions.assertEquals(Token.TokenTypes.OR, res.get(11).getType());
            Assertions.assertEquals(Token.TokenTypes.SHARED, res.get(12).getType());
            Assertions.assertEquals(Token.TokenTypes.CONSTRUCT, res.get(13).getType());
            Assertions.assertEquals(Token.TokenTypes.NEW, res.get(14).getType());
            Assertions.assertEquals(Token.TokenTypes.PRIVATE, res.get(15).getType());
            Assertions.assertEquals(Token.TokenTypes.IMPLEMENTS, res.get(16).getType());
            Assertions.assertEquals(Token.TokenTypes.TRUE, res.get(17).getType());
            Assertions.assertEquals(Token.TokenTypes.FALSE, res.get(18).getType());
            Assertions.assertEquals(Token.TokenTypes.NOT, res.get(19).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test //more specific test for private test above
    public void doublenewline() {
        var l = new Lexer("5wo");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());
            Assertions.assertEquals("5", res.get(0).getValue());
            Assertions.assertEquals("wo", res.get(1).getValue()); //skips first letter fsr

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test //fixed
    public void doublenewline1() {
        var l = new Lexer(">=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(1, res.size());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHANEQUAL, res.get(0).getType());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void indenttestatend() {
        var l = new Lexer(
                "loop keepGoing\n" +
                        "\tif n >= 15"
        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(9, res.size());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MixedTest2() {
        var l = new Lexer(")");
        try {
            var res = l.Lex();
            Assertions.assertEquals(1, res.size());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(0).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void interpretertestcarryover() {
        var l = new Lexer(
        "class SimpleAdd\n" +
                "    shared start()\n" +
                "\n" +
                "        number x\n  " +
                "        number y\n  " +
                "        number z\n" +
                "\n" +
                "        x = 6\n" +
                "        y = 6\n" +
                "        z = x + y \n" +
                "        console.write(z)\n"

    );

        try {
            var res = l.Lex();
            Assertions.assertEquals(44, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }

    }

}


