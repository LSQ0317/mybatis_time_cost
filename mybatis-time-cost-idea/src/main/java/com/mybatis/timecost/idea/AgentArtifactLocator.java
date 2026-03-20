package com.mybatis.timecost.idea;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class AgentArtifactLocator {
    private static final Logger LOG = Logger.getInstance(AgentArtifactLocator.class);
    private static final String AGENT_PREFIX = "mybatis-time-cost-javaagent-";
    private static final String PLUGIN_DIR_NAME = "mybatis-time-cost-idea";

    private AgentArtifactLocator() {
    }

    public static Path locateAgentJar() {
        List<String> attemptedLocations = new ArrayList<String>();
        try {
            Path pluginsDir = Paths.get(PathManager.getPluginsPath());
            Path standardPluginLibDir = pluginsDir.resolve(PLUGIN_DIR_NAME).resolve("lib");
            attemptedLocations.add(standardPluginLibDir.toString());
            Path standardLocationJar = findAgentJarInDirectory(standardPluginLibDir);
            if (standardLocationJar != null) {
                LOG.info("[MyBatis-TimeCost] Agent jar located in standard plugin lib dir: " + standardLocationJar);
                return standardLocationJar;
            }

            CodeSource codeSource = AgentArtifactLocator.class.getProtectionDomain().getCodeSource();
            URL locationUrl = codeSource != null ? codeSource.getLocation() : null;
            if (locationUrl != null) {
                URI uri = locationUrl.toURI();
                Path location = Paths.get(uri);
                LOG.info("[MyBatis-TimeCost] Agent locator code source: " + location);

                for (Path candidateDir : collectCandidateDirectories(location)) {
                    attemptedLocations.add(candidateDir.toString());
                    Path agentJar = findAgentJarInDirectory(candidateDir);
                    if (agentJar != null) {
                        LOG.info("[MyBatis-TimeCost] Agent jar located: " + agentJar);
                        return agentJar;
                    }
                }
            } else {
                LOG.warn("[MyBatis-TimeCost] Agent locator code source is null, fallback to plugins path");
            }

            Path classResourceLocation = locateFromClassResource();
            if (classResourceLocation != null) {
                LOG.info("[MyBatis-TimeCost] Agent locator class resource path: " + classResourceLocation);
                for (Path candidateDir : collectCandidateDirectories(classResourceLocation)) {
                    attemptedLocations.add(candidateDir.toString());
                    Path agentJar = findAgentJarInDirectory(candidateDir);
                    if (agentJar != null) {
                        LOG.info("[MyBatis-TimeCost] Agent jar located via class resource path: " + agentJar);
                        return agentJar;
                    }
                }
            }

            attemptedLocations.add(pluginsDir.toString() + "/**");
            Path fallbackJar = findAgentJarRecursively(pluginsDir);
            if (fallbackJar != null) {
                LOG.info("[MyBatis-TimeCost] Agent jar located via plugins path fallback: " + fallbackJar);
                return fallbackJar;
            }
        } catch (Exception e) {
            LOG.warn("[MyBatis-TimeCost] Failed while locating agent jar", e);
        }

        if (!attemptedLocations.isEmpty()) {
            LOG.warn("[MyBatis-TimeCost] Agent jar not found. Attempted locations: " + attemptedLocations);
        } else {
            LOG.warn("[MyBatis-TimeCost] Agent jar not found. No candidate locations were available.");
        }
        return null;
    }

    private static Path locateFromClassResource() {
        try {
            String resourceName = AgentArtifactLocator.class.getSimpleName() + ".class";
            URL resourceUrl = AgentArtifactLocator.class.getResource(resourceName);
            if (resourceUrl == null) {
                return null;
            }
            String externalForm = resourceUrl.toExternalForm();
            int jarSeparator = externalForm.indexOf("!/");
            if (!externalForm.startsWith("jar:") || jarSeparator < 0) {
                return null;
            }
            String jarPath = externalForm.substring("jar:".length(), jarSeparator);
            if (!jarPath.startsWith("file:")) {
                return null;
            }
            String decodedPath = URLDecoder.decode(jarPath.substring("file:".length()), StandardCharsets.UTF_8.name());
            return Paths.get(decodedPath);
        } catch (Exception e) {
            LOG.debug("[MyBatis-TimeCost] Failed to resolve agent locator class resource path", e);
            return null;
        }
    }

    private static List<Path> collectCandidateDirectories(Path location) {
        Set<Path> result = new LinkedHashSet<Path>();
        Path directory = Files.isDirectory(location) ? location : location.getParent();
        while (directory != null) {
            result.add(directory);
            result.add(directory.resolve("lib"));
            directory = directory.getParent();
        }
        return new ArrayList<Path>(result);
    }

    private static Path findAgentJarInDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return null;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(AGENT_PREFIX) && name.endsWith(".jar");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOG.debug("[MyBatis-TimeCost] Failed to scan directory for agent jar: " + directory, e);
            return null;
        }
    }

    private static Path findAgentJarRecursively(Path pluginsDir) {
        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) {
            return null;
        }
        try (Stream<Path> stream = Files.walk(pluginsDir, 4)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(AGENT_PREFIX) && name.endsWith(".jar");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOG.debug("[MyBatis-TimeCost] Failed to scan plugins directory recursively: " + pluginsDir, e);
            return null;
        }
    }
}
