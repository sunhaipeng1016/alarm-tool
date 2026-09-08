package com.github.sunhaipeng.alarmtool.feishu;

import com.github.sunhaipeng.alarmtool.config.AlarmConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 飞书 Webhook 发送器，通过机器人 Webhook URL 发送纯文本消息。
 * 无需飞书应用凭证，只需在群聊中添加自定义机器人即可使用。
 *
 * @author sunhaipeng
 */
public class FeishuWebhookSender {

    private static final Logger logger = LoggerFactory.getLogger(FeishuWebhookSender.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String webhookUrl;

    public FeishuWebhookSender(AlarmConfig config) {
        this.webhookUrl = config.getFeishuWebhookUrl();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /** 发送文本消息到飞书，消息内容会经过 JSON 转义 */
    public void send(String message) throws IOException {
        String escapedMessage = escapeJsonString(message);
        String jsonBody = "{\"msg_type\":\"text\",\"content\":{\"text\":\"" + escapedMessage + "\"}}";

        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBodyStr = response.body() != null ? response.body().string() : "";
            logger.debug("Feishu webhook response. Status: {}, Body: {}", response.code(), responseBodyStr);

            if (!response.isSuccessful()) {
                throw new IOException("Feishu webhook error: status=" + response.code() + ", body=" + responseBodyStr);
            }
        }
    }

    /** 对字符串中的特殊字符进行 JSON 转义，防止注入 */
    private String escapeJsonString(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 释放 HTTP 连接资源 */
    public void close() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}