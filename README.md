# UniCoupon 优惠券系统

## 项目简介

**UniCoupon** 是一款高性能、高可用的优惠券系统，基于 Spring Boot 3 和 JDK 17 构建，能够承受近十万次查询和分发请求的高并发压力。系统提供从优惠券模板创建、批量分发、结算、搜索、核销到管理的完整生命周期能力，适用于电商平台、本地生活服务等多种业务场景。

## 业务架构

系统采用微服务架构设计，涵盖服务层、组件层和基础设施层，各模块职责清晰，支持水平扩展和高并发场景。

```
┌─────────────────────────────────────────────────────────────┐
│                        网关层 (Gateway)                       │
│              动态路由 / 日志记录 / 限流熔断                      │
├─────────────────────────────────────────────────────────────┤
│   商户后台          引擎服务         分发服务      结算服务      搜索服务  │
│ (merchant-admin)    (engine)    (distribution) (settlement) (search) │
├─────────────────────────────────────────────────────────────┤
│                       基础框架 (framework)                    │
│             统一返回 / 全局异常 / 幂等拦截 / 自动装配            │
├─────────────────────────────────────────────────────────────┤
│                        基础设施层                              │
│   MySQL + ShardingSphere │ Redis + Redisson │ RocketMQ       │
│   Elasticsearch │ XXL-Job │ Nacos │ Sentinel                │
└─────────────────────────────────────────────────────────────┘
```

## 技术架构

| 类别 | 技术选型 | 版本 |
|------|---------|------|
| 基础框架 | Spring Boot | 3.0.7 |
| 微服务治理 | Spring Cloud + Spring Cloud Alibaba | 2022.0.3 |
| JDK | Java | 17 |
| ORM | MyBatis-Plus | 3.5.7 |
| 分库分表 | Apache ShardingSphere | 5.3.2 |
| 消息队列 | Apache RocketMQ | 5.x (spring-boot-starter 2.3.0) |
| 缓存 | Redis + Redisson | 3.27.2 |
| 搜索引擎 | Elasticsearch | spring-boot-starter 2.6.12 |
| 定时任务 | XXL-Job | 2.4.1 |
| API 文档 | Knife4j (Swagger) | 4.5.0 |
| 操作日志 | BizLog SDK | 3.0.6 |
| Excel 处理 | EasyExcel | 4.0.1 |
| 工具库 | Hutool / Fastjson2 / Guava | 5.8.27 / 2.0.36 / 30.0 |

## 模块说明

| 模块 | 端口 | 描述 |
|------|------|------|
| **framework** | - | 基础架构模块，提供统一返回结果、错误码、全局异常拦截、幂等防重、Web 自动装配等公共功能 |
| **gateway** | - | 网关模块，基于 Spring Cloud Gateway，提供动态路由、请求日志记录、链路追踪及限流熔断 |
| **merchant-admin** | - | 商户后台管理模块，负责优惠券模板创建/管理、发放批次创建、XXL-Job 定时扫描、延迟消息消费 |
| **engine** | - | 引擎模块，负责优惠券详情查询、列表查看、优惠券锁定以及核销等核心功能 |
| **distribution** | - | 分发模块，负责按批次分发用户优惠券，支持站内信、弹框推送、短信、邮件等多种通知方式 |
| **settlement** | - | 结算模块，负责用户下单时订单金额计算，与订单相关联，流量较大 |
| **search** | - | 搜索模块，基于 Elasticsearch 提供用户优惠券搜索功能 |

## 核心特性

### 1. 分库分表 (ShardingSphere)

采用自定义 HashMod 分片算法，对优惠券模板表 `t_coupon_template` 和用户优惠券表 `t_user_coupon` 进行分库分表，支持海量数据存储与高效查询：

- `t_coupon_template`：2 库 × 16 表（按 `shop_number` 分片）
- `t_user_coupon`：2 库 × 32 表（按 `user_id` 分片）

### 2. 缓存与布隆过滤器

- **Redis 缓存**：优惠券模板数据通过 Lua 脚本原子写入 Redis，设置业务有效期自动过期
- **Redisson Bloom Filter**：缓存预热前通过布隆过滤器快速判断优惠券模板是否存在，防止缓存穿透
- **分布式锁**：Redisson 提供分布式锁，保证高并发下的数据一致性

### 3. 消息队列 (RocketMQ)

- **延时消息**：优惠券模板创建后发送延时消息，到期自动修改模板状态为已结束
- **异步分发**：优惠券推送任务通过 RocketMQ 异步执行，解耦发放流程，提升系统吞吐量

### 4. 幂等防重

基于注解 `@NoDuplicateSubmit` + Redis Token 机制，防止用户重复提交表单（如重复创建优惠券模板、重复创建推送任务），保证接口幂等性。

### 5. 批量分发与 Excel 处理

支持通过上传 Excel 文件进行优惠券批量分发，使用 EasyExcel 流式读取百万级数据，内存占用极低。

### 6. 操作日志

通过 BizLog SDK 的 `@LogRecord` 注解，记录优惠券创建、发行量变更、结束等关键操作，支持操作审计与溯源。

### 7. 责任链校验

优惠券模板创建时采用责任链模式进行参数校验，将参数非空校验、基础校验、业务规则校验等划分为独立节点，支持灵活扩展。

### 8. 定时任务

通过 XXL-Job 实现定时扫描待执行的优惠券推送任务，支持立即发送和定时发送两种模式。

## 项目结构

```
oneCoupon/
├── framework/                    # 基础架构模块
│   └── src/main/java/edu/cnan/onecoupon/framework/
│       ├── config/               # 自动装配配置
│       │   ├── IdempotentConfiguration.java
│       │   └── WebAutoConfiguration.java
│       ├── errorcode/            # 错误码定义
│       │   ├── BaseErrorCode.java
│       │   └── IErrorCode.java
│       ├── exception/            # 异常类
│       │   ├── AbstractException.java
│       │   ├── ClientException.java
│       │   ├── RemoteException.java
│       │   └── ServiceException.java
│       ├── idempotent/           # 幂等防重
│       │   ├── NoDuplicateSubmit.java
│       │   └── NoDuplicateSubmitAspect.java
│       ├── result/               # 统一返回
│       │   └── Result.java
│       └── web/                  # Web 层
│           ├── GlobalExceptionHandler.java
│           └── Results.java
├── gateway/                      # 网关模块
│   └── src/main/java/edu/cnan/onecoupon/gateway/
│       ├── filter/
│       │   └── RequestLoggingFilter.java
│       └── GatewayApplication.java
├── merchant-admin/               # 商户后台管理模块
│   └── src/main/java/edu/cnan/onecoupon/merchant/admin/
│       ├── controller/
│       │   ├── CouponTaskController.java
│       │   └── CouponTemplateController.java
│       ├── service/
│       │   ├── impl/             # 业务实现
│       │   └── handler/filter/   # 责任链过滤器
│       ├── dao/
│       │   ├── entity/           # 数据实体
│       │   ├── mapper/           # MyBatis Mapper
│       │   └── sharding/         # 分片算法
│       ├── mq/
│       │   ├── consumer/         # MQ 消费者
│       │   ├── producer/         # MQ 生产者
│       │   └── event/            # 消息事件
│       └── job/                  # XXL-Job 处理器
├── distribution/                 # 分发模块
│   └── src/main/java/edu/cnan/onecoupon/distribution/
│       ├── dao/
│       │   ├── entity/           # CouponTaskDO / CouponTemplateDO / UserCouponDO
│       │   ├── mapper/
│       │   └── sharding/
│       ├── mq/
│       │   ├── consumer/         # CouponTaskExecuteConsumer
│       │   └── event/
│       └── service/handler/
│           └── excel/            # Excel 读取监听器
├── engine/                       # 引擎模块
│   └── src/main/java/edu/cnan/onecoupon/engine/
│       ├── controller/           # CouponTemplateController
│       └── service/
├── settlement/                   # 结算模块
│   └── src/main/java/edu/cnan/onecoupon/settlement/
└── search/                       # 搜索模块
    └── src/main/java/edu/cnan/onecoupon/search/
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RocketMQ 5.x
- Nacos 2.x
- Elasticsearch 7.x+

### 启动步骤

1. **克隆项目**

```bash
git clone <repository-url>
cd oneCoupon
```

2. **配置数据库**

修改各模块 `src/main/resources/shardingsphere-config.yaml` 中的数据源连接信息。

3. **启动依赖服务**

确保 MySQL、Redis、RocketMQ、Nacos、Elasticsearch 等服务已启动并可访问。

4. **编译项目**

```bash
mvn clean install -DskipTests
```

5. **按顺序启动模块**

```
framework（编译依赖） → gateway → merchant-admin → engine → distribution → settlement → search
```

## 数据库表设计

| 表名 | 说明 | 分片策略 |
|------|------|---------|
| `t_coupon_template` | 优惠券模板表 | 按 shop_number 分库分表 (2库×16表) |
| `t_user_coupon` | 用户优惠券表 | 按 user_id 分库分表 (2库×32表) |
| `t_coupon_task` | 优惠券推送任务表 | - |
| `t_coupon_template_log` | 优惠券模板操作日志表 | - |

## 核心业务流程

### 优惠券创建与分发

```
商户创建模板 → 责任链校验 → 写入数据库 → 缓存预热 + 布隆过滤器
    → 发送延时消息(到期自动结束)
    → 商户创建推送任务 → XXL-Job 扫描/立即执行
    → RocketMQ 消息 → 分发服务消费 → 批量写入用户优惠券
    → 通知用户(站内信/弹框/短信/邮件)
```

### 优惠券使用与结算

```
用户选择优惠券 → 引擎服务锁定优惠券
    → 下单结算 → 结算服务计算优惠后金额
    → 支付完成 → 优惠券核销
```

## License

本项目保留所有权利。
