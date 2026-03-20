package com.mybatis.timecost.agent;

import net.bytebuddy.asm.Advice;

public final class MybatisMethodAdvice {
    private MybatisMethodAdvice() {
    }

    @Advice.OnMethodEnter
    public static long onEnter() {
        return MybatisAgentHelper.enter();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Enter long startedAtNs, @Advice.AllArguments Object[] args) {
        MybatisAgentHelper.exit(startedAtNs, args);
    }
}
