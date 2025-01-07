public class TextManager {

    private String text;
    private int position;

    public TextManager(String input) {
        this.text=input;
        this.position=0;

    }

    public boolean isAtEnd() {
        if(position>=text.length()){
            return true;
        }
        else {
            return false;
        }

    }

    public char peekCharacter() {
        return text.charAt(position);

    }

    public char peekCharacter(int distance) {
        return text.charAt(position+distance);

    }

    public char getCharacter() {
        // should work as intended

        return text.charAt(position++);
    }
}
