package com.fitness.db;

import com.fitness.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private static DatabaseManager instance;

    private DatabaseManager() {
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Establishes and returns a new JDBC Connection using AppConfig properties.
     */
    public Connection getConnection() throws SQLException {
        String url = AppConfig.DB_URL;
        String user = AppConfig.DB_USER;
        String pass = AppConfig.DB_PASSWORD;

        // If user is null or empty, allow JDBC to extract user/password from URL (e.g. Neon, Render, Supabase)
        if (user == null || user.trim().isEmpty()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, user, pass != null ? pass : "");
    }

    private void initDatabase() {
        try {
            // Attempt to load PostgreSQL JDBC Driver
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException ignored) {
                // Driver autoloaded via JDBC 4.0 SPI
            }

            try (Connection conn = getConnection()) {
                System.out.println("[DatabaseManager] Connected to PostgreSQL via JDBC: " + AppConfig.DB_URL);
                seedIfEmpty();
                System.out.println("[DatabaseManager] Database initialized successfully.");
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Database connection initialization note: " + e.getMessage());
        }
    }

    public synchronized void seedIfEmpty() {
        try {
            // Check if users table exists in public schema
            List<Map<String, Object>> tables = query(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users';",
                    Collections.emptyList()
            );

            if (tables.isEmpty()) {
                System.out.println("[DatabaseManager] Tables missing. Applying schema & initial data...");
                if (new File(AppConfig.SCHEMA_FILE).exists()) {
                    executeSqlFile(AppConfig.SCHEMA_FILE);
                }
                if (new File(AppConfig.SAMPLE_DATA_FILE).exists()) {
                    executeSqlFile(AppConfig.SAMPLE_DATA_FILE);
                }
                syncSequences();
                return;
            }

            // Tables exist: check if users table is empty
            List<Map<String, Object>> userRows = query("SELECT COUNT(*) AS cnt FROM users;", Collections.emptyList());
            long count = 0;
            if (!userRows.isEmpty()) {
                Object cntObj = userRows.get(0).get("cnt");
                if (cntObj instanceof Number) {
                    count = ((Number) cntObj).longValue();
                }
            }

            if (count == 0) {
                System.out.println("[DatabaseManager] Users table is empty. Seeding sample demo data...");
                if (new File(AppConfig.SAMPLE_DATA_FILE).exists()) {
                    executeSqlFile(AppConfig.SAMPLE_DATA_FILE);
                }
                syncSequences();
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] seedIfEmpty notice: " + e.getMessage());
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

    /**
     * Executes SQL script using JDBC Connection and Statement.
     */
    public synchronized void executeScript(String script) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Execute SQL statements separated by semicolons
            String[] commands = script.split(";");
            for (String cmd : commands) {
                // Strip SQL line comments so statements following comments are executed properly
                String cleaned = cmd.replaceAll("(?m)^--.*$", "").trim();
                if (!cleaned.isEmpty()) {
                    stmt.execute(cleaned);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing SQL script via JDBC: " + e.getMessage(), e);
        }
    }

    /**
     * Sync PostgreSQL SERIAL sequences with current MAX(id)
     */
    private void syncSequences() {
        String[] tables = {"users", "workout_plans", "plan_exercises", "workout_logs", "messages", "user_feedback"};
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                try {
                    stmt.execute("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), COALESCE(max(id), 1)) FROM " + table + ";");
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /**
     * Safely executes a SELECT query using PreparedStatement and returns rows as List of Maps.
     */
    public synchronized List<Map<String, Object>> query(String sql, List<Object> params) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setParameters(pstmt, params);

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = metaData.getColumnLabel(i);
                        if (colName == null || colName.isEmpty()) {
                            colName = metaData.getColumnName(i);
                        }
                        row.put(colName.toLowerCase(), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed for SQL [" + sql + "]: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Executes a SELECT query expecting a single row result or null.
     */
    public synchronized Map<String, Object> queryOne(String sql, List<Object> params) {
        List<Map<String, Object>> list = query(sql, params);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Executes an INSERT query safely using PreparedStatement and returns the generated primary key ID.
     */
    public synchronized long executeInsert(String sql, List<Object> params) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setParameters(pstmt, params);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database insert failed for SQL [" + sql + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Executes an UPDATE or DELETE query safely using PreparedStatement and returns affected rows count.
     */
    public synchronized int executeUpdate(String sql, List<Object> params) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setParameters(pstmt, params);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database update failed for SQL [" + sql + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Binds parameters safely to PreparedStatement.
     */
    private void setParameters(PreparedStatement pstmt, List<Object> params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
        }
    }
}
