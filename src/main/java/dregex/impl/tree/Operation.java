package dregex.impl.tree;

import java.util.Objects;

public abstract class Operation implements Node {

    public final Node left;

    public final Node right;

    Operation(Node left, Node right) {
        this.left = left;
        this.right = right;
    }

    public String toRegex() {
        throw new UnsupportedOperationException();
    }

    public int precedence() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operation operation = (Operation) o;
        return Objects.equals(left, operation.left) && Objects.equals(right, operation.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
