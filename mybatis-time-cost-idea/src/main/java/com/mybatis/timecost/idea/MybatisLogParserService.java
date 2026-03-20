package com.mybatis.timecost.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(Service.Level.APP)
public final class MybatisLogParserService {
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^\\[(?<timestamp>[^\\]]+)]\\s*\\[(?<thread>[^\\]]+)]\\s*\\[(?<mapper>[^\\]]*)]\\s*:\\s*(?<payload>.*)$"
    );
    private static final Pattern TIMESTAMP_PREFIX_PATTERN = Pattern.compile(
            "^(?<timestamp>\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?(?:[+-]\\d{2}:?\\d{2}|Z)?)"
    );
    private static final Pattern PREPARING_PATTERN = Pattern.compile("^==>\\s+Preparing:\\s*(?<sql>.+)$");
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("^==>\\s+Parameters:\\s*(?<params>.+)$");
    private static final Pattern TOTAL_PATTERN = Pattern.compile("^<==\\s+Total:\\s*(?<total>.+)$");
    private static final DateTimeFormatter[] TIME_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
    };

    private final Map<String, PendingSql> pendingSqlMap = new ConcurrentHashMap<>();

    public static MybatisLogParserService getInstance() {
        return ApplicationManager.getApplication().getService(MybatisLogParserService.class);
    }

    public void acceptLine(Project project, String line) {
        SqlSettingsState settings = SqlSettingsState.getInstance();
        if (!settings.isCaptureEnabled() || !settings.isLogCaptureEnabled()) {
            return;
        }

        Matcher matcher = LOG_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            return;
        }

        String mapper = blankToNull(matcher.group("mapper"));
        if (mapper == null) {
            return;
        }
        String threadName = matcher.group("thread").trim();
        String payload = matcher.group("payload").trim();
        Long timestamp = parseTimestampMillis(matcher.group("timestamp").trim());
        String key = threadName + "|" + mapper;

        Matcher preparingMatcher = PREPARING_PATTERN.matcher(payload);
        if (preparingMatcher.matches()) {
            PendingSql pendingSql = new PendingSql();
            pendingSql.mapperId = mapper;
            pendingSql.threadName = threadName;
            pendingSql.projectName = project.getName();
            pendingSql.sqlTemplate = preparingMatcher.group("sql").trim();
            pendingSql.preparingAt = timestamp;
            pendingSqlMap.put(key, pendingSql);
            return;
        }

        Matcher parametersMatcher = PARAMETERS_PATTERN.matcher(payload);
        if (parametersMatcher.matches()) {
            PendingSql pendingSql = pendingSqlMap.get(key);
            if (pendingSql != null) {
                pendingSql.parameters = parseParameterList(parametersMatcher.group("params").trim());
            }
            return;
        }

        Matcher totalMatcher = TOTAL_PATTERN.matcher(payload);
        if (totalMatcher.matches()) {
            PendingSql pendingSql = pendingSqlMap.remove(key);
            if (pendingSql == null || pendingSql.sqlTemplate == null) {
                return;
            }
            Long durationMs = null;
            if (pendingSql.preparingAt != null && timestamp != null && timestamp >= pendingSql.preparingAt) {
                durationMs = timestamp - pendingSql.preparingAt;
            }
            String renderedSql = renderSql(pendingSql.sqlTemplate, pendingSql.parameters);
            SqlEventStore.getInstance().addEvent(new SqlEvent(
                    System.currentTimeMillis(),
                    renderedSql,
                    durationMs,
                    pendingSql.mapperId,
                    pendingSql.threadName
            ));
        }
    }

    private static String renderSql(String sqlTemplate, List<ParameterValue> parameters) {
        if (sqlTemplate == null || parameters == null || parameters.isEmpty()) {
            return compact(sqlTemplate);
        }
        StringBuilder sb = new StringBuilder();
        int paramIndex = 0;
        for (int i = 0; i < sqlTemplate.length(); i++) {
            char c = sqlTemplate.charAt(i);
            if (c == '?' && paramIndex < parameters.size()) {
                sb.append(formatParameter(parameters.get(paramIndex++)));
            } else {
                sb.append(c);
            }
        }
        return compact(sb.toString());
    }

    private static String formatParameter(ParameterValue parameter) {
        if (parameter == null || parameter.value == null || "null".equalsIgnoreCase(parameter.value)) {
            return "NULL";
        }
        String type = parameter.type == null ? "" : parameter.type.toLowerCase();
        if (type.contains("int") || type.contains("long") || type.contains("short")
                || type.contains("byte") || type.contains("double") || type.contains("float")
                || type.contains("bigdecimal") || type.contains("biginteger")) {
            return parameter.value;
        }
        if (type.contains("bool")) {
            return parameter.value;
        }
        return "'" + parameter.value.replace("'", "''") + "'";
    }

    private static List<ParameterValue> parseParameterList(String text) {
        List<ParameterValue> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        for (String part : splitParameters(text)) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int idx = trimmed.lastIndexOf('(');
            if (idx > 0 && trimmed.endsWith(")")) {
                result.add(new ParameterValue(trimmed.substring(0, idx), trimmed.substring(idx + 1, trimmed.length() - 1)));
            } else {
                result.add(new ParameterValue(trimmed, null));
            }
        }
        return result;
    }

    private static List<String> splitParameters(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && depth > 0) {
                depth--;
            }
            if (c == ',' && depth == 0) {
                result.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    private static Long parseTimestampMillis(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Matcher matcher = TIMESTAMP_PREFIX_PATTERN.matcher(text);
        if (matcher.find()) {
            text = matcher.group("timestamp");
        }
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                if (formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                    return OffsetDateTime.parse(text, formatter).toInstant().toEpochMilli();
                }
                LocalDateTime dateTime = LocalDateTime.parse(text, formatter);
                return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static String compact(String sql) {
        return sql == null ? "" : sql.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ").trim();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class PendingSql {
        private String mapperId;
        private String threadName;
        private String projectName;
        private String sqlTemplate;
        private Long preparingAt;
        private List<ParameterValue> parameters = new ArrayList<>();
    }

    private static final class ParameterValue {
        private final String value;
        private final String type;

        private ParameterValue(String value, String type) {
            this.value = Objects.requireNonNullElse(value, "").trim();
            this.type = type;
        }
    }
}
