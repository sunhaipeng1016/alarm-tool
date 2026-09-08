package com.github.sunhaipeng.alarmtool.feishu;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TemplateReplacer 单元测试
 *
 * @author sunhaipeng
 */
class TemplateReplacerTest {

    @Test
    void testReplacePlaceholders() {
        String template = "Hello ${name}, your score is ${score}";
        Map<String, String> values = new HashMap<>();
        values.put("name", "World");
        values.put("score", "100");

        String result = TemplateReplacer.replacePlaceholders(template, values);
        assertEquals("Hello World, your score is 100", result);
    }

    @Test
    void testReplaceWithNullTemplate() {
        Map<String, String> values = new HashMap<>();
        values.put("key", "value");
        assertNull(TemplateReplacer.replacePlaceholders(null, values));
    }

    @Test
    void testReplaceWithNullValues() {
        String template = "Hello ${name}";
        assertEquals("Hello ${name}", TemplateReplacer.replacePlaceholders(template, null));
    }

    @Test
    void testReplaceWithEmptyValues() {
        String template = "Hello ${name}";
        Map<String, String> values = new HashMap<>();
        assertEquals("Hello ${name}", TemplateReplacer.replacePlaceholders(template, values));
    }

    @Test
    void testReplaceWithMissingKey() {
        String template = "Hello ${name}, ${missing}";
        Map<String, String> values = new HashMap<>();
        values.put("name", "World");

        String result = TemplateReplacer.replacePlaceholders(template, values);
        assertEquals("Hello World, ", result);
    }

    @Test
    void testReplaceWithSpecialCharacters() {
        String template = "Message: ${msg}";
        Map<String, String> values = new HashMap<>();
        values.put("msg", "Line1\nLine2\t\"quoted\"");

        String result = TemplateReplacer.replacePlaceholders(template, values);
        assertEquals("Message: Line1\\nLine2\\t\\\"quoted\\\"", result);
    }

    @Test
    void testReplaceWithNullValue() {
        String template = "Value: ${key}";
        Map<String, String> values = new HashMap<>();
        values.put("key", null);

        String result = TemplateReplacer.replacePlaceholders(template, values);
        assertEquals("Value: ", result);
    }
}