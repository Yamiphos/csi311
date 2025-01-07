import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TokenManager {

    private int listposition =0;
    private List<Token> Tokenlist;


    public TokenManager(List<Token> tokens) {
        this.Tokenlist = tokens;

    }

    public boolean done() {

        if(listposition == Tokenlist.size()){
            return true;
        }
        else return false;
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        Token CurrentToken;

        CurrentToken = Tokenlist.get(listposition);

        if(CurrentToken.getType() == t){
            listposition++;

            return Optional.of(CurrentToken);

        }

        return Optional.empty();
    }

    public Optional<Token> peek(int i) {
        Token CurrentToken;

        int temp = listposition +i;

        if(temp<Tokenlist.size()){

            CurrentToken = Tokenlist.get(listposition+i);

            return Optional.of(CurrentToken);
        }

        return Optional.empty();
    }

    //from word doc
    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second){

        if(Tokenlist.get(listposition).getType()==first && Tokenlist.get(listposition+1).getType() == second){
            return true;
        }
        else {
            return false;
        }

    }

    public int getCurrentLine(){
        Token CurrentTokenLine;

        CurrentTokenLine = Tokenlist.get(listposition);

        return CurrentTokenLine.getLineNumber();
    }

    public int getCurrentColumnNumber(){
        Token CurrentTokenColumn;

        CurrentTokenColumn = Tokenlist.get(listposition);

        return CurrentTokenColumn.getColumnNumber();
    }
}
