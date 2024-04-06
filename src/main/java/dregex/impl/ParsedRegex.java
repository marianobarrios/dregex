package dregex.impl;

import dregex.impl.tree.Node;

public class ParsedRegex {

    private final String literal;
    private final Node tree;
    private final CaseNormalization norm;

    public ParsedRegex(String literal, Node tree, CaseNormalization norm) {
        this.literal = literal;
        this.tree = tree;
        this.norm = norm;
    }

    public String getLiteral() {
        return literal;
    }

    public Node getTree() {
        return tree;
    }

    public CaseNormalization getNorm() {
        return norm;
    }
}
