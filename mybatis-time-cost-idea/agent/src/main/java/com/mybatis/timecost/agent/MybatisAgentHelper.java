package com.mybatis.timecost.agent;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

public final class MybatisAgentHelper {
    private static volatile AgentSqlEventSender sender = new AgentSqlEventSender("127.0.0.1", 17777);

    private MybatisAgentHelper() {
    }

    public static void initialize(AgentConfig config) {
        sender = new AgentSqlEventSender(config.host(), config.port());
        AgentDebugLog.info("Initialized MyBatis agent helper with host=" + config.host() + ", port=" + config.port());
    }

    public static long enter() {
        return System.nanoTime();
    }

    public static void exit(long startedAtNs, Object[] args) {
        if (args == null || args.length == 0 || !(args[0] instanceof MappedStatement)) {
            AgentDebugLog.info("Skip agent exit: no MappedStatement in invocation arguments");
            return;
        }
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args.length > 1 ? args[1] : null;
        BoundSql boundSql;
        if (args.length >= 5 && args[4] instanceof BoundSql) {
            boundSql = (BoundSql) args[4];
        } else {
            boundSql = ms.getBoundSql(parameter);
        }
        long durationMs = (System.nanoTime() - startedAtNs) / 1_000_000;
        String renderedSql = AgentSqlRenderUtil.render(boundSql, ms.getConfiguration(), AgentDialect.mysql());
        AgentDebugLog.info("Captured SQL via javaagent, mapperId=" + ms.getId() + ", durationMs=" + durationMs);
        sender.send(renderedSql, durationMs, ms.getId(), Thread.currentThread().getName());
    }
}
