package dregex.impl.tree;

import dregex.impl.CaseNormalization;

public interface Node {

    String toRegex();

    Node canonical();

    int precedence();

    Node caseNormalize(CaseNormalization normalizer);

    Node unicodeNormalize();
}
