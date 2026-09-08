package com.github.sunhaipeng.alarmtool.core;

import com.github.sunhaipeng.alarmtool.feishu.model.CardMessage;

import java.util.Map;

/**
 * 告警发送器接口，所有告警通道（飞书、钉钉、企微等）都应实现此接口。
 *
 * @author sunhaipeng
 */
public interface AlarmSender {

    /**
     * 发送告警消息。
     * values 中可包含的 key：exception_type、alarm_level、occurrence_time、
     * host_name、error_info、stack_info（可选）、message（可选）、zipkin_url（可选）
     */
    void send(Map<String, String> values) throws Exception;

    /**
     * 获取卡片消息模板，子类可重写以定制卡片样式。
     */
    CardMessage getCardMessageTemplate();
}