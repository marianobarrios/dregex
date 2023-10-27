package dregex;

import java.util.Arrays;
import java.util.List;

class SetOpsExample {

    public static void main(String[] args) {
        {
            List<Regex> regexes = Regex.compile(Arrays.asList("[a-z]+", "[A-Z]+", "[a-z]+|[A-Z]+"));
            // In Scala 2.12
            // List<CompiledRegex> regexes = Regex.compile(Arrays.asList("[a-z]+", "[A-Z]+", "[a-z]+|[A-Z]+"));
            Regex lower = regexes.get(0);
            Regex upper = regexes.get(1);
            Regex both = regexes.get(2);
            System.out.println(lower.doIntersect(upper)); // false
            System.out.println(both.equiv(lower.union(upper))); // true
        }
        {
            List<Regex> regexes = Regex.compile(Arrays.asList("[a-z]+|[A-Z]+", "[A-Z]+"));
            // In Scala 2.12
            // List<CompiledRegex> regexes = Regex.compile(Arrays.asList("[a-z]+|[A-Z]+", "[A-Z]+"));
            Regex all = regexes.get(0);
            Regex upper = regexes.get(1);
            Regex lower = all.diff(upper);
            System.out.println(lower.matches("aaa")); // true
            System.out.println(lower.matches("Aaa")); // false
        }
    }
}
