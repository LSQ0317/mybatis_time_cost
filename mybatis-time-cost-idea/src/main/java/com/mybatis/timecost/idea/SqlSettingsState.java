package com.mybatis.timecost.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service(Service.Level.APP)
@State(name = "MyBatisTimeCostSettings", storages = @Storage("mybatis-time-cost.xml"))
public final class SqlSettingsState implements PersistentStateComponent<SqlSettingsState.State> {
    private State state = new State();
    private final List<SqlSettingsListener> listeners = new CopyOnWriteArrayList<>();

    public static SqlSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SqlSettingsState.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
        notifySettingsChanged();
    }

    public boolean isCaptureEnabled() {
        return state.captureEnabled;
    }

    public void setCaptureEnabled(boolean captureEnabled) {
        state.captureEnabled = captureEnabled;
    }

    public int getPort() {
        String prop = System.getProperty("mybatis.timecost.port");
        Integer resolvedFromProp = parsePort(prop);
        if (resolvedFromProp != null) {
            return resolvedFromProp;
        }

        String env = System.getenv("MYBATIS_TIME_COST_PORT");
        Integer resolvedFromEnv = parsePort(env);
        if (resolvedFromEnv != null) {
            return resolvedFromEnv;
        }

        return state.port;
    }

    public int getConfiguredPort() {
        return state.port;
    }

    public void setPort(int port) {
        state.port = port;
    }

    public boolean isPortOverridden() {
        return parsePort(System.getProperty("mybatis.timecost.port")) != null
                || parsePort(System.getenv("MYBATIS_TIME_COST_PORT")) != null;
    }

    public int getMaxEvents() {
        return state.maxEvents;
    }

    public void setMaxEvents(int maxEvents) {
        state.maxEvents = maxEvents;
    }

    public boolean isAutoCopyToClipboard() {
        return state.autoCopyToClipboard;
    }

    public void setAutoCopyToClipboard(boolean autoCopyToClipboard) {
        state.autoCopyToClipboard = autoCopyToClipboard;
    }

    public int getSlowThresholdMs() {
        return state.slowThresholdMs;
    }

    public void setSlowThresholdMs(int slowThresholdMs) {
        state.slowThresholdMs = slowThresholdMs;
    }

    public boolean isLogCaptureEnabled() {
        return state.logCaptureEnabled;
    }

    public void setLogCaptureEnabled(boolean logCaptureEnabled) {
        state.logCaptureEnabled = logCaptureEnabled;
    }

    public boolean isHttpCaptureEnabled() {
        return state.httpCaptureEnabled;
    }

    public void setHttpCaptureEnabled(boolean httpCaptureEnabled) {
        state.httpCaptureEnabled = httpCaptureEnabled;
    }

    public boolean isAgentInjectionEnabled() {
        return state.agentInjectionEnabled;
    }

    public void setAgentInjectionEnabled(boolean agentInjectionEnabled) {
        state.agentInjectionEnabled = agentInjectionEnabled;
    }

    public void addListener(SqlSettingsListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SqlSettingsListener listener) {
        listeners.remove(listener);
    }

    public void notifySettingsChanged() {
        for (SqlSettingsListener listener : listeners) {
            listener.onSettingsChanged();
        }
    }

    public static final class State {
        public boolean captureEnabled = true;
        public boolean logCaptureEnabled = false;
        public boolean httpCaptureEnabled = true;
        public boolean agentInjectionEnabled = true;
        public int port = 17777;
        public int maxEvents = 500;
        public boolean autoCopyToClipboard = true;
        public int slowThresholdMs = 500;
    }

    private static Integer parsePort(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1 && port <= 65535 ? port : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
