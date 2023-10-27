package dregex;

/**
 * Exception thrown to indicate a syntax error in a regular-expression pattern.
 */
public class InvalidRegexException extends RuntimeException {

    private static final long serialVersionUID = 1;

    /**
     * Constructs a new instance of this class.
     *
     * @param message the detail message
     */
    public InvalidRegexException(String message) {
        super(message);
    }
}
