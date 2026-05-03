package com.touchgrass.bl.games;

public final class MazeEscapeLogic {
    private static final int SIZE = 10;
    private static final int[][] MAZE = {
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {1, 0, 1, 0, 1, 1, 0, 1, 0, 1},
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 1},
            {1, 1, 1, 1, 0, 1, 1, 1, 0, 1},
            {0, 0, 0, 1, 0, 0, 0, 1, 0, 0},
            {0, 1, 0, 1, 1, 1, 0, 1, 1, 0},
            {0, 1, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
            {1, 1, 1, 1, 1, 1, 1, 0, 0, 0}
    };

    private int playerRow;
    private int playerCol;
    private int steps;
    private boolean gameOver;

    public synchronized void moveUp() {
        moveTo(playerRow - 1, playerCol);
    }

    public synchronized void moveDown() {
        moveTo(playerRow + 1, playerCol);
    }

    public synchronized void moveLeft() {
        moveTo(playerRow, playerCol - 1);
    }

    public synchronized void moveRight() {
        moveTo(playerRow, playerCol + 1);
    }

    public synchronized int getPlayerRow() {
        return playerRow;
    }

    public synchronized int getPlayerCol() {
        return playerCol;
    }

    public synchronized boolean isWall(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return true;
        }
        return MAZE[row][col] == 1;
    }

    public synchronized int getSize() {
        return SIZE;
    }

    public synchronized int getSteps() {
        return steps;
    }

    public synchronized int getScore() {
        return Math.max(0, 250 - (steps * 4));
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized int getGoalRow() {
        return SIZE - 1;
    }

    public synchronized int getGoalCol() {
        return SIZE - 1;
    }

    private void moveTo(int nextRow, int nextCol) {
        if (gameOver || isWall(nextRow, nextCol)) {
            return;
        }
        playerRow = nextRow;
        playerCol = nextCol;
        steps++;
        if (playerRow == SIZE - 1 && playerCol == SIZE - 1) {
            gameOver = true;
        }
    }
}
