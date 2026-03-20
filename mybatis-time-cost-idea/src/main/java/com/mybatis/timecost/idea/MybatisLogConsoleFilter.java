package com.mybatis.timecost.idea;

import com.intellij.execution.filters.Filter;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public final class MybatisLogConsoleFilter implements Filter {
    private final Project project;

    public MybatisLogConsoleFilter(Project project) {
        this.project = project;
    }

    @Override
    public @Nullable Result applyFilter(String line, int entireLength) {
        MybatisLogParserService.getInstance().acceptLine(project, line);
        return null;
    }
}
