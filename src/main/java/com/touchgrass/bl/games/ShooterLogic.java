package com.touchgrass.bl.games;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class ShooterLogic {
    public static final double FIELD_WIDTH = 100.0;
    public static final double FIELD_HEIGHT = 100.0;
    public static final double PLAYER_WIDTH = 9.0;
    public static final double PLAYER_HEIGHT = 5.0;
    public static final double PLAYER_Y = 92.0;
    public static final double PLAYER_SPEED = 1.6;
    public static final double BULLET_WIDTH = 1.6;
    public static final double BULLET_HEIGHT = 4.0;
    public static final double BULLET_SPEED = 3.4;
    public static final double ENEMY_SIZE = 6.5;
    public static final double ENEMY_BASE_SPEED = 0.55;
    public static final int MAX_LIVES = 3;

    private final Random random = new Random();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private double playerX = (FIELD_WIDTH - PLAYER_WIDTH) / 2.0;
    private boolean movingLeft;
    private boolean movingRight;
    private int score;
    private int lives = MAX_LIVES;
    private int ticksSinceSpawn;
    private int spawnInterval = 24;
    private int fireCooldown;
    private boolean gameOver;

    public synchronized void update() {
        if (gameOver) {
            return;
        }

        if (movingLeft) {
            playerX = Math.max(0, playerX - PLAYER_SPEED);
        }
        if (movingRight) {
            playerX = Math.min(FIELD_WIDTH - PLAYER_WIDTH, playerX + PLAYER_SPEED);
        }
        if (fireCooldown > 0) {
            fireCooldown--;
        }

        ticksSinceSpawn++;
        if (ticksSinceSpawn >= spawnInterval) {
            spawnEnemy();
            ticksSinceSpawn = 0;
            spawnInterval = Math.max(10, spawnInterval - 1);
        }

        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.y -= BULLET_SPEED;
            if (bullet.y + BULLET_HEIGHT < 0) {
                bulletIterator.remove();
            }
        }

        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            enemy.y += enemy.speed;
            if (enemy.y > FIELD_HEIGHT) {
                lives--;
                enemyIterator.remove();
                if (lives <= 0) {
                    gameOver = true;
                    return;
                }
                continue;
            }
            if (overlapsPlayer(enemy)) {
                lives--;
                enemyIterator.remove();
                if (lives <= 0) {
                    gameOver = true;
                    return;
                }
            }
        }

        Iterator<Bullet> shooterIterator = bullets.iterator();
        while (shooterIterator.hasNext()) {
            Bullet bullet = shooterIterator.next();
            boolean consumed = false;
            Iterator<Enemy> targetIterator = enemies.iterator();
            while (targetIterator.hasNext()) {
                Enemy enemy = targetIterator.next();
                if (overlap(bullet.x, bullet.y, BULLET_WIDTH, BULLET_HEIGHT, enemy.x, enemy.y, ENEMY_SIZE, ENEMY_SIZE)) {
                    targetIterator.remove();
                    consumed = true;
                    score += 10;
                    break;
                }
            }
            if (consumed) {
                shooterIterator.remove();
            }
        }
    }

    public synchronized void setMovingLeft(boolean active) {
        movingLeft = active;
    }

    public synchronized void setMovingRight(boolean active) {
        movingRight = active;
    }

    public synchronized void fire() {
        if (gameOver || fireCooldown > 0) {
            return;
        }
        bullets.add(new Bullet(playerX + (PLAYER_WIDTH / 2.0) - (BULLET_WIDTH / 2.0), PLAYER_Y - BULLET_HEIGHT));
        fireCooldown = 4;
    }

    public synchronized double getPlayerX() {
        return playerX;
    }

    public synchronized List<Bullet> getBullets() {
        List<Bullet> snapshot = new ArrayList<>();
        for (Bullet bullet : bullets) {
            snapshot.add(new Bullet(bullet.x, bullet.y));
        }
        return snapshot;
    }

    public synchronized List<Enemy> getEnemies() {
        List<Enemy> snapshot = new ArrayList<>();
        for (Enemy enemy : enemies) {
            snapshot.add(new Enemy(enemy.x, enemy.y, enemy.speed));
        }
        return snapshot;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized int getLives() {
        return lives;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    private void spawnEnemy() {
        double x = random.nextDouble() * (FIELD_WIDTH - ENEMY_SIZE);
        double speed = ENEMY_BASE_SPEED + random.nextDouble() * 0.55;
        enemies.add(new Enemy(x, -ENEMY_SIZE, speed));
    }

    private boolean overlapsPlayer(Enemy enemy) {
        return overlap(enemy.x, enemy.y, ENEMY_SIZE, ENEMY_SIZE, playerX, PLAYER_Y, PLAYER_WIDTH, PLAYER_HEIGHT);
    }

    private boolean overlap(double ax, double ay, double aw, double ah, double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    public static final class Bullet {
        private double x;
        private double y;

        public Bullet(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }
    }

    public static final class Enemy {
        private double x;
        private double y;
        private final double speed;

        public Enemy(double x, double y, double speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public double speed() {
            return speed;
        }
    }
}
