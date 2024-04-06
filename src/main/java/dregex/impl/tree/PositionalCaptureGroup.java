package dregex.impl.tree;

import dregex.impl.Normalizer;

public class PositionalCaptureGroup extends CaptureGroup {

    public PositionalCaptureGroup(Node value) {
        super(value);
    }

    @Override
    public String toRegex() {
        return String.format("(%s)", value.toRegex());
    }

    @Override
    public Node caseNormalize(Normalizer normalizer) {
        return new PositionalCaptureGroup(value.caseNormalize(normalizer));
    }
}
