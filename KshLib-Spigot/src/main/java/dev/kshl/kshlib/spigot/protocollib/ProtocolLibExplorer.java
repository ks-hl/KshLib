package dev.kshl.kshlib.spigot.protocollib;

import com.comphenix.protocol.events.AbstractStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ProtocolLibExplorer {
    public static String explore(PacketContainer packet) {
        StringBuilder out = new StringBuilder("------------------\nMODIFIERS:");

        int i = 0;
        for (FieldAccessor field : packet.getModifier().getFields()) {
            out.append("\n").append(i).append(". ");
            out.append(field.getField().getType().getName());
            out.append(" = ");
            out.append(packet.getModifier().read(i));

            i++;
        }

        out.append("\nMETHODS:");

        out.append(exploreMethods(packet, AbstractStructure.class, 0, false));

        return out + "\n------------------";
    }

    private static String toStringLimit100(Object value) {
        String toString = String.valueOf(value);
        if (toString.length() <= 100) return toString;
        return toString.substring(0, 100) + "...";
    }

    public static String exploreMethods(Object object, Class<?> clazz, int indent, boolean forceAccessible) {
        if (indent > 50) throw new IllegalArgumentException("Recursion limit reached");
        String onePad = "| ";
        String pad = onePad.repeat(indent);
        String newLine = "\n" + pad;

        StringBuilder out = new StringBuilder();
        for (Method method : clazz.getDeclaredMethods()) {
            try {
                if (method.getParameterCount() > 0) continue;
                if (forceAccessible) {
                    method.setAccessible(true);
                } else {
                    try {
                        if (!method.canAccess(object)) continue;
                    } catch (IllegalArgumentException e) {
                        // static method
                        continue;
                    }
                }
                StringBuilder line = new StringBuilder();
                line.append(newLine).append(method.getName()).append(": ");

                StructureModifier<?> modifier;
                try {
                    modifier = (StructureModifier<?>) method.invoke(object);
                } catch (ClassCastException ignored) {
                    continue;
                }
                if (modifier.size() == 0) continue;
                line.append(modifier.size());
                for (int i1 = 0; i1 < modifier.size(); i1++) {
                    line.append(newLine).append(onePad).append(i1).append(". ");
                    line.append(toStringLimit100(modifier.read(i1)));
                }
                out.append(line);
            } catch (Throwable t) {
                out.append(newLine).append(method.getName()).append(": ");
                out.append(t.getClass().getName()).append(" (").append(toStringLimit100(t.getMessage())).append(")");
            }
        }
        return out.toString();
    }

    public static String exploreFields(Object object, Class<?> clazz, boolean forceAccessible, int recursionLimit, int indent) {
        if (recursionLimit > 3) {
            throw new IllegalArgumentException("Max recursionLimit is 3");
        }
        if (indent > recursionLimit) {
            throw new IllegalArgumentException("Recursion limit reached");
        }
        if (clazz.getSuperclass() == null) return "";
        StringBuilder out = new StringBuilder();
        String pad = "| ".repeat(indent);
        pad = "\n" + pad;
        for (Field field : clazz.getDeclaredFields()) {
            try {
                if (forceAccessible) {
                    field.setAccessible(true);
                } else {
                    try {
                        if (!field.canAccess(object)) continue;
                    } catch (IllegalArgumentException e) {
                        // static method
                        continue;
                    }
                }
                Object value = field.get(object);
                out.append(pad)
                        .append(clazz.getName())
                        .append("#")
                        .append(field.getType().getName())
                        .append(" ")
                        .append(field.getName())
                        .append(" = ")
                        .append(value);
                if (value != null && !(value instanceof Enum<?>)) {
                    out.append(exploreFields(value, value.getClass(), forceAccessible, recursionLimit, indent + 1));
                }
            } catch (Throwable t) {
                out.append(pad).append(field.getName()).append(": ");
                out.append(t.getClass().getName()).append(" (").append(toStringLimit100(t.getMessage())).append(")");
            }
        }
        if (clazz.getSuperclass() != null) {
            out.append(exploreFields(object, clazz.getSuperclass(), forceAccessible, recursionLimit, indent));
        }
        return out.toString();
    }
}
