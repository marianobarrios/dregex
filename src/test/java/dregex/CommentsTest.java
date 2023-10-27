package dregex;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dregex.impl.CompiledRegex;
import dregex.impl.RegexParser;
import dregex.impl.Universe;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommentsTest {

    private boolean equivComments(String withComments, String withoutComments) {
        var normal = RegexParser.parse(withoutComments, new RegexParser.Flags());

        var flags = new RegexParser.Flags();
        flags.comments = true;
        var comments = RegexParser.parse(withComments, flags);

        var universe = new Universe(java.util.List.of(normal.getTree(), comments.getTree()), normal.getNorm());
        var cNormal = new CompiledRegex(withoutComments, normal.getTree(), universe);
        var cComments = new CompiledRegex(withComments, comments.getTree(), universe);
        return cNormal.equiv(cComments);
    }

    @Test
    void testCommentsWithFlag() {
        assertTrue(equivComments("a b # comment", "ab"));
        assertTrue(equivComments("a b # comment\nc ", "abc"));
        assertTrue(equivComments(" a | b c  ", "a|bc"));
        assertTrue(equivComments(" ( a |  b ) c ", "(a|b)c"));
        assertTrue(equivComments(" a  b + c ", "ab+c"));
        assertTrue(equivComments(" a\\ b", "a b"));
    }

    private boolean equivEmbedded(String a, String b) {
        var c = Regex.compile(List.of(a, b));
        return c.get(0).equiv(c.get(1));
    }

    @Test
    void testCommentsWithEmbeddedFlag() {
        assertTrue(equivEmbedded("(?x) a  b c", "abc"));
        assertTrue(equivEmbedded("(?x) a  b c", "abc"));
    }
}
