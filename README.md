<p align="center">
  <img src="assets/alarm-tool-logo.png" width="440" alt="AlarmTool Logo">
</p>

# AlarmTool

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Maven Central](https://img.shields.io/badge/maven-v1.0.0-green.svg)](https://github.com/sunhaipeng/alarm-tool)

轻量级 Java 异常报警工具，**一行代码**将异常告警推送到飞书群聊，支持卡片消息和 Webhook 两种模式。

---

## 目录

- [功能特性](#功能特性)
- [效果预览](#效果预览)
- [技术选型](#技术选型)
- [快速开始](#快速开始)
- [使用方式](#使用方式)
- [配置说明](#配置说明)
- [高并发保护](#高并发保护)
- [项目结构](#项目结构)
- [扩展开发](#扩展开发)
- [开源协议](#开源协议)

---

## 功能特性

- **零框架依赖**：无需 Spring，纯 Java SE 即可使用，兼容任何 Java 项目
- **异步非阻塞**：内置线程池异步发送，不阻塞业务线程
- **双模式支持**：
  - **卡片消息**（推荐）：基于飞书 Lark SDK，发送美观的交互式卡片
  - **Webhook 消息**：通过机器人 Webhook 发送纯文本，无需申请应用凭证
- **可折叠堆栈**：堆栈信息默认折叠，避免刷屏
- **链路追踪**：支持附带 Zipkin / SkyWalking 链路地址，快速定位问题
- **告警分级**：支持 LOW / MEDIUM / HIGH / CRITICAL 四个级别
- **可配置**：通过接口抽象配置，支持 Properties 文件、Apollo、Nacos 等
- **线程安全**：所有公开方法均为线程安全设计
- **高并发保护**：内置去重、限流、队列丢弃三层防护，防止告警风暴

---

## 效果预览

飞书群聊中收到的卡片消息效果：

```
┌─────────────────────────────────────┐
│  🔔 系统异常报警通知                   │  ← 蓝色标题栏
├─────────────────────────────────────┤
│  【异常类型】NullPointerException      │
│  【告警级别】3高                       │
│  【发生时间】2024-01-01 12:00:00      │
│  【主机名】server-01                  │
│  【错误信息】Cannot invoke "xxx"      │
│                                     │
│  【堆栈信息】              🔽 展开     │  ← 可折叠面板
│  ┌───────────────────────────────┐  │
│  │ java.lang.NullPointerException│  │
│  │   at com.example.OrderService │  │
│  │   ...                         │  │
│  └───────────────────────────────┘  │
│                                     │
│  【自定义消息】                        │  ← 仅当传入 message 时显示
│  订单支付失败，订单号：12345            │
│                                     │
│  【链路查看】                          │  ← 仅当传入 zipkinUrl 时显示
│  http://zipkin/xxx/trace/abc        │
└─────────────────────────────────────┘
```

---

## 技术选型

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 8+ | 开发语言，兼容 JDK 8 及以上 |
| **Jackson** | 2.15.x | JSON 序列化 / 反序列化 |
| **OkHttp** | 4.12.x | HTTP 客户端（Webhook 模式） |
| **oapi-sdk** | 2.8.x | 飞书官方 SDK（卡片消息模式） |
| **SLF4J** | 2.0.x | 日志门面 |

**为什么选这些技术？**

- **Jackson**：Spring Boot 默认 JSON 库，生态成熟，大部分项目已依赖，不需要额外引入
- **OkHttp**：Square 出品，连接池复用、HTTP/2 支持，Android 和 Java 服务端通用
- **飞书 oapi-sdk**：飞书官方提供，封装了 Token 管理和 API 调用，比手写 HTTP 请求更可靠
- **SLF4J**：日志门面，不绑定具体实现，用户可自行选择 Logback / Log4j2

---

## 快速开始

### 前提条件

- JDK 8 及以上
- Maven 3.6 及以上
- 飞书应用凭证（AppId + AppSecret）或 飞书机器人 Webhook 地址

### 1. 添加 Maven 依赖

```xml
<dependency>
    <groupId>com.github.sunhaipeng</groupId>
    <artifactId>alarm-tool</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 创建配置文件

在 `src/main/resources` 下创建 `alarm.properties`：

```properties
# 飞书应用凭证（卡片消息模式，推荐）
feishu.application.appid=cli_xxxxxxxx
feishu.application.appsecret=xxxxxxxxxxxxxxxx
feishu.application.receiveidtype=chat_id
feishu.application.msgtype=interactive
feishu.application.receiveid=oc_xxxxxxxx

# 飞书 Webhook 地址（纯文本模式，可选）
feishu.webhook.url=https://open.feishu.cn/open-apis/bot/v2/hook/xxxxxxxx

# 高并发保护参数（可选，不配置则使用默认值 60）
alarm.dedup.window.seconds=60
alarm.max.per.minute=60
```

### 3. 初始化并发送告警

```java
import com.github.sunhaipeng.alarmtool.config.AlarmConfig;
import com.github.sunhaipeng.alarmtool.core.AlarmTool;
import com.github.sunhaipeng.alarmtool.core.AlarmLevel;

import java.io.InputStream;
import java.util.Properties;

public class Demo {
    public static void main(String[] args) {
        // 方式一：从 classpath 加载配置文件
        InputStream is = Demo.class.getResourceAsStream("/alarm.properties");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(is);
        AlarmTool.init(config);

        // 方式二：代码构建配置
        Properties props = new Properties();
        props.setProperty("feishu.application.appid", "cli_xxx");
        props.setProperty("feishu.application.appsecret", "xxx");
        props.setProperty("feishu.application.receiveid", "oc_xxx");
        AlarmConfig config2 = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config2);

        // 发送告警
        try {
            // 你的业务代码...
            int result = 1 / 0;
        } catch (Exception e) {
            // 最简形式（安全模式，自动去重+限流）
            AlarmTool.alarm(e);

            // 带自定义消息
            AlarmTool.alarm(e, "订单支付失败，订单号：12345");

            // 完整参数
            AlarmTool.alarm(e, "订单支付失败", AlarmLevel.HIGH,
                    "http://zipkin.example.com/trace/abc", true);

            // 全量模式（关键业务场景，每条都发到飞书）
            AlarmTool.alarm(e, "支付回调失败", AlarmLevel.CRITICAL,
                    "http://zipkin.example.com/trace/abc", true, true);
        }
    }
}
```

---

## 使用方式

### AlarmTool 核心 API

```java
// 初始化（必须在发送告警前调用，幂等操作）
AlarmTool.init(config);

// 发送告警 - 最简形式（安全模式）
AlarmTool.alarm(exception);

// 发送告警 - 附带自定义消息（安全模式）
AlarmTool.alarm(exception, "自定义消息");

// 发送告警 - 完整参数（安全模式）
AlarmTool.alarm(exception, message, alarmLevel, zipkinUrl, printStack);

// 发送告警 - 全量模式（跳过去重+限流，每条都发）
AlarmTool.alarm(exception, message, alarmLevel, zipkinUrl, printStack, true);

// 监控指标
long failures = AlarmTool.getFailureCount();
long dedup = AlarmTool.getDedupCount();
long rateLimit = AlarmTool.getRateLimitCount();
long discard = AlarmTool.getDiscardCount();

// 获取主机名
String hostname = AlarmTool.getHostname();

// 关闭（应用关闭时调用，释放线程池和 HTTP 连接）
AlarmTool.shutdown();
```

### 参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `exception` | `Exception` | 是 | 异常对象，为 null 时忽略本次告警 |
| `message` | `String` | 否 | 自定义消息，会显示在卡片中的"自定义消息"模块 |
| `alarmLevel` | `AlarmLevel` | 否 | 告警级别，默认 LOW；可选 LOW / MEDIUM / HIGH / CRITICAL |
| `zipkinUrl` | `String` | 否 | 链路追踪地址，会显示在卡片中的"链路查看"模块 |
| `printStack` | `boolean` | 否 | 是否包含堆栈信息，默认 true；堆栈较长时可设为 false |
| `fullReport` | `boolean` | 否 | 是否全量上报，默认 false（走安全模式）；true 则跳过所有保护直接发送 |

### 告警级别

| 级别 | 数字 | 说明 | 适用场景 |
|------|------|------|----------|
| `LOW` | 1 | 低 | 一般性异常，如参数校验失败 |
| `MEDIUM` | 2 | 中 | 需要关注的异常，如第三方接口偶发超时 |
| `HIGH` | 3 | 高 | 较严重的异常，如数据库连接失败 |
| `CRITICAL` | 4 | 严重 | 核心业务异常，如支付失败、订单丢失 |

---

## 配置说明

### 配置接口

项目通过 `AlarmConfig` 接口抽象配置，内置了 `PropertiesAlarmConfig` 实现。你也可以实现该接口对接其他配置中心：

```java
// 对接 Apollo 配置中心
public class ApolloAlarmConfig implements AlarmConfig {
    @Override
    public String getFeishuAppId() {
        return ConfigService.getConfig("alarm").getProperty("feishu.appid", "");
    }
    // ... 其他方法类似
}
```

### 配置项

| 配置 Key | 默认值 | 说明 |
|----------|--------|------|
| `feishu.application.appid` | 空 | 飞书应用 AppId |
| `feishu.application.appsecret` | 空 | 飞书应用 AppSecret |
| `feishu.application.receiveidtype` | `open_id` | 接收者类型：open_id（用户）/ user_id（用户）/ chat_id（群聊） |
| `feishu.application.msgtype` | `interactive` | 消息类型，卡片消息固定为 interactive |
| `feishu.application.receiveid` | 空 | 接收者 ID，群聊以 oc_ 开头 |
| `feishu.webhook.url` | 空 | 飞书机器人 Webhook 地址 |
| `alarm.dedup.window.seconds` | `60` | 去重窗口（秒），同一异常在此时间内只发一次 |
| `alarm.max.per.minute` | `60` | 每分钟最大告警条数，超过则丢弃 |

### 发送模式优先级

1. 如果配置了 `appid` + `appsecret`：使用**卡片消息**模式（推荐）
2. 如果只配置了 `webhook.url`：使用 **Webhook 纯文本**模式
3. 两者都配置：优先使用卡片消息，卡片消息发送失败**不会**自动降级到 Webhook

---

## 高并发保护

当业务代码在高并发下出现大量报错时，会产生告警风暴。AlarmTool 内置三层防护，默认开启，无需额外配置。

### 三层防护

```
调用 AlarmTool.alarm()
        │
        ▼
  ┌─ 第一层：去重 ─────────────────────────────┐
  │ 同一异常（类名+错误信息）在 60 秒窗口内     │
  │ 只发一次，其余跳过并记录 DEDUP_COUNT       │
  └──────────────────────────────────────────┘
        │ 通过
        ▼
  ┌─ 第二层：限流 ─────────────────────────────┐
  │ 每分钟最多发送 60 条告警，超过则丢弃        │
  │ 并记录 RATE_LIMIT_COUNT                    │
  └──────────────────────────────────────────┘
        │ 通过
        ▼
  ┌─ 第三层：队列丢弃 ─────────────────────────┐
  │ 线程池队列容量 500，满后丢弃而非阻塞        │
  │ 调用方线程，并记录 DISCARD_COUNT           │
  └──────────────────────────────────────────┘
        │ 进入队列
        ▼
     异步发送到飞书
```

### 全量模式

某些关键业务场景（如支付回调失败）需要每条告警都送达飞书，此时可传入 `fullReport=true` 跳过所有保护：

```java
// 安全模式（默认）：受去重和限流保护
AlarmTool.alarm(e, "普通异常");

// 全量模式：跳过去重和限流，每条都发到飞书
AlarmTool.alarm(e, "支付回调失败", AlarmLevel.CRITICAL, null, true, true);
```

### 监控指标

可通过以下方法获取各层拦截次数，便于接入 Prometheus 等监控系统：

| 方法 | 含义 |
|------|------|
| `AlarmTool.getDedupCount()` | 去重拦截次数 |
| `AlarmTool.getRateLimitCount()` | 限流拦截次数 |
| `AlarmTool.getDiscardCount()` | 队列满丢弃次数 |
| `AlarmTool.getFailureCount()` | 发送失败次数 |

---

## 项目结构

```
alarm-tool/
├── pom.xml
├── src/main/java/com/github/sunhaipeng/alarmtool/
│   ├── config/
│   │   └── AlarmConfig.java              # 配置接口 + Properties 默认实现
│   ├── core/
│   │   ├── AlarmLevel.java               # 告警级别枚举
│   │   ├── AlarmSender.java              # 发送器接口
│   │   └── AlarmTool.java                # 核心入口类（含去重、限流、线程池）
│   └── feishu/
│       ├── FeishuCardSender.java         # 飞书卡片消息发送器
│       ├── FeishuWebhookSender.java      # 飞书 Webhook 发送器
│       ├── TemplateReplacer.java         # 模板占位符替换工具
│       └── model/
│           └── CardMessage.java          # 飞书卡片消息实体类
├── src/main/resources/
│   └── alarm.properties                  # 配置文件模板
└── src/test/java/com/github/sunhaipeng/alarmtool/
    ├── core/
    │   └── AlarmToolTest.java            # 核心类单元测试（含去重、限流、全量模式）
    └── feishu/
        └── TemplateReplacerTest.java     # 模板替换工具测试
```

**分层说明：**

| 包 | 职责 |
|----|------|
| `config` | 配置抽象层，定义配置接口和默认实现 |
| `core` | 核心层，定义告警入口、发送器接口、告警级别、高并发保护 |
| `feishu` | 飞书实现层，包含卡片消息和 Webhook 两种发送方式 |
| `feishu.model` | 飞书卡片消息实体类，对应飞书卡片 JSON 结构 |

---

## 扩展开发

### 接入其他 IM 平台（钉钉 / 企微）

实现 `AlarmSender` 接口即可：

```java
public class DingTalkAlarmSender implements AlarmSender {
    @Override
    public void send(Map<String, String> values) throws Exception {
        // 构建钉钉消息并发送
    }

    @Override
    public CardMessage getCardMessageTemplate() {
        // 钉钉无卡片概念，返回 null 或抛异常
        return null;
    }
}
```

然后在 `AlarmTool.init()` 中注册即可。

---

## 开源协议

本项目基于 [MIT License](LICENSE) 开源，可自由使用、修改和分发。

---

## 联系方式

- **作者**：sunhaipeng
- **GitHub**：[https://github.com/sunhaipeng/alarm-tool](https://github.com/sunhaipeng/alarm-tool)
- **Bug 反馈**：[提交 Issue](https://github.com/sunhaipeng/alarm-tool/issues)