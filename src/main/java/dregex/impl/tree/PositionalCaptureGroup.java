package dregex.impl.tree;

import dregex.impl.CaseExpansion;

public class PositionalCaptureGroup extends CaptureGroup {

    public PositionalCaptureGroup(Node value) {
        super(value);
    }

    @Override
    public String toRegex() {
        return String.format("(%s)", value.toRegex());
    }

    @Override
    public Node caseExpansion(CaseExpansion caseExpansion) {
        return new PositionalCaptureGroup(value.caseExpansion(caseExpansion));
    }

    @Override
    public Node unicodeNormalize() {
        return new PositionalCaptureGroup(value.unicodeNormalize());
    }
}
