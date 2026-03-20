package com.mybatis.timecost.mybatis;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.Properties;

@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class TimeCostInterceptor implements Interceptor {
    private static final ThreadLocal<Long> PREPARE_NS = new ThreadLocal<>();
    private Dialect dialect = Dialect.mysql();
    private SqlEventSender sender = SqlEventSender.defaultSender();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (invocation.getTarget() instanceof StatementHandler) {
            long s = System.nanoTime();
            try {
                return invocation.proceed();
            } finally {
                long e = System.nanoTime();
                PREPARE_NS.set(e - s);
            }
        }
        if (invocation.getTarget() instanceof Executor) {
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args.length > 1 ? args[1] : null;
            BoundSql boundSql = resolveBoundSql(ms, parameter, args);
            String sqlTemplate = boundSql.getSql();
            long startedAt = System.currentTimeMillis();
            long s = System.nanoTime();
            try {
                return invocation.proceed();
            } finally {
                long e = System.nanoTime();
                long endedAt = System.currentTimeMillis();
                long durationMs = (e - s) / 1_000_000;
                try {
                    String sqlRendered = SqlRenderUtil.render(boundSql, ms.getConfiguration(), dialect);
                    String mapperId = ms.getId();
                    String thread = Thread.currentThread().getName();
                    Long prepareMs = null;
                    Long prepNs = PREPARE_NS.get();
                    if (prepNs != null) {
                        prepareMs = prepNs / 1_000_000;
                    }
                    sender.send(sqlTemplate, sqlRendered, startedAt, endedAt, durationMs, mapperId, thread, prepareMs);
                } finally {
                    PREPARE_NS.remove();
                }
            }
        }
        return invocation.proceed();
    }

    private BoundSql resolveBoundSql(MappedStatement ms, Object parameter, Object[] args) {
        if (args.length >= 6 && args[5] instanceof BoundSql) {
            return (BoundSql) args[5];
        }
        return ms.getBoundSql(parameter);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
