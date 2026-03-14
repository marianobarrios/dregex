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
            } else if (codePoint == lower) {
                return new int[]{upper, codePoint};
            } else {
                return new int[]{codePoint, lower};
            }
        }
    };

    public abstract int[] expand(int codePoint);
}
