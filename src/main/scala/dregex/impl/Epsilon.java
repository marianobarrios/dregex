package dregex.impl;

public final class Epsilon implements AtomPart {

    public String toString() {
        return "ε";
    }

    @Override
    public boolean equals(Object other) {
        return getClass().equals(other.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
