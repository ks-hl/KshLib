package dev.kshl.kshlib.misc;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class TreeSetString extends TreeSet<String> {
    public TreeSetString() {
        super();
    }

    public TreeSetString(Comparator<String> comparator) {
        super(comparator);
    }

    /**
     * Method to retrieve a sublist containing only the elements which start with a given string
     */
    public SortedSet<String> getSubList(String prefix) {
        return subSet(prefix, prefix + Character.MAX_VALUE);
    }

    public static class CaseInsensitive extends TreeSetString {
        public CaseInsensitive() {
            super(Comparator.comparing(String::toLowerCase));
        }
    }

    @Override
    public boolean add(String s) {
        return super.add(Objects.requireNonNull(s));
    }
}
