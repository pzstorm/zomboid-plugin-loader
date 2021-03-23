package dev.weary.zomboid.util.reflect;

import zombie.Lua.Event;

import java.lang.reflect.Field;

public class ReflectionUtil {
    public static Field getField(Class<?> classType, String fieldName) {
        try {
            Field declaredField = classType.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return declaredField;
        }
        catch (Exception e) {
            throw new RuntimeException("Couldn't access field " + fieldName + " of class " + classType.getName(), e);
        }
    }

    public static Object getValue(Object targetObject, Field targetField) {
        try {
            return targetField.get(targetObject);
        }
        catch (Exception e) {
            throw new RuntimeException("Couldn't read field " + targetField.getName() + " of class " + targetField.getDeclaringClass().getName(), e);
        }
    }

    public static int getIntValue(Object targetObject, Field targetField) {
        try {
            return targetField.getInt(targetObject);
        }
        catch (Exception e) {
            throw new RuntimeException("Couldn't read field " + targetField.getName() + " of class " + targetField.getDeclaringClass().getName(), e);
        }
    }
}
