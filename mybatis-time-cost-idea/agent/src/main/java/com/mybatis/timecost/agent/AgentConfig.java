package com.mybatis.timecost.agent;

public final class AgentConfig {
    private final String host;
    private final int port;

    private AgentConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public static AgentConfig parse(String arguments) {
        String host = "127.0.0.1";
        int port = 17777;
        if (arguments == null || arguments.trim().isEmpty()) {
            return new AgentConfig(host, port);
        }
        for (String token : arguments.split(",")) {
            String[] pair = token.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String key = pair[0].trim();
            String value = pair[1].trim();
            if ("host".equalsIgnoreCase(key) && !value.isEmpty()) {
                host = value;
            } else if ("port".equalsIgnoreCase(key)) {
                try {
                    port = Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new AgentConfig(host, port);
    }
}
