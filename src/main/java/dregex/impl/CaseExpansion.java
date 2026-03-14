package dregex.impl;

public enum CaseExpansion {
    NoExpansion {
        @Override
        public int[] expand(int codePoint) {
            return new int[]{codePoint};
        }
    },

    Ascii {
        @Override
        public int[] expand(int codePoint) {
            if (codePoint >= 'A' && codePoint <= 'Z') {
                return new int[]{codePoint, codePoint + 0x20};
            } else if (codePoint >= 'a' && codePoint <= 'z') {
                return new int[]{codePoint - 0x20, codePoint};
            } else {
                return new int[]{codePoint};
            }
        }
    },

    Unicode {
        @Override
        public int[] expand(int codePoint) {
            int lower = Character.toLowerCase(codePoint);
            int upper = Character.toUpperCase(codePoint);
            if (lower == upper) {
                return new int[]{codePoint};
            }
            // Title-case characters are relatively small set in which the title case (small upper case) has its own
            // code point. This means that case insensitivity muse accept 3 (not 2) codepoints for the same pattern.
            int title = Character.toTitleCase(codePoint);
            if (title != lower && title != upper) {
                // Three-way case: upper, title, and lower are all distinct (e.g., Ǆ/ǅ/ǆ)
                return new int[]{upper, title, lower};
            }
            return new int[]{upper, lower};
        }
    };

    public abstract int[] expand(int codePoint);
}
