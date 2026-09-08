package com.github.sunhaipeng.alarmtool.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 告警配置接口，定义飞书告警所需的配置项。
 * 内置 Properties 默认实现，也可对接 Apollo、Nacos 等配置中心。
 *
 * @author sunhaipeng
 */
public interface AlarmConfig {

    /** 飞书应用 AppId，从飞书开放平台获取 */
    String getFeishuAppId();

    /** 飞书应用 AppSecret */
    String getFeishuAppSecret();

    /** 接收者 ID 类型，如 open_id、user_id、chat_id */
    String getFeishuReceiveIdType();

    /** 消息类型，卡片消息固定为 interactive */
    String getFeishuMsgType();

    /** 接收者 ID，可以是用户、群聊或部门 */
    String getFeishuReceiveId();

    /** 飞书机器人 Webhook 地址，仅 Webhook 模式需要 */
    String getFeishuWebhookUrl();

    /** 去重窗口（秒），同一异常在此时间内只发一次告警，默认 60 秒 */
    int getAlarmDedupWindowSeconds();

    /** 每分钟最大告警条数，超过则丢弃，默认 60 */
    int getAlarmMaxPerMinute();

    /**
     * 基于 Properties 的默认配置实现。
     * 配置 key 命名规范：feishu.application.* 和 feishu.webhook.*
     */
    class PropertiesAlarmConfig implements AlarmConfig {

        private final Properties properties;

        public PropertiesAlarmConfig(InputStream inputStream) throws IOException {
            this.properties = new Properties();
            this.properties.load(inputStream);
        }

        public PropertiesAlarmConfig(Properties properties) {
            this.properties = properties;
        }

        @Override
        public String getFeishuAppId() {
            return properties.getProperty("feishu.application.appid", "");
        }

        @Override
        public String getFeishuAppSecret() {
            return properties.getProperty("feishu.application.appsecret", "");
        }

        @Override
        public String getFeishuReceiveIdType() {
            return properties.getProperty("feishu.application.receiveidtype", "open_id");
        }

        @Override
        public String getFeishuMsgType() {
            return properties.getProperty("feishu.application.msgtype", "interactive");
        }

        @Override
        public String getFeishuReceiveId() {
            return properties.getProperty("feishu.application.receiveid", "");
        }

        @Override
        public String getFeishuWebhookUrl() {
            return properties.getProperty("feishu.webhook.url", "");
        }

        @Override
        public int getAlarmDedupWindowSeconds() {
            String val = properties.getProperty("alarm.dedup.window.seconds", "60");
            return Integer.parseInt(val);
        }

        @Override
        public int getAlarmMaxPerMinute() {
            String val = properties.getProperty("alarm.max.per.minute", "60");
            return Integer.parseInt(val);
        }
    }
}