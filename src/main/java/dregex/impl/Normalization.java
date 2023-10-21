package dregex.impl;

public enum Normalization implements Normalizer {
    NoNormalization {
        @Override
        public CharSequence normalize(CharSequence str) {
            return str;
        }
    },

    LowerCase {
        @Override
        public CharSequence normalize(CharSequence str) {
            var builder = new StringBuilder(str.length());
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                boolean isAsciiUpperCase = ch >= 'A' && ch <= 'Z';
                builder.append(isAsciiUpperCase ? ch + 0x20 : ch);
            }
            return builder.toString();
        }
    },

    UnicodeLowerCase {
        @Override
        public CharSequence normalize(CharSequence str) {
            var builder = new StringBuilder();
            str.codePoints().forEach(codePoint -> {
                builder.append(Character.toLowerCase(codePoint));
            });
            return builder.toString();
        }
    },

    CanonicalDecomposition {
        @Override
        public CharSequence normalize(CharSequence str) {
            return java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        }
    };
}
