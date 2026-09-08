package com.github.sunhaipeng.alarmtool.feishu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板替换工具，将模板中的 ${key} 占位符替换为 Map 中对应的值，并自动 JSON 转义。
 *
 * @author sunhaipeng
 */
public class TemplateReplacer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TemplateReplacer() {
    }

    /**
     * 替换模板中的 ${key} 占位符。Map 中不存在的 key 用空字符串替换。
     * 替换值会经过 JSON 转义，确保特殊字符不破坏 JSON 结构。
     */
    public static String replacePlaceholders(String template, Map<String, String> values) {
        if (template == null || values == null || values.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.getOrDefault(key, "");
            String escapedValue = jsonEscape(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(escapedValue));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /** 利用 Jackson 将字符串转为 JSON 字面量，去掉首尾引号后返回 */
    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        try {
            String quoted = OBJECT_MAPPER.writeValueAsString(value);
            if (quoted.length() >= 2) {
                return quoted.substring(1, quoted.length() - 1);
            }
            return quoted;
        } catch (JsonProcessingException e) {
            return value;
        }
    }
}