package dregex.impl.tree;

public class Intersection extends Operation {

    public Intersection(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Intersection canonical() {
        return new Intersection(left.canonical(), right.canonical());
    }

}

