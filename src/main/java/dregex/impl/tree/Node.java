package dregex.impl.tree;

import dregex.impl.CaseExpansion;

public interface Node {

    String toRegex();

    Node canonical();

    int precedence();

    Node caseExpansion(CaseExpansion caseExpansion);

    Node unicodeNormalize();
}
