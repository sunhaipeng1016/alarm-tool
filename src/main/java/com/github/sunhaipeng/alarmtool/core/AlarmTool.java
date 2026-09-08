package com.github.sunhaipeng.alarmtool.core;

import com.github.sunhaipeng.alarmtool.config.AlarmConfig;
import com.github.sunhaipeng.alarmtool.feishu.FeishuCardSender;
import com.github.sunhaipeng.alarmtool.feishu.FeishuWebhookSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 飞书告警工具类，异步发送告警消息到飞书群聊。
 *
 * <pre>
 * 使用示例：
 *   Properties props = new Properties();
 *   props.setProperty("feishu.application.appid", "xxx");
 *   props.setProperty("feishu.application.appsecret", "xxx");
 *   props.setProperty("feishu.application.receiveid", "ou_xxx");
 *   AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
 *   AlarmTool.init(config);
 *   AlarmTool.alarm(exception, "订单支付失败");
 * </pre>
 *
 * @author sunhaipeng
 */
public final class AlarmTool {

    private static final Logger log = LoggerFactory.getLogger(AlarmTool.class);

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAXIMUM_POOL_SIZE = 4;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 500;

    private static volatile boolean initialized = false;
    private static volatile AlarmSender alarmSender;
    private static volatile FeishuWebhookSender webhookSender;
    private static volatile String hostname;

    /** 去重窗口（秒），同一异常在此时间内只发一次 */
    private static volatile int dedupWindowSeconds = 60;
    /** 每分钟最大告警数 */
    private static volatile int maxPerMinute = 60;

    private static final AtomicLong FAILURE_COUNT = new AtomicLong(0);
    /** 去重拦截次数 */
    private static final AtomicLong DEDUP_COUNT = new AtomicLong(0);
    /** 限流拦截次数 */
    private static final AtomicLong RATE_LIMIT_COUNT = new AtomicLong(0);
    /** 队列满丢弃次数 */
    private static final AtomicLong DISCARD_COUNT = new AtomicLong(0);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 去重缓存：key = 异常类型|错误信息，value = 上一次发送时间戳 */
    private static final ConcurrentHashMap<String, Long> DEDUP_CACHE = new ConcurrentHashMap<>();

    /** 每分钟发送计数器：key = 当前分钟时间戳(秒/60)，value = 已发送数量 */
    private static final ConcurrentHashMap<Long, AtomicInteger> RATE_LIMIT_COUNTER = new ConcurrentHashMap<>();

    /** 异步发送线程池：核心2、最大4、队列500、空闲60秒回收、守护线程、队列满时丢弃 */
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(QUEUE_CAPACITY),
            new ThreadFactory() {
                private final AtomicLong threadNumber = new AtomicLong(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("alarm-tool-sender-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY);
                    return t;
                }
            },
            new ThreadPoolExecutor.DiscardPolicy() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                    DISCARD_COUNT.incrementAndGet();
                    log.warn("Alarm queue full, discard alarm. discarded:{}", DISCARD_COUNT.get());
                }
            }
    );

    private AlarmTool() {
    }

    /**
     * 初始化告警工具，幂等操作。
     * 优先使用卡片消息（需 AppId），其次使用 Webhook（需 WebhookUrl）。
     */
    public static synchronized void init(AlarmConfig config) {
        if (initialized) {
            return;
        }
        if (config.getFeishuAppId() != null && !config.getFeishuAppId().isEmpty()) {
            alarmSender = new FeishuCardSender(config);
        }
        if (config.getFeishuWebhookUrl() != null && !config.getFeishuWebhookUrl().isEmpty()) {
            webhookSender = new FeishuWebhookSender(config);
        }
        dedupWindowSeconds = config.getAlarmDedupWindowSeconds();
        maxPerMinute = config.getAlarmMaxPerMinute();
        initialized = true;
        log.info("AlarmTool initialized. dedupWindow={}s, maxPerMinute={}", dedupWindowSeconds, maxPerMinute);
    }

    /** 发送异常告警，默认 LOW 级别、安全模式 */
    public static void alarm(Exception exception) {
        alarm(exception, null, AlarmLevel.LOW, null, true, false);
    }

    /** 发送异常告警 + 自定义消息，默认 LOW 级别、安全模式 */
    public static void alarm(Exception exception, String message) {
        alarm(exception, message, AlarmLevel.LOW, null, true, false);
    }

    /**
     * 发送异常告警（完整参数）。
     *
     * @param exception  异常对象，为 null 时忽略
     * @param message    自定义消息，可为 null
     * @param alarmLevel 告警级别，为 null 时默认 LOW
     * @param zipkinUrl  链路追踪地址，可为 null
     * @param printStack 是否包含堆栈信息
     * @param fullReport 是否全量上报：true 则跳过去重和限流，每条都发到飞书；false 则走安全模式
     */
    public static void alarm(Exception exception, String message, AlarmLevel alarmLevel,
                             String zipkinUrl, boolean printStack, boolean fullReport) {
        if (exception == null) {
            return;
        }
        if (!initialized) {
            log.warn("AlarmTool not initialized, alarm ignored");
            return;
        }

        long now = System.currentTimeMillis();

        if (!fullReport) {
            // 去重检查：同一异常在去重窗口内只发一次
            String dedupKey = exception.getClass().getName() + "|" + exception.getMessage();
            Long lastSent = DEDUP_CACHE.putIfAbsent(dedupKey, now);
            if (lastSent != null) {
                long elapsed = now - lastSent;
                if (elapsed < dedupWindowSeconds * 1000L) {
                    DEDUP_COUNT.incrementAndGet();
                    log.debug("Alarm deduplicated. key={}, elapsed={}ms", dedupKey, elapsed);
                    return;
                }
                DEDUP_CACHE.put(dedupKey, now);
            }

            // 限流检查：每分钟最多发送 maxPerMinute 条
            long minuteKey = now / 60000;
            AtomicInteger minuteCount = RATE_LIMIT_COUNTER.computeIfAbsent(minuteKey, k -> new AtomicInteger(0));
            if (minuteCount.incrementAndGet() > maxPerMinute) {
                RATE_LIMIT_COUNT.incrementAndGet();
                log.warn("Alarm rate limited. minuteKey={}, count={}, max={}", minuteKey, minuteCount.get(), maxPerMinute);
                return;
            }
        }

        Map<String, String> alarmMessageMap = buildAlarmMessageMap(exception, message, alarmLevel, zipkinUrl, printStack);

        try {
            EXECUTOR.execute(() -> {
                try {
                    if (alarmSender != null) {
                        alarmSender.send(alarmMessageMap);
                    } else if (webhookSender != null) {
                        webhookSender.send(buildAlarmMessage(exception, message));
                    }
                } catch (Throwable t) {
                    FAILURE_COUNT.incrementAndGet();
                    log.error("Failed to send alarm", t);
                }
            });
        } catch (RejectedExecutionException e) {
            // 已在 DiscardPolicy 中统计
        }
    }

    /** 获取累计发送失败次数 */
    public static long getFailureCount() {
        return FAILURE_COUNT.get();
    }

    /** 获取去重拦截次数 */
    public static long getDedupCount() {
        return DEDUP_COUNT.get();
    }

    /** 获取限流拦截次数 */
    public static long getRateLimitCount() {
        return RATE_LIMIT_COUNT.get();
    }

    /** 获取队列满丢弃次数 */
    public static long getDiscardCount() {
        return DISCARD_COUNT.get();
    }

    /** 获取主机名，首次获取后缓存，DCL 保证线程安全 */
    public static String getHostname() {
        if (hostname != null) {
            return hostname;
        }
        synchronized (AlarmTool.class) {
            if (hostname != null) {
                return hostname;
            }
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                hostname = "unknown-host";
            }
            return hostname;
        }
    }

    /** 关闭线程池和 HTTP 连接，同时重置所有计数器，关闭后需重新 init */
    public static void shutdown() {
        EXECUTOR.shutdown();
        if (webhookSender != null) {
            webhookSender.close();
        }
        DEDUP_CACHE.clear();
        RATE_LIMIT_COUNTER.clear();
        FAILURE_COUNT.set(0);
        DEDUP_COUNT.set(0);
        RATE_LIMIT_COUNT.set(0);
        DISCARD_COUNT.set(0);
        initialized = false;
    }

    /** 将异常信息组装成 key-value 结构，stack_info、message、zipkin_url 为可选字段 */
    private static Map<String, String> buildAlarmMessageMap(Exception exception, String message,
                                                             AlarmLevel alarmLevel, String zipkinUrl,
                                                             boolean printStack) {
        Map<String, String> data = new HashMap<>();
        data.put("exception_type", exception.getClass().getSimpleName());
        if (Objects.isNull(alarmLevel)) {
            data.put("alarm_level", AlarmLevel.LOW.getLevelWithDescription());
        } else {
            data.put("alarm_level", alarmLevel.getLevelWithDescription());
        }
        data.put("occurrence_time", LocalDateTime.now().format(TIME_FORMATTER));
        data.put("host_name", getHostname());
        data.put("error_info", exception.getMessage() != null ? exception.getMessage() : "null");
        if (printStack) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            pw.close();
            data.put("stack_info", sw.toString());
        }
        if (message != null && !message.isEmpty()) {
            data.put("message", message);
        }
        if (zipkinUrl != null && !zipkinUrl.isEmpty()) {
            data.put("zipkin_url", zipkinUrl);
        }
        return data;
    }

    /** 构建纯文本告警消息，Webhook 模式使用 */
    private static String buildAlarmMessage(Exception exception, String customMsg) {
        StringBuilder sb = new StringBuilder();
        if (customMsg != null && !customMsg.trim().isEmpty()) {
            sb.append(customMsg.trim()).append("\n\n");
        }
        sb.append("【异常类型】").append(exception.getClass().getSimpleName()).append("\n");
        sb.append("【错误信息】").append(exception.getMessage() != null ? exception.getMessage() : "null").append("\n");
        sb.append("【发生时间】").append(LocalDateTime.now().format(TIME_FORMATTER)).append("\n");
        sb.append("【主机名】").append(getHostname()).append("\n\n");

        sb.append("【堆栈信息】\n");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        pw.close();
        sb.append(sw);

        return sb.toString();
    }
}