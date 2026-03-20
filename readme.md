# IntelliJ IDEA 插件：MyBatis SQL 可视化与耗时分析（需求说明）

本插件用于在 IntelliJ IDEA 内实时捕获并展示 Java 项目中 MyBatis 执行的 SQL，生成可直接在数据库客户端粘贴运行的最终 SQL，并提供精确的耗时分析与统计能力。

## 目标与范围
- 目标：提高 MyBatis 开发/调试效率，快速定位慢查询与错误 SQL，并一键复制可运行 SQL。
- 范围：适配 MyBatis 3.5+（含 MyBatis-Plus 可选支持），Java 8+，Spring/Spring Boot 与非 Spring 场景；支持多数据源与批量执行。
- 运行环境：IntelliJ IDEA 2021.3+ 社区版/旗舰版。

## 核心功能清单
1. 实时采集
   - 拦截 MyBatis 执行，捕获 SQL 模板、渲染后的可运行 SQL、参数、数据源信息、执行耗时、结果/影响行数与异常。
   - 支持批量语句与多数据源；支持采样率与忽略列表。
2. 可视化展示
   - 提供 Tool Window “MyBatis SQL Inspector” 列表视图与详情面板；双击/快捷键跳转源码（Mapper XML/注解方法）。
   - 在编辑器左侧 Gutter/内联提示显示最近一次该语句耗时与慢查询标记。
3. 一键复制可运行 SQL
   - 基于方言（MySQL/PostgreSQL/Oracle/SQL Server）生成可直接在数据库客户端运行的 SQL 字面量。
   - 支持批量语句展开、日期/时间/二进制/大数格式化、字符串转义与 NULL 处理。
4. 性能分析与统计
   - 展示每条语句的 prepare/execute/fetch/total 分段耗时；列表支持慢查询阈值标记与排序。
   - 聚合统计：按 Mapper/方法/数据源/SQL 类型维度展示平均值、P95、最大值与次数。
5. 过滤与搜索
   - 支持按时间区间、耗时范围、项目/模块/包/类/方法、线程名、数据源名、SQL 类型、状态（成功/失败）过滤与全文搜索。
6. 导出与分享
   - 导出单条或多条记录为 JSON/CSV/Markdown（可选脱敏）；复制记录详情或仅复制可运行 SQL。
7. 控制台与日志集成（可选）
   - 可选在 Run/Debug 控制台打印精简版信息或完整 JSON；兼容 IDEA Console Filters 的跳转。
8. 安全合规与隐私
   - 所有数据仅本地收集与展示；支持字段/值脱敏、JDBC URL/用户名脱敏、大字段与二进制截断。
9. 可配置与可扩展
   - 设置页提供保留策略、慢查询阈值、方言、脱敏、日志级别、采样、开关等；SPI 允许自定义方言与脱敏策略。

## 数据模型（字段与类型）

### SqlExecutionRecord
- id: string（UUID）
- startedAt: long（起始时间，epochMillis）
- endedAt: long（结束时间，epochMillis）
- durationMs: int（总耗时）
- threadName: string（线程名）
- projectName: string（项目名）
- moduleName: string（模块名）
- packageName: string（包名）
- className: string（Java 类名）
- methodName: string（方法名）
- mapperId: string（Mapper 命名空间与 id，形如 namespace.id）
- filePath: string（源码文件绝对路径/相对路径）
- lineNumber: int（源码行号）
- sqlTemplate: string（带“?”或占位符的原始 SQL）
- sqlRendered: string（替换参数后的可运行 SQL，按选定方言转义）
- sqlType: enum（SELECT | INSERT | UPDATE | DELETE | DDL | CALL | OTHER）
- parameters: ParameterBinding[]（参数绑定列表，按顺序或名称）
- dataSource: DataSourceInfo（数据源信息）
- transaction: TransactionInfo（事务信息）
- timing: TimingBreakdown（分段耗时）
- result: ResultInfo（结果信息）
- sourceContext: SourceContext（调用上下文/栈）
- flags: Flags（标记）
- batch: BatchInfo（批量信息，可选）

### ParameterBinding
- index: int（1 基）
- name: string（命名参数名，可选）
- jdbcType: string（JDBC 类型，如 VARCHAR）
- javaType: string（Java 类型简单名）
- valuePreview: string（字符串化预览，脱敏/截断后）
- originalValue: any（原始值，可配置是否持久化，默认不持久化敏感值）
- isNull: boolean
- length: int（值长度，若可计算）
- isBinary: boolean（是否二进制）
- isLarge: boolean（是否大字段）

### DataSourceInfo
- name: string（数据源名）
- productName: string（数据库产品名）
- productVersion: string（数据库版本）
- jdbcUrl: string（JDBC URL，按规则脱敏）
- username: string（用户名，按规则脱敏）

### TransactionInfo
- transactionId: string（事务标识）
- autoCommit: boolean
- isolation: string（READ_COMMITTED 等）
- readOnly: boolean
- propagation: string（若由 Spring 提供）

### TimingBreakdown
- prepareMs: int（Statement 准备耗时）
- executeMs: int（执行耗时）
- fetchMs: int（结果提取耗时）
- totalMs: int（总耗时）

### ResultInfo
- affectedRows: int（写操作影响行数）
- rowCount: int（读取行数，可能为估计或采样）
- sampleRows: Array<Map<string,string>>（可选，结果预览，按列名->字符串，脱敏/截断）
- error: ErrorInfo（若出错）

### ErrorInfo
- message: string
- sqlState: string
- errorCode: int
- stackTraceTop: string（首帧或摘要）
- cause: string（根因摘要）

### SourceContext
- callStack: StackFrame[]（采样栈）
- entryPoint: StackFrame（业务入口）
- requestId: string（Web 请求关联 ID，可选）
- sessionId: string（会话标识，可选）

### StackFrame
- className: string
- methodName: string
- fileName: string
- lineNumber: int

### Flags
- slow: boolean（是否超慢阈值）
- sampled: boolean（是否采样记录）
- redacted: boolean（是否发生脱敏）
- batch: boolean（是否批量）

### BatchInfo（可选）
- batchSize: int（批次数量）
- batchIndex: int（当前语句索引）
- statements: string[]（每条渲染后 SQL）

## UI/UX 规格

### Tool Window：MyBatis SQL Inspector
- 列表列（可配置显示/顺序）：
  - 时间（startedAt） | 耗时（durationMs） | 慢标记 | SQL 类型（sqlType）
  - Mapper（mapperId 简写） | 方法（className#methodName） | 数据源（dataSource.name）
  - 行数/影响行数（result.rowCount/affectedRows） | 状态（成功/失败）
- 行样式：
  - 超过慢阈值加高亮；失败行以错误色显示；悬浮显示 SQL 预览。
- 详情面板分区：
  - 基本信息、参数、渲染后 SQL（支持语法高亮/折叠/复制）、分段耗时、结果/错误、调用栈、数据源与事务。
- 工具栏动作：
  - 开/关采集、清空、导出、复制 SQL、复制记录 JSON、跳转源码、过滤器、统计视图切换、设置入口。

### 编辑器集成
- Gutter/内联提示：显示最近一次该 Mapper/方法的耗时与慢标记；点击打开详情。
- 控制台集成（可选）：打印精简日志行（时间、耗时、mapperId、SQL 预览/ID）。

## 可运行 SQL 生成规则
- 字符串转义：
  - MySQL：使用单引号并转义 `'` -> `''`
  - PostgreSQL：使用单引号并转义；支持 E'' 形式可选
  - Oracle：单引号转义；日期时间使用 TO_DATE/TO_TIMESTAMP
  - SQL Server：单引号转义；日期时间 `'YYYY-MM-DD HH:MM:SS'`
- 日期/时间：
  - LocalDate -> `'YYYY-MM-DD'`
  - LocalDateTime/Timestamp -> 方言对应格式（MySQL/PG 直接字面量，Oracle 使用 TO_TIMESTAMP）
- 布尔：
  - MySQL/SQL Server -> 0/1（可选 true/false）
  - PostgreSQL -> true/false
  - Oracle -> 1/0 或 'Y'/'N'（可配置）
- 数字与大数：使用非科学计数法字符串（BigDecimal.toPlainString）
- 二进制：以十六进制（MySQL 0x.., PG E'\\x..'::bytea, Oracle hextoraw）表示
- NULL：直接字面量 NULL，避免引号
- 批量：将每批渲染为独立 SQL；INSERT 批量值列表拼接可选
- 限制与声明：不处理存储过程 OUT 参数；方言选择影响生成；建议在相同数据库执行

## 过滤与搜索字段
- 时间范围：startedAt, endedAt
- 耗时范围：durationMs, prepareMs/executeMs/fetchMs
- 维度：project/module/package/class/method/mapperId
- 数据源：dataSource.name, dataSource.productName
- SQL 类型：sqlType
- 线程：threadName
- 状态：result.error 是否为空
- 关键字：sqlTemplate/sqlRendered 全文

## 设置（Settings）项与字段
- 采集控制：
  - enableCapture: boolean（默认 true）
  - samplingRate: double（0.0–1.0，默认 1.0）
  - ignoredPackages: string[]（前缀匹配）
  - maxStackDepth: int（默认 50）
- 保留与清理：
  - maxRecords: int（最大记录数，默认 5000）
  - retentionMinutes: int（时间保留上限，默认 60）
  - autoPurge: boolean（达阈值自动清理）
- 慢查询：
  - slowThresholdMs: int（默认 500）
  - slowHighlight: boolean（默认 true）
- 可运行 SQL：
  - defaultDialect: enum（MYSQL | POSTGRES | ORACLE | SQLSERVER）
  - stringMaxLen: int（渲染时字符串最大长度，默认 4096）
  - binaryAsHex: boolean（默认 true）
- 结果预览：
  - enableResultSample: boolean（默认 false）
  - sampleRowsLimit: int（默认 10）
  - sampleColumnWidth: int（默认 200）
- 脱敏与隐私：
  - redactJdbcUrl: boolean（默认 true）
  - redactUsername: boolean（默认 true）
  - valueRedactionRules: RedactionRule[]（正则/字段名/类型规则）
  - truncateLargeValues: boolean（默认 true）
  - maxValueLength: int（默认 512）
- 控制台/日志：
  - consolePrintMode: enum（OFF | BRIEF | JSON）
  - logLevel: enum（INFO | DEBUG | TRACE）
- 集成方式：
  - interceptorMode: enum（MYBATIS_INTERCEPTOR | JDBC_PROXY | LOG_ADAPTER）
  - springBootAutoConfig: boolean（默认启用 Spring 环境下自动注入）

### RedactionRule
- type: enum（FIELD_NAME | REGEX | TYPE）
- pattern: string（字段名或正则）
- replacement: string（替换内容，默认为 `***`）

## 兼容性与性能目标
- MyBatis 3.5+，可兼容 MyBatis-Plus（常见 API 场景）
- Java 8+；IntelliJ IDEA 2021.3+
- 多数据源与批处理场景兼容
- 运行时开销：常见查询平均 <5%（可通过采样与过滤控制）

## 集成与实现要点（技术方案概述）
- MyBatis Interceptor：
  - 拦截 StatementHandler.prepare、Executor.query/update，记录 BoundSql、ParameterObject，测量 prepare/execute/fetch。
  - 通过 TypeHandler 提取参数值；渲染最终 SQL（按方言转义）。
  - 记录调用栈（排除 JDK/框架包），定位业务入口与 Mapper 源码位置。
- 备选方案：
  - JDBC 代理：包装 DataSource/Connection/PreparedStatement 以埋点，兼容性强但入侵更广。
  - 日志适配：解析 MyBatis-logging 输出，侵入小但精度/结构化较弱。
- Spring Boot：
  - 提供自动配置/开关，在存在 SqlSessionFactory 时自动注册拦截器。

## 导出格式（示例 JSON）

```json
{
  "id": "e4b6f3b0-9c8b-4c2b-a1e7-2a8f2b1c9d23",
  "startedAt": 1718690023123,
  "endedAt": 1718690023456,
  "durationMs": 333,
  "threadName": "http-nio-8080-exec-7",
  "mapperId": "com.acme.user.UserMapper.findById",
  "sqlType": "SELECT",
  "sqlTemplate": "select * from user where id = ?",
  "sqlRendered": "select * from user where id = 42",
  "parameters": [
    { "index": 1, "jdbcType": "BIGINT", "javaType": "Long", "valuePreview": "42", "isNull": false }
  ],
  "dataSource": { "name": "primary", "productName": "MySQL", "jdbcUrl": "jdbc:mysql://***", "username": "***" },
  "timing": { "prepareMs": 5, "executeMs": 300, "fetchMs": 28, "totalMs": 333 },
  "result": { "rowCount": 1, "affectedRows": 0 },
  "flags": { "slow": false, "sampled": true, "redacted": true, "batch": false }
}
```

## 安全与隐私
- 数据仅存储在 IDE 内存中（可选持久化到 .idea 下本地文件，默认关闭）。
- 对 JDBC URL、用户名与匹配规则指定字段值进行脱敏；对超长/二进制值截断或十六进制格式化。
- 不上传任何数据到远端；遵循企业合规要求可配置脱敏强度。

## 安装与使用（预期）
1. 从插件市场或本地安装插件，重启 IDEA。
2. 打开项目，运行应用；打开 Tool Window “MyBatis SQL Inspector”。
3. 在设置页配置方言、慢查询阈值、脱敏与采样策略（可选）。
4. 在列表中查看记录；选中一条，点击“复制可运行 SQL”即可粘贴到数据库客户端执行。
5. 使用过滤器缩小范围；使用统计视图定位热点与慢查询。

## 非功能性要求
- 稳定性：拦截失败不影响业务执行；异常安全。
- 性能：低开销、可退化为采样；可配置关闭。
- 可维护性：模块化方言/脱敏 SPI；单元测试覆盖关键转换逻辑。
- 国际化：优先中文，后续支持英文。

## 版本与路线图（简）
- v0.1 原型：拦截与记录、列表与详情、复制可运行 SQL（MySQL）；
- v0.2 加入统计、慢查询标记、PG/Oracle/SQLServer 方言；
- v1.0 完整设置、导出与脱敏、Spring Boot 自动集成、Gutter 提示。

---

本文档为需求基线，后续迭代以此为准并补充详细设计说明与接口契约。

