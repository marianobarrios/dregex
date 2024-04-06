package dregex.impl.tree;

import dregex.impl.Normalizer;

public interface Node {

    String toRegex();

    Node canonical();

    int precedence();

    Node caseNormalize(Normalizer normalizer);
}
