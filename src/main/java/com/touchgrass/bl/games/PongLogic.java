package com.touchgrass.bl.games;

public final class PongLogic {
    public static final double FIELD_WIDTH = 920;
    public static final double FIELD_HEIGHT = 540;
    public static final double PADDLE_HEIGHT = 96;
    public static final double PADDLE_WIDTH = 14;
    public static final double BALL_SIZE = 14;

    private static final double PADDLE_SPEED = 16;
    private static final double BALL_START_SPEED_X = 5;
    private static final double BALL_START_SPEED_Y = 3;
    private static final double MAX_BALL_SPEED = 12;
    private static final double LEFT_PADDLE_X = 24;
    private static final double RIGHT_PADDLE_X = FIELD_WIDTH - 24 - PADDLE_WIDTH;

    private double paddle1Y;
    private double paddle2Y;
    private double ballX;
    private double ballY;
    private double ballVelocityX;
    private double ballVelocityY;
    private int scorePlayer1;
    private int scorePlayer2;

    public PongLogic() {
        reset();
    }

    public synchronized void update() {
        ballX += ballVelocityX;
        ballY += ballVelocityY;

        if (ballY <= 0 || ballY >= FIELD_HEIGHT - BALL_SIZE) {
            ballVelocityY = -ballVelocityY;
            ballY = clamp(ballY, 0, FIELD_HEIGHT - BALL_SIZE);
        }

        boolean leftCollision = ballX <= LEFT_PADDLE_X + PADDLE_WIDTH
                && ballX + BALL_SIZE >= LEFT_PADDLE_X
                && ballY + BALL_SIZE >= paddle1Y
                && ballY <= paddle1Y + PADDLE_HEIGHT;

        boolean rightCollision = ballX + BALL_SIZE >= RIGHT_PADDLE_X
                && ballX <= RIGHT_PADDLE_X + PADDLE_WIDTH
                && ballY + BALL_SIZE >= paddle2Y
                && ballY <= paddle2Y + PADDLE_HEIGHT;

        if (leftCollision && ballVelocityX < 0) {
            ballVelocityX = boostedVelocity(-ballVelocityX);
            ballX = LEFT_PADDLE_X + PADDLE_WIDTH;
            ballVelocityY = adjustedBounceVelocity(paddle1Y);
        } else if (rightCollision && ballVelocityX > 0) {
            ballVelocityX = -boostedVelocity(ballVelocityX);
            ballX = RIGHT_PADDLE_X - BALL_SIZE;
            ballVelocityY = adjustedBounceVelocity(paddle2Y);
        }

        if (ballX < -BALL_SIZE) {
            scorePlayer2++;
            resetBall(1);
        } else if (ballX > FIELD_WIDTH) {
            scorePlayer1++;
            resetBall(-1);
        }
    }

    public synchronized void processCommand(InputCommand command, int playerNumber) {
        if (command != InputCommand.UP && command != InputCommand.DOWN) {
            return;
        }
        double delta = command == InputCommand.UP ? -PADDLE_SPEED : PADDLE_SPEED;
        if (playerNumber == 1) {
            paddle1Y = clamp(paddle1Y + delta, 0, FIELD_HEIGHT - PADDLE_HEIGHT);
        } else if (playerNumber == 2) {
            paddle2Y = clamp(paddle2Y + delta, 0, FIELD_HEIGHT - PADDLE_HEIGHT);
        }
    }

    public synchronized GameState toGameState() {
        return new GameState(ballX, ballY, paddle1Y, paddle2Y, scorePlayer1, scorePlayer2);
    }

    public synchronized double getBallCenterY() {
        return ballY + (BALL_SIZE / 2.0);
    }

    public synchronized double getPaddleCenterY(int playerNumber) {
        if (playerNumber == 1) {
            return paddle1Y + (PADDLE_HEIGHT / 2.0);
        }
        if (playerNumber == 2) {
            return paddle2Y + (PADDLE_HEIGHT / 2.0);
        }
        return 0;
    }

    private void reset() {
        paddle1Y = (FIELD_HEIGHT - PADDLE_HEIGHT) / 2.0;
        paddle2Y = (FIELD_HEIGHT - PADDLE_HEIGHT) / 2.0;
        scorePlayer1 = 0;
        scorePlayer2 = 0;
        resetBall(-1);
    }

    private void resetBall(int direction) {
        ballX = (FIELD_WIDTH - BALL_SIZE) / 2.0;
        ballY = (FIELD_HEIGHT - BALL_SIZE) / 2.0;
        ballVelocityX = BALL_START_SPEED_X * direction;
        ballVelocityY = ballVelocityY < 0 ? -BALL_START_SPEED_Y : BALL_START_SPEED_Y;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double boostedVelocity(double currentVelocity) {
        double boosted = currentVelocity * 1.04;
        return clamp(boosted, -MAX_BALL_SPEED, MAX_BALL_SPEED);
    }

    private double adjustedBounceVelocity(double paddleY) {
        double paddleCenter = paddleY + (PADDLE_HEIGHT / 2.0);
        double ballCenter = ballY + (BALL_SIZE / 2.0);
        double offset = (ballCenter - paddleCenter) / (PADDLE_HEIGHT / 2.0);
        return clamp(offset * 5.2, -MAX_BALL_SPEED, MAX_BALL_SPEED);
    }
}
