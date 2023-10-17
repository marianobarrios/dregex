package dregex;

public class InvalidRegexException extends RuntimeException {

    private static final long serialVersionUID = 1;

    public InvalidRegexException(String message) {
        super(message);
    }
}
