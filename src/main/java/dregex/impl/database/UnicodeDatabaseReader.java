package dregex.impl.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class UnicodeDatabaseReader {

    public static final class Range implements Comparable<Range> {

        public final int from;
        public final int to;

        public Range(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return from + ".." + to;
        }

        @Override
        public int compareTo(Range other) {
            return Integer.compare(from, other.from);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Range range = (Range) o;
            return from == range.from && to == range.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

    private static SortedMap<Range, String> parseDatabase(Reader database) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(database);

        SortedMap<Range, String> ret = new TreeMap<>();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            // remove comments
            int commentStart = line.indexOf("#");
            if (commentStart != -1) {
                line = line.substring(0, commentStart) + " ";
            }
            // remove spaces
            line = line.trim();

            // slip empty
            if (line.isEmpty()) {
                continue;
            }

            int semicolon = line.indexOf(";");
            String rangeStr = line.substring(0, semicolon).trim();
            String name = line.substring(semicolon + 1).trim();
            int dots = rangeStr.indexOf("..");

            if (dots != -1) {
                String fromStr = rangeStr.substring(0, dots);
                String toStr = rangeStr.substring(dots + 2);
                int from = Integer.parseInt(fromStr, 16);
                int to = Integer.parseInt(toStr, 16);
                ret.put(new Range(from, to), name);
            } else {
                int single = Integer.parseInt(rangeStr, 16);
                ret.put(new Range(single, single), name);
            }
        }
        return ret;
    }

    public static SortedMap<String, Range> getBlocks(Reader reader) throws IOException {
        SortedMap<Range, String> data = parseDatabase(reader);
        SortedMap<String, Range> ret = new TreeMap<>();
        for (Map.Entry<Range, String> entry : data.entrySet()) {
            ret.put(entry.getValue(), entry.getKey());
        }
        return ret;
    }

    public static SortedMap<String, List<Range>> getScripts(Reader reader) throws IOException {
        SortedMap<Range, String> data = parseDatabase(reader);
        SortedMap<String, List<Range>> ret = new TreeMap<>();
        for (Map.Entry<Range, String> entry : data.entrySet()) {
            List<Range> list = ret.computeIfAbsent(entry.getValue(), x -> new ArrayList<>());
            list.add(entry.getKey());
        }
        return ret;
    }

    public static SortedMap<String, List<Range>> getGeneralCategories(Reader reader) throws IOException {
        SortedMap<Range, String> data = parseDatabase(reader);
        SortedMap<String, List<Range>> ret = new TreeMap<>();
        // process categories (two letters)
        for (Map.Entry<Range, String> entry : data.entrySet()) {
            List<Range> list = ret.computeIfAbsent(entry.getValue(), x -> new ArrayList<>());
            list.add(entry.getKey());
        }
        // process category groups (first letter)
        for (Map.Entry<Range, String> entry : data.entrySet()) {
            var categoryGroup = entry.getValue().substring(0, 1); // first letter of the two
            List<Range> list = ret.computeIfAbsent(categoryGroup, x -> new ArrayList<>());
            list.add(entry.getKey());
        }
        return ret;
    }

    public static String canonicalizeBlockName(String name) {
        return name.replaceAll("[-_\\s]+", "").toUpperCase();
    }
}
