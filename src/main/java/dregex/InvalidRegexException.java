package dregex;

import java.io.Serial;

public class InvalidRegexException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1;

    public InvalidRegexException(String message) {
        super(message);
    }
}
