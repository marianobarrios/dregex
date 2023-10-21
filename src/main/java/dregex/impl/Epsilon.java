package dregex.impl;

import java.util.Objects;

public final class Epsilon implements AtomPart {

    public static final Epsilon instance = new Epsilon();

    private Epsilon() {}

    public String toString() {
        return "ε";
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return Objects.hash();
    }
}
