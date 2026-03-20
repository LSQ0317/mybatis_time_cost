package com.mybatis.timecost.mybatis;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public final class SqlRenderUtil {
    private SqlRenderUtil() {
    }

    public static String render(BoundSql boundSql, Configuration configuration, Dialect dialect) {
        String sql = boundSql.getSql();
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        Object parameterObject = boundSql.getParameterObject();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        MetaObject metaObject = parameterObject == null ? null : configuration.newMetaObject(parameterObject);
        StringBuilder sb = new StringBuilder();
        int paramIndex = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '?' && paramIndex < mappings.size()) {
                ParameterMapping pm = mappings.get(paramIndex++);
                Object value = getValue(pm.getProperty(), parameterObject, metaObject, boundSql, typeHandlerRegistry);
                sb.append(formatValue(value, dialect));
            } else {
                sb.append(c);
            }
        }
        return compact(sb.toString());
    }

    private static Object getValue(String property, Object parameterObject, MetaObject metaObject, BoundSql boundSql,
                                   TypeHandlerRegistry typeHandlerRegistry) {
        if (property == null) {
            return null;
        }
        if (boundSql.hasAdditionalParameter(property)) {
            return boundSql.getAdditionalParameter(property);
        }
        PropertyTokenizer propertyTokenizer = new PropertyTokenizer(property);
        if (boundSql.hasAdditionalParameter(propertyTokenizer.getName())) {
            Object additionalParameter = boundSql.getAdditionalParameter(propertyTokenizer.getName());
            if (additionalParameter == null) {
                return null;
            }
            if (propertyTokenizer.getChildren() == null) {
                return additionalParameter;
            }
            return resolveNestedValue(SystemMetaObject.forObject(additionalParameter), propertyTokenizer.getChildren());
        }
        if (metaObject == null) {
            return null;
        }
        if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            return parameterObject;
        }
        if (metaObject.hasGetter(property)) {
            return metaObject.getValue(property);
        }
        if (parameterObject instanceof Map) {
            return ((Map<?, ?>) parameterObject).get(property);
        }
        return null;
    }

    private static Object resolveNestedValue(MetaObject metaObject, String property) {
        if (metaObject == null) {
            return null;
        }
        if (property == null || property.isEmpty()) {
            return null;
        }
        if (!metaObject.hasGetter(property)) {
            return null;
        }
        return metaObject.getValue(property);
    }

    private static String formatValue(Object v, Dialect dialect) {
        if (v == null) return dialect.nullLiteral();
        if (v instanceof String) return dialect.escapeString((String) v);
        if (v instanceof Character) return dialect.escapeString(v.toString());
        if (v instanceof Boolean) return dialect.formatBoolean((Boolean) v);
        if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte || v instanceof Double || v instanceof Float || v instanceof BigDecimal) {
            return dialect.formatNumber((Number) v);
        }
        if (v instanceof java.util.Date) {
            Instant ins = Instant.ofEpochMilli(((java.util.Date) v).getTime());
            LocalDateTime dt = LocalDateTime.ofInstant(ins, ZoneId.systemDefault());
            return dialect.formatTimestamp(dt);
        }
        if (v instanceof Timestamp) {
            Instant ins = Instant.ofEpochMilli(((Timestamp) v).getTime());
            LocalDateTime dt = LocalDateTime.ofInstant(ins, ZoneId.systemDefault());
            return dialect.formatTimestamp(dt);
        }
        if (v instanceof LocalDate) return dialect.formatDate((LocalDate) v);
        if (v instanceof LocalDateTime) return dialect.formatTimestamp((LocalDateTime) v);
        if (v instanceof byte[]) return dialect.formatBinary((byte[]) v);
        return dialect.escapeString(v.toString());
    }

    private static String compact(String sql) {
        return sql.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ").trim();
    }
}
