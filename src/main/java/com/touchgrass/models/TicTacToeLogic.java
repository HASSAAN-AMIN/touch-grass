package com.touchgrass.models;

public final class TicTacToeLogic {
    public static final int GRID_SIZE = 3;

    private final char[][] board;
    private char currentPlayer;
    private boolean gameOver;
    private char winner;
    private boolean draw;

    public TicTacToeLogic() {
        this.board = new char[GRID_SIZE][GRID_SIZE];
        reset();
    }

    public synchronized void reset() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                board[row][col] = ' ';
            }
        }
        currentPlayer = 'X';
        gameOver = false;
        winner = ' ';
        draw = false;
    }

    public synchronized boolean play(int row, int col) {
        if (gameOver || row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE) {
            return false;
        }
        if (board[row][col] != ' ') {
            return false;
        }

        board[row][col] = currentPlayer;
        if (hasLine(currentPlayer)) {
            gameOver = true;
            winner = currentPlayer;
            draw = false;
            return true;
        }
        if (isBoardFull()) {
            gameOver = true;
            winner = ' ';
            draw = true;
            return true;
        }

        currentPlayer = currentPlayer == 'X' ? 'O' : 'X';
        return true;
    }

    public synchronized char[][] getBoardCopy() {
        char[][] copy = new char[GRID_SIZE][GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            System.arraycopy(board[row], 0, copy[row], 0, GRID_SIZE);
        }
        return copy;
    }

    public synchronized char getCurrentPlayer() {
        return currentPlayer;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized char getWinner() {
        return winner;
    }

    public synchronized boolean isDraw() {
        return draw;
    }

    private boolean hasLine(char mark) {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (board[i][0] == mark && board[i][1] == mark && board[i][2] == mark) {
                return true;
            }
            if (board[0][i] == mark && board[1][i] == mark && board[2][i] == mark) {
                return true;
            }
        }
        return (board[0][0] == mark && board[1][1] == mark && board[2][2] == mark)
                || (board[0][2] == mark && board[1][1] == mark && board[2][0] == mark);
    }

    private boolean isBoardFull() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (board[row][col] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }
}
