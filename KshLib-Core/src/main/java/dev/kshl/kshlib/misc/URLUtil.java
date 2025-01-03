package dev.kshl.kshlib.misc;

public class URLUtil {
    public static String stripLeadingSlash(String line) {
        return stripSlash(line, true);
    }

    public static String stripTrailingSlash(String line) {
        return stripSlash(line, false);
    }

    private static String stripSlash(String line, boolean leading) {
        if (leading) {
            if (!line.startsWith("/")) return line;
            return line.substring(1);
        } else {
            if (!line.endsWith("/")) return line;
            return line.substring(line.length() - 1);
        }
    }

    /**
     * Combines two or more URLs together
     *
     * @param url The parts of the URL
     * @return The URLs, combined, separated by one '/' each with the trailing '/' of the last url maintained
     */
    public static String concatURL(String... url) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < url.length; i++) {
            String part = url[i];
            part = part.trim();
            part = stripLeadingSlash(part);
            if (i < url.length - 1) part = stripTrailingSlash(part);
            part = part.trim();
            if (!out.isEmpty() && !part.isEmpty()) out.append("/");
            out.append(part);
        }
        return out.toString();
    }

    /**
     * Constructs a query URL like <a href="">https://google.com?key1=value1&key2=value2</a>
     *
     * @return The constructed query URL
     */
    public static String concatQuery(String url, String... query) {
        if (query == null || query.length == 0) return url;
        StringBuilder out = new StringBuilder(url);
        for (int i = 0; i < query.length; i++) {
            out.append(i == 0 ? "?" : "&");
            out.append(query[i]);
        }
        return out.toString();
    }
}
