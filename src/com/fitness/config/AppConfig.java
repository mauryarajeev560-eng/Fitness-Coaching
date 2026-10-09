package com.fitness.config;

import java.io.File;
import java.net.URI;

public class AppConfig {
    public static final int PORT = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080"));

    public static final String BASE_DIR = System.getProperty("user.dir");
    public static final String DB_DIR = BASE_DIR + File.separator + "data";
    public static final String DB_FILE = DB_DIR + File.separator + "fitness.db";
    public static final String SCHEMA_FILE = BASE_DIR + File.separator + "sql" + File.separator + "postgres_schema.sql";
    public static final String SAMPLE_DATA_FILE = BASE_DIR + File.separator + "sql" + File.separator + "sample_data.sql";
    public static final String WEB_DIR = BASE_DIR + File.separator + "web";
    public static final String SQLITE_BIN = resolveSqliteBin();

    // Parsed Database Connection
    public static final DbConfig DB = parseDatabaseConfig();
    public static final String DB_URL = DB.jdbcUrl;
    public static final String DB_USER = DB.user;
    public static final String DB_PASSWORD = DB.password;

    public static class DbConfig {
        public final String jdbcUrl;
        public final String user;
        public final String password;

        public DbConfig(String jdbcUrl, String user, String password) {
            this.jdbcUrl = jdbcUrl;
            this.user = user;
            this.password = password;
        }
    }

    private static DbConfig parseDatabaseConfig() {
        String raw = System.getenv("DB_URL");
        if (raw == null || raw.trim().isEmpty()) {
            raw = System.getenv("DATABASE_URL");
        }
        if (raw == null || raw.trim().isEmpty()) {
            return new DbConfig("jdbc:postgresql://localhost:5432/fitness_platform", "postgres", "");
        }
        raw = raw.trim();

        // Handle URI parsing if raw string contains user:password@host (e.g. Neon, Render, Supabase)
        String parseable = raw;
        if (parseable.startsWith("jdbc:")) {
            parseable = parseable.substring("jdbc:".length());
        }

        try {
            URI uri = new URI(parseable);
            String host = uri.getHost();
            if (host != null) {
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath() != null && !uri.getPath().isEmpty() ? uri.getPath() : "/neondb";
                String userInfo = uri.getUserInfo();

                String user = System.getenv("DB_USER");
                String pass = System.getenv("DB_PASSWORD");

                if (userInfo != null && !userInfo.isEmpty()) {
                    String[] parts = userInfo.split(":", 2);
                    if (user == null || user.trim().isEmpty()) {
                        user = parts[0];
                    }
                    if ((pass == null || pass.trim().isEmpty()) && parts.length > 1) {
                        pass = parts[1];
                    }
                }

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":").append(port)
                        .append(path);

                String query = uri.getQuery();
                if (query != null && !query.isEmpty()) {
                    StringBuilder cleanQuery = new StringBuilder();
                    for (String p : query.split("&")) {
                        // channel_binding is for libpq, not a standard JDBC parameter
                        if (!p.startsWith("channel_binding=")) {
                            if (cleanQuery.length() > 0) cleanQuery.append("&");
                            cleanQuery.append(p);
                        }
                    }
                    if (cleanQuery.length() > 0) {
                        jdbcUrl.append("?").append(cleanQuery);
                    }
                } else {
                    jdbcUrl.append("?sslmode=require");
                }

                return new DbConfig(jdbcUrl.toString(), user, pass != null ? pass : "");
            }
        } catch (Exception ignored) {}

        // Fallback for standard JDBC URL
        String finalUrl = raw.startsWith("jdbc:") ? raw : "jdbc:" + raw;
        return new DbConfig(finalUrl, System.getenv("DB_USER"), System.getenv().getOrDefault("DB_PASSWORD", ""));
    }

    private static String resolveSqliteBin() {
        String env = System.getenv("SQLITE_BIN");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        if (new File("/usr/bin/sqlite3").exists()) {
            return "/usr/bin/sqlite3";
        }
        if (new File("/usr/local/bin/sqlite3").exists()) {
            return "/usr/local/bin/sqlite3";
        }
        return "sqlite3";
    }
}
