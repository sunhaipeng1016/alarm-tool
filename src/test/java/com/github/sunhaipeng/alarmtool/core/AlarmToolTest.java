package com.github.sunhaipeng.alarmtool.core;

import com.github.sunhaipeng.alarmtool.config.AlarmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AlarmTool 单元测试
 *
 * @author sunhaipeng
 */
class AlarmToolTest {

    private Properties baseProps;

    @BeforeEach
    void setUp() {
        AlarmTool.shutdown();
        baseProps = new Properties();
        baseProps.setProperty("feishu.application.appid", "test-app-id");
        baseProps.setProperty("feishu.application.appsecret", "test-app-secret");
        baseProps.setProperty("feishu.application.receiveid", "ou_test");
    }

    @AfterEach
    void tearDown() {
        AlarmTool.shutdown();
    }

    @Test
    void testAlarmLevelEnum() {
        assertEquals(1, AlarmLevel.LOW.getLevel());
        assertEquals("低", AlarmLevel.LOW.getDescription());
        assertEquals("1低", AlarmLevel.LOW.getLevelWithDescription());
        assertEquals(4, AlarmLevel.CRITICAL.getLevel());
    }

    @Test
    void testHostname() {
        String hostname = AlarmTool.getHostname();
        assertNotNull(hostname);
        assertTrue(hostname.length() > 0);
    }

    @Test
    void testAlarmWithNullException() {
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(baseProps);
        AlarmTool.init(config);

        AlarmTool.alarm(null);
        assertEquals(0, AlarmTool.getFailureCount());
    }

    @Test
    void testAlarmNotInitialized() {
        AlarmTool.alarm(new RuntimeException("test"));
        // 未初始化，不会发送，不会抛异常
    }

    @Test
    void testInitTwice() {
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(baseProps);
        AlarmTool.init(config);
        AlarmTool.init(config);
    }

    @Test
    void testAlarmConfigProperties() {
        Properties props = new Properties();
        props.setProperty("feishu.application.appid", "app-123");
        props.setProperty("feishu.application.appsecret", "secret-456");
        props.setProperty("feishu.application.receiveidtype", "open_id");
        props.setProperty("feishu.application.msgtype", "interactive");
        props.setProperty("feishu.application.receiveid", "ou_xxx");
        props.setProperty("feishu.webhook.url", "https://open.feishu.cn/xxx");

        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);

        assertEquals("app-123", config.getFeishuAppId());
        assertEquals("secret-456", config.getFeishuAppSecret());
        assertEquals("open_id", config.getFeishuReceiveIdType());
        assertEquals("interactive", config.getFeishuMsgType());
        assertEquals("ou_xxx", config.getFeishuReceiveId());
        assertEquals("https://open.feishu.cn/xxx", config.getFeishuWebhookUrl());
    }

    @Test
    void testAlarmConfigDefaultValues() {
        Properties props = new Properties();
        props.setProperty("feishu.application.appid", "app-123");
        props.setProperty("feishu.application.appsecret", "secret-456");

        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);

        assertEquals("open_id", config.getFeishuReceiveIdType());
        assertEquals("interactive", config.getFeishuMsgType());
        assertEquals("", config.getFeishuReceiveId());
        assertEquals("", config.getFeishuWebhookUrl());
    }

    @Test
    void testAlarmConfigDedupAndRateLimitDefaults() {
        Properties props = new Properties();
        props.setProperty("feishu.application.appid", "app-123");
        props.setProperty("feishu.application.appsecret", "secret-456");

        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);

        assertEquals(60, config.getAlarmDedupWindowSeconds());
        assertEquals(60, config.getAlarmMaxPerMinute());
    }

    @Test
    void testAlarmConfigDedupAndRateLimitCustom() {
        Properties props = new Properties();
        props.setProperty("feishu.application.appid", "app-123");
        props.setProperty("feishu.application.appsecret", "secret-456");
        props.setProperty("alarm.dedup.window.seconds", "120");
        props.setProperty("alarm.max.per.minute", "30");

        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);

        assertEquals(120, config.getAlarmDedupWindowSeconds());
        assertEquals(30, config.getAlarmMaxPerMinute());
    }

    @Test
    void testDedup() throws InterruptedException {
        // 设置去重窗口为 1 秒，方便测试
        Properties props = new Properties(baseProps);
        props.setProperty("alarm.dedup.window.seconds", "1");
        props.setProperty("alarm.max.per.minute", "100");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config);

        RuntimeException sameException = new RuntimeException("same error");

        AlarmTool.alarm(sameException);
        AlarmTool.alarm(sameException);
        AlarmTool.alarm(sameException);

        long dedupCount = AlarmTool.getDedupCount();
        assertTrue(dedupCount >= 2, "Expected at least 2 dedup hits, got " + dedupCount);
    }

    @Test
    void testDedupWindowExpired() throws InterruptedException {
        // 设置去重窗口为 1 秒
        Properties props = new Properties(baseProps);
        props.setProperty("alarm.dedup.window.seconds", "1");
        props.setProperty("alarm.max.per.minute", "100");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config);

        RuntimeException ex = new RuntimeException("expired test");

        AlarmTool.alarm(ex);
        long dedupBefore = AlarmTool.getDedupCount();

        // 等待超过去重窗口
        Thread.sleep(1500);

        AlarmTool.alarm(ex);
        long dedupAfter = AlarmTool.getDedupCount();

        assertEquals(dedupBefore, dedupAfter, "After window expired, should not be deduplicated");
    }

    @Test
    void testRateLimit() {
        // 设置每分钟最多 3 条
        Properties props = new Properties(baseProps);
        props.setProperty("alarm.dedup.window.seconds", "0");
        props.setProperty("alarm.max.per.minute", "3");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config);

        // 发送超过 3 条不同异常
        for (int i = 0; i < 5; i++) {
            AlarmTool.alarm(new RuntimeException("error" + i));
        }

        long rateLimitCount = AlarmTool.getRateLimitCount();
        assertTrue(rateLimitCount >= 2, "Expected at least 2 rate-limited, got " + rateLimitCount);
    }

    @Test
    void testFullReportBypassDedup() {
        Properties props = new Properties(baseProps);
        props.setProperty("alarm.dedup.window.seconds", "60");
        props.setProperty("alarm.max.per.minute", "100");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config);

        RuntimeException sameException = new RuntimeException("full report test");

        // 安全模式：第一条发出，第二条被去重
        AlarmTool.alarm(sameException, null, AlarmLevel.LOW, null, true, false);
        long dedupBefore = AlarmTool.getDedupCount();
        AlarmTool.alarm(sameException, null, AlarmLevel.LOW, null, true, false);
        assertTrue(AlarmTool.getDedupCount() > dedupBefore, "Should be deduplicated in safe mode");

        // 全量模式：跳过去重，直接发送
        AlarmTool.alarm(sameException, null, AlarmLevel.LOW, null, true, true);
        long dedupAfterFull = AlarmTool.getDedupCount();
        // 全量模式不会增加去重计数
        assertEquals(dedupBefore + 1, dedupAfterFull, "Full report should not increment dedup count");
    }

    @Test
    void testFullReportBypassRateLimit() {
        Properties props = new Properties(baseProps);
        props.setProperty("alarm.dedup.window.seconds", "0");
        props.setProperty("alarm.max.per.minute", "2");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config);

        // 安全模式：前 2 条发出，第 3 条被限流
        AlarmTool.alarm(new RuntimeException("rl1"), null, AlarmLevel.LOW, null, true, false);
        AlarmTool.alarm(new RuntimeException("rl2"), null, AlarmLevel.LOW, null, true, false);
        AlarmTool.alarm(new RuntimeException("rl3"), null, AlarmLevel.LOW, null, true, false);
        long rateLimitBefore = AlarmTool.getRateLimitCount();
        assertTrue(rateLimitBefore >= 1, "Should be rate-limited");

        // 全量模式：跳过低流，直接发送
        AlarmTool.alarm(new RuntimeException("full1"), null, AlarmLevel.LOW, null, true, true);
        // 全量模式不会增加限流计数
        assertEquals(rateLimitBefore, AlarmTool.getRateLimitCount(), "Full report should not increment rate-limit count");
    }

    @Test
    void testQueueDiscard() {
        Properties props = new Properties(baseProps);
        props.setProperty("alarm.dedup.window.seconds", "0");
        props.setProperty("alarm.max.per.minute", "10000");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config);

        // 大量发送告警，超过队列容量 500
        for (int i = 0; i < 600; i++) {
            try {
                AlarmTool.alarm(new RuntimeException("discard " + i), null, AlarmLevel.LOW, null, true, false);
            } catch (Exception e) {
                // 忽略
            }
        }

        long discardCount = AlarmTool.getDiscardCount();
        // 队列容量 500，发送 600，应该至少有丢弃
        assertTrue(discardCount > 0, "Expected some discards, got " + discardCount);
    }

    @Test
    void testFailureCount() {
        Properties props = new Properties(baseProps);
        props.setProperty("alarm.dedup.window.seconds", "60");
        props.setProperty("alarm.max.per.minute", "100");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config);

        long before = AlarmTool.getFailureCount();
        // 发送告警（appid 是假的，会失败，但失败计数在异步回调中增加）
        AlarmTool.alarm(new RuntimeException("failure test"));
        // 等待异步回调
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long after = AlarmTool.getFailureCount();
        assertTrue(after >= before, "Failure count should not decrease");
    }

    @Test
    void testWebhookMode() {
        Properties props = new Properties();
        props.setProperty("feishu.webhook.url", "https://open.feishu.cn/open-apis/bot/v2/hook/test");
        props.setProperty("alarm.max.per.minute", "100");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config);

        AlarmTool.alarm(new RuntimeException("webhook test"));
    }

    @Test
    void testShutdown() {
        Properties props = new Properties(baseProps);
        props.setProperty("alarm.max.per.minute", "100");
        AlarmConfig config = new AlarmConfig.PropertiesAlarmConfig(props);
        AlarmTool.init(config);

        AlarmTool.shutdown();

        long dedup = AlarmTool.getDedupCount();
        assertEquals(0, dedup, "After shutdown, counters should be clean");
    }
}