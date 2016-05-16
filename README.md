Dregex - Deterministic Regular Expression Engine
================================================

Dregex is a Scala/JVM library that implements a regular expression engine using deterministic finite automata (DFA). It supports some Perl-style features and yet retains linear matching time.

Most mainstream engines work with flavors of regular expressions based on the one that appeared Perl 5 in 1994. Those flavors include a wide range of features, which make state-machine based implementation impossible. As they rely on recursive backtracking, these engines can also have exponential matching time.

On the other hand, there is a mathematical definition of regular expressions, as they were invented by Stephen Kleene in 1956. In the most minimalistic version these expressions consist of just literal characters, alternation ("|") and repetition ("*"). They can be matched again arbitrary text of length n in O(n), using a Definite Finite Automaton (DFA). Using DFA also allows to do set operations; i.e., union, intersection and difference.

There are some features of Perl regular expressions that are impossible to express in a DFA, most notable backreferences (i.e., forcing to match the same text more than once). Nevertheless, backreferences are seldom used in practice and it is possible to select a practical subset of the Perl flavor substantially bigger than their mathematical counterpart (or the POSIX's regex) yet expressible using standard DFA.

Dregex is an attempt to implement such a subset and make a fast implementation for the Java Virtual Machine.

[![Build Status](https://travis-ci.org/marianobarrios/dregex.svg?branch=master)](https://travis-ci.org/marianobarrios/dregex)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.marianobarrios/dregex_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.marianobarrios/dregex_2.11)
[![Scaladoc](http://javadoc-badge.appspot.com/com.github.marianobarrios/dregex_2.11.svg?label=scaladoc)](http://javadoc-badge.appspot.com/com.github.marianobarrios/dregex_2.11)

Supported regex flavor
----------------------

### Supported features

* Literals
* Escaped characters
* Standard regular language constructs: `|`, `?`, `*` and `+` and parenthesis grouping.
* General quantifiers: `{n}`, `{n,m}`, `{n,}`
* Dot wildcards
* Simple character classes: `[abc]`
* Simple negated character classes: `[^abc]`
* Ranges in character classes: `[a-z]`
* Special character classes: `\w`, `\s`, `\d`.
* Negated special character classes: `\W`, `\S`, `\D`.
* Special character classes inside regular character classes: `[\d\s]`, `[\D]`
* Lookahead (positive and negative)

### Not (yet) supported

* Lookbehind

### Not supported

* Backreferences

Internals
---------

### DFA construction

The library parses the regular expressions and builds a NFA (Nondeterministic Finite Automaton) using a variation of the [Thompson algorithm](http://en.wikipedia.org/w/index.php?title=Thompson%27s_construction_algorithm&oldid=649249684). Then uses the "powerset construction" to build a DFA (Deterministic Finite Automaton). One the DFA is built, the matching algorithm is straightforward.

### Wildcards and character classes

Character classes are expanded as disjunctions before NFA creation. Respectively, wildcards are expanded as a universal class. 

Example transformations with alphabet: `abcdefgh`

* `[abc]` → `a|b|c`
* `[^abc]` → `d|e|f|g|h`
* `def[^abc]` → `def(d|e|f|g|h)`
* `.` → `a|b|c|d|e|f|g|h`
* `abc.` → `abc(a|b|c|d|e|f|g|h)`

As the alphabet can be potentially huge (such as Unicode is) something must be done to reduce the number of disjunctions:

* `[abc]` → `a|b|c`
* `[^abc]` → `<other_char>`
* `def[^abc]` → `def(d|e|f|<other_char>)`
* `.` → `<other_char>`
* `abc.` → `abc(a|b|c|<other_char>)`

Where `<other_char>` is a special metacharacter that matches any of the characters of the alphabet not present in the regex. Note that with this technique knowing the whole alphabet explicitly is not needed. Care must be taken when the regex is meant to be used for an operation with another regex (such as intersection or difference). In this case, `<other_char>` must match only the characters present in neither regex. 

#### Example:

Regex space: `[abc]` and `[^cd]`

Characters present in any regex: `abcd`

* `[abc]` → `a|b|c`
* `[^cd]` → `a|b|<other_char>`

### Set operations

Intersections, unions and differences between regex are done using the "product construction". The following pages include graphical explanations of this technique:

* [http://stackoverflow.com/q/7780521/4505326](http://stackoverflow.com/q/7780521/4505326)
* [http://cs.stackexchange.com/a/7108](http://cs.stackexchange.com/a/7108)

This is a relatively straightforward algorithm that is implemented using the already generated DFA.

### Lookahead

Lookaround constructions are transformed into an equivalent DFA operation, and the result of it trivially transformed into a NFA again for insertion into the outer expression:

* `(?=B)C` → `C ∩ B.*`
* `(?!B)C` → `C - B.*`

In the case of more than one lookaround, the transformation is applied recursively.

Lookaround expressions are supported anywhere in the regex, including inside repetition constructions. Nevertheless, there is a difference in the meaning of those lookarounds with respect to Perl-like engines. Consider:

`((?!aa)a)+`

This expression matches, in this engine, `a`, `aa`, `aaa` and so on, being effectively equivalent to `a+`, because the negative condition (`aa`) can never happen inside `a`. The fact that the expression is repeated does not change its inner logic. However, in Perl-like engines, the aforementioned regex does not match any `a` longer than one character, because those engines treat lookarounds specially, effectively running a sub-regex at the point of occurrence, irrespective of the context.

The different behavior of this engine is, of course, a direct consequence of the way lookarounds are implemented. Nevertheless, it can be argued that this definition is conceptually simpler and, more importantly, easier to reason about in the context of complex expression. Regarding practical uses, looped lookarounds like the one in the example are quite rare anyway.
 

Similar efforts
---------------

* [RE2](https://github.com/google/re2) is an efficient (linear) C++ library that implements a subset of Perl features, writen by Russ Cox. The author has written a [set of articles](http://swtch.com/~rsc/regexp/regexp1.html) explaining the problem.
* [TRE](https://github.com/laurikari/tre/) is an efficient C library and command-line tool that implements POSIX-compliant and approximate (fuzzy) regex matching. It is written by Ville Laurikari.
* [Plan 9 grep](http://swtch.com/usr/local/plan9/src/cmd/grep/) is an efficient DFA implementation that supports egrep syntax. It was written by Ken Thompson.
