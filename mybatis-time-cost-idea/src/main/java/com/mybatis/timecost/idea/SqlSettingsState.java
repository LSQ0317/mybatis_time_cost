package com.mybatis.timecost.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.APP)
@State(name = "MyBatisTimeCostSettings", storages = @Storage("mybatis-time-cost.xml"))
public final class SqlSettingsState implements PersistentStateComponent<SqlSettingsState.State> {
    private State state = new State();

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
    }

    public boolean isCaptureEnabled() {
        return state.captureEnabled;
    }

    public void setCaptureEnabled(boolean captureEnabled) {
        state.captureEnabled = captureEnabled;
    }

    public int getPort() {
        return state.port;
    }

    public void setPort(int port) {
        state.port = port;
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

    public static final class State {
        public boolean captureEnabled = true;
        public boolean logCaptureEnabled = true;
        public boolean httpCaptureEnabled = true;
        public int port = 17777;
        public int maxEvents = 500;
        public boolean autoCopyToClipboard = true;
        public int slowThresholdMs = 500;
    }
}
