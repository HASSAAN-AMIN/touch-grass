package com.touchgrass.bl;

import com.touchgrass.db.DatabaseConnection;

import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

public final class LeaderboardManager {
    private static final String FIND_PROFILE_QUERY =
            "SELECT p.profileId FROM PlayerProfile p INNER JOIN Account a ON p.accountId = a.accountId WHERE a.username = ? LIMIT 1";
    private static final String INSERT_SCORE_QUERY =
            "INSERT INTO Score (scoreId, profileId, pointsValue, dateAchieved) VALUES (?, ?, ?, ?)";
    private static final String TOP_SCORES_QUERY =
            "SELECT a.username, s.pointsValue "
                    + "FROM Score s "
                    + "INNER JOIN PlayerProfile p ON s.profileId = p.profileId "
                    + "INNER JOIN Account a ON p.accountId = a.accountId "
                    + "ORDER BY s.pointsValue DESC "
                    + "LIMIT 10";

    public boolean insertScore(String username, String gameId, int scoreValue) {
        if (isNullOrBlank(username) || isNullOrBlank(gameId) || scoreValue < 0) {
            return false;
        }

        try {
            Connection connection = DatabaseConnection.getInstance().getConnection();
            String profileId = findProfileId(connection, username);
            if (profileId == null) {
                return false;
            }

            try (PreparedStatement stmt = connection.prepareStatement(INSERT_SCORE_QUERY)) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, profileId);
                stmt.setInt(3, scoreValue);
                stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Database Error: " + e.getMessage());
            return false;
        }
    }

    public List<String> getTopScores() {
        List<String> topScores = new ArrayList<>();
        try {
            Connection connection = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(TOP_SCORES_QUERY);
                 ResultSet rs = stmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    String username = rs.getString("username");
                    int points = rs.getInt("pointsValue");
                    topScores.add(rank + ". " + username + " - " + points + " pts");
                    rank++;
                }
            }
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Database Error: " + e.getMessage());
        }
        return topScores;
    }

    private String findProfileId(Connection connection, String username) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(FIND_PROFILE_QUERY)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("profileId");
                }
                return null;
            }
        }
    }

    private boolean isNullOrBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
