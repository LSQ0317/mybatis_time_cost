package com.mybatis.timecost.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.nameEndsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class MybatisTimeCostAgent {
    private MybatisTimeCostAgent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        AgentConfig config = AgentConfig.parse(arguments);
        MybatisAgentHelper.initialize(config);

        new AgentBuilder.Default()
                .type(hasSuperType(named("org.apache.ibatis.executor.BaseExecutor")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(MybatisMethodAdvice.class).on(
                                isMethod().and(named("doQuery").or(named("doUpdate")))
                        )))
                .installOn(instrumentation);
    }
}
