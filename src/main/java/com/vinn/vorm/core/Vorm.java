package com.vinn.vorm.core;

import com.vinn.vorm.annotations.Entity;
import com.vinn.vorm.annotations.Id;
import com.vinn.vorm.config.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Vorm {

    private final JdbcTemplate jdbcTemplate;

    public Vorm(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public <T> void save(T entity) {
        Class<?> clazz = entity.getClass();
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + clazz.getSimpleName() + " is not an Entity.");
        }
        Field idField = getIdField(clazz);
        idField.setAccessible(true);
        Object idValue;
        try {
            idValue = idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access ID field", e);
        }
        if (idValue != null && findById(clazz, idValue) != null) {
            update(entity);
        } else {
            insert(entity);
        }
    }

    public <T> T findById(Class<T> clazz, Object id) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + clazz.getSimpleName() + " is not an Entity.");
        }
        String tableName = getTableName(clazz);
        Field idField = getIdField(clazz);
        String idColumnName = idField.getName();
        String sql = "SELECT * FROM " + tableName + " WHERE " + idColumnName + " = ?";
        return jdbcTemplate.queryForObject(sql, rs -> mapResultSetToEntity(rs, clazz), id);
    }

    public <T> List<T> findAll(Class<T> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + clazz.getSimpleName() + " is not an Entity.");
        }
        String tableName = getTableName(clazz);
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql, rs -> mapResultSetToEntity(rs, clazz));
    }

    public <T> void delete(T entity) {
        Class<?> clazz = entity.getClass();
        Field idField = getIdField(clazz);
        idField.setAccessible(true);
        try {
            Object idValue = idField.get(entity);
            deleteById(clazz, idValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access ID for deletion", e);
        }
    }

    public <T> void deleteById(Class<T> clazz, Object id) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + clazz.getSimpleName() + " is not an Entity.");
        }
        String tableName = getTableName(clazz);
        String idColumnName = getIdField(clazz).getName();
        String sql = "DELETE FROM " + tableName + " WHERE " + idColumnName + " = ?";
        int affectedRows = jdbcTemplate.update(sql, id);
        if (affectedRows > 0) {
            System.out.println("Successfully deleted " + clazz.getSimpleName() + " with ID: " + id);
        }
    }
    
    private <T> void insert(T entity) {
        Class<?> clazz = entity.getClass();
        String tableName = getTableName(clazz);
        List<Field> fields = getPersistableFields(clazz);
        String columns = fields.stream().map(Field::getName).collect(Collectors.joining(", "));
        String placeholders = fields.stream().map(f -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
        Object[] params = fields.stream().map(field -> getFieldValue(field, entity)).toArray();
        jdbcTemplate.update(sql, params);
        System.out.println("Successfully inserted entity: " + entity);
    }
    
    private <T> void update(T entity) {
        Class<?> clazz = entity.getClass();
        String tableName = getTableName(clazz);
        Field idField = getIdField(clazz);
        List<Field> updatableFields = getUpdatableFields(clazz);
        String setClause = updatableFields.stream().map(field -> field.getName() + " = ?").collect(Collectors.joining(", "));
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + idField.getName() + " = ?";
        List<Object> paramsList = updatableFields.stream().map(field -> getFieldValue(field, entity)).collect(Collectors.toList());
        paramsList.add(getFieldValue(idField, entity));
        jdbcTemplate.update(sql, paramsList.toArray());
        System.out.println("Successfully updated entity: " + entity);
    }

    private <T> T mapResultSetToEntity(ResultSet rs, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : getPersistableFields(clazz)) {
                field.setAccessible(true);
                Object value = rs.getObject(field.getName());
                field.set(instance, value);
            }
            return instance;
        } catch (SQLException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Failed to map ResultSet to entity", e);
        }
    }

    private Object getFieldValue(Field field, Object entity) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access field value: " + field.getName(), e);
        }
    }

    private String getTableName(Class<?> clazz) {
        Entity entityAnnotation = clazz.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();
        return tableName.isEmpty() ? clazz.getSimpleName().toLowerCase() + "s" : tableName;
    }

    private Field getIdField(Class<?> clazz) {
        return Stream.of(clazz.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Id.class)).findFirst().orElseThrow(() -> new IllegalArgumentException("Entity " + clazz.getSimpleName() + " has no @Id field."));
    }
    
    private List<Field> getPersistableFields(Class<?> clazz) {
         return Arrays.asList(clazz.getDeclaredFields());
    }

    private List<Field> getUpdatableFields(Class<?> clazz) {
        return Stream.of(clazz.getDeclaredFields()).filter(f -> !f.isAnnotationPresent(Id.class)).collect(Collectors.toList());
    }
}