# MyBatis Time Cost IDEA 插件

这是仓库里的 IntelliJ IDEA 插件模块。它负责：
- 在本地启动 HTTP 服务接收 SQL 事件
- 解析可选的控制台 MyBatis 日志
- 在 Tool Window 中展示 SQL、耗时、Mapper、线程信息
- 自动复制最新 SQL
- 在 Run/Debug 时自动为 Java 程序注入 Java Agent

## 当前能力

当前插件已经具备以下功能：
- Tool Window 展示最近 SQL 事件
- 本地 HTTP 接收：`POST /sql`
- 控制台日志解析
- Run/Debug 自动注入 Java Agent
- 设置页配置端口、慢查询阈值、事件上限和采集开关
- 慢 SQL 高亮

## 兼容版本

当前构建目标已调整为：
- IntelliJ IDEA `2020.1`
- `since-build = 201`
- 插件模块字节码目标 Java 8

## 打包与调试

在 `mybatis-time-cost-idea` 目录下执行：

```bash
./gradlew runIde
./gradlew buildPlugin
```

打包后插件 ZIP 位于：
- `build/distributions/mybatis-time-cost-idea-0.1.0.zip`

## 本地接收协议

监听地址默认是：
- `http://127.0.0.1:17777/sql`

端口覆盖方式：
- 环境变量：`MYBATIS_TIME_COST_PORT`
- JVM 参数：`-Dmybatis.timecost.port=18080`

请求方法：
- `POST /sql`

当前插件实际消费的关键字段：
- `sqlRendered` 或 `sql`
- `durationMs`
- `mapperId`
- `threadName`

返回示例：

```json
{"status":"ok"}
```

请求示例：

```bash
curl -X POST http://127.0.0.1:17777/sql \
  -H 'Content-Type: application/json' \
  -d '{"sqlRendered":"select * from user where id = 42","durationMs":123,"mapperId":"com.acme.UserMapper.findById","threadName":"main"}'
```

## 内部结构

主要代码分成几块：
- `SqlReceiverService`
  - 本地 HTTP 服务
- `SqlEventStore`
  - 事件存储和监听通知
- `SqlToolWindowPanel`
  - Tool Window UI
- `SqlSettingsState` / `SqlSettingsConfigurable`
  - 设置持久化和设置页
- `MybatisLogConsoleFilter` / `MybatisLogParserService`
  - 控制台日志解析
- `MybatisAgentRunConfigurationExtension`
  - Run/Debug 自动注入 Java Agent
- `agent/`
  - ByteBuddy Java Agent 实现

## 与业务应用的两种集成方式

### 1. 通过 MyBatis 拦截器库集成

业务应用引入 `mybatis-time-cost-mybatis`，由它采集并发送 SQL 到本插件。

### 2. 通过插件自动注入 Java Agent

如果应用是从 IDEA 里启动的，插件可以在运行配置中自动加入 `-javaagent`，由 agent 在目标 JVM 内采集 SQL。

## 进一步阅读

更详细的源码讲解见：
- [code_read.md](/Users/wintermist/IdeaProjects/mybatis_time_cost/code_read.md)
