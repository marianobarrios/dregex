package dregex.impl;

public enum CaseNormalization {
    NoNormalization {
        @Override
        public int normalize(int codePoint) {
            return codePoint;
        }
    },

    LowerCase {
        @Override
        public int normalize(int codePoint) {
            boolean isAsciiUpperCase = codePoint >= 'A' && codePoint <= 'Z';
            // cast to prevent promotion to int and calling the wrong overload
            return isAsciiUpperCase ? codePoint + 0x20 : codePoint;
        }
    },

    UnicodeLowerCase {
        @Override
        public int normalize(int codePoint) {
            return Character.toLowerCase(codePoint);
        }
    };

    public abstract int normalize(int codePoint);
}
