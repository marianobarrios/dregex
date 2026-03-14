package dregex.impl;

import dregex.impl.tree.Node;

public class ParsedRegex {

    private final String literal;
    private final Node tree;

    public ParsedRegex(String literal, Node tree) {
        this.literal = literal;
        this.tree = tree;
    }

    public String getLiteral() {
        return literal;
    }

    public Node getTree() {
        return tree;
    }
}
