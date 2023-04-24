package dregex;

import dregex.impl.Normalizer;
import dregex.impl.tree.Node;

public class ParsedRegex {

    private final String literal;
    private final Node tree;
    private final Normalizer norm;

    public ParsedRegex(String literal, Node tree, Normalizer norm) {
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

    public Normalizer getNorm() {
        return norm;
    }
}