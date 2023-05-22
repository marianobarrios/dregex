package dregex.impl;

import dregex.InvalidRegexException;
import dregex.impl.database.UnicodeBlocks;
import dregex.impl.database.UnicodeDatabaseReader;
import dregex.impl.database.UnicodeGeneralCategories;
import dregex.impl.database.UnicodeScripts;
import dregex.impl.tree.CharRange;
import dregex.impl.tree.CharSet;
import dregex.impl.tree.Lit;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.pattern.CharPredicate;
import org.jparsec.pattern.CharPredicates;

import static org.jparsec.pattern.Patterns.*;
import static org.jparsec.Parsers.*;
import static org.jparsec.pattern.Patterns.isChar;

public class RegexParserJava {

    private static int stringToCodePoint(String ch) {
        if (ch.length() == 1) {
            return ch.charAt(0);
        } else if (ch.length() == 2) {
            char highSurrogate = ch.charAt(0);
            char lowSurrogate = ch.charAt(1);
            if (Character.isHighSurrogate(highSurrogate)) {
                if (Character.isLowSurrogate(lowSurrogate)) {
                    return Character.toCodePoint(highSurrogate, lowSurrogate);
                } else {
                    throw new RuntimeException("invalid UTF-16 pair, high surrogate followed by non-low surrogate");
                }
            } else {
                throw new RuntimeException("invalid UTF-16 pair, first character is not a high surrogate");
            }
        } else {
            throw new RuntimeException("Unsupported literal: " + ch);
        }
    }

    private static Parser<Integer> litChar(char ch) {
        return isChar(ch).toScanner("literal char: " + ch).map(x -> (int) ch);
    }

    public final boolean comments = true;

    public final CharPredicate IS_OCTAL_DIGIT = new CharPredicate() {
        @Override public boolean isChar(char c) {
            return c>='0' && c <= '7';
        }
        @Override public String toString() {
            return "[0-7]";
        }
    };

    private final Parser<Integer> backslash = litChar('\\');

    private final Parser<Integer> hexNumber = many1(CharPredicates.IS_HEX_DIGIT)
            .toScanner("hex number").source()
            .map(s -> Integer.parseInt(s, 16));

    private final Parser<Character> hexNumber4 = repeat(4, CharPredicates.IS_HEX_DIGIT)
            .toScanner("hex number").source()
            .map(s -> (char) Integer.parseInt(s, 16));

    private final Parser<Character> hexNumber2 = repeat(2, CharPredicates.IS_HEX_DIGIT)
            .toScanner("hex number").source()
            .map(s -> (char) Integer.parseInt(s, 16));

    private final Parser<Integer> octalNumber = times(1, 3, IS_OCTAL_DIGIT)
            .toScanner("octal number").source()
            .map(s -> Integer.parseInt(s, 8));

    private final Parser<Long> decimalNumber = many1(CharPredicates.IS_DIGIT)
            .toScanner("decimal number").source()
            .map(s -> Long.parseLong(s));

//    private final Pattern charSpecialInsideClasses = or(isChar('\\'), isChar(']'), isChar('^'), isChar('-'));

//    private final Pattern charSpecial = or(
//            isChar('\\'),
//            isChar('.'),
//            isChar('|'),
//            isChar('('),
//            isChar(')'),
//            isChar('['),
//            isChar(']'),
//            isChar('+'),
//            isChar('*'),
//            isChar('?'),
//            isChar('^'),
//            isChar('$'));

    private final Parser<Lit> controlEscape = Parsers.sequence(
            backslash,
            litChar('c'),
            isChar(CharPredicates.ALWAYS).toScanner(""))
            .next(x -> fail("Unsupported feature: control escape"));

    private final Parser<Lit> backReference = Parsers.sequence(
            backslash,
            many1(CharPredicates.IS_DIGIT).toScanner(""))
            .next(x -> fail("Unsupported feature: back reference"));

    private final Parser<Lit> anchor = or(isChar('^'), isChar('$'))
            .toScanner("anchor")
            .next(x -> fail("Unsupported feature: anchor"));

    private final Parser<Void> sp;
    {
        if (comments) {
            sp = regex("\\s*").toScanner("white space").skipMany(); // ASCII white space intentionally for Java compatibility
        } else {
            sp = Parsers.never();
        }
    }

    private final Parser<Lit> specialEscape =
            sequence(backslash, regex( "[^dwsDWSuxcpR0123456789]").toScanner("").source())
            .map(code -> {
                switch (code) {
                    case "n": return new Lit('\n');
                    case "r": return new Lit('\r');
                    case "t": return new Lit('\t');
                    case "f": return new Lit('\f');
                    case "b": return new Lit('\b');
                    case "v": return new Lit(0xB); // vertical tab
                    case "a": return new Lit(0x7); // bell
                    case "e": return new Lit(0x1B); // escape
                    case "B": return new Lit('\\');
                    default: return Lit.fromSingletonString(code) ;// remaining escaped characters stand for themselves
                }
            });

    private final Parser<Character> unicodeEscape = sequence(backslash, litChar('u'), hexNumber4);

    private final Parser<Lit> simpleUnicodeEscape = unicodeEscape.map(ch -> new Lit(ch));

    private final Parser<Lit> doubleUnicodeEscapeOld = unicodeEscape.next(highNumber -> {
       if (Character.isHighSurrogate(highNumber)) {
           return unicodeEscape.map(lowNumber -> {
               if (Character.isLowSurrogate(lowNumber)) {
                   return new Lit(Character.toCodePoint(highNumber, lowNumber));
               } else {
                   throw new InvalidRegexException("invalid UTF-16 pair, high surrogate followed by non-low surrogate");
               }
           });
       } else {
           return never();
       }
    });

    private final Parser<Lit> doubleUnicodeEscape = unicodeEscape.next(highNumber -> {
        if (Character.isHighSurrogate(highNumber)) {
            return unicodeEscape.next(lowNumber -> {
                if (Character.isLowSurrogate(lowNumber)) {
                    return constant(new Lit(Character.toCodePoint(highNumber, lowNumber)));
                } else {
                    return fail("invalid UTF-16 pair, high surrogate followed by non-low surrogate");
                }
            });
        } else {
            return never();
        }
    });

    private final Parser<Lit> hexEscape = sequence(backslash, litChar('x'), hexNumber2).map(ch -> new Lit(ch));

    private final Parser<Lit> longHexEscape = sequence(backslash, litChar('x'), hexNumber.between(litChar('{'), litChar('}')))
            .map(ch -> new Lit(ch));

    private final Parser<Lit> octalEscape = sequence(backslash, litChar('0'), octalNumber)
            .map(ch -> new Lit(ch));

    /**
     * Order between Unicode escapes is important
     */
    private final Parser<Lit> anyEscape = or(
            specialEscape,
            doubleUnicodeEscape,
            simpleUnicodeEscape,
            hexEscape,
            longHexEscape,
            octalEscape,
            controlEscape,
            backReference);

    private final Parser<Lit> charLit = or(
            anchor,
            isChar(CharPredicates.notAmong("\\.|()[]+*?^$")).toScanner("").source().map(ch -> new Lit(stringToCodePoint(ch))),
            anyEscape);

    private final Parser<Lit> characterClassLit = or(
            isChar(CharPredicates.notAmong("\\]^-")).toScanner("").source().map(ch -> new Lit(stringToCodePoint(ch))),
            anyEscape);

    private final Parser<CharSet> singleCharacterClassLit = characterClassLit.map(lit -> new CharSet(lit));

    private final Parser<CharSet> charClassRange = sequence(
            characterClassLit, litChar('-'), characterClassLit,
            (start, dash, end) -> new CharSet(new CharRange(start.codePoint, end.codePoint)));

    //private final unicodeSubsetName = "[0-9a-zA-Z_ -]+".r

    private final Parser<CharSet> specialCharSetByName = sequence(
            backslash,
            litChar('p'),
            litChar('{'),
            regex("[a-z_]+").toScanner("").source(),
            litChar('='),
            regex("[0-9a-zA-Z_ -]+").toScanner("").source(),
            litChar('}'), (bl, p, op, propName, eq, propValue, cl) -> {
                switch (propName) {
                    case "block":
                    case "blk":
                        var canonicalBlockName = UnicodeDatabaseReader.canonicalizeBlockName(propValue);
                        var block = UnicodeBlocks.charSets.get(canonicalBlockName);
                        if (block == null) {
                            throw new InvalidRegexException("Invalid Unicode block: " + propValue);
                        }
                        return block;
                    case "script":
                    case "sc":
                        var script = UnicodeScripts.chatSets.get(propValue.toUpperCase());
                        if (script == null) {
                            throw new InvalidRegexException("Invalid Unicode script: " + propValue);
                        }
                        return script;
                    case "general_category":
                    case "gc":
                        var gc = UnicodeGeneralCategories.charSets.get(propValue);
                        if (gc == null) {
                            throw new InvalidRegexException("Invalid Unicode general category: " + propValue);
                        }
                        return gc;
                    default:
                        throw new InvalidRegexException("Invalid Unicode character property name: " + propName);
                }
            });

}
