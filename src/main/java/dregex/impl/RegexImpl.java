package dregex.impl;

import dregex.IncompatibleRegexException;
import dregex.MatchResult;
import java.text.Normalizer;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexImpl {

    private static final Logger logger = LoggerFactory.getLogger(RegexImpl.class);

    private final Dfa dfa;

    /**
     * Return this regex's [[Universe]]. Only regexes of the same universe can be operated together.
     */
    private final Universe universe;

    protected RegexImpl(Dfa dfa, Universe universe) {
        this.dfa = dfa;
        this.universe = universe;
    }

    public Dfa getDfa() {
        return dfa;
    }

    public Universe getUniverse() {
        return universe;
    }

    private void checkUniverse(RegexImpl other) {
        if (other.getUniverse() != getUniverse()) {
            throw new IncompatibleRegexException();
        }
    }

    public MatchResult matchAndReport(CharSequence string) {
        // Unicode normalization
        if (universe.hasCanonicalEquivalence()) {
            string = Normalizer.normalize(string, Normalizer.Form.NFD);
        }

        // Case normalization
        var caseNormalization = universe.getNormalization();
        var normalized = string.codePoints().map(caseNormalization::normalize).toArray();

        return DfaAlgorithms.matchString(dfa, normalized);
    }

    public RegexImpl intersect(RegexImpl other) {
        checkUniverse(other);
        var start = System.nanoTime();
        var ret = new SyntheticRegex(
                DfaAlgorithms.rewriteWithSimpleStates(DfaAlgorithms.doIntersect(this.dfa, other.dfa)), universe);
        var time = Duration.ofNanos(System.nanoTime() - start);
        logger.trace("{} and {} intersected in {}", this, other, time);
        return ret;
    }

    public RegexImpl diff(RegexImpl other) {
        checkUniverse(other);
        var start = System.nanoTime();
        var ret = new SyntheticRegex(
                DfaAlgorithms.rewriteWithSimpleStates(DfaAlgorithms.diff(this.dfa, other.dfa)), universe);
        var time = Duration.ofNanos(System.nanoTime() - start);
        logger.trace("{} and {} diffed in {}", this, other, time);
        return ret;
    }

    public RegexImpl union(RegexImpl other) {
        checkUniverse(other);
        var start = System.nanoTime();
        var ret = new SyntheticRegex(
                DfaAlgorithms.rewriteWithSimpleStates(DfaAlgorithms.union(this.dfa, other.dfa)), universe);
        var time = Duration.ofNanos(System.nanoTime() - start);
        logger.trace("{} and {} unioned in {}", this, other, time);
        return ret;
    }

    public boolean doIntersect(RegexImpl other) {
        checkUniverse(other);
        return DfaAlgorithms.isIntersectionNotEmpty(this.dfa, other.dfa);
    }

    public boolean isSubsetOf(RegexImpl other) {
        checkUniverse(other);
        return DfaAlgorithms.isSubsetOf(this.dfa, other.dfa);
    }

    public boolean isProperSubsetOf(RegexImpl other) {
        checkUniverse(other);
        return DfaAlgorithms.isProperSubset(this.dfa, other.dfa);
    }

    public boolean equiv(RegexImpl other) {
        checkUniverse(other);
        return DfaAlgorithms.equivalent(this.dfa, other.dfa);
    }

    public boolean matchesAtLeastOne() {
        return DfaAlgorithms.matchesAtLeastOne(dfa);
    }

    /**
     * Create a regular expression that does not match anything. Note that that is different from matching the empty
     * string. Despite the theoretical equivalence of automata and regular expressions, in practice there is no regular
     * expression that does not match anything.
     */
    public static RegexImpl nullRegex(Universe u) {
        return new SyntheticRegex(Dfa.nothingDfa, u);
    }
}
