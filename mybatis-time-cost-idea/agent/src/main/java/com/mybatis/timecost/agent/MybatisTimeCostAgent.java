package com.mybatis.timecost.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class MybatisTimeCostAgent {
    private MybatisTimeCostAgent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        AgentConfig config = AgentConfig.parse(arguments);
        MybatisAgentHelper.initialize(config);

        new AgentBuilder.Default()
                .type(hasSuperType(named("org.apache.ibatis.executor.BaseExecutor")))
                .transform(new AgentBuilder.Transformer() {
                    // Support both old and new ByteBuddy Transformer signatures.
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module) {
                        return applyAdvice(builder);
                    }

                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module,
                                                            ProtectionDomain protectionDomain) {
                        return applyAdvice(builder);
                    }

                    private DynamicType.Builder<?> applyAdvice(DynamicType.Builder<?> builder) {
                        return builder.visit(Advice.to(MybatisMethodAdvice.class).on(
                                isMethod().and(named("doQuery").or(named("doUpdate")))
                        ));
                    }
                })
                .installOn(instrumentation);
    }
}
