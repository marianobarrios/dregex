package dregex.impl;

import dregex.impl.tree.Node;

/**
 * A fully-compiled regular expression that was generated from a string literal.
 */
public final class CompiledRegex extends RegexImpl {

    private final String originalString;
    private final Node parsedTree;

    public CompiledRegex(String originalString, Node parsedTree, Universe universe) {
        super(new Compiler(universe.getAlphabet()).fromTree(parsedTree), universe);
        this.originalString = originalString;
        this.parsedTree = parsedTree;
    }

    public String originalString() {
        return originalString;
    }

    public Node parsedTree() {
        return parsedTree;
    }

    @Override
    public String toString() {
        return String.format("⟪%s⟫ (DFA states: %s)", originalString, getDfa().stateCount());
    }
}
