package com.fitness.db;

import com.fitness.config.AppConfig;
import com.fitness.util.JsonUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DatabaseManager {

    private static DatabaseManager instance;
    private final String dbPath;
    private final String sqliteBin;

    private DatabaseManager() {
        this.dbPath = AppConfig.DB_FILE;
        this.sqliteBin = AppConfig.SQLITE_BIN;
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initDatabase() {
        try {
            File dbDir = new File(AppConfig.DB_DIR);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            File dbFile = new File(dbPath);
            boolean isNewDb = !dbFile.exists() || dbFile.length() == 0;

            if (isNewDb) {
                System.out.println("[DatabaseManager] Initializing new database at: " + dbPath);
                if (new File(AppConfig.SCHEMA_FILE).exists()) {
                    System.out.println("[DatabaseManager] Applying schema: " + AppConfig.SCHEMA_FILE);
                    executeSqlFile(AppConfig.SCHEMA_FILE);
                }
                if (new File(AppConfig.SAMPLE_DATA_FILE).exists()) {
                    System.out.println("[DatabaseManager] Seeding sample data: " + AppConfig.SAMPLE_DATA_FILE);
                    executeSqlFile(AppConfig.SAMPLE_DATA_FILE);
                }
            } else {
                // Ensure tables exist
                List<Map<String, Object>> tables = query("SELECT name FROM sqlite_master WHERE type='table' AND name='users';", Collections.emptyList());
                if (tables.isEmpty()) {
                    System.out.println("[DatabaseManager] Users table missing. Applying schema...");
                    executeSqlFile(AppConfig.SCHEMA_FILE);
                    executeSqlFile(AppConfig.SAMPLE_DATA_FILE);
                }
            }
            System.out.println("[DatabaseManager] Database initialized successfully.");
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void executeSqlFile(String filePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            executeScript(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read SQL file: " + filePath, e);
        }
    }

    public synchronized void executeScript(String script) {
        try {
            ProcessBuilder pb = new ProcessBuilder(sqliteBin, dbPath);
            Process p = pb.start();
            try (OutputStream os = p.getOutputStream()) {
                os.write(script.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                String err = readStream(p.getErrorStream());
                throw new RuntimeException("SQLite script error: " + err);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing script: " + e.getMessage(), e);
        }
    }

    public synchronized List<Map<String, Object>> query(String sql, List<Object> params) {
        String formatted = SqlHelper.formatSql(sql, params);
        try {
            ProcessBuilder pb = new ProcessBuilder(sqliteBin, "-json", dbPath, formatted);
            Process p = pb.start();
            String stdout = readStream(p.getInputStream());
            String stderr = readStream(p.getErrorStream());
            int exitCode = p.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Query error: " + stderr);
            }

            stdout = stdout.trim();
            if (stdout.isEmpty()) {
                return new ArrayList<>();
            }

            List<Object> list = JsonUtil.parseList(stdout);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) item;
                    result.add(map);
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Database query failed for SQL: [" + formatted + "]: " + e.getMessage(), e);
        }
    }

    public synchronized Map<String, Object> queryOne(String sql, List<Object> params) {
        List<Map<String, Object>> list = query(sql, params);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public synchronized long executeInsert(String sql, List<Object> params) {
        String formatted = SqlHelper.formatSql(sql, params);
        String fullSql = formatted + "; SELECT last_insert_rowid() AS id;";
        try {
            ProcessBuilder pb = new ProcessBuilder(sqliteBin, "-json", dbPath, fullSql);
            Process p = pb.start();
            String stdout = readStream(p.getInputStream());
            String stderr = readStream(p.getErrorStream());
            int exitCode = p.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Insert error: " + stderr);
            }

            List<Object> list = JsonUtil.parseList(stdout.trim());
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) list.get(0);
                Object idObj = map.get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                }
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Database insert failed: " + e.getMessage(), e);
        }
    }

    public synchronized int executeUpdate(String sql, List<Object> params) {
        String formatted = SqlHelper.formatSql(sql, params);
        String fullSql = formatted + "; SELECT changes() AS affected;";
        try {
            ProcessBuilder pb = new ProcessBuilder(sqliteBin, "-json", dbPath, fullSql);
            Process p = pb.start();
            String stdout = readStream(p.getInputStream());
            String stderr = readStream(p.getErrorStream());
            int exitCode = p.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Update error: " + stderr);
            }

            List<Object> list = JsonUtil.parseList(stdout.trim());
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) list.get(0);
                Object affObj = map.get("affected");
                if (affObj instanceof Number) {
                    return ((Number) affObj).intValue();
                }
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Database update failed: " + e.getMessage(), e);
        }
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
