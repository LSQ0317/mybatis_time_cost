# MyBatis Time Cost

`mybatis_time_cost` 是一个“运行时采集 + 本地传输 + IDEA 内展示”的组合项目，目标是把 MyBatis 执行出来的最终 SQL 和耗时带回 IntelliJ IDEA 中直接查看。

当前仓库里一共有三块核心内容：
- `mybatis-time-cost-idea`：IntelliJ IDEA 插件。负责接收 SQL 事件、保存事件、展示 Tool Window、提供设置页，并可在 Run/Debug 时自动注入 Java Agent。
- `mybatis-time-cost-mybatis`：业务应用侧的 MyBatis 拦截器库。通过 MyBatis `Interceptor` 拦截 `Executor` 和 `StatementHandler`，渲染 SQL 并上报到 IDEA 插件。
- `mybatis-time-cost-demo`：最小演示工程。基于 Spring Boot + MyBatis + H2，用来验证从业务应用到 IDEA 插件的整条链路。

## 当前实现状态

当前代码不是“完整需求版”，而是“可工作的第一阶段原型”。已经落地的能力主要是：
- 在 IDEA 插件里提供 Tool Window 展示接收到的 SQL 事件
- 本地 HTTP 接收 `/sql` 事件
- 可选解析控制台里的 MyBatis 日志
- 可选在 Run/Debug 启动 Java 程序时自动注入 Java Agent
- 在业务侧通过 MyBatis 拦截器或 Java Agent 采集 SQL
- 自动复制最新 SQL 到剪贴板
- 慢 SQL 颜色高亮、事件上限控制、基本设置页

还没有完全落地的内容：
- 多方言完整支持，当前实际主要围绕 MySQL 风格渲染
- 完整的 prepare / execute / fetch 分段耗时模型
- 导出、聚合统计、源码跳转、结果集预览等增强功能

## 兼容性

当前 IDEA 插件已经调整为兼容 IntelliJ IDEA `2020.1`：
- 构建平台：`2020.1`
- `since-build`：`201`
- 插件模块字节码目标：Java 8

相关配置在：
- [mybatis-time-cost-idea/build.gradle.kts](/Users/wintermist/IdeaProjects/mybatis_time_cost/mybatis-time-cost-idea/build.gradle.kts)
- [mybatis-time-cost-idea/src/main/resources/META-INF/plugin.xml](/Users/wintermist/IdeaProjects/mybatis_time_cost/mybatis-time-cost-idea/src/main/resources/META-INF/plugin.xml)

## 仓库结构

```text
mybatis_time_cost/
├── mybatis-time-cost-idea/        IDEA 插件
│   ├── agent/                     Java Agent，供插件自动注入
│   └── src/main/java/.../idea/    插件 UI、接收服务、设置、事件存储
├── mybatis-time-cost-mybatis/     MyBatis 拦截器库
├── mybatis-time-cost-demo/        Spring Boot 演示工程
├── plan.md                        规划文档
└── code_read.md                   源码讲解文档
```

## 两条采集链路

当前项目里实际上存在两条并行的采集路径。

### 1. MyBatis 拦截器链路

适合业务项目显式接入依赖的场景：

1. 业务工程引入 `mybatis-time-cost-mybatis`
2. 注册 `TimeCostInterceptor`
3. 拦截器在 MyBatis 执行时拿到 `MappedStatement`、`BoundSql`、参数对象
4. 通过 `SqlRenderUtil` 把带 `?` 的 SQL 渲染成最终可执行 SQL
5. 通过 `SqlEventSender` 发 HTTP JSON 到 `127.0.0.1:<port>/sql`
6. IDEA 插件中的 `SqlReceiverService` 收到事件
7. `SqlEventStore` 保存事件
8. Tool Window 刷新展示

### 2. Java Agent 链路

适合“从 IDEA 直接运行 Java 程序，不想手动改业务项目配置”的场景：

1. 插件在 Run/Debug 启动时通过 `MybatisAgentRunConfigurationExtension` 自动加上 `-javaagent`
2. `agent` 模块里的 ByteBuddy Agent 在应用 JVM 中织入 MyBatis `BaseExecutor`
3. `MybatisMethodAdvice` 记录方法进入/退出时间
4. `MybatisAgentHelper` 渲染 SQL 并发送到 IDEA 插件
5. 插件接收并展示

这两条链路最终都汇聚到同一个 IDEA 插件接收入口，所以 Tool Window 展示逻辑是统一的。

## IDEA 插件模块说明

`mybatis-time-cost-idea` 当前包含五类职责：

- 插件注册层：
  - `plugin.xml`
  - 注册 application service、设置页、Tool Window、console filter、run configuration extension
- 接收层：
  - `SqlReceiverService`
  - 在本地开启 HTTP 服务，处理 `/sql`
- 状态层：
  - `SqlSettingsState`
  - `SqlEventStore`
- UI 层：
  - `SqlToolWindowFactory`
  - `SqlToolWindowPanel`
  - `SqlSettingsConfigurable`
- 控制台/运行配置集成层：
  - `MybatisLogConsoleFilter`
  - `MybatisConsoleFilterProvider`
  - `MybatisLogParserService`
  - `MybatisAgentRunConfigurationExtension`

## MyBatis 拦截器模块说明

`mybatis-time-cost-mybatis` 是一个独立的 Java 库，负责在业务进程里采集 SQL：

- `TimeCostInterceptor`：真正挂到 MyBatis 执行链上的拦截器
- `SqlRenderUtil`：根据 `BoundSql` 和参数对象还原最终 SQL
- `Dialect`：SQL 字面量格式化策略，当前内置 `mysql()`
- `SqlEventSender`：异步 HTTP 上报

## Demo 模块说明

`mybatis-time-cost-demo` 主要有两个作用：
- 提供最小运行样例
- 验证 MyBatis 拦截器链路是否打通

它依赖：
- Spring Boot 3.3.3
- MyBatis Spring Boot Starter 3.0.3
- `mybatis-time-cost-mybatis`
- H2

## 常用命令

### 打包 IDEA 插件

```bash
cd mybatis-time-cost-idea
./gradlew buildPlugin
```

打包产物默认在：
- [mybatis-time-cost-idea/build/distributions/mybatis-time-cost-idea-0.1.0.zip](/Users/wintermist/IdeaProjects/mybatis_time_cost/mybatis-time-cost-idea/build/distributions/mybatis-time-cost-idea-0.1.0.zip)

### 运行 IDEA 插件沙箱

```bash
cd mybatis-time-cost-idea
./gradlew runIde
```

### 运行 Demo

```bash
./mybatis-time-cost-idea/gradlew -p mybatis-time-cost-demo bootRun
```

## 配置入口

IDEA 插件设置页当前支持：
- 是否启用采集
- 是否自动注入 Java Agent
- 是否启用控制台日志解析
- 是否启用本地 HTTP 接收
- 是否自动复制 SQL
- 监听端口
- 最大事件数
- 慢查询阈值

端口也可通过以下方式覆盖：
- 环境变量：`MYBATIS_TIME_COST_PORT`
- JVM 参数：`-Dmybatis.timecost.port=17777`

## 推荐阅读顺序

如果你要理解整个项目，建议按这个顺序读：

1. [code_read.md](/Users/wintermist/IdeaProjects/mybatis_time_cost/code_read.md)
2. [mybatis-time-cost-idea/src/main/resources/META-INF/plugin.xml](/Users/wintermist/IdeaProjects/mybatis_time_cost/mybatis-time-cost-idea/src/main/resources/META-INF/plugin.xml)
3. [mybatis-time-cost-idea/src/main/java/com/mybatis/timecost/idea/SqlReceiverService.java](/Users/wintermist/IdeaProjects/mybatis_time_cost/mybatis-time-cost-idea/src/main/java/com/mybatis/timecost/idea/SqlReceiverService.java)
4. [mybatis-time-cost-idea/src/main/java/com/mybatis/timecost/idea/SqlEventStore.java](/Users/wintermist/IdeaProjects/mybatis_time_cost/mybatis-time-cost-idea/src/main/java/com/mybatis/timecost/idea/SqlEventStore.java)
5. [mybatis-time-cost-idea/src/main/java/com/mybatis/timecost/idea/SqlToolWindowPanel.java](/Users/wintermist/IdeaProjects/mybatis_time_cost/mybatis-time-cost-idea/src/main/java/com/mybatis/timecost/idea/SqlToolWindowPanel.java)
6. [mybatis-time-cost-mybatis/src/main/java/com/mybatis/timecost/mybatis/TimeCostInterceptor.java](/Users/wintermist/IdeaProjects/mybatis_time_cost/mybatis-time-cost-mybatis/src/main/java/com/mybatis/timecost/mybatis/TimeCostInterceptor.java)
7. [mybatis-time-cost-idea/agent/src/main/java/com/mybatis/timecost/agent/MybatisTimeCostAgent.java](/Users/wintermist/IdeaProjects/mybatis_time_cost/mybatis-time-cost-idea/agent/src/main/java/com/mybatis/timecost/agent/MybatisTimeCostAgent.java)

## 进一步说明

更详细的源码级说明见：
- [code_read.md](/Users/wintermist/IdeaProjects/mybatis_time_cost/code_read.md)

