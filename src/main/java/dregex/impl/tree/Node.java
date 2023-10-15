package dregex.impl.tree;

public interface Node {

    String toRegex();
    Node canonical();
    int precedence();
}
