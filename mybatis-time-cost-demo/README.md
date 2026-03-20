# mybatis-time-cost-demo

最小可运行测试项目：Spring Boot + MyBatis + H2。

## 作用

- 启动后自动建表并插入一条测试数据
- 自动执行一条 MyBatis 查询
- 通过 `mybatis-time-cost-mybatis` 将完整 SQL 和耗时发送到本地 IDEA 插件

## 启动前提

先启动 IDEA 插件沙箱：

```bash
cd ../mybatis-time-cost-idea
./gradlew runIde
```

## 启动 Demo

在仓库根目录执行：

```bash
./mybatis-time-cost-idea/gradlew -p mybatis-time-cost-demo bootRun
```

## 成功标志

- 终端打印 `Loaded user: User{id=1, name='Alice'}`
- 沙箱 IDEA 日志出现一条 `select id, name from users where id = 1`
