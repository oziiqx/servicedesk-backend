package pl.servicedesk.support;

import java.lang.reflect.Field;

public final class TestEntities {

    private TestEntities() {
    }

    public static <T> T withId(T entity, long id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
                return entity;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot set id on " + entity.getClass(), e);
            }
        }
        throw new IllegalStateException("No id field found on " + entity.getClass());
    }
}
