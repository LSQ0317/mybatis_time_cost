package com.mybatis.timecost.mybatis;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface Dialect {
    String escapeString(String s);
    String formatBoolean(Boolean b);
    String formatNumber(Number n);
    String formatDate(LocalDate d);
    String formatTimestamp(LocalDateTime dt);
    String formatBinary(byte[] bytes);
    String nullLiteral();

    static Dialect mysql() {
        return new Dialect() {
            private String toHex(byte[] bytes) {
                StringBuilder sb = new StringBuilder(bytes.length * 2);
                for (byte b : bytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            }
            public String escapeString(String s) {
                return "'" + s.replace("'", "''") + "'";
            }
            public String formatBoolean(Boolean b) {
                if (b == null) return nullLiteral();
                return b ? "1" : "0";
            }
            public String formatNumber(Number n) {
                if (n == null) return nullLiteral();
                if (n instanceof BigDecimal) return ((BigDecimal) n).toPlainString();
                return new BigDecimal(n.toString()).toPlainString();
            }
            public String formatDate(LocalDate d) {
                if (d == null) return nullLiteral();
                return "'" + d.toString() + "'";
            }
            public String formatTimestamp(LocalDateTime dt) {
                if (dt == null) return nullLiteral();
                DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return "'" + f.format(dt) + "'";
            }
            public String formatBinary(byte[] bytes) {
                if (bytes == null) return nullLiteral();
                return "0x" + toHex(bytes);
            }
            public String nullLiteral() {
                return "NULL";
            }
        };
    }
}

