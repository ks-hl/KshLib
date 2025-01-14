package dev.kshl.kshlib.misc;

import dev.kshl.kshlib.function.ThrowingConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

public class FileUtil {
    @SuppressWarnings("UnusedReturnValue")
    public static boolean createNewFile(File file) throws IOException {
        boolean created = file.getParentFile().mkdirs();
        created = file.createNewFile() | created;
        return created;
    }

    public static String read(File file) {
        StringBuilder builder = new StringBuilder();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNext()) {
                if (!builder.isEmpty()) builder.append("\n");
                builder.append(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            return null;
        }
        return builder.toString();
    }

    public static void write(File file, String content) throws IOException {
        createNewFile(file);
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(content);
            writer.flush();
        }
    }

    public static String getSHA256Hash(File file) throws IOException {
        return getSHA256Hash(file.getAbsolutePath());
    }

    public static String getSHA256Hash(String filePath) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }

        try (InputStream is = new FileInputStream(filePath); DigestInputStream dis = new DigestInputStream(is, md)) {
            //noinspection StatementWithEmptyBody
            while (dis.read() != -1) ; //empty loop to clear the data
            md = dis.getMessageDigest();
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static boolean delete(File file) {
        if (!file.exists()) return false;

        walkFileTree(file, -1, file1 -> {
            if (file1.isFile()) file1.delete();
        });
        walkFileTree(file, -1, File::delete);

        return !file.exists();
    }

//    public static void walkFileTree(File file, int depthLimit, Consumer<File> fileConsumer) throws IOException {
//        walkFileTree(file, depthLimit, (ThrowingConsumer<File, IOException>) fileConsumer::accept);
//    }

    public static <E extends Exception> void walkFileTree(File file, int depthLimit, ThrowingConsumer<File, E> fileConsumer) throws E {
        fileConsumer.accept(file);

        if (depthLimit == 0) return;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return;
            for (File subFile : files) {
                walkFileTree(subFile, depthLimit - 1, fileConsumer);
            }
        }
    }

    public static class CSV {
        private final List<String> headers;
        private final List<Row> contents = new ArrayList<>();

        public CSV(String contentString) {
            String[] lines = contentString.split("\n");
            this.headers = StringUtil.splitCommasExceptQuotes(lines[0]);
            for (int i = 1; i < lines.length; i++) contents.add(new Row(lines[i]));
        }

        public List<String> getHeaders() {
            return Collections.unmodifiableList(headers);
        }

        public List<Row> getContents() {
            return Collections.unmodifiableList(contents);
        }

        public class Row {
            private final LinkedHashMap<String, String> values = new LinkedHashMap<>();

            public Row(String line) {
                var spl = StringUtil.splitCommasExceptQuotes(line);
                for (int i = 0; i < headers.size(); i++) {
                    values.put(headers.get(i), i >= spl.size() ? null : spl.get(i));
                }
            }

            public String get(String key, String def) {
                String val = values.get(key);
                return val == null ? def : val;
            }

            public String get(String key) {
                if (!values.containsKey(key)) throw new IllegalArgumentException("No value for " + key);
                return get(key, null);
            }

            public LinkedHashMap<String, String> getValues() {
                return values;
            }

            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                for (String header : headers) {
                    if (!builder.isEmpty()) builder.append(", ");
                    builder.append(header).append("=").append(get(header, null));
                }
                return builder.toString();
            }
        }

        // TODO toFile
    }


    /**
     * Gets a file in the specified directory that does not exist, appending (1), (2), etc. until the file does not exist
     *
     * @param directory    The parent directory of the intended file location. (e.g. for /var/log/log.txt specify just `/var/log/`)
     * @param name         The name of the file without an extension. (e.g. for /var/log/log.txt specify just `log`)
     * @param extension    The file type of the file, or null for no extension (e.g. for /var/log/log.txt specify just `txt`)
     * @param suffixFormat The format to which String.format will be applied with the integer to create the unique suffix.
     * @return The file, guaranteed to not exist
     */
    public static File getFirstNewFile(File directory, String name, @Nullable String extension, @Nonnull String suffixFormat) {
        File out;
        String suffix = extension == null ? "" : ("." + extension);
        int i = 0;
        do {
            String increment = i == 0 ? "" : String.format(suffixFormat, i);
            out = new File(directory, name + increment + suffix);
            i++;
        } while (out.exists() && i < 10000);
        if (out.exists()) {
            throw new IllegalStateException("Failed to find new file after 10000 attempts");
        }
        return out;
    }

    public static File getFirstNewFile(File directory, String name, @Nullable String extension) {
        return getFirstNewFile(directory, name, extension, " (%s)");
    }
}
