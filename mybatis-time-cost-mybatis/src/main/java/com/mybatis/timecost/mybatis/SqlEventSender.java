package com.mybatis.timecost.mybatis;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

public final class SqlEventSender {
    private static final Gson GSON = new Gson();
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(256),
            new SenderThreadFactory(),
            new ThreadPoolExecutor.DiscardPolicy()
    );
    private final String host;
    private final int port;

    public SqlEventSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void send(String sqlTemplate, String sqlRendered, long startedAt, long endedAt, long durationMs, String mapperId, String threadName, Long prepareMs) {
        Map<String, Object> json = new HashMap<>();
        json.put("sqlTemplate", sqlTemplate);
        json.put("sqlRendered", sqlRendered);
        json.put("startedAt", startedAt);
        json.put("endedAt", endedAt);
        json.put("durationMs", durationMs);
        json.put("mapperId", mapperId);
        json.put("threadName", threadName);
        Map<String, Object> timing = new HashMap<>();
        timing.put("prepareMs", prepareMs);
        timing.put("totalMs", durationMs);
        json.put("timing", timing);
        String body = GSON.toJson(json);
        try {
            EXECUTOR.execute(() -> {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL("http://" + host + ":" + port + "/sql");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(100);
                    conn.setReadTimeout(200);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    conn.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(bytes);
                    }
                    conn.getResponseCode();
                } catch (Exception ignored) {
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
        }
    }

    public static SqlEventSender defaultSender() {
        String env = System.getenv("MYBATIS_TIME_COST_PORT");
        int p = 17777;
        if (env != null) {
            try { p = Integer.parseInt(env); } catch (Exception ignored) {}
        }
        String prop = System.getProperty("mybatis.timecost.port");
        if (prop != null) {
            try { p = Integer.parseInt(prop); } catch (Exception ignored) {}
        }
        return new SqlEventSender("127.0.0.1", p);
    }

    private static final class SenderThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "mybatis-time-cost-sender");
            thread.setDaemon(true);
            return thread;
        }
    }
}
