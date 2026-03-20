package com.mybatis.timecost.agent;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class AgentSqlEventSender {
    private static final Gson GSON = new Gson();

    private final String host;
    private final int port;

    public AgentSqlEventSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void send(String sqlRendered, long durationMs, String mapperId, String threadName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sqlRendered", sqlRendered);
        payload.put("durationMs", durationMs);
        payload.put("mapperId", mapperId);
        payload.put("threadName", threadName);
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://" + host + ":" + port + "/sql").openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(100);
            connection.setReadTimeout(200);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bytes);
            }
            connection.getResponseCode();
        } catch (Exception ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
