package dev.kshl.kshlib.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class YamlConfig {

    @Nullable
    private final YamlConfig parent;
    @Nullable
    private final String key;
    @Nullable
    private final File file;
    private final Supplier<InputStream> inputStreamSupplier;
    private DataMap data;
    private boolean unsavedChanges;

    private YamlConfig() {
        this.parent = null;
        this.key = null;
        this.file = null;
        this.inputStreamSupplier = null;
        data = new DataMap();
    }

    public YamlConfig(@Nullable File file, @Nullable Supplier<InputStream> inputStreamSupplier) {
        this(null, null, file, inputStreamSupplier);
    }

    private YamlConfig(YamlConfig parent, String key) {
        this(parent, key, null, null);
        initializeDataMap();
    }

    public static YamlConfig empty() {
        return new YamlConfig();
    }

    private YamlConfig(@Nullable YamlConfig parent, @Nullable String key, @Nullable File file, @Nullable Supplier<InputStream> inputStreamSupplier) {
        if (parent == null && file == null && inputStreamSupplier == null) {
            throw new NullPointerException("file and inputStreamSupplier cannot both be null.");
        }
        this.parent = parent;
        this.key = key;
        this.file = file;
        this.inputStreamSupplier = inputStreamSupplier;
    }

    public void initializeDataMap() {
        this.data = new DataMap();
    }

    public YamlConfig load() throws IOException {
        if (file == null) {
            if (inputStreamSupplier == null) return this; // empty
            return loadFromStream(inputStreamSupplier.get());
        }
        if (!file.exists()) {
            if (inputStreamSupplier != null) {
                //noinspection ResultOfMethodCallIgnored
                file.toPath().toAbsolutePath().getParent().toFile().mkdirs();
                InputStream inputStream = inputStreamSupplier.get();
                if (inputStream == null) {
                    throw new NullPointerException("InputStream is null");
                }
                Files.copy(inputStream, file.toPath());
            } else {
                throw new FileNotFoundException();
            }
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return loadFromStream(fileInputStream);
        }
    }

    private YamlConfig loadFromStream(InputStream in) {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()), new Representer(new DumperOptions()), new DumperOptions(), new Resolver() {
            @Override
            public void addImplicitResolver(Tag tag, Pattern regexp, String first, int limit) {
                if (tag.equals(Tag.BOOL)) {
                    regexp = Pattern.compile("^(?:true|True|TRUE|false|False|FALSE)$");
                    first = "tTfF";
                }
                super.addImplicitResolver(tag, regexp, first, limit);
            }
        });
        this.data = new DataMap(yaml.load(in));
        return this;
    }

    public void save() throws IOException {
        saveCopyTo(file);
        unsavedChanges = false;
    }

    public void saveCopyTo(File file) throws IOException {
        Objects.requireNonNull(file, "No file to save to");
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        try (FileWriter writer = new FileWriter(file)) {
            new Yaml(options).dump(data, writer);
        }
    }

    public Optional<Object> get(String key) {
        return getResult(key).value();
    }

    public YamlResult<Object> getResult(String key) {
        return get(key, o -> true, o -> o, "Object");
    }

    public Object getOrSet(String key, Object def) {
        return getOrSet(key, s -> true, o -> o, def);
    }

    public Optional<String> getString(String key) {
        return getStringResult(key).value();
    }

    public YamlResult<String> getStringResult(String key) {
        return get(key, s -> s instanceof String, o -> (String) o, "String");
    }

    public String getStringOrSet(String key, String def) {
        return getOrSet(key, s -> s instanceof String, o -> (String) o, def);
    }

    public Optional<Integer> getInt(String key) {
        return getIntResult(key).value();
    }

    public YamlResult<Integer> getIntResult(String key) {
        return get(key, s -> s instanceof Integer, o -> (Integer) o, "Integer");
    }

    public Integer getIntOrSet(String key, Integer def) {
        return getOrSet(key, s -> s instanceof Integer, o -> (Integer) o, def);
    }

    public Optional<Long> getLong(String key) {
        return getLongResult(key).value();
    }

    public YamlResult<Long> getLongResult(String key) {
        return get(key, s -> s instanceof Long || s instanceof Integer, o -> {
            if (o instanceof Integer i) {
                return i.longValue();
            } else {
                return (Long) o;
            }
        }, "Long");
    }

    public Long getLongOrSet(String key, Long def) {
        return getOrSet(key, s -> s instanceof Long || s instanceof Integer, o -> {
            if (o instanceof Integer i) {
                return i.longValue();
            } else {
                return (Long) o;
            }
        }, def);
    }

    public Optional<Double> getDouble(String key) {
        return getDoubleResult(key).value();
    }

    public YamlResult<Double> getDoubleResult(String key) {
        return get(key, s -> s instanceof Double || s instanceof Integer, o -> {
            if (o instanceof Double d) return d;
            if (o instanceof Integer i) return (double) i;
            throw new IllegalArgumentException("Wrong type");
        }, "Double");
    }

    public Double getDoubleOrSet(String key, Double def) {
        return getOrSet(key, s -> s instanceof Double, o -> (Double) o, def);
    }

    public Optional<Boolean> getBoolean(String key) {
        return getBooleanResult(key).value();
    }

    public YamlResult<Boolean> getBooleanResult(String key) {
        return get(key, s -> s instanceof Boolean, o -> (Boolean) o, "Boolean");
    }

    public Boolean getBooleanOrSet(String key, Boolean def) {
        return getOrSet(key, s -> s instanceof Boolean, o -> (Boolean) o, def);
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public Optional<List<String>> getStringList(String key) {
        return getStringListResult(key).value();
    }

    public YamlResult<List<String>> getStringListResult(String key) {
        return get(key, o -> o instanceof Collection<?>, o -> {
            List<String> outList = new ArrayList<>();
            for (Object element : (Collection<?>) o) {
                outList.add(Objects.toString(element));
            }
            return outList;
        }, "List of Strings");
    }

    public Optional<List<YamlConfig>> getSectionList(String key) {
        return getSectionListResult(key).value();
    }

    public YamlResult<List<YamlConfig>> getSectionListResult(String key) {
        AtomicInteger counter = new AtomicInteger();
        return get(key, o -> o instanceof Collection<?>, o -> {
            List<YamlConfig> outList = new ArrayList<>();
            for (Object element : (Collection<?>) o) {
                if (!(element instanceof LinkedHashMap<?, ?> dataMap)) continue;
                YamlConfig config = new YamlConfig(this, key + "[" + counter.getAndIncrement() + "]");
                config.data = new DataMap(dataMap);
                outList.add(config);
            }
            return outList;
        }, "List of YAML Sections");
    }

    public Optional<YamlConfig> getSection(String key) {
        return getSectionResult(key).value();
    }

    public YamlResult<YamlConfig> getSectionResult(String key) {
        return get(key, o -> o instanceof DataMap, o -> {
            YamlConfig config = new YamlConfig(this, key);
            config.data = (DataMap) o;
            return config;
        }, "YAML Section");
    }

    public YamlConfig getOrCreateSection(String key) {
        var opt = getSection(key);
        if (opt.isPresent()) return opt.get();
        YamlConfig out = new YamlConfig(this, key);
        set(key, out);
        return out;
    }

    public <E extends Enum<E>> Optional<E> getEnum(String key, Class<E> enumClass) {
        return getEnumResult(key, enumClass).value();
    }

    public <E extends Enum<E>> YamlResult<E> getEnumResult(String key, Class<E> enumClass) {
        Objects.requireNonNull(enumClass, "enumClass");
        return get(
                key,
                o -> o instanceof String,
                o -> Enum.valueOf(enumClass, o.toString().trim().toUpperCase(Locale.ROOT)),
                "Enum " + enumClass.getSimpleName()
        );
    }

    public void set(String key, Object value) {
        checkKey(key);

        if (value instanceof YamlConfig yamlConfig) {
            set(key, yamlConfig.data);
            return;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            boolean anyChange = false;
            for (Object o : list) {
                if (o instanceof YamlConfig yamlConfig) {
                    out.add(yamlConfig.data);
                    anyChange = true;
                } else {
                    out.add(o);
                }
            }
            if (anyChange) {
                set(key, out);
                return;
            }
        }
        if (value instanceof Enum<?>) {
            set(key, String.valueOf(value));
            return;
        }

        boolean change = false;
        if (key.contains(".")) {
            int index = key.indexOf(".");
            String sectionKey = key.substring(0, index);
            var opt = getSection(sectionKey);
            YamlConfig section;
            if (opt.isEmpty()) {
                if (value == null) return;
                section = new YamlConfig(this, key);
                change = !Objects.equals(data.put(sectionKey, section.data), section.data);
            } else section = opt.get();

            key = key.substring(index + 1);
            section.set(key, value);
            if (section.isEmpty()) {
                change = data.remove(sectionKey) != null;
            }
        } else {
            if (value == null) {
                change = data.remove(key) != null;
            } else {
                change = !Objects.equals(data.put(key, value), value);
            }
        }
        if (change) setUnsavedChanges();
    }

    @Nullable
    public String getKey() {
        return key;
    }

    @Nullable
    public String getFullKey() {
        String key = this.key;
        if (key == null) return null;

        if (parent != null) {
            String parentKey = parent.getFullKey();
            if (parentKey != null) {
                return parentKey + "." + key;
            }
        }
        return key;
    }

    private String getFullKey(String subKey) {
        String key = this.getFullKey();
        if (key == null) {
            return subKey;
        }

        return key + "." + subKey;
    }

    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    public Set<String> getKeys(boolean deep) {
        Set<String> keys = new HashSet<>(data.keySet());
        if (!deep) {
            keys.removeIf(key -> key.contains("."));
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof YamlConfig yamlConfig)) return false;
        return Objects.equals(data, yamlConfig.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        var data = this.data;
        if (data == null) return "YamlConfig[null]";
        StringBuilder builder = new StringBuilder();
        toString(builder, 0, data, false);
        return builder.toString();
    }

    private <T> YamlResult<T> get(String key, Predicate<Object> predicateInstanceOf, Function<Object, T> cast, String expectedType) {
        checkKey(key);
        int index = key.indexOf(".");
        if (index > 0) {
            YamlConfig section = getSection(key.substring(0, index)).orElse(null);
            if (section == null) return YamlResult.empty(key, expectedType);
            return section.get(key.substring(index + 1), predicateInstanceOf, cast, expectedType);
        }
        Optional<Object> rawValue = flatGet(key);
        Optional<T> value;
        try {
            value = rawValue.filter(predicateInstanceOf).map(cast);
        } catch (IllegalArgumentException e) {
            value = Optional.empty();
        }
        return new YamlResult<>(getFullKey(key), value, rawValue, expectedType);
    }

    private Optional<Object> flatGet(String key) {
        if (data == null) return Optional.empty();
        return Optional.ofNullable(data.get(key));
    }

    private <T> T getOrSet(String key, Predicate<Object> predicateInstanceOf, Function<Object, T> cast, T def) {
        return get(key, predicateInstanceOf, cast, null).orElseGet(() -> {
            set(key, def);
            return def;
        });
    }

    private void checkKey(String key) {
        if (key == null) throw new IllegalArgumentException("Key can not be null");
        if (key.endsWith(".") || key.startsWith(".")) throw new IllegalArgumentException("Key can not start or end with '.'");
        if (key.isBlank()) throw new IllegalArgumentException("Key can not be blank");
    }

    private void setUnsavedChanges() {
        if (parent != null) parent.setUnsavedChanges();
        unsavedChanges = true;
    }

    private void toString(StringBuilder builder, int indent, Map<?, ?> data, boolean skipFirstIndent) {
        if (data == null) return;
        boolean first = true;
        for (Map.Entry<?, ?> entry : data.entrySet()) {
            if (!first || !skipFirstIndent) {
                builder.append(" ".repeat(indent));
            }
            first = false;
            builder.append(entry.getKey());
            builder.append(": ");
            if (entry.getValue() instanceof DataMap map) {
                if (map.isEmpty()) {
                    builder.append("[]");
                } else {
                    builder.append("\n");
                    toString(builder, indent + 2, map, false);
                }
            } else if (entry.getValue() instanceof List<?> list) {
                if (list.isEmpty()) {
                    builder.append("[]");
                } else {
                    String indentString = " ".repeat(indent + 2);
                    builder.append("\n");
                    for (Object o : list) {
                        builder.append(indentString);
                        builder.append("- ");
                        if (o instanceof Map<?, ?> dataMap) {
                            toString(builder, indent + 4, dataMap, true);
                        } else {
                            builder.append(o.toString());
                        }
                    }
                }
            } else {
                builder.append(entry.getValue()).append("\n");
            }
        }
    }

    public static final class DataMap extends LinkedHashMap<String, Object> {
        public DataMap(LinkedHashMap<?, ?> handle) {
            if (handle == null) return;
            for (Map.Entry<?, ?> entry : handle.entrySet()) {
                Object val = entry.getValue();
                if (entry.getValue() instanceof LinkedHashMap<?, ?> linkedHashMap) {
                    val = new DataMap(linkedHashMap);
                }
                put(Objects.toString(entry.getKey()), val);
            }
        }

        public DataMap() {
        }
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public void delete() {
        //noinspection ResultOfMethodCallIgnored
        Objects.requireNonNull(file).delete();
    }

    public @Nullable File getFile() {
        return file;
    }

    @Nullable
    YamlConfig getParent() {
        return parent;
    }
}
