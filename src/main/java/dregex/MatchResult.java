package dregex;

/**
 * The result of a match operation.
 * <p>
 * This class contains query methods used to determine the results of a match against a regular expression.
 */
public class MatchResult {

    private final boolean matches;
    private final int position;

    /**
     * Returns whether the match was successful
     *
     * @return true if the match was successful
     */
    public boolean matches() {
        return matches;
    }

    /**
     * Returns the one past last position in the input string that matched the regex. If the regex matched completely,
     * the number returnes is the size of the input string.
     *
     * @return the position up to which the input string matched
     */
    public int getPosition() {
        return position;
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param matches if the match was successful
     * @param position the position up to which the input string matched
     */
    public MatchResult(boolean matches, int position) {
        this.matches = matches;
        this.position = position;
    }
}
