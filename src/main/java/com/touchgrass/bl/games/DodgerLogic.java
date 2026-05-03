package com.touchgrass.bl.games;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class DodgerLogic {
    public static final int GRID_WIDTH = 14;
    public static final int GRID_HEIGHT = 20;

    private final List<Cell> obstacles = new ArrayList<>();
    private final Random random = new Random();
    private int playerX = GRID_WIDTH / 2;
    private int ticks;
    private int score;
    private boolean gameOver;

    public synchronized void update() {
        if (gameOver) {
            return;
        }

        ticks++;
        if (ticks % 3 == 0) {
            obstacles.add(new Cell(random.nextInt(GRID_WIDTH), 0));
        }

        List<Cell> movedObstacles = new ArrayList<>();
        Iterator<Cell> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Cell obstacle = iterator.next();
            Cell moved = new Cell(obstacle.x(), obstacle.y() + 1);
            if (moved.y() >= GRID_HEIGHT) {
                score += 2;
                continue;
            }
            if (moved.y() == GRID_HEIGHT - 1 && moved.x() == playerX) {
                gameOver = true;
                return;
            }
            movedObstacles.add(moved);
        }
        obstacles.clear();
        obstacles.addAll(movedObstacles);
    }

    public synchronized void moveLeft() {
        if (gameOver) {
            return;
        }
        playerX = Math.max(0, playerX - 1);
    }

    public synchronized void moveRight() {
        if (gameOver) {
            return;
        }
        playerX = Math.min(GRID_WIDTH - 1, playerX + 1);
    }

    public synchronized int getPlayerX() {
        return playerX;
    }

    public synchronized List<Cell> getObstacles() {
        return List.copyOf(obstacles);
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public record Cell(int x, int y) {}
}
