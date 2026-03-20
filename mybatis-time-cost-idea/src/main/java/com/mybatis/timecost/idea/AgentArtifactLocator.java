package com.mybatis.timecost.idea;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public final class AgentArtifactLocator {
    private AgentArtifactLocator() {
    }

    public static Path locateAgentJar() {
        try {
            URI uri = AgentArtifactLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path location = Paths.get(uri);
            Path libDir = Files.isDirectory(location) ? location : location.getParent();
            if (libDir == null || !Files.isDirectory(libDir)) {
                return null;
            }
            return Files.list(libDir)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("mybatis-time-cost-javaagent-") && name.endsWith(".jar");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
