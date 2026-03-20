# MyBatis Time Cost IDEA 插件（无 UI 第一阶段）

功能（第一阶段）：
- 接收 SQL（本地 HTTP /sql）
- 打印日志（IDEA idea.log）
- 自动复制 SQL 到剪贴板
- 显示耗时（日志中展示）

## 运行与调试
- 需要本机安装 Gradle（或在 IDE 中使用 Gradle 执行）。
- 在 `mybatis-time-cost-idea` 目录执行：
  - 调试运行：`gradle runIde`
  - 打包插件：`gradle buildPlugin`

默认在本机启动 HTTP 监听：`http://127.0.0.1:17777/sql`
- 覆盖端口：设置环境变量 `MYBATIS_TIME_COST_PORT` 或 JVM 参数 `-Dmybatis.timecost.port=18080`

## 接口协议
- 方法：POST `/sql`
- Content-Type：`application/json`
- 请求体字段（至少提供 `sqlRendered` 或 `sql` 其一）：
  - sqlRendered: string（最终可运行 SQL，推荐）
  - sql: string（别名，若未提供 sqlRendered 则使用）
  - durationMs: number（耗时，毫秒，可选）
  - mapperId: string（可选）
  - threadName: string（可选）

### 返回
```json
{ "status": "ok" }
```

## 示例
```bash
curl -X POST http://127.0.0.1:17777/sql \
  -H 'Content-Type: application/json' \
  -d '{"sqlRendered":"select * from user where id = 42","durationMs":123,"mapperId":"com.acme.UserMapper.findById"}'
```

IDEA 日志将出现类似内容（同时 SQL 已复制到剪贴板）：
```
[MyBatis-TimeCost] duration=123ms mapper=com.acme.UserMapper.findById thread=main sql=select * from user where id = 42
[MyBatis-TimeCost] SQL copied to clipboard
```

## 与业务系统集成（思路）
- 在你的 MyBatis 工程中，通过自定义拦截器或 AOP 采集 `BoundSql` 渲染后的 SQL 与耗时，然后 POST 到上述接口。
- 若只需最快可用，可在现有日志钩子处将最终 SQL 与耗时发送到本地端口。

