package com.spendwise.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {

    private static final Properties PROPERTIES = new Properties();
    private static boolean testMode = false;

    static {
        loadProperties();
    }

    private DatabaseConnection() {
    }

    private static void loadProperties() {
        try (InputStream input = DatabaseConnection.class.getResourceAsStream("/application.properties")) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load application.properties: " + e.getMessage());
        }
    }

    public static synchronized void setTestMode(boolean isTest) {
        testMode = isTest;
    }

    public static synchronized boolean isTestMode() {
        return testMode;
    }

    public static String getUrl() {
        String env = System.getenv("SPENDWISE_DB_URL");
        if (env != null && !env.isBlank()) {
            return env;
        }
        if (testMode) {
            String testUrl = PROPERTIES.getProperty("db.test.url");
            if (testUrl != null && !testUrl.isBlank()) {
                return testUrl;
            }
        }
        return PROPERTIES.getProperty("db.url", "jdbc:mysql://localhost:3306/spendwise?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    }

    public static String getUser() {
        String env = System.getenv("SPENDWISE_DB_USER");
        if (env != null && !env.isBlank()) {
            return env;
        }
        if (testMode) {
            String testUser = PROPERTIES.getProperty("db.test.user");
            if (testUser != null && !testUser.isBlank()) {
                return testUser;
            }
        }
        return PROPERTIES.getProperty("db.user", "spendwise_app");
    }

    public static String getPassword() {
        String env = System.getenv("SPENDWISE_DB_PASSWORD");
        if (env != null) {
            return env;
        }
        if (testMode) {
            String testPass = PROPERTIES.getProperty("db.test.password");
            if (testPass != null && !testPass.isBlank()) {
                return testPass;
            }
        }
        return PROPERTIES.getProperty("db.password", "");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    public static boolean checkConnectivity() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
