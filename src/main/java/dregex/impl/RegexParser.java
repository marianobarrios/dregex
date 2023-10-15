package dregex.impl;

import dregex.InvalidRegexException;
import dregex.ParsedRegex;
import dregex.impl.database.*;
import dregex.impl.tree.*;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.jparsec.pattern.CharPredicate;
import org.jparsec.pattern.CharPredicates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jparsec.pattern.Patterns.*;
import static org.jparsec.Parsers.*;
import static org.jparsec.pattern.Patterns.isChar;

public class RegexParser {

    private final DotMatch dotMatch;
    private final boolean unicodeClasses;

    public RegexParser(DotMatch dotMatch, boolean unicodeClasses) {
        this.dotMatch = dotMatch;
        this.unicodeClasses = unicodeClasses;
    }

    private static Parser<Integer> litChar(char ch) {
        return isChar(ch).toScanner("literal char: " + ch).map(x -> (int) ch);
    }

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

    private final Parser<Lit> controlEscape = sequence(
            backslash,
            litChar('c'),
            isChar(CharPredicates.ALWAYS).toScanner(""))
            .map(x -> {
                throw new InvalidRegexException("Unsupported feature: control escape");
            });

    private final Parser<Lit> backReference = sequence(
            backslash,
            many1(CharPredicates.IS_DIGIT).toScanner(""))
            .map(x -> {
                throw new InvalidRegexException("Unsupported feature: back reference");
            });

    private final Parser<Lit> anchor = or(isChar('^'), isChar('$'))
            .toScanner("")
            .map(x -> {
                throw new InvalidRegexException("Unsupported feature: anchor");
            });

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
            anyEscape,
            regex("[^\\\\.|()\\[\\]+*?]").toScanner("").source().map(ch -> Lit.fromSingletonString(ch)));

    private final Parser<Lit> characterClassLit = or(
            anyEscape,
            regex("[^\\\\^\\]-]").toScanner("").source().map(ch -> Lit.fromSingletonString(ch)));

    private final Parser<CharSet> singleCharacterClassLit = characterClassLit.map(lit -> new CharSet(lit));

    private final Parser<CharSet> charClassRange = sequence(
            characterClassLit, litChar('-'), characterClassLit,
            (start, dash, end) -> new CharSet(new CharRange(start.codePoint, end.codePoint)));

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

    /**
     * If the property starts with "Is" it could be either a script,
     * general category or a binary property. Look for all.
     */
    private final Parser<CharSet> specialCharSetWithIs = sequence(
            backslash,
            litChar('p'),
            litChar('{'),
            regex("Is").toScanner("").source(),
            regex("[0-9a-zA-Z_ -]+").toScanner("").source(),
            litChar('}'), (bl, p, op, is, propValue, cl) -> {
                var upperCaseValue = propValue.toUpperCase();
                var script = UnicodeScripts.chatSets.get(upperCaseValue);
                if (script != null) {
                    return script;
                }
                var gc = UnicodeGeneralCategories.charSets.get(propValue);
                if (gc != null) {
                    return gc;
                }
                var ubp = UnicodeBinaryProperties.charSets.get(upperCaseValue);
                if (ubp != null) {
                    return ubp;
                }
                throw new InvalidRegexException("Invalid Unicode script, general category or binary property: " + propValue);
            });

    private final Parser<CharSet> specialCharSetWithIn = sequence(
            backslash,
            litChar('p'),
            litChar('{'),
            regex("In").toScanner("").source(),
            regex("[0-9a-zA-Z_ -]+").toScanner("").source(),
            litChar('}'), (bl, p, op, in, blockName, cl) -> {
                var block = UnicodeBlocks.charSets.get(UnicodeDatabaseReader.canonicalizeBlockName(blockName));
                if (block == null) {
                    throw new InvalidRegexException("Invalid Unicode block: " + blockName);
                }
                return block;
            });

    private final Parser<CharSet> specialCharSetWithJava = sequence(
            backslash,
            litChar('p'),
            litChar('{'),
            regex("java").toScanner("").source(),
            regex("[0-9a-zA-Z_ -]+").toScanner("").source(),
            litChar('}'),
            (bl, p, op, java, charClass, cl) -> {
        var ret = JavaProperties.charSets.get("java" + charClass);
        if (ret == null) {
            var validOptions = String.join(",", JavaProperties.charSets.keySet());
            throw new InvalidRegexException(String.format(
                    "invalid Java character class: %1$s " +
                    "(note: for such a class to be valid, a method java.lang.Character.is%1$s() must exist) " +
                    "(valid options: %2$s)", charClass, validOptions));
        }
        return ret;
    });

    private Parser<CharSet> specialCharSetImplicit() {
        return sequence(
                backslash,
                litChar('p'),
                litChar('{'),
                regex("[0-9a-zA-Z_ -]+").toScanner("").source(),
                litChar('}'), (bl, p, op, name, cl) -> {
                    Map<String, CharSet> effPosixClasses;
                    if (unicodeClasses) {
                        effPosixClasses = UnicodePosixCharSets.charSets;
                    } else {
                        effPosixClasses = PosixCharSets.charSets;
                    }
                    var posixCharset = effPosixClasses.get(name);
                    if (posixCharset != null) {
                        return posixCharset;
                    }
                    var unicodeCharset = UnicodeGeneralCategories.charSets.get(name);
                    if (unicodeCharset != null) {
                        return unicodeCharset;
                    }
                    throw new InvalidRegexException("Invalid POSIX character class: " + name);
                });
    }


    private Parser<CharSet> shorthandCharSetDigit() {
        return sequence(backslash, litChar('d')).map(x -> {
            if (unicodeClasses) {
                return UnicodeBinaryProperties.charSets.get("DIGIT");
            } else {
                return PosixCharSets.digit;
            }
        });
    }

    private Parser<CharSet> shorthandCharSetDigitCompl() {
        return sequence(backslash, litChar('D')).map(x -> {
            if (unicodeClasses) {
                return UnicodeBinaryProperties.charSets.get("DIGIT").complement();
            } else {
                return PosixCharSets.digit.complement();
            }
        });
    }

    private Parser<CharSet> shorthandCharSetSpace() {
        return sequence(backslash, litChar('s')).map(x -> {
            if (unicodeClasses) {
                return UnicodeBinaryProperties.charSets.get("WHITE_SPACE");
            } else {
                return PosixCharSets.space;
            }
        });
    }

    private Parser<CharSet> shorthandCharSetSpaceCompl() {
        return sequence(backslash, litChar('S')).map(x -> {
            if (unicodeClasses) {
                return UnicodeBinaryProperties.charSets.get("WHITE_SPACE").complement();
            } else {
                return PosixCharSets.space.complement();
            }
        });
    }

    private Parser<CharSet> shorthandCharSetWord() {
        return sequence(backslash, litChar('w')).map(x -> {
            if (unicodeClasses) {
                return UnicodePosixCharSets.wordCharSet;
            } else {
                return PosixCharSets.wordChar;
            }
        });
    }

    private Parser<CharSet> shorthandCharSetWordCompl() {
        return sequence(backslash, litChar('W')).map(x -> {
            if (unicodeClasses) {
                return UnicodePosixCharSets.wordCharSet.complement();
            } else {
                return PosixCharSets.wordChar.complement();
            }
        });
    }

    private Parser<CharSet> shorthandCharSet() {
        return or(
                shorthandCharSetDigit(),
                shorthandCharSetDigitCompl(),
                shorthandCharSetSpace(),
                shorthandCharSetSpaceCompl(),
                shorthandCharSetWord(),
                shorthandCharSetWordCompl());
    }

    private Parser<CharSet> specialCharSet() {
        return or(
                specialCharSetByName,
                specialCharSetWithIs,
                specialCharSetWithIn,
                specialCharSetWithJava,
                specialCharSetImplicit());
    }

    private Parser<CharSet> charClassAtom() {
        return or(charClassRange,
                singleCharacterClassLit,
                shorthandCharSet(),
                specialCharSet());
    }

    private final Parser<CharSet> charClass = sequence(
            litChar('['),
            litChar('^').asOptional(),
            litChar('-').asOptional(),
            charClassAtom().many1(),
            litChar('-').asOptional(),
            litChar(']'),
            (op, negated, leftDash, charClass, rightDash, cl) -> {
                List<CharSet> chars = new ArrayList<>(charClass);
                if (leftDash.isPresent() || rightDash.isPresent()) {
                    chars.add(new CharSet(new Lit('-')));
                }
                var set = new CharSet(chars.stream().flatMap(x -> x.ranges.stream()).collect(Collectors.toList()));
                if (negated.isPresent()) {
                    return set.complement();
                } else {
                    return set;
                }
        });

    // There is the special case of a character class with only one character: the dash. This is valid, but
    // not easily parsed by the general constructs.
    private final Parser<CharSet> dashClass = sequence(
            litChar('['),
            litChar('^').asOptional(),
            litChar('-'),
            litChar(']'), (op, negated, dash, cl) -> {
                var set = new CharSet(new Lit('-'));
                if (negated.isPresent()) {
                    return set.complement();
                } else {
                    return set;
                }
            });

    private final Parser<Juxt> quotedLiteral = between(
            sequence(backslash, litChar('Q')),
            regex(".").toScanner("").source().until(sequence(backslash, litChar('E'))),
            sequence(backslash, litChar('E'))).map(
            literals -> {
                return new Juxt(literals.stream().map(ch -> new Lit(ch.codePointAt(0))).collect(Collectors.toList()));
            });

    private final Parser<Disj> unicodeLineBreak = sequence(backslash, litChar('R')).map(x -> {
        return new Disj(
                new Juxt(new Lit(0xD), new Lit(0xA)),
                new Lit(0xA), new Lit(0xB), new Lit(0xC), new Lit(0xD),
                new Lit(0x85), new Lit(0x2028), new Lit(0x2029));
        });

    private final Parser.Reference<Node> regexRef = Parser.newReference();

    private final Parser<Node> group = sequence(
            litChar('('),
            tuple(
                    litChar('?'),
                    litChar('<').asOptional(),
                    or(litChar(':'), litChar('='), litChar('!'))
            ).asOptional(),
            regexRef.lazy(),
            litChar(')'), (paren1, optModifiers, value, paren2) -> {
                if (optModifiers.isEmpty()) {
                    // Naked parenthesis
                    return new PositionalCaptureGroup(value);
                } else {
                    var modifiers = optModifiers.orElseThrow();
                    char typeOfCapture = (char) modifiers.c.intValue();

                    Direction direction;
                    if (modifiers.b.isPresent()) {
                        direction = Direction.Behind;
                    } else {
                        direction = Direction.Ahead;
                    }

                    switch (typeOfCapture) {
                        case ':':
                            if (modifiers.b.isPresent()) {
                                throw new InvalidRegexException("Invalid grouping: <: ");
                            } else {
                                return value;
                            }
                        case '=':
                            return new Lookaround(direction, Condition.Positive, value);
                        case '!':
                            return new Lookaround(direction, Condition.Negative, value);
                        default:
                            throw new IllegalArgumentException();
                    }
                }
            });

    private final Parser<NamedCaptureGroup> namedGroup = sequence(
            litChar('('),
            litChar('?'),
            litChar('<'),
            regex("[a-zA-Z][a-zA-Z0-9]*").toScanner("").source(),
            litChar('>'),
            regexRef.lazy(),
            litChar(')'),
            (paren1, a, b, name, c, value, paren2) -> new NamedCaptureGroup(name, value));

    private Parser<Node> charWildcard() {
        return litChar('.').map(ch -> {
            switch (dotMatch) {
                case All:
                    return Wildcard.instance;
                case JavaLines:
                    return new CharSet(
                            new Lit('\n'),
                            new Lit('\r'),
                            new Lit(0x85),
                            new Lit(0x2028),
                            new Lit(0x2829)).complement();
                case UnixLines:
                    return new CharSet(new Lit('\n')).complement();
                default:
                    throw new IllegalArgumentException();
            }
        });
    }

    private final Parser<Node> regexAtom = or(
            quotedLiteral,
            charLit,
            charWildcard(),
            charClass,
            unicodeLineBreak,
            dashClass,
            shorthandCharSet(),
            specialCharSet(),
            group,
            namedGroup);

    private final Parser<Quantification> predefQuantifier = or(litChar('+'), litChar('*'), litChar('?')).map(ch -> {
        switch (ch.intValue()) {
            case '+': return new Quantification(1);
            case '*': return new Quantification(0);
            case '?': return new Quantification(0, 1);
            default: throw new IllegalArgumentException();
        }
    });

    private final Parser<Quantification> generalQuantifier = sequence(
            litChar('{'),
            decimalNumber, sequence(litChar(','), decimalNumber.asOptional()).asOptional(),
            litChar('}'),
            (br1, minVal, maxVal, br2) -> {
                if (maxVal.isPresent()) {
                    var max = maxVal.orElseThrow();
                    if (max.isPresent()) {
                        var maxVal2 = max.orElseThrow();
                        // Quantifiers of the for {min,max}
                        if (minVal <= maxVal2)
                           return new Quantification(minVal.intValue(), maxVal2.intValue());
                        else
                            throw new InvalidRegexException("invalid range in quantifier");
                    } else {
                        // Quantifiers of the form {min,}
                        return new Quantification(minVal.intValue());
                    }
                } else {
                    // Quantifiers of the form "{n}", the value is captured as "min", despite being also the max
                    return new Quantification(minVal.intValue(), minVal.intValue());
                }
            });

    private final Parser<Quantification> quantifier = or(predefQuantifier, generalQuantifier);

    private final Parser<Node> lazyQuantifiedBranch = sequence(regexAtom, quantifier, litChar('?'))
            .map(ch -> {
                throw new InvalidRegexException("reluctant quantifiers are not supported");
            });

    private final Parser<Node> possesivelyQuantifiedBranch = sequence(regexAtom, quantifier, litChar('+'))
            .map(ch -> {
                throw new InvalidRegexException("possessive quantifiers are not supported");
            });

    private final Parser<Rep> quantifiedBranch = sequence(regexAtom, quantifier,
            (atom, q) -> new Rep(q.min, q.max, atom));

    private final Parser<Node> branch =
            or(lazyQuantifiedBranch, possesivelyQuantifiedBranch, quantifiedBranch, regexAtom).many1().map(parts -> {
                if (parts.isEmpty()) {
                    throw new AssertionError();
                }
                if (parts.size() == 1) {
                    return parts.get(0);
                } else {
                    return new Juxt(parts);
                }
            });

    private final Parser<Node> emptyRegex = regex("").toScanner("").map(x -> new Juxt(List.of()));

    private final Parser<Node> nonEmptyRegex = sequence(
            branch, sequence(litChar('|'), regexRef.lazy()).asOptional(), (left, optRight) -> {
                if (optRight.isEmpty()) {
                    return left;
                } else {
                    return new Disj(left, optRight.orElseThrow());
                }
            });

    private final Parser<Node> regex = or(nonEmptyRegex, emptyRegex);
    {
        regexRef.set(regex);
    }

    private static final java.util.regex.Pattern commentPattern = java.util.regex.Pattern.compile("(?<!\\\\)#[^\\n]*");

    private static final java.util.regex.Pattern spacePattern = java.util.regex.Pattern.compile("((?<!\\\\)\\s)+");

    private static final java.util.regex.Pattern embeddedFlagPattern = java.util.regex.Pattern.compile("\\(\\?([a-z]*)\\)");

    public static class Flags {
            public DotMatch dotMatch  = DotMatch.All;
            public boolean literal = false;
            public boolean comments = false;
            public boolean unicodeClasses = false;
            public boolean caseInsensitive = false;
            public boolean unicodeCase = false;
            public boolean canonicalEq = false;
            public boolean multiline = false;
    }

    public static ParsedRegex parse(String regex, Flags flags) {
        if (flags.literal) {
            return parseLiteralRegex(regex);
        } else {
            // process embedded flags
            var effRegex = regex;
            var matcher = embeddedFlagPattern.matcher(regex);
            while (matcher.find()) {
                if (matcher.start() > 0) {
                    throw new InvalidRegexException("embedded flag are only valid at the beginning of the pattern");
                }
                for (int i = 0; i < matcher.group(1).length(); i++){
                    char flag = matcher.group(1).charAt(i);
                    switch (flag) {
                        case 'x':
                            flags.comments = true;
                            break;
                        case 's':
                            flags.dotMatch = DotMatch.All;
                            break;
                        case 'd':
                            flags.dotMatch = DotMatch.UnixLines;
                            break;
                        case 'U':
                            flags.unicodeClasses = true;
                            break;
                        case 'i':
                            flags.caseInsensitive = true;
                            break;
                        case 'u':
                            flags.unicodeCase = true;
                            break;
                        case 'm':
                            flags.multiline = true;
                            break;
                        default: throw new InvalidRegexException(String.format("invalid embedded flag: %s", flag));
                    }
                    effRegex = effRegex.substring(matcher.end());
                }
            }
            if (flags.multiline) {
                throw new InvalidRegexException("multiline flag is not supported; this class always works in multiline mode");
            }

            if (flags.comments) {
                // replace comments
                effRegex = commentPattern.matcher(effRegex).replaceAll(" ");
                // remove whitespace
                effRegex = spacePattern.matcher(effRegex).replaceAll("");
            }
            return parseRegexImpl(effRegex, flags);
        }
    }

    /**
     * Parse a quoted regex. They don't really need parsing.
     */
    private static ParsedRegex parseLiteralRegex(String regex) {
        var literals = regex.codePoints().mapToObj(ch -> new Lit(ch)).collect(Collectors.toList());
        return new ParsedRegex(regex, new Juxt(literals), Normalization.NoNormalization);
    }

    /**
     * Parse an actual regex that is not a literal.
     */
    private static ParsedRegex parseRegexImpl(String regex, Flags flags) {
        // normalize case
        Normalizer normalizer;
        if (flags.caseInsensitive) {
            if (flags.unicodeClasses | flags.unicodeCase) {
                normalizer = Normalization.UnicodeLowerCase;
            } else {
                normalizer = Normalization.LowerCase;
            }
        } else {
            normalizer = Normalization.NoNormalization;
        }

        if (flags.canonicalEq) {
            normalizer = Normalizer.combine(Normalization.CanonicalDecomposition, normalizer);
        }

        // parsing proper
        var parser = new RegexParser(flags.dotMatch, flags.unicodeClasses);

        try {
            var tree = parser.regex.parse(normalizer.normalize(regex));
            return new ParsedRegex(regex, tree, normalizer);
        } catch (ParserException e) {
            throw new InvalidRegexException(e.getMessage());
        }
    }

}
