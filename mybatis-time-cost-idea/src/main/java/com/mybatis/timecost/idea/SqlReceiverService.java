package com.mybatis.timecost.idea;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public final class SqlReceiverService implements Disposable {
    private static final Logger LOG = Logger.getInstance(SqlReceiverService.class);
    private volatile boolean started = false;
    private HttpServer server;
    private ExecutorService executor;
    private int currentPort = -1;

    public static SqlReceiverService getInstance() {
        return ApplicationManager.getApplication().getService(SqlReceiverService.class);
    }

    public SqlReceiverService() {
        reloadConfiguration();
    }

    public synchronized void reloadConfiguration() {
        SqlSettingsState settings = SqlSettingsState.getInstance();
        if (!settings.isCaptureEnabled() || !settings.isHttpCaptureEnabled()) {
            stopServer();
            return;
        }
        int port = settings.getPort();
        if (started && currentPort == port) {
            return;
        }
        stopServer();
        try {
            startServer(port);
        } catch (IOException e) {
            if (e instanceof BindException && !settings.isPortOverridden()) {
                int fallbackPort = tryStartOnFallbackPort(port + 1, Math.min(port + 20, 65535));
                if (fallbackPort > 0) {
                    settings.setPort(fallbackPort);
                    settings.notifySettingsChanged();
                    LOG.warn("[MyBatis-TimeCost] Port " + port + " is in use, switched to " + fallbackPort);
                    return;
                }
            }
            currentPort = -1;
            LOG.warn("[MyBatis-TimeCost] Failed to start HTTP server on port " + port, e);
        }
    }

    private void startServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/sql", new SqlHandler());
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
        started = true;
        currentPort = port;
        LOG.info("[MyBatis-TimeCost] HTTP server started on http://127.0.0.1:" + port + "/sql");
    }

    @Override
    public synchronized void dispose() {
        stopServer();
    }

    private void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (started) {
            LOG.info("[MyBatis-TimeCost] HTTP server stopped");
        }
        started = false;
        currentPort = -1;
    }

    public boolean isStarted() {
        return started;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    private int tryStartOnFallbackPort(int startInclusive, int endInclusive) {
        for (int port = startInclusive; port <= endInclusive; port++) {
            try {
                startServer(port);
                return port;
            } catch (IOException ignored) {
                // Keep scanning until an actual bind succeeds.
            }
        }
        return -1;
    }

    private static final class SqlHandler implements HttpHandler {
        @Override
        public void handle(@NotNull HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "Method Not Allowed");
                return;
            }
            String body = readBody(exchange.getRequestBody());
            try {
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                String sql = getAsString(obj, "sqlRendered", getAsString(obj, "sql", null));
                Long duration = getAsLong(obj, "durationMs", null);
                String mapperId = getAsString(obj, "mapperId", null);
                String thread = getAsString(obj, "threadName", Thread.currentThread().getName());

                if (sql == null || sql.isEmpty()) {
                    respond(exchange, 400, "Missing field: sql/sqlRendered");
                    return;
                }

                String logLine = "[MyBatis-TimeCost] duration=" + DurationFormatUtil.format(duration)
                        + (mapperId != null ? " mapper=" + mapperId : "")
                        + " thread=" + thread
                        + " sql=" + compact(sql);
                Logger.getInstance(SqlReceiverService.class).info(logLine);
                SqlEventStore.getInstance().addEvent(
                        new SqlEvent(System.currentTimeMillis(), sql, duration, mapperId, thread)
                );

                if (SqlSettingsState.getInstance().isAutoCopyToClipboard()) {
                    try {
                        CopyPasteManager.getInstance().setContents(new StringSelection(sql));
                        Logger.getInstance(SqlReceiverService.class).info("[MyBatis-TimeCost] SQL copied to clipboard");
                    } catch (Throwable t) {
                        Logger.getInstance(SqlReceiverService.class).warn("[MyBatis-TimeCost] Clipboard copy failed", t);
                    }
                }

                respond(exchange, 200, "{\"status\":\"ok\"}");
            } catch (Throwable t) {
                Logger.getInstance(SqlReceiverService.class).warn("[MyBatis-TimeCost] JSON parse error", t);
                respond(exchange, 400, "Invalid JSON");
            }
        }

        private static String readBody(InputStream is) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }

        private static void respond(HttpExchange exchange, int code, String text) throws IOException {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private static String getAsString(JsonObject obj, String name, String def) {
            if (obj.has(name) && !obj.get(name).isJsonNull()) {
                return Objects.toString(obj.get(name).getAsString(), def);
            }
            return def;
        }

        private static Long getAsLong(JsonObject obj, String name, Long def) {
            if (obj.has(name) && !obj.get(name).isJsonNull()) {
                try { return obj.get(name).getAsLong(); } catch (Exception ignored) {}
            }
            return def;
        }

        private static String compact(String sql) {
            return sql.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ").trim();
        }
    }
}
