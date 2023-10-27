package dregex;

/**
 * Exception thrown to indicate an attempt to do an operation between two incompatible regexes.
 */
public class IncompatibleRegexException extends RuntimeException {

    private static final long serialVersionUID = 1;

    /**
     * Constructs a new instance of this class.
     */
    public IncompatibleRegexException() {
        super("cannot make operations between regex compiled separately");
    }
}
