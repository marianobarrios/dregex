package dregex.impl.tree;

public class NamedCaptureGroup extends CaptureGroup {

    public final String name;

    public NamedCaptureGroup(String name, Node value) {
        super(value);
        this.name = name;
    }

    @Override
    public String toRegex() {
        return String.format("(?<%s)", value.toRegex());
    }
}
