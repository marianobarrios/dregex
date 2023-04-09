package dregex.impl.tree;

import java.util.Objects;

public abstract class CaptureGroup implements Node {

    public final Node value;

    protected CaptureGroup(Node value) {
        this.value = value;
    }

    @Override
    public CaptureGroup canonical() {
        // TODO: ?
        return this;
    }

    @Override
    public int precedence() {
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaptureGroup that = (CaptureGroup) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
