package dregex.impl;

/**
 * A regular expression that was generated by an operation between others (not parsing a string), so it lacks a
 * literal expression or NFA.
 */
public class SynteticRegex extends RegexImpl {

    public SynteticRegex(Dfa dfa, Universe universe) {
        super(dfa, universe);
    }

    @Override
    public String toString() {
        return String.format("[synthetic] (DFA states: %s)", getDfa().stateCount());
    }
}