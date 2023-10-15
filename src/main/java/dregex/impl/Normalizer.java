package dregex.impl;

public interface Normalizer {

    CharSequence normalize(CharSequence str);

    static Normalizer combine(Normalizer first, Normalizer second) {
        return str -> second.normalize(first.normalize(str));
    }
}
