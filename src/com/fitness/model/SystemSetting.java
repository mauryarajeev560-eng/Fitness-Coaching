package com.fitness.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class SystemSetting {
    private String key;
    private String value;
    private String description;
    private String updatedAt;

    public SystemSetting() {}

    public SystemSetting(String key, String value, String description, String updatedAt) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.updatedAt = updatedAt;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("value", value);
        map.put("description", description);
        map.put("updated_at", updatedAt);
        return map;
    }
}
