package com.mybatis.timecost.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.Disposable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SqlEventStore implements Disposable {
    private final List<SqlEvent> events = new ArrayList<>();
    private final CopyOnWriteArrayList<SqlEventListener> listeners = new CopyOnWriteArrayList<>();

    public static SqlEventStore getInstance() {
        return ApplicationManager.getApplication().getService(SqlEventStore.class);
    }

    public void addEvent(SqlEvent event) {
        List<SqlEventListener> snapshotListeners;
        synchronized (events) {
            events.add(0, event);
            trimLocked();
            snapshotListeners = new ArrayList<>(listeners);
        }
        for (SqlEventListener listener : snapshotListeners) {
            listener.onSqlEvent(event);
        }
    }

    public List<SqlEvent> getEvents() {
        synchronized (events) {
            return Collections.unmodifiableList(new ArrayList<>(events));
        }
    }

    public void addListener(SqlEventListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(SqlEventListener listener) {
        listeners.remove(listener);
    }

    public void clear() {
        synchronized (events) {
            events.clear();
        }
        for (SqlEventListener listener : listeners) {
            listener.onSqlEvent(null);
        }
    }

    public void trimToMaxEvents() {
        synchronized (events) {
            trimLocked();
        }
    }

    private void trimLocked() {
        int maxEvents = Math.max(1, SqlSettingsState.getInstance().getMaxEvents());
        while (events.size() > maxEvents) {
            events.remove(events.size() - 1);
        }
    }

    @Override
    public void dispose() {
        listeners.clear();
        synchronized (events) {
            events.clear();
        }
    }
}
