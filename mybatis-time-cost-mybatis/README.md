# mybatis-time-cost-mybatis

基于 MyBatis 插件机制的 SQL 采集与耗时上报组件，用于与 IntelliJ IDEA 插件“mybatis-time-cost-idea”配合，在 IDE 内实时显示可直接粘贴执行的 SQL 与耗时。

## 集成方式

### 1. MyBatis 原生配置

在 mybatis-config.xml 中注册插件：

```xml
<plugins>
  <plugin interceptor="com.mybatis.timecost.mybatis.TimeCostInterceptor"/>
</plugins>
```

### 2. Spring Boot Java 配置

```java
@Bean
public Interceptor timeCostInterceptor() {
  return new com.mybatis.timecost.mybatis.TimeCostInterceptor();
}
```

## 端口与传输

- 默认上报地址：http://127.0.0.1:17777/sql
- 可通过以下任一方式修改端口：
  - 环境变量：`MYBATIS_TIME_COST_PORT`
  - JVM 参数：`-Dmybatis.timecost.port=17888`

## 当前能力

- 拦截 `StatementHandler.prepare` 统计准备阶段耗时（内部记录），拦截 `Executor.query/update` 统计总耗时
- 获取 `BoundSql` 与参数，基于 MySQL 方言渲染可运行 SQL（字符串转义、数字、日期时间、二进制、布尔、NULL）
- 将 `sqlTemplate/sqlRendered/startedAt/endedAt/durationMs/mapperId/threadName` 以 JSON 上报到本地 IDE 插件

## 后续计划

- 完整分段耗时（prepare/execute/fetch）
- 支持 PostgreSQL/Oracle/SQL Server 方言与更多类型映射
- 采样率、忽略包与脱敏策略

