# 开发计划：IntelliJ IDEA 插件（MyBatis SQL 可视化与耗时分析）

本文基于《readme.md》的需求，规划插件端与采集端的总体方案与落地步骤，重点阐述参数获取、方言支持与时间计算。版本里程碑采用“小步快跑、先可用后完善”的策略。

## 一、总体架构
- 形态：IDEA 插件（展示/控制）+ 运行时采集（MyBatis 拦截器/可选 JDBC 代理/日志适配）+ 本地传输通道（HTTP/Socket，JSON 事件）
- 数据流：应用进程采集 → 127.0.0.1 通道上报 → IDE 应用服务接收与存储 → Tool Window 实时展示/复制/跳转
- 采集模式（三选一，可配置优先级）：
  - MYBATIS_INTERCEPTOR（默认）：注册 MyBatis 拦截器，直接访问 BoundSql 与参数映射，精确渲染最终 SQL，记录分段耗时
  - JDBC_PROXY（可选）：集成 p6spy 或 datasource-proxy，于 JDBC 层抓取 SQL 与时长，参数字面按驱动输出解析/重渲染
  - LOG_ADAPTER（兜底）：解析 MyBatis 日志（Preparing/Parameters/Total），结构化能力弱，但零侵入
- 传输：沿用现有本地 HTTP 接收（/sql），定义事件 schema v0，后续支持 Socket/UDS 以降低开销

## 二、参数获取（如何拿到参数并渲染为可运行 SQL）
- 拦截点选择：
  - StatementHandler.prepare（统计 prepare）与 Executor.query/update（统计 execute）以及 ResultSetHandler.handleResultSets（统计 fetch）
  - 从 MappedStatement/BoundSql 读取 sqlTemplate 与 ParameterMappings
- 值解析策略：
  - 基于 ParameterMapping 列表与 BoundSql.getParameterObject，按顺序/名称读取值
  - 处理命名参数、Map/POJO、集合与数组（foreach IN 场景），枚举与 Null
  - 调用 TypeHandler（若可用）或基于 JDBC Type/Java Type 的默认转换，生成 valuePreview 与 isNull 等标记
  - 批量执行：捕获 batch 中每一组参数，渲染为多条独立 SQL 或合并 INSERT VALUES 列表（可配置）
- 渲染规则（与方言解耦，见下一节）：
  - 逐个替换“?”占位符：对字符串/日期/时间/二进制/数字/布尔/NULL 做字面量格式化与必要转义
  - IN 列表按实际参数展开；不支持的 OUT 参数/存储过程标注限制
- 安全与隐私：
  - originalValue 默认不持久化，仅保留 valuePreview；支持字段名/类型/正则脱敏
  - 大字段与二进制值截断；binary 可转十六进制预览

## 三、方言支持（如何输出可直接粘贴运行的 SQL）
- 设计 SPI：SqlDialect 接口
  - 方法：escapeString、formatDate、formatTimestamp、formatBoolean、formatBinary、quoteIdentifier、formatNumber、nullLiteral 等
  - 自动推断：优先读取 DataSource 元信息/JDBC URL，或按设置页 defaultDialect 覆盖
- 实现优先级：
  - v0.1：MySQL（单引号转义、0xHEX、日期/时间字面量、布尔 0/1）
  - v0.2：PostgreSQL（E'' 可选、bytea、true/false）、Oracle（TO_DATE/TO_TIMESTAMP、hextoraw）、SQL Server（N''/日期格式）
- 测试与校验：
  - 为每种类型建立 golden cases（字符串边界、emoji/多字节、极端日期/闰秒、BigDecimal 非科学计数法、二进制）
  - 单元测试覆盖每个方言的转义与格式化

## 四、时间计算（如何精准计算耗时）
- 分段定义：prepareMs、executeMs、fetchMs、totalMs（total ≈ prepare+execute+fetch，但以实测为准）
- 采集方式：
  - MYBATIS_INTERCEPTOR：在拦截器中使用 System.nanoTime() 包裹各阶段；确保 finally 记录
  - JDBC_PROXY：在 PreparedStatement.execute/executeQuery/executeUpdate 前后计时，fetch 通过 ResultSet 包装器统计首行与遍历时间（可采样）
  - LOG_ADAPTER：从日志解析 “Total: XXX ms” 作为 fallback，仅 total 可用
- 精度与开销：
  - 使用单次 nanoTime 差值，避免频繁时钟查询；可配置采样率降低开销
  - 异常路径保证记录 endedAt 与可用的分段值

## 五、IDEA 插件端实现（展示与控制）
- 接收服务：沿用 applicationService（SqlReceiverService），端口解析规则与回环地址，增加 schema 校验与限流
- Tool Window：“MyBatis SQL Inspector”
  - 列表列：时间/总耗时/慢标记/SQL 类型/Mapper/方法/数据源/行数/状态
  - 详情页：渲染后 SQL（语法高亮/复制）、参数表、分段耗时、结果/错误、调用栈
  - 操作：复制可运行 SQL、复制 JSON、跳转源码（定位 mapper XML/注解方法）、过滤与搜索
- 运行配置集成：
  - 为 From IDEA 启动的 Run/Debug 自动注入 -Dmybatis.timecost.enabled=true、-Dmybatis.timecost.port=xxxx
  - 提供 Settings 开关与方言/慢阈值/脱敏/采样等配置
- 存储与清理：环形缓冲/最大记录数与时间保留策略；支持导出 JSON/CSV/Markdown

## 六、传输协议（事件 Schema v0）
- 字段对齐《readme.md》数据模型（id/startedAt/endedAt/durationMs/threadName/mapperId/sqlTemplate/sqlRendered/parameters/dataSource/timing/result/flags/batch）
- 控制项：schemaVersion、projectName/moduleName、ideHints（用于跳转）
- 大小控制：valuePreview 有长度上限；binaryAsHex 可选；支持 sampled 标记

## 七、设置项（Settings）
- 采集控制：enableCapture、samplingRate、ignoredPackages、maxStackDepth
- 慢查询：slowThresholdMs、slowHighlight
- 可运行 SQL：defaultDialect、stringMaxLen、binaryAsHex
- 隐私：redactJdbcUrl、redactUsername、valueRedactionRules、maxValueLength
- 集成方式：interceptorMode（MYBATIS_INTERCEPTOR/JDBC_PROXY/LOG_ADAPTER）、springBootAutoConfig

## 八、测试与验证
- 单元测试：方言转义/类型映射/参数展开/IN 列表/空值/边界值
- 集成测试：示例 Spring Boot 项目（H2/MySQL），校验渲染 SQL 与分段耗时；IDEA 沙箱验证 Tool Window 功能
- 性能评估：常见查询平均开销 <5%，在高频场景下通过采样/忽略包减载

## 九、里程碑与交付
- v0.1（原型可用）
  - 实现 MyBatis 拦截器采集 + 分段耗时
  - MySQL 方言渲染与“可运行 SQL”复制
  - 本地 HTTP 事件接收与 Tool Window 基础列表/详情
  - 设置页最小集（端口/方言/慢阈值/开关），日志适配兜底
- v0.2
  - PostgreSQL/Oracle/SQL Server 方言
  - 统计视图（AVG/P95/MAX/次数），过滤与搜索增强
  - Run/Debug 自动注入与 Spring Boot 自动配置
  - Gutter/内联提示与源码跳转
- v1.0
  - JDBC 代理与 p6spy 集成、导出/脱敏完善、国际化与稳定性收尾

## 十、风险与对策
- 端口占用：支持自动重试与配置端口，IDE 与应用协商
- 高版本 JDK/IDE 兼容：使用官方 openapi API，持续 CI 验证
- 多数据源与批量：以 BatchInfo 与 DataSourceInfo 建模，完善测试用例
- 不同日志格式：可配置 log patterns，或提示启用拦截器/代理模式

## 十一、验收标准（关键用例）
- 能在 Tool Window 中看到实时记录，包含渲染后 SQL 与总耗时
- 复制的 SQL 可直接在对应数据库客户端运行（MySQL 起步）
- 分段耗时与慢阈值标记正确；异常查询可见错误摘要
- 可通过设置切换方言/端口/采样/脱敏，不影响业务执行

—— 以上计划将结合现有 `SqlReceiverService` 与 `plugin.xml` 基础，优先完成“参数获取 → 可运行 SQL（MySQL）→ 时间计算 → IDE 展示”的主干功能，再逐步补齐方言与统计能力。

