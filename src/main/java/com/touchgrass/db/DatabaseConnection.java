package com.touchgrass.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton database connection manager for MySQL.
 */
public final class DatabaseConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/touchgrass";
    private static final String DB_USER = "root";
    private static final String PASSWORD = "5796";

    private static volatile DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            this.connection = DriverManager.getConnection(DB_URL, DB_USER, PASSWORD);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to establish database connection.", e);
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL, DB_USER, PASSWORD);
            }
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to retrieve database connection.", e);
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new IllegalStateException("Unable to close database connection.", e);
            }
        }
    }
}
