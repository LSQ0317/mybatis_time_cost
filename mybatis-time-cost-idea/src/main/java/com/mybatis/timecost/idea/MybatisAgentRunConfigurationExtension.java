package com.mybatis.timecost.idea;

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.ParametersList;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class MybatisAgentRunConfigurationExtension extends RunConfigurationExtension {
    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase configuration) {
        return configuration instanceof CommonJavaRunConfigurationParameters;
    }

    @Override
    public <T extends RunConfigurationBase> void updateJavaParameters(T configuration,
                                                                      @NotNull JavaParameters javaParameters,
                                                                      RunnerSettings runnerSettings) throws ExecutionException {
        SqlSettingsState settings = SqlSettingsState.getInstance();
        if (!settings.isCaptureEnabled() || !settings.isAgentInjectionEnabled()) {
            return;
        }
        Path agentJar = AgentArtifactLocator.locateAgentJar();
        if (agentJar == null) {
            return;
        }
        ParametersList vmParameters = javaParameters.getVMParametersList();
        String existing = vmParameters.getParametersString();
        if (existing != null && existing.contains("-javaagent:" + agentJar)) {
            return;
        }
        String agentArgs = "host=127.0.0.1,port=" + settings.getPort();
        vmParameters.add("-javaagent:" + agentJar.toAbsolutePath() + "=" + agentArgs);
    }
}
