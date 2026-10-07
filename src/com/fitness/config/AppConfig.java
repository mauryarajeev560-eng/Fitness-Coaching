package com.fitness.config;

import java.io.File;

public class AppConfig {
    public static final int PORT = 8080;
    public static final String BASE_DIR = System.getProperty("user.dir");
    public static final String DB_DIR = BASE_DIR + File.separator + "data";
    public static final String DB_FILE = DB_DIR + File.separator + "fitness.db";
    public static final String SCHEMA_FILE = BASE_DIR + File.separator + "sql" + File.separator + "schema.sql";
    public static final String SAMPLE_DATA_FILE = BASE_DIR + File.separator + "sql" + File.separator + "sample_data.sql";
    public static final String WEB_DIR = BASE_DIR + File.separator + "web";
    public static final String SQLITE_BIN = "/usr/bin/sqlite3";
}
