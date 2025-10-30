package com.nva.printing.inventory;

/**
 * Simple DB configuration holder. Uses environment variables when available.
 */
public final class DatabaseConfig {
    public static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    public static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    public static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "dbinventory");
    public static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    public static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "");

    public static final String JDBC_URL = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC",
            DB_HOST, DB_PORT, DB_NAME);

    private DatabaseConfig() {}
}
