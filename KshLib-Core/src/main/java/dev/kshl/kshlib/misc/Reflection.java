package dev.kshl.kshlib.misc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Reflection {
    public static Object getFieldValue(Object object, int index) throws IllegalAccessException {
        Field field = object.getClass().getDeclaredFields()[index];
        field.setAccessible(true);
        return field.get(object);
    }

    public static Field getFieldAccessible(Object object, int index) throws IllegalAccessException {
        Field field = object.getClass().getDeclaredFields()[index];
        field.setAccessible(true);
        return field;
    }

    public static <T> T getFieldValue(Object object, int index, Class<T> ofClass) throws NoSuchFieldException, IllegalAccessException {
        int i = 0;
        for (Field field : object.getClass().getDeclaredFields()) {
            if (!field.getType().equals(ofClass)) continue;
            if (i++ == index) {
                field.setAccessible(true);
                Object value = field.get(object);
                //noinspection unchecked
                return (T) value;
            }
        }
        throw new NoSuchFieldException();
    }

    public static Object getFieldValue(Object object, Class<?> targetClass, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field;
        try {
            field = targetClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (targetClass.getSuperclass() == null) throw e;
            return getFieldValue(object, targetClass.getSuperclass(), name);
        }
        field.setAccessible(true);
        return field.get(object);
    }

    public static Method getMethodAccessible(Object object, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = object.getClass().getMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    public static Object getMethodValue(Object object, String name, Object... parameters) throws ReflectiveOperationException {
        Class<?>[] parameterTypes;
        if (parameters == null) parameterTypes = null;
        else {
            parameterTypes = new Class<?>[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Object param = parameters[i];
                parameterTypes[i] = param == null ? Object.class : param.getClass();
            }
        }
        Method method = getMethodAccessible(object, name, parameterTypes);
        return method.invoke(object, parameters);
    }
}
