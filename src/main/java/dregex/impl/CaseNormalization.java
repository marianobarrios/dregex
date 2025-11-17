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
