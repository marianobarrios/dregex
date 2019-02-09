# Dregex - Deterministic Regular Expression Engine

Dregex is a JVM library that implements a regular expression engine using deterministic finite automata (DFA). It supports some Perl-style features and yet retains linear matching time. It can, additionally, do set operations (union, intersection, and difference).

[![Build Status](https://travis-ci.org/marianobarrios/dregex.svg?branch=master)](https://travis-ci.org/marianobarrios/dregex)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.marianobarrios/dregex_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.marianobarrios/dregex_2.12)
[![Scaladoc](http://javadoc-badge.appspot.com/com.github.marianobarrios/dregex_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/com.github.marianobarrios/dregex_2.12)

## Rationale

### History

"Regular expressions" are a mathematically-defined concept, invented by Stephen Kleene in 1956. In the most minimalistic (and original) version these expressions define languages using just literal characters, alternation ("|") and repetition ("*"). They have well-understood properties; centrally, they can be matched against text using another well-understood abstract device, a Definite Finite Automaton (DFA). DFA have the important property of running on arbitrary text of length n in O(n) time and using O(1) space. Presented with a regular expression and a candidate text, a DFA decides whether the text matches the expression.

Some years later, regular expressions entered actual usage in the UNIX world, when Ken Thompson included them as a feature in the editor [QED](https://en.wikipedia.org/wiki/QED_(text_editor). After that, they became a standard language in the world of UNIX, appearing in a variety of programs, notably the still-used [grep](https://en.wikipedia.org/wiki/Grep) tool.

Circa 1994, Henry Spencer wrote a [grep implementation](https://github.com/garyhouston/regexp.old) that used a Non-Deterministic Finite Automaton (NFA). Using NFA simplifies the compilation of expressions (that is, building the automata) at the cost of complicating the execution. More relevant, NFA-based (more precisely, recursive backtracking) makes it easy to add new features (many of which are impossible to do with a state-machine (DFA) based implementation). And that's precisely what started to happen when the library was used to implement regular expressions in Perl 5 in 1994.

After the release of Perl 5, the extended feature-set was quickly embraced by users, and Perl-style regular expression implementations became part of the standard libraries of essentially all popular programming languages (with the notable exception of Go) and also standard infrastructure components, like the Apache and Nginx web servers.

As mentioned, Perl-style expressions don't give any execution time guarantee. On the other hand, there are some features of Perl regular expressions that are impossible to express in a DFA, most notable backreferences (i.e., forcing to match the same text more than once). There are also some other features that, albeit not infeasible, complicate a DFA solution  significantly, like search and capturing groups.

### Proposal

Regular expressions were born as a very specific tool and, almost as an a accident, grew to one of the most versatile (and abused) tools in the world of software. There is, however, a fundamental trade-off between the two prototypical implementations, which is usually ignored. Unbounded execution time is undesirable for many (if not all) interactive uses, as problems can happen whether either the regular expression or the text are supplied by the user. As example see:

- [Coding horror: regex performance](https://blog.codinghorror.com/regex-performance/)
- [Stack Exchange regex outage postmortem](http://stackstatus.net/post/147710624694/outage-postmortem-july-20-2016)

For cases when advanced Perl-style features are not needed, and predictable performance is desired, using DFA-based matching is usually a compelling alternative, unfortunately made difficult by the scarcity of proper implementations in most languages. Dregex offers an alternative for the Java ecosystem.

## Supported regex flavor

Unless specified, the regular expression flavor supported attempts to be compatible with the [Java flavor](https://docs.oracle.com/javase/9/docs/api/java/util/regex/Pattern.html).

### Supported features

* Literals
* Escaped characters
* Standard regular language constructs: `|`, `?`, `*` and `+`, and parenthesis grouping.
* General quantifiers: `{n}`, `{n,m}`, `{n,}`
* Dot wildcards
* Simple character classes: `[abc]`
* Simple negated character classes: `[^abc]`
* Ranges in character classes: `[a-z]`
* Special character classes, including negated versions: `\w`, `\s`, `\d`, `\W`, `\S`, `\D`
* POSIX character classes (using the syntax and definition of [java.util.regex.Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)), e.g., `\p{Lower}`, `\p{Punct}`, `\P{Lower}`
* Unicode character classes (using the syntax and definition of [java.util.regex.Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)):
	* [Blocks](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#ubc), e.g., `\p{InGreek}`, `\p{block=Greek}`, `\p{blk=Greek}`
	* [Scripts](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#usc), e.g., `\p{IsLatin}`, `\p{script=Latin}`, `\p{sc=Latin}`
	* [General categories](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#ucc), e.g., `\p{Lu}`, `\p{IsLu}`, `\p{general_category=Lu}`, `\p{gc=Lu}`
	* [Binary properties](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#ubpc), e.g., `\p{IsAlphabetic}`
* Java-defined character classes (using the syntax and definition of [java.util.regex.Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)), e.g., `\p{javaLowerCase}`, `\p{javaWhitespace}` 
* Special character classes inside regular character classes: `[\d\s]`, `[\D]`
* Unicode line breaks: `\R`
* Block quotes: `\Q`...`\E`
* Lookaround (lookahead and lookbehind; both positive and negative) (see note below)

#### Compile flags

With one exception, all compile flags defined by `java.util.regex.Pattern` are supported, including in embedded form:

* [LITERAL](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#LITERAL)
* [COMMENTS](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#COMMENTS) (also in embedded form: `(?x)`). Note: The flag intentionally behaves ignoring exactly the same set of white space characters as the standard Java implementation, that is, only ASCII white space, not Unicode. 
* [DOTALL](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#DOTALL) (also in embedded form: `(?s)`)
* [UNIX_LINES](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#UNIX_LINES) (also in embedded form: `(?d)`)
* [UNICODE_CHARACTER_CLASS](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#UNICODE_CHARACTER_CLASS) (also in embedded form: `(?U)`)
* [CASE_INSENSITIVE](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#CASE_INSENSITIVE) (also in embedded form: `(?i)`)
* [UNICODE_CASE](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#UNICODE_CASE) (also in embedded form: `(?u)`)
* [CANON_EQ](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#CANON_EQ)

### Not supported

* Searching (the engine matches only against the full input string)
* Capturing groups
* Backreferences
* Anchors (`ˆ` and `$`), as they are redundant, as the expressions only operate over the complete text.
* Reluctant (`+?`, `*?`, `??`, `{...}?`) and possessive (`++`, `*+`, `?+`, `{...}+`) quantifiers , because they are meaningless for a pure-matching engine, as they, by definition, only affect capturing groups, not whether the expressions match or not.
* Compile flag [MULTILINE](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#MULTILINE), because it is meaningless for a pure-matching engine, that works always in multi-line mode.

**Note**: for the sake of safety, the presence of unsupported features in a regular expression will cause it to fail to compile (with the exception of unnamed capturing groups, as they have no syntax: they are just a pair of parenthesis).

## Set operations

On top of regular matching, Dregex fully supports set operations of regular expressions. Set operations work on regular expressions themselves, that is, they don't involve strings.

It possible to do union, intersection and difference:

```java
List<CompiledRegex> regexes = Regex.compile(Arrays.asList("[a-z]+", "[A-Z]+", "[a-z]+|[A-Z]+"));
Regex lower = regexes.get(0);
Regex upper = regexes.get(1);
Regex both = regexes.get(2);
System.out.println(lower.doIntersect(upper)); // false
System.out.println(both.equiv(lower.union(upper))); // true
```

```java
List<CompiledRegex> regexes = Regex.compile(Arrays.asList("[a-z]+|[A-Z]+", "[A-Z]+"));
Regex all = regexes.get(0);
Regex upper = regexes.get(1);
Regex lower = all.diff(upper);
System.out.println(lower.matches("aaa")); // true
System.out.println(lower.matches("Aaa")); // false

```

The motivating use case was detecting non-intersecting expressions. Once it can be established that a set of expressions don't intersect (that they are disjoint) it becomes possible to short-circuit evaluations. Moreover, they can be tested in any order, so it becomes possible to reorder based on matching stats. This can be especially important in cases when there is a matching of several expressions in a performance-critical path—load balances being a prototypical example.

## Note on lookaround

Lookaround constructions are transformed into an equivalent DFA operation, and the result of it trivially transformed into a NFA again for insertion into the outer expression:

Lookahead:

* `(?=B)C` → `C ∩ B.*`
* `(?!B)C` → `C - B.*`

Lookbehind:

* `A(?<=B)` → `A ∩ .*B`
* `A(?<!B)` → `A - .*B`

In the case of more than one lookaround, the transformation is applied recursively.

Lookaround expressions are supported anywhere in the regex, including inside repetition constructions. Nevertheless, there is a difference in the meaning of those lookarounds with respect to Perl-like engines. Consider:

`((?!aa)a)+`

This expression matches, in this engine, `a`, `aa`, `aaa` and so on, being effectively equivalent to `a+`, because the negative condition (`aa`) can never happen inside `a`. The fact that the expression is repeated does not change its inner logic. However, in Perl-like engines, the aforementioned regex does not match any `a` longer than one character, because those engines treat lookarounds specially, effectively running a sub-regex at the point of occurrence, irrespective of the context.

The different behavior of this engine is, of course, a direct consequence of the way lookarounds are implemented. Nevertheless, it can be argued that this definition is conceptually simpler and, more importantly, easier to reason about in the context of complex expression. Regarding practical uses, looped lookarounds like the one in the example are quite rare anyway.

## Internals

### DFA construction

The library parses the regular expressions and builds a NFA (Nondeterministic Finite Automaton) using a variation of the [Thompson algorithm](http://en.wikipedia.org/w/index.php?title=Thompson%27s_construction_algorithm&oldid=649249684). Then uses the "powerset construction" to build a DFA (Deterministic Finite Automaton). One the DFA is built, the matching algorithm is straightforward.

### Wildcards and character classes

Character classes are expanded as disjunctions before NFA creation. However, because of the number of possible Unicode code points, internaly, non-overlapping code point intervals are used to avoid disjunctions with too many alternatives.

#### Example:

* `[abc]` → `[a-c]`
* `[^efg]` → `[0-d]|[h-MAX]`
* `mno[^efg]` → `mno(0-d|h-l|m|n|o|p-MAX)`
* `.` → `[0-MAX]`

### Set operations

Intersections, unions and differences between regex are done using the "product construction". The following pages include graphical explanations of this technique:

* [http://stackoverflow.com/q/7780521/4505326](http://stackoverflow.com/q/7780521/4505326)
* [http://cs.stackexchange.com/a/7108](http://cs.stackexchange.com/a/7108)

This is a relatively straightforward algorithm that is implemented using the already generated DFA.

## Requirements

Dregex requires Java 8.

### Logging

The library uses [SLF4J](https://www.slf4j.org/) for logging, which is the most widely used pluggable logging framework for the JVM. As a policy, all logging event emitted are at TRACE level, which is below the default threshold in most logging implementations and thus completely silent by default.

### Dependencies

Dregex is written in Scala (but fully usable from Java), so it depends on the Scala runtime library (5 MB). There are two more small dependencies: [SLF4J](https://www.slf4j.org/) and [scala-parser-combinators](https://github.com/scala/scala-parser-combinators). The main jar file is about 200 KB.

## Similar efforts

* [RE2](https://github.com/google/re2) is an efficient (linear) C++ library that implements a subset of Perl features, writen by Russ Cox. The author has written a [set of articles](http://swtch.com/~rsc/regexp/regexp1.html) explaining the problem.
* [TRE](https://github.com/laurikari/tre/) is an efficient C library and command-line tool that implements POSIX-compliant and approximate (fuzzy) regex matching, using "tagged DFA". It is written by Ville Laurikari.
* [Plan 9 grep](http://swtch.com/usr/local/plan9/src/cmd/grep/) is an efficient DFA implementation that supports egrep syntax. It was written by Ken Thompson.
* [regex-tdfa](https://github.com/ChrisKuklewicz/regex-tdfa) is a "tagged DFA" implementation written in Haskell by Chris Kuklewicz.
