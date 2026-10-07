package com.fitness.dao;

import com.fitness.db.DatabaseManager;
import com.fitness.model.SystemSetting;

import java.util.*;

public class SystemSettingsDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public Map<String, String> getAllAsMap() {
        String sql = "SELECT setting_key, setting_value FROM system_settings;";
        List<Map<String, Object>> rows = db.query(sql, Collections.emptyList());
        Map<String, String> result = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String k = String.valueOf(r.get("setting_key"));
            String v = r.get("setting_value") != null ? r.get("setting_value").toString() : "";
            result.put(k, v);
        }
        return result;
    }

    public List<SystemSetting> getAllSettings() {
        String sql = "SELECT * FROM system_settings ORDER BY setting_key ASC;";
        List<Map<String, Object>> rows = db.query(sql, Collections.emptyList());
        List<SystemSetting> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            SystemSetting s = new SystemSetting();
            s.setKey(String.valueOf(r.get("setting_key")));
            s.setValue(r.get("setting_value") != null ? r.get("setting_value").toString() : "");
            s.setDescription(r.get("setting_description") != null ? r.get("setting_description").toString() : "");
            s.setUpdatedAt(r.get("updated_at") != null ? r.get("updated_at").toString() : "");
            list.add(s);
        }
        return list;
    }

    public String get(String key) {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = ?;";
        Map<String, Object> row = db.queryOne(sql, Collections.singletonList(key));
        if (row != null && row.get("setting_value") != null) {
            return row.get("setting_value").toString();
        }
        return null;
    }

    public boolean update(String key, String value) {
        String sql = "INSERT INTO system_settings (setting_key, setting_value, updated_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value, updated_at = CURRENT_TIMESTAMP;";
        return db.executeUpdate(sql, Arrays.asList(key, value)) > 0;
    }

    public void updateBatch(Map<String, Object> settings) {
        for (Map.Entry<String, Object> e : settings.entrySet()) {
            if (e.getValue() != null) {
                update(e.getKey(), e.getValue().toString());
            }
        }
    }
}
