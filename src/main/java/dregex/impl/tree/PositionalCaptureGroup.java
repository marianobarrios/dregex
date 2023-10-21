package dregex.impl.tree;

public class PositionalCaptureGroup extends CaptureGroup {

    public PositionalCaptureGroup(Node value) {
        super(value);
    }

    @Override
    public String toRegex() {
        return String.format("(%s)", value.toRegex());
    }
}
