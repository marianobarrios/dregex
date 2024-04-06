package dregex.impl.tree;

import dregex.impl.CaseNormalization;

public class PositionalCaptureGroup extends CaptureGroup {

    public PositionalCaptureGroup(Node value) {
        super(value);
    }

    @Override
    public String toRegex() {
        return String.format("(%s)", value.toRegex());
    }

    @Override
    public Node caseNormalize(CaseNormalization normalizer) {
        return new PositionalCaptureGroup(value.caseNormalize(normalizer));
    }

    @Override
    public Node unicodeNormalize() {
        return new PositionalCaptureGroup(value.unicodeNormalize());
    }
}
