package dregex;

import dregex.impl.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A regular expression, ready to be tested against strings, or to take part in an operation against another.
 * Internally, instances of this type have a DFA (Deterministic Finite Automaton).
 */
public class Regex {

    private final RegexImpl regexImpl;

    /**
     * Returns whether {@code this} regex is compatible with the {@code other} one, and they can participate in the
     * sale operation. This will be the case if they were compiled together.
     *
     * @param other the regex to test compatibility.
     *
     * @return whether the regexes are compatible.
     */
    public boolean isCompatible(Regex other) {
        return regexImpl.getUniverse() == other.regexImpl.getUniverse();
    }

    Regex(RegexImpl regexImpl) {
        this.regexImpl = regexImpl;
    }

    /**
     * Return whether the input string is matched by the regular expression (i.e. whether the string is included in the
     * language generated by the expression). As the match is done using a DFA, its complexity is O(n), where n is the
     * length of the string. It is constant with respect to the length of the expression.
     *
     * @param input the string to match
     *
     * @return whether the input matches the regex
     */
    public boolean matches(CharSequence input) {
        return matchAndReport(input).matches();
    }

    /**
     * Return whether the input string is matched by the regular expression (i.e. whether the string is included in the
     * language generated by the expression). As the match is done using a DFA, its complexity is O(n), where n is the
     * length of the string. It is constant with respect to the length of the expression.
     * <p>
     * This method is similar to method {@link #matches(CharSequence)}, except that also return how many characters
     * were successfully matched in case of failure.
     *
     * @param input the string to match
     *
     * @return an object with information about the matching attempt
     */
    public MatchResult matchAndReport(CharSequence input) {
        return regexImpl.matchAndReport(input);
    }

    /**
     * Intersect this regular expression with another. The resulting expression will match the strings that are
     * matched by the operands, and only those. Intersections take O(n⋅m) time, where n and m are the number of states of
     * the DFA of the operands.
     *
     * @param other the regex to intersect
     *
     * @return the resulting intersection regex
     */
    public Regex intersect(Regex other) {
        return new Regex(regexImpl.intersect(other.regexImpl));
    }

    /**
     * Subtract other regex from this one. The resulting expression will match the strings that are
     * matched this expression and are not matched by the other, and only those. Differences take O(n⋅m) time, where n
     * and m are the number of states of the DFA of the operands.
     *
     * @param other the regex to subtract
     *
     * @return the resulting differential regex
     */
    public Regex diff(Regex other) {
        return new Regex(regexImpl.diff(other.regexImpl));
    }

    /**
     * Unite this regex with another. The resulting expression will match the strings that are matched by
     * either of the operands, and only those. Unions take O(n⋅m) time, where n and m are the number of states of the DFA
     * of the operands.
     *
     * @param other the regex to union
     *
     * @return the resulting combined regex
     */
    public Regex union(Regex other) {
        return new Regex(regexImpl.union(other.regexImpl));
    }

    /**
     * Return whether this expression matches at least one string in common with another. Intersections take O(n⋅m) time,
     * where n and m are the number of states of the DFA of the operands.
     *
     * @param other the regex to intersect
     *
     * @return whether the two regexes have a non-empty intersection
     */
    public boolean doIntersect(Regex other) {
        return regexImpl.doIntersect(other.regexImpl);
    }

    /**
     * Return whether this regex matches every expression that is matched by the supplied other one. An {@link #diff(Regex)}
     * between the two operands is done internally.
     *
     * @param other the regex to evaluate
     *
     * @return whether the other regex is a subset of this one
     */
    public boolean isSubsetOf(Regex other) {
        return regexImpl.isSubsetOf(other.regexImpl);
    }

    /**
     * Return whether this expression matches every expression that is matched by another, but the expressions are not
     * equal. Two {@link #diff(Regex)} between the two operands are done internally.
     *
     * @param other the regex to evaluate.
     *
     * @return whether the other regex is a proper subset of this one
     */
    public boolean isProperSubsetOf(Regex other) {
        return regexImpl.isProperSubsetOf(other.regexImpl);
    }

    /**
     * Return whether this regular expression is equivalent to others. Two regular expressions are equivalent if they
     * match exactly the same set of strings. This operation takes O(n⋅m) time, where n and m are the number of states of
     * the DFA of the operands.
     *
     * @param other the regex to evaluate.
     *
     * @return whether the other regex is equivalent to this one
     */
    public boolean equiv(Regex other) {
        return regexImpl.equiv(other.regexImpl);
    }

    /**
     * Return whether this regular expression matches anything. Note that the empty string is a valid match.
     *
     * @return whether this regex matches at least one input string
     */
    public boolean matchesAtLeastOne() {
        return regexImpl.matchesAtLeastOne();
    }

    private static RegexParser.Flags flagsFromBits(int bits) {
        var flags = new RegexParser.Flags();
        flags.dotMatch = dotMatcherFromFlags(bits);
        flags.literal = (bits & Pattern.LITERAL) != 0;
        flags.comments = (bits & Pattern.COMMENTS) != 0;
        flags.unicodeClasses = (bits & Pattern.UNICODE_CHARACTER_CLASS) != 0;
        flags.caseInsensitive = (bits & Pattern.CASE_INSENSITIVE) != 0;
        flags.unicodeCase = (bits & Pattern.UNICODE_CASE) != 0;
        flags.canonicalEq = (bits & Pattern.CANON_EQ) != 0;
        return flags;
    }

    /**
     * Compile a regex from a string, with the given flags.
     *
     * @param regex the expression to be compiled
     *
     * @param flags match flags, a bit mask that accepts flags from {@link Pattern}
     *
     * @return the compiled regex
     */
    public static Regex compile(String regex, int flags) {
        var parsedFlags = flagsFromBits(flags);
        var parsedRegex = RegexParser.parse(regex, parsedFlags);
        var universe = new Universe(List.of(parsedRegex.getTree()), parsedRegex.getNorm(), parsedFlags.canonicalEq);
        return new Regex(new CompiledRegex(regex, parsedRegex.getTree(), universe));
    }

    private static DotMatch dotMatcherFromFlags(int flags) {
        if ((flags & Pattern.DOTALL) != 0) {
            return DotMatch.All;
        } else {
            if ((flags & Pattern.UNIX_LINES) != 0) {
                return DotMatch.UnixLines;
            } else {
                return DotMatch.JavaLines;
            }
        }
    }

    /**
     * Compile a regex from a string.
     *
     * @param regex the expression to be compiled
     *
     * @return the compiled regex
     */
    public static Regex compile(String regex) {
        return compile(regex, 0);
    }

    /**
     * Compiles a set of regular expressions, with the given flags. The resulted regexes will be able to participate
     * in operations.
     *
     * @param regexes the expressions to be compiled
     *
     * @param flags match flags, a bit mask that accepts flags from {@link Pattern}
     *
     * @return the compiled regexes
     */
    public static List<Regex> compile(List<String> regexes, int flags) {
        var parsedFlags = flagsFromBits(flags);
        var parsedRegexes =
                regexes.stream().map(r -> RegexParser.parse(r, parsedFlags)).collect(Collectors.toList());
        var universe = new Universe(
                parsedRegexes.stream().map(pr -> pr.getTree()).collect(Collectors.toList()),
                parsedRegexes.get(0).getNorm(),
                parsedFlags.canonicalEq);
        return parsedRegexes.stream()
                .map(pr -> new Regex(new CompiledRegex(pr.getLiteral(), pr.getTree(), universe)))
                .collect(Collectors.toList());
    }

    /**
     * Compiles a set of regular expressions, with the given flags. The resulted regexes will be able to participate
     * in operations.
     *
     * @param regexes the expressions to be compiled
     *
     * @return the compiled regexes
     */
    public static List<Regex> compile(List<String> regexes) {
        return compile(regexes, 0);
    }
}
