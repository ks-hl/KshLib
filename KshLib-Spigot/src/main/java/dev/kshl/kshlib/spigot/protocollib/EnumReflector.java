package dev.kshl.kshlib.spigot.protocollib;

public class EnumReflector {
    public static Object getEnum(String clazz, String name) throws ClassNotFoundException {
        for (Object enumConstant : Class.forName(clazz).getEnumConstants()) {
            if (enumConstant.toString().equalsIgnoreCase(name)) {
                return enumConstant;
            }
        }
        throw new ClassNotFoundException("ADD enum not found");
    }
}
