package com.mybatis.timecost.idea;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class MybatisAgentRunConfigurationExtension extends RunConfigurationExtension {
    private static final Logger LOG = Logger.getInstance(MybatisAgentRunConfigurationExtension.class);

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase configuration) {
        return true;
    }

    @Override
    public boolean isEnabledFor(RunConfigurationBase applicableConfiguration, RunnerSettings runnerSettings) {
        return true;
    }

    @Override
    public <T extends RunConfigurationBase> void updateJavaParameters(T configuration,
                                                                      @NotNull JavaParameters javaParameters,
                                                                      RunnerSettings runnerSettings) throws ExecutionException {
        SqlSettingsState settings = SqlSettingsState.getInstance();
        if (!settings.isCaptureEnabled() || !settings.isAgentInjectionEnabled()) {
            LOG.info("[MyBatis-TimeCost] Skip javaagent injection for " + configuration.getClass().getName()
                    + ": captureEnabled=" + settings.isCaptureEnabled()
                    + ", agentInjectionEnabled=" + settings.isAgentInjectionEnabled());
            return;
        }
        Path agentJar = AgentArtifactLocator.locateAgentJar();
        if (agentJar == null) {
            LOG.warn("[MyBatis-TimeCost] Skip javaagent injection for " + configuration.getClass().getName()
                    + ": agent jar not found");
            return;
        }
        ParametersList vmParameters = javaParameters.getVMParametersList();
        String existing = vmParameters.getParametersString();
        if (existing != null && existing.contains("-javaagent:" + agentJar)) {
            LOG.info("[MyBatis-TimeCost] Javaagent already present for " + configuration.getClass().getName());
            return;
        }
        String agentArgs = "host=127.0.0.1,port=" + settings.getPort();
        vmParameters.add("-javaagent:" + agentJar.toAbsolutePath() + "=" + agentArgs);
        LOG.info("[MyBatis-TimeCost] Injected javaagent into " + configuration.getClass().getName()
                + ": " + agentJar.toAbsolutePath());
    }
}
