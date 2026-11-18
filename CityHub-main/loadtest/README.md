# CityHub 秒杀压测

## 准备

1. 导入数据库并启动 MySQL、Redis、RabbitMQ 和后端服务。
2. 创建至少 1000 个测试用户。
3. 运行 `VoucherOrderControllerTest.login`，生成：

   `target/loadtest/tokens.csv`

   文件包含 `token,ip` 两列。每个虚拟用户使用独立 JWT 和独立 IP，避免秒杀接口的 IP 滑动窗口限流把整轮压测视为同一来源。
4. 准备一个秒杀券，并记录其 ID。

## 执行

```bash
jmeter \
  -n \
  -t loadtest/jmeter/seckill.jmx \
  -Jtokens=target/loadtest/tokens.csv \
  -JVOUCHER_ID=1 \
  -JBASE_URL=http://localhost:8081 \
  -l target/loadtest/seckill-result.jtl \
  -e -o target/loadtest/report
```

计划默认 1000 并发、1 秒启动完成、每用户执行 1 次。压测结果取决于硬件、库存和中间件配置，仓库不写入固定 QPS 或响应时间结论。
