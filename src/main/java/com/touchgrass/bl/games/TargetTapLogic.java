package com.touchgrass.bl.games;

import java.util.Random;

public final class TargetTapLogic {
    private static final int INITIAL_TIMER_TICKS = 300;
    private static final double MIN_RADIUS = 0.035;
    private static final double MAX_RADIUS = 0.070;

    private final Random random = new Random();
    private double targetXRatio;
    private double targetYRatio;
    private double targetRadius;
    private int timerTicks = INITIAL_TIMER_TICKS;
    private int score;
    private boolean gameOver;

    public TargetTapLogic() {
        spawnTarget();
    }

    public synchronized void update() {
        if (gameOver) {
            return;
        }
        timerTicks--;
        if (timerTicks <= 0) {
            gameOver = true;
        }
    }

    public synchronized boolean tap(double x, double y) {
        if (gameOver) {
            return false;
        }
        double dx = x - targetXRatio;
        double dy = y - targetYRatio;
        if (Math.sqrt((dx * dx) + (dy * dy)) <= targetRadius) {
            score += 10;
            spawnTarget();
            return true;
        }
        return false;
    }

    public synchronized double getTargetX() {
        return targetXRatio;
    }

    public synchronized double getTargetY() {
        return targetYRatio;
    }

    public synchronized double getTargetRadius() {
        return targetRadius;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized int getTimerTicks() {
        return timerTicks;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    private void spawnTarget() {
        targetRadius = MIN_RADIUS + random.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
        targetXRatio = 0.13 + random.nextDouble() * 0.74;
        targetYRatio = 0.18 + random.nextDouble() * 0.62;
    }
}
