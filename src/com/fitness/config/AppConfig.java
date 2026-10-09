package com.fitness.config;

import java.io.File;

public class AppConfig {
    public static final int PORT = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080"));

    public static final String BASE_DIR = System.getProperty("user.dir");
    public static final String DB_DIR = BASE_DIR + File.separator + "data";
    public static final String DB_FILE = DB_DIR + File.separator + "fitness.db";
    public static final String SCHEMA_FILE = BASE_DIR + File.separator + "sql" + File.separator + "postgres_schema.sql";
    public static final String SAMPLE_DATA_FILE = BASE_DIR + File.separator + "sql" + File.separator + "sample_data.sql";
    public static final String WEB_DIR = BASE_DIR + File.separator + "web";

    public static final String DB_URL = System.getenv().getOrDefault(
            "DB_URL", "jdbc:postgresql://localhost:5432/fitness_platform");
    public static final String DB_USER = System.getenv().getOrDefault("DB_USER", "postgres");
    public static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");
    public static final String SQLITE_BIN = resolveSqliteBin();

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
