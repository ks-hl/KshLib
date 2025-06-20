package dev.kshl.kshlib.misc;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Version implements Comparable<Version> {
    private final List<Integer> version;

    public Version(String versionString) {
        if (versionString.startsWith("v")) versionString = versionString.substring(1);

        if (!versionString.matches("\\d+(\\.\\d+)*((-pre|-rc)\\d)?")) {
            throw new IllegalArgumentException("Malformed version string: " + versionString);
        }

        String[] parts = versionString.split("[.-]", 0);
        ArrayList<Integer> array = new ArrayList<>();
        for (String s : parts) {
            try {
                int part;
                if (s.startsWith("pre")) part = -1000000;
                else if (s.startsWith("rc")) part = -500000;
                else part = 0;

                s = s.replaceAll("\\D", "");
                part += Integer.parseInt(s);
                array.add(part);
            } catch (NumberFormatException ignored) {
                array.add(0);
            }
        }
        version = Collections.unmodifiableList(array);
    }

    @Override
    public int compareTo(@Nonnull Version other) {
        for (int i = 0; i < version.size() || i < other.version.size(); i++) {
            int versionIntThis = 0;
            int versionIntOther = 0;
            if (i < version.size()) versionIntThis = version.get(i);
            if (i < other.version.size()) versionIntOther = other.version.get(i);
            if (versionIntThis < versionIntOther) return -1;
            if (versionIntThis > versionIntOther) return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Version otherVersion)) return false;
        return this.version.equals(otherVersion.version);
    }

    @Override
    public int hashCode() {
        return this.version.hashCode();
    }
}