package com.touchgrass.bl.games;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public final class SnakeLogic {
    public static final int GRID_WIDTH = 20;
    public static final int GRID_HEIGHT = 20;
    public static final int TILE_SIZE = 24;

    private final LinkedList<Cell> snakeBody;
    private final Random random;
    private Direction direction;
    private Cell food;
    private boolean gameOver;
    private int score;

    public SnakeLogic() {
        this.snakeBody = new LinkedList<>();
        this.random = new Random();
        reset();
    }

    public synchronized void update() {
        if (gameOver) {
            return;
        }

        Cell head = snakeBody.getFirst();
        Cell nextHead = move(head, direction);

        if (hitsWall(nextHead) || hitsSelf(nextHead)) {
            gameOver = true;
            return;
        }

        snakeBody.addFirst(nextHead);
        if (nextHead.equals(food)) {
            score++;
            spawnFood();
            return;
        }
        snakeBody.removeLast();
    }

    public synchronized void processCommand(InputCommand command) {
        Direction nextDirection = mapDirection(command);
        if (nextDirection == null || nextDirection.isOpposite(direction)) {
            return;
        }
        direction = nextDirection;
    }

    public synchronized List<Cell> getSnakeBody() {
        return List.copyOf(new ArrayList<>(snakeBody));
    }

    public synchronized Cell getFood() {
        return food;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized int getScore() {
        return score;
    }

    private void reset() {
        snakeBody.clear();
        snakeBody.add(new Cell(10, 10));
        snakeBody.add(new Cell(9, 10));
        snakeBody.add(new Cell(8, 10));
        direction = Direction.RIGHT;
        score = 0;
        gameOver = false;
        spawnFood();
    }

    private Direction mapDirection(InputCommand command) {
        return switch (command) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case LEFT -> Direction.LEFT;
            case RIGHT -> Direction.RIGHT;
            default -> null;
        };
    }

    private Cell move(Cell head, Direction moveDirection) {
        return switch (moveDirection) {
            case UP -> new Cell(head.x(), head.y() - 1);
            case DOWN -> new Cell(head.x(), head.y() + 1);
            case LEFT -> new Cell(head.x() - 1, head.y());
            case RIGHT -> new Cell(head.x() + 1, head.y());
        };
    }

    private boolean hitsWall(Cell position) {
        return position.x() < 0
                || position.y() < 0
                || position.x() >= GRID_WIDTH
                || position.y() >= GRID_HEIGHT;
    }

    private boolean hitsSelf(Cell nextHead) {
        return snakeBody.contains(nextHead);
    }

    private void spawnFood() {
        Cell candidate;
        do {
            candidate = new Cell(random.nextInt(GRID_WIDTH), random.nextInt(GRID_HEIGHT));
        } while (snakeBody.contains(candidate));
        food = candidate;
    }

    private enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT;

        private boolean isOpposite(Direction other) {
            return (this == UP && other == DOWN)
                    || (this == DOWN && other == UP)
                    || (this == LEFT && other == RIGHT)
                    || (this == RIGHT && other == LEFT);
        }
    }

    public record Cell(int x, int y) {}
}
