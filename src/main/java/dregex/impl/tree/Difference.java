package dregex.impl.tree;

public class Difference extends Operation {

    public Difference(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Difference canonical() {
        return new Difference(left.canonical(), right.canonical());
    }
}
