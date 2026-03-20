package com.mybatis.timecost.idea;

public final class SqlEvent {
    private final long receivedAt;
    private final String sql;
    private final Long durationMs;
    private final String mapperId;
    private final String threadName;

    public SqlEvent(long receivedAt, String sql, Long durationMs, String mapperId, String threadName) {
        this.receivedAt = receivedAt;
        this.sql = sql;
        this.durationMs = durationMs;
        this.mapperId = mapperId;
        this.threadName = threadName;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public String getSql() {
        return sql;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getMapperId() {
        return mapperId;
    }

    public String getThreadName() {
        return threadName;
    }
}
