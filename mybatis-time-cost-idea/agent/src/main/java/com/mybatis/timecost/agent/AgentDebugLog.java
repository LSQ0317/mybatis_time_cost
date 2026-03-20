package com.mybatis.timecost.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

final class AgentDebugLog {
    private static final File LOG_FILE = resolveLogFile();

    private AgentDebugLog() {
    }

    static synchronized void info(String message) {
        write("INFO", message, null);
    }

    static synchronized void warn(String message, Throwable throwable) {
        write("WARN", message, throwable);
    }

    private static void write(String level, String message, Throwable throwable) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(LOG_FILE, true));
            writer.println("[" + level + "] " + message);
            if (throwable != null) {
                throwable.printStackTrace(writer);
            }
            writer.flush();
        } catch (IOException ignored) {
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        try {
            System.err.println("[MyBatis-TimeCost-Agent][" + level + "] " + message + " (log=" + LOG_FILE.getAbsolutePath() + ")");
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        } catch (Throwable ignored) {
        }
    }

    private static File resolveLogFile() {
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.trim().isEmpty()) {
            return new File(userHome, "mybatis-time-cost-agent.log");
        }
        return new File(System.getProperty("java.io.tmpdir"), "mybatis-time-cost-agent.log");
    }
}
