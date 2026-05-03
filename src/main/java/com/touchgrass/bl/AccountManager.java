package com.touchgrass.bl;

import com.touchgrass.db.DatabaseConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountManager {
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";

    private static final String AUTH_QUERY =
            "SELECT 1 FROM Account WHERE username = ? AND passwordHash = ? LIMIT 1";
    private static final String EXISTENCE_QUERY =
            "SELECT 1 FROM Account WHERE username = ? OR email = ? LIMIT 1";
    private static final String INSERT_ACCOUNT_QUERY =
            "INSERT INTO Account (accountId, username, passwordHash, email, createdAt) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_PROFILE_QUERY =
            "INSERT INTO PlayerProfile (profileId, accountId, avatarUrl, totalGamesPlayed, isOnline) VALUES (?, ?, ?, ?, ?)";
    private String lastErrorMessage;

    public boolean authenticate(String username, String password) {
        lastErrorMessage = null;
        if (isNullOrBlank(username) || isNullOrBlank(password)) {
            lastErrorMessage = "Username and password are required.";
            return false;
        }

        if (DEFAULT_ADMIN_USERNAME.equals(username) && DEFAULT_ADMIN_PASSWORD.equals(password)) {
            return true;
        }

        String passwordHash = hashPassword(password);
        try {
            Connection connection = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(AUTH_QUERY)) {
                stmt.setString(1, username);
                stmt.setString(2, passwordHash);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException | IllegalStateException e) {
            lastErrorMessage = "Database Error: " + e.getMessage();
            System.err.println(lastErrorMessage);
            return false;
        }
    }

    public boolean registerUser(String username, String email, String password) {
        lastErrorMessage = null;
        if (isNullOrBlank(username) || isNullOrBlank(email) || isNullOrBlank(password)) {
            lastErrorMessage = "Username, email, and password are required.";
            return false;
        }

        String accountId = UUID.randomUUID().toString();
        String profileId = UUID.randomUUID().toString();
        String passwordHash = hashPassword(password);

        try {
            Connection connection = DatabaseConnection.getInstance().getConnection();

            if (accountExists(connection, username, email)) {
                lastErrorMessage = "Username or email is already in use.";
                return false;
            }

            boolean initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement accountStmt = connection.prepareStatement(INSERT_ACCOUNT_QUERY)) {
                    accountStmt.setString(1, accountId);
                    accountStmt.setString(2, username);
                    accountStmt.setString(3, passwordHash);
                    accountStmt.setString(4, email);
                    accountStmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                    accountStmt.executeUpdate();
                }

                try (PreparedStatement profileStmt = connection.prepareStatement(INSERT_PROFILE_QUERY)) {
                    profileStmt.setString(1, profileId);
                    profileStmt.setString(2, accountId);
                    profileStmt.setString(3, null);
                    profileStmt.setInt(4, 0);
                    profileStmt.setBoolean(5, false);
                    profileStmt.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (SQLIntegrityConstraintViolationException e) {
                connection.rollback();
                lastErrorMessage = "Username or email is already in use.";
                System.err.println("Database Error: " + e.getMessage());
                return false;
            } catch (SQLException e) {
                connection.rollback();
                lastErrorMessage = "Database Error: " + e.getMessage();
                System.err.println(lastErrorMessage);
                return false;
            } finally {
                connection.setAutoCommit(initialAutoCommit);
            }
        } catch (SQLException | IllegalStateException e) {
            lastErrorMessage = "Database Error: " + e.getMessage();
            System.err.println(lastErrorMessage);
            return false;
        }
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private boolean accountExists(Connection connection, String username, String email) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(EXISTENCE_QUERY)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable.", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private boolean isNullOrBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
