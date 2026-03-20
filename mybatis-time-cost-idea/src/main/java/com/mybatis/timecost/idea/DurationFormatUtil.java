package com.mybatis.timecost.idea;

import java.util.Locale;

public final class DurationFormatUtil {
    private DurationFormatUtil() {
    }

    public static String format(Long durationMs) {
        if (durationMs == null) {
            return "n/a";
        }
        return format(durationMs.longValue());
    }

    public static String format(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        }
        if (durationMs < 60_000) {
            return formatSeconds(durationMs);
        }

        long hours = durationMs / 3_600_000;
        long minutes = (durationMs % 3_600_000) / 60_000;
        long seconds = (durationMs % 60_000) / 1000;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    public static String formatWithRaw(Long durationMs) {
        if (durationMs == null) {
            return "n/a";
        }
        return format(durationMs) + " (" + durationMs + "ms)";
    }

    private static String formatSeconds(long durationMs) {
        if (durationMs % 1000 == 0) {
            return (durationMs / 1000) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", durationMs / 1000.0d);
    }
}
