package com.github.sunhaipeng.alarmtool.core;

/**
 * 告警级别：LOW(1)、MEDIUM(2)、HIGH(3)、CRITICAL(4)
 *
 * @author sunhaipeng
 */
public enum AlarmLevel {

    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高"),
    CRITICAL(4, "严重");

    private final int level;
    private final String description;

    AlarmLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    /** 返回 "数字+描述" 组合，如 "3高" */
    public String getLevelWithDescription() {
        return level + description;
    }

    @Override
    public String toString() {
        return level + "(" + description + ")";
    }
}