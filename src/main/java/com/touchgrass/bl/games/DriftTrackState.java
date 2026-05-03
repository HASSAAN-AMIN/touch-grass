package com.touchgrass.bl.games;

import java.io.Serializable;
import java.util.List;

public final class DriftTrackState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int player1Lane;
    private final int player2Lane;
    private final int player1Distance;
    private final int player2Distance;
    private final boolean player1Crashed;
    private final boolean player2Crashed;
    private final int winningPlayer;
    private final boolean finished;
    private final List<Obstacle> obstacles;

    public DriftTrackState(int player1Lane, int player2Lane, int player1Distance, int player2Distance,
                           boolean player1Crashed, boolean player2Crashed, int winningPlayer,
                           boolean finished, List<Obstacle> obstacles) {
        this.player1Lane = player1Lane;
        this.player2Lane = player2Lane;
        this.player1Distance = player1Distance;
        this.player2Distance = player2Distance;
        this.player1Crashed = player1Crashed;
        this.player2Crashed = player2Crashed;
        this.winningPlayer = winningPlayer;
        this.finished = finished;
        this.obstacles = obstacles;
    }

    public int getPlayer1Lane() {
        return player1Lane;
    }

    public int getPlayer2Lane() {
        return player2Lane;
    }

    public int getPlayer1Distance() {
        return player1Distance;
    }

    public int getPlayer2Distance() {
        return player2Distance;
    }

    public boolean isPlayer1Crashed() {
        return player1Crashed;
    }

    public boolean isPlayer2Crashed() {
        return player2Crashed;
    }

    public int getWinningPlayer() {
        return winningPlayer;
    }

    public boolean isFinished() {
        return finished;
    }

    public List<Obstacle> getObstacles() {
        return obstacles;
    }

    public static final class Obstacle implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int side;
        private final int lane;
        private final double y;

        public Obstacle(int side, int lane, double y) {
            this.side = side;
            this.lane = lane;
            this.y = y;
        }

        public int getSide() {
            return side;
        }

        public int getLane() {
            return lane;
        }

        public double getY() {
            return y;
        }
    }
}
