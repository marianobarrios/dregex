package dregex;

import dregex.impl.Compiler;
import dregex.impl.Dfa;
import dregex.impl.SimpleState;
import lombok.NonNull;
import dregex.impl.tree.Node;

/**
 * A fully-compiled regular expression that was generated from a string literal.
 */
public class CompiledRegex implements Regex {

    private final Universe _universe;
    private final String _originalString;
    private final Node _parsedTree;
    private final Dfa<SimpleState> _dfa;

    public CompiledRegex(@NonNull String originalString, @NonNull Node parsedTree, @NonNull Universe universe) {
        this._universe = universe;
        this._originalString = originalString;
        this._parsedTree = parsedTree;
        this._dfa = new Compiler(_universe.alphabet()).fromTree(_parsedTree);
    }

    @Override
    public Universe universe() {
        return _universe;
    }

    public String originalString() {
        return _originalString;
    }

    public Node parsedTree() {
        return _parsedTree;
    }

    @Override
    public Dfa<SimpleState> dfa() {
        return _dfa;
    }

    @Override
    public String toString() {
        return String.format("⟪%s⟫ (DFA states: %s)", _originalString, _dfa.stateCount());
    }
}