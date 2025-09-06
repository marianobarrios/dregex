package dregex.impl.tree;

import dregex.impl.CaseNormalization;

public class NamedCaptureGroup extends CaptureGroup {

    public final String name;

    public NamedCaptureGroup(String name, Node value) {
        super(value);
        this.name = name;
    }

    @Override
    public String toRegex() {
        return String.format("(?<%s>%s)", name, value.toRegex());
    }

    @Override
    public Node caseNormalize(CaseNormalization normalizer) {
        return new NamedCaptureGroup(name, value.caseNormalize(normalizer));
    }

    @Override
    public Node unicodeNormalize() {
        return new NamedCaptureGroup(name, value.unicodeNormalize());
    }
}
