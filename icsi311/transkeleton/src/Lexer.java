import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;


public class Lexer {
    HashMap<String, Token.TokenTypes> PUNChashmap;
    HashMap<String, Token.TokenTypes> Keywordshashmap;

    int LinePosition=1;
    int CharacterPosition=1;

    int previndet =0;
    int currentindent=0;


    private Token ParseNumber(char x) {


        //set currentNumber empty to start
        String currentNumber = "";

        boolean decimal = false;


        while (!textmanager.isAtEnd()) {

            char p = textmanager.peekCharacter(); //check for a newline character may be unnecessary
                                                    //when dealing with digits
            if (p == '\n') {
                LinePosition++;
                break; //if found leave while loop and return what is in currentNumber
            }

            //char c = textmanager.getCharacter();
            CharacterPosition++;

            if(p=='.'&& textmanager.isAtEnd()){
                currentNumber = currentNumber + '.';
                decimal =true;
                break;
            }

            if(p=='.'){
                currentNumber = currentNumber + '.';
                p = textmanager.getCharacter();
                CharacterPosition++;
                decimal =true;
            }

            if(p=='-'){
                break;
            }
            p = textmanager.peekCharacter();

            if (!Character.isDigit(p)) {
                //check for not a digit then check if currentNumber is not empty, if both are true,
                // break and return current number
                if (!currentNumber.isEmpty()) {
                    break;
                }


            } else { //if c is a valid character add it to the currentNumber string and loop
                currentNumber = currentNumber + p;
                textmanager.getCharacter();
            }

        }

            //creating the token to be returned when while has finished to ensure its accurate.
        Token ReturnToken = new Token(Token.TokenTypes.NUMBER, LinePosition, CharacterPosition, currentNumber);
        return ReturnToken;

    }

    private Token ParseComments(char x) throws Exception{
        boolean endofcomments= false;
        String comment="";

        textmanager.getCharacter();
        CharacterPosition++;

        if(textmanager.isAtEnd() && endofcomments == false ){
            throw new SyntaxErrorException("File has ended in a comment",LinePosition,CharacterPosition);
        }

        while(!textmanager.isAtEnd() && endofcomments == false){

            if(textmanager.peekCharacter() == '}') {
                textmanager.getCharacter();
                CharacterPosition++;
                endofcomments = true;
            }
            else{
                textmanager.getCharacter();
                CharacterPosition++;
            }

        }
        return null;
    }

    private Token QuotedCharacter(char x){
        textmanager.getCharacter();
        CharacterPosition++;

        char Characterquote = textmanager.getCharacter();
        CharacterPosition++;

        while(!textmanager.isAtEnd()){
            textmanager.getCharacter();
            CharacterPosition++;
        }

        return new Token(Token.TokenTypes.QUOTEDCHARACTER, LinePosition, CharacterPosition, String.valueOf(Characterquote));

    }

    private Token Quotedstrings(char x) throws Exception{

        boolean Quote = false;
        String Quotedword = "";

        textmanager.getCharacter();
        CharacterPosition++;

        if(textmanager.isAtEnd() && Quote == false ){
            throw new SyntaxErrorException("File has ended in a quoted string",LinePosition,CharacterPosition);
        }


        while(!textmanager.isAtEnd() && Quote == false){


            if (textmanager.peekCharacter() == '\"'){
                textmanager.getCharacter();
                CharacterPosition++;
                Quote=true;
            }
            else{
                Quotedword = Quotedword + textmanager.getCharacter();
                CharacterPosition++;
            }

        }
        return new Token(Token.TokenTypes.QUOTEDSTRING, LinePosition, CharacterPosition, Quotedword);
    }

    private Token ParsePunctuation(char x) {

        String result= String.valueOf(x);
        textmanager.getCharacter();
        CharacterPosition++;

        if(!textmanager.isAtEnd()) {
            char q = textmanager.peekCharacter();
            result += String.valueOf(q);
        }


        if(PUNChashmap.containsKey(result)&&result.length()>1){
            Token.TokenTypes holder = PUNChashmap.get(result);
            Token ReturnToken = new Token(holder, LinePosition, CharacterPosition);
            textmanager.getCharacter();
            return ReturnToken;
        }
        else{
            char a = result.charAt(0);
            Token.TokenTypes holder = PUNChashmap.get(String.valueOf(a));
            Token ReturnToken = new Token(holder, LinePosition, CharacterPosition);
            return ReturnToken;
        }


    }

    private Token ParseWord(char x) {
        //similar to ParseNumber in setting string to empty
        String currentWord = "";
        //very similar logic to ParseNumber

        //checks are changed to the appropriate data type, letters here

        while (!textmanager.isAtEnd()) {

            char p = textmanager.peekCharacter();
            if (p == '\n') {
                LinePosition++;
                break;
            }

            char c = textmanager.peekCharacter();

            if (!Character.isLetter(c)) {

                if (!currentWord.isEmpty()) {
                    break;
                }


            } else {
                currentWord = currentWord + c;
                textmanager.getCharacter();
                CharacterPosition++;
            }

        }
        if(Keywordshashmap.containsKey(String.valueOf(currentWord))){
            Token.TokenTypes holder = Keywordshashmap.get(String.valueOf(currentWord));
            Token ReturnToken = new Token(holder, LinePosition, CharacterPosition);
            return ReturnToken;
        }

        //return token once again built at the very end of the method to minimize error
        Token ReturnToken = new Token(Token.TokenTypes.WORD, LinePosition, CharacterPosition, currentWord);
        return ReturnToken;
    }


    private TextManager textmanager;

    public Lexer(String input) {
        this.textmanager = new TextManager(input);

    }

    public List<Token> Lex() throws Exception {

    //punctuation hashmap
        PUNChashmap = new HashMap<>();
        PUNChashmap.put("=", Token.TokenTypes.ASSIGN);
        PUNChashmap.put("(", Token.TokenTypes.LPAREN);
        PUNChashmap.put(")", Token.TokenTypes.RPAREN);
        PUNChashmap.put(":", Token.TokenTypes.COLON);
        PUNChashmap.put(".", Token.TokenTypes.DOT);
        PUNChashmap.put("+", Token.TokenTypes.PLUS);
        PUNChashmap.put("-", Token.TokenTypes.MINUS);
        PUNChashmap.put("*", Token.TokenTypes.TIMES);
        PUNChashmap.put("/", Token.TokenTypes.DIVIDE);
        PUNChashmap.put("%", Token.TokenTypes.MODULO);
        PUNChashmap.put(",", Token.TokenTypes.COMMA);
        PUNChashmap.put("==", Token.TokenTypes.EQUAL);
        PUNChashmap.put("!=", Token.TokenTypes.NOTEQUAL);
        PUNChashmap.put("<", Token.TokenTypes.LESSTHAN);
        PUNChashmap.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        PUNChashmap.put(">", Token.TokenTypes.GREATERTHAN);
        PUNChashmap.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
        PUNChashmap.put("&&", Token.TokenTypes.AND);
        PUNChashmap.put("||", Token.TokenTypes.OR);
        PUNChashmap.put("!", Token.TokenTypes.NOT);


        //keywords hashmap
        Keywordshashmap = new HashMap<>();
        Keywordshashmap.put("accessor", Token.TokenTypes.ACCESSOR);
        Keywordshashmap.put("mutator", Token.TokenTypes.MUTATOR);
        Keywordshashmap.put("implements", Token.TokenTypes.IMPLEMENTS);
        Keywordshashmap.put("class", Token.TokenTypes.CLASS);
        Keywordshashmap.put("interface", Token.TokenTypes.INTERFACE);
        Keywordshashmap.put("loop", Token.TokenTypes.LOOP);
        Keywordshashmap.put("if", Token.TokenTypes.IF);
        Keywordshashmap.put("else", Token.TokenTypes.ELSE);
        Keywordshashmap.put("true", Token.TokenTypes.TRUE);
        Keywordshashmap.put("new", Token.TokenTypes.NEW);
        Keywordshashmap.put("false", Token.TokenTypes.FALSE);
        Keywordshashmap.put("private", Token.TokenTypes.PRIVATE);
        Keywordshashmap.put("shared", Token.TokenTypes.SHARED);
        Keywordshashmap.put("construct", Token.TokenTypes.CONSTRUCT);

        List<Token> ListOfTokens = new LinkedList<Token>();


        while (!textmanager.isAtEnd()) {
            char c = textmanager.peekCharacter();

            if (Character.isLetter(c)) {
                Token wordtoken = ParseWord(c);
                ListOfTokens.add(wordtoken);

            }
            else if (Character.isDigit(c)) {
                Token Numbertoken = ParseNumber(c);
                ListOfTokens.add(Numbertoken);
            }
            else if (c == '\n') {
                Token newlineToken = new Token(Token.TokenTypes.NEWLINE, LinePosition, CharacterPosition, "\n");
                ListOfTokens.add(newlineToken);
                int spaces = 0;


                c=textmanager.getCharacter();
                CharacterPosition++;


                if(!textmanager.isAtEnd()) {
                    c= textmanager.peekCharacter();
                }

                while (c == ' ' || c == '\t' ) {
                    if (c == ' ') {
                        spaces++;
                    } else if (c == '\t') {
                        spaces += 4; // adds 4 spaces for each tab
                    }
                    textmanager.getCharacter();
                    CharacterPosition++;

                    if (!textmanager.isAtEnd()) {
                        c = textmanager.peekCharacter();
                    }
                }

                int indentcheck = 1;

                if(spaces >0){
                    indentcheck=spaces%4;
                }

                //can maybe wrap all of this into an if statement. if indent check is 0,
                // do all indent stuff else break and treat as whitespace

                if(indentcheck == 0) {
                    currentindent = spaces / 4;

                    if (currentindent > previndet) {
                        Token IndentToken = new Token(Token.TokenTypes.INDENT, LinePosition, CharacterPosition);
                        ListOfTokens.add(IndentToken);
                        previndet = currentindent;
                        currentindent=0;
                    } else if (previndet > currentindent) {
                        int DedentAmt = previndet - currentindent;

                        for (int i = 1; i <= DedentAmt; i++) {
                            Token DedentToken = new Token(Token.TokenTypes.DEDENT, LinePosition, CharacterPosition);
                            ListOfTokens.add(DedentToken);
                        }
                        previndet = currentindent;
                        currentindent=0;
                    }
                    currentindent=0;
                }

                CharacterPosition=0;
                LinePosition++;
            }
            else if (c == '.'){
                Token DotToken = new Token(Token.TokenTypes.DOT, LinePosition, CharacterPosition, ".");
                ListOfTokens.add(DotToken);
                char next = textmanager.peekCharacter(0);


                if(!textmanager.isAtEnd()) {
                    next = textmanager.peekCharacter(); // get character after the '.'


                    Token Wordtokenone;
                    Token PunctuationToken;

                    if (Character.isDigit(next)) {
                        Wordtokenone = ParseNumber(c);
                        ListOfTokens.add(Wordtokenone);
                    } else {
                        PunctuationToken = ParsePunctuation(c);
                    }
                }
            }
            else if (c == '-'){
                Token minustoken = new Token(Token.TokenTypes.MINUS, LinePosition, CharacterPosition, "");
                ListOfTokens.add(minustoken);
                textmanager.getCharacter();
                Token numbertoken;
                numbertoken = ParseNumber(c);
                ListOfTokens.add(numbertoken);

            }
            else if(c=='\"'){
                Token StringToken = Quotedstrings(c);
                ListOfTokens.add(StringToken);
            }
            else if(c=='\''){
                Token QuoteToken = QuotedCharacter(c);
                ListOfTokens.add(QuoteToken);
            }
            else if (c == '{') {
                ParseComments(c);

            } else if (c == ' ') {
                textmanager.getCharacter();
                CharacterPosition++;
            }
            else if(c=='\r'){
                textmanager.getCharacter();
                CharacterPosition++;
            }
            else{
                Token PunctuationToken = ParsePunctuation(c);
                ListOfTokens.add(PunctuationToken);
            }

        }

        //indent check after text manager is finished

        if (currentindent > previndet) {
            Token IndentToken = new Token(Token.TokenTypes.INDENT, LinePosition, CharacterPosition);
            ListOfTokens.add(IndentToken);
            previndet = currentindent;
            currentindent=0;
        } else if (previndet > currentindent) {
            int DedentAmt = previndet - currentindent;

            for (int i = 1; i <= DedentAmt; i++) {
                Token DedentToken = new Token(Token.TokenTypes.DEDENT, LinePosition, CharacterPosition);
                ListOfTokens.add(DedentToken);
            }
            previndet = currentindent;
        }

        //end of indent checking may not work

        return ListOfTokens;
    }
}