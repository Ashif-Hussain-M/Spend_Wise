package com.spendwise.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseConnectionTest {

    @Test
    void testConnectivity() {
        boolean connected = DatabaseConnection.checkConnectivity();
        assertTrue(connected);
    }

    @Test
    void testGetConnection() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn);
            assertTrue(conn.isValid(2));
        }
    }
}
