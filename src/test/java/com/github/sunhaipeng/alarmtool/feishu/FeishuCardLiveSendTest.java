package com.github.sunhaipeng.alarmtool.feishu;

import com.github.sunhaipeng.alarmtool.config.AlarmConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 活体发送测试：默认跳过，设置 FEISHU_APP_ID / FEISHU_APP_SECRET / FEISHU_RECEIVE_ID
 * 环境变量后才向真实会话发送卡片，用于秒级验证卡片 2.0 结构。
 *
 * @author sunhaipeng
 */
class FeishuCardLiveSendTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "FEISHU_APP_ID", matches = ".+")
    void liveSendCard() throws Exception {
        Properties props = new Properties();
        props.setProperty("feishu.application.appid", System.getenv("FEISHU_APP_ID"));
        props.setProperty("feishu.application.appsecret", System.getenv("FEISHU_APP_SECRET"));
        props.setProperty("feishu.application.receiveidtype",
                System.getenv().getOrDefault("FEISHU_RECEIVE_ID_TYPE", "chat_id"));
        props.setProperty("feishu.application.receiveid", System.getenv("FEISHU_RECEIVE_ID"));

        FeishuCardSender sender = new FeishuCardSender(new AlarmConfig.PropertiesAlarmConfig(props));

        Map<String, String> values = new HashMap<>();
        values.put("message", "卡片 2.0 结构活体验证");
        values.put("stack_info", "com.example.DemoException: live send test\n\tat Demo.main(Demo.java:42)");
        sender.send(values);
    }
}
