package com.mybatis.timecost.idea;

import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class MybatisConsoleFilterProvider implements ConsoleFilterProvider {
    @Override
    public Filter @NotNull [] getDefaultFilters(@NotNull Project project) {
        return new Filter[]{new MybatisLogConsoleFilter(project)};
    }
}
