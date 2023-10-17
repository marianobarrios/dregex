package dregex.impl.tree;

public class Union extends Operation {

    public Union(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Union canonical() {
        return new Union(left.canonical(), right.canonical());
    }
}
