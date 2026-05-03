package com.touchgrass.bl.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class SudokuLogic {
    public static final int SIZE = 9;
    public static final int SUBGRID = 3;

    private final int[][] solution = new int[SIZE][SIZE];
    private final int[][] puzzle = new int[SIZE][SIZE];
    private final int[][] board = new int[SIZE][SIZE];
    private final boolean[][] given = new boolean[SIZE][SIZE];
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int mistakes;
    private int hintsRemaining = 3;
    private int score;
    private boolean gameOver;
    private boolean solved;

    public SudokuLogic() {
        generate(40);
    }

    public synchronized void update() {
        if (solved || gameOver) {
            return;
        }
        if (mistakes >= 5) {
            gameOver = true;
        }
    }

    public synchronized void selectCell(int row, int col) {
        if (gameOver || solved) {
            return;
        }
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return;
        }
        selectedRow = row;
        selectedCol = col;
    }

    public synchronized boolean enterValue(int value) {
        if (gameOver || solved) {
            return false;
        }
        if (selectedRow < 0 || selectedCol < 0) {
            return false;
        }
        if (given[selectedRow][selectedCol]) {
            return false;
        }
        if (value < 0 || value > SIZE) {
            return false;
        }
        if (value == 0) {
            board[selectedRow][selectedCol] = 0;
            return true;
        }
        if (solution[selectedRow][selectedCol] == value) {
            board[selectedRow][selectedCol] = value;
            score += 5;
            checkSolved();
            return true;
        }
        mistakes++;
        score = Math.max(0, score - 2);
        return false;
    }

    public synchronized boolean useHint() {
        if (gameOver || solved || hintsRemaining <= 0) {
            return false;
        }
        if (selectedRow < 0 || selectedCol < 0) {
            return false;
        }
        if (given[selectedRow][selectedCol] || board[selectedRow][selectedCol] == solution[selectedRow][selectedCol]) {
            return false;
        }
        board[selectedRow][selectedCol] = solution[selectedRow][selectedCol];
        hintsRemaining--;
        checkSolved();
        return true;
    }

    public synchronized int getValue(int row, int col) {
        return board[row][col];
    }

    public synchronized boolean isGiven(int row, int col) {
        return given[row][col];
    }

    public synchronized boolean isCorrect(int row, int col) {
        return board[row][col] != 0 && board[row][col] == solution[row][col];
    }

    public synchronized int getSelectedRow() {
        return selectedRow;
    }

    public synchronized int getSelectedCol() {
        return selectedCol;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized int getMistakes() {
        return mistakes;
    }

    public synchronized int getHintsRemaining() {
        return hintsRemaining;
    }

    public synchronized boolean isSolved() {
        return solved;
    }

    public synchronized boolean isGameOver() {
        return gameOver || solved;
    }

    private void checkSolved() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] != solution[row][col]) {
                    return;
                }
            }
        }
        solved = true;
        score += 100 + Math.max(0, hintsRemaining * 10);
    }

    private void generate(int givenCount) {
        Random random = new Random();
        fillBoard(0, 0, random);
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(solution[row], 0, puzzle[row], 0, SIZE);
        }

        List<int[]> cells = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                cells.add(new int[]{row, col});
            }
        }
        Collections.shuffle(cells, random);

        int cellsToHide = (SIZE * SIZE) - givenCount;
        for (int i = 0; i < cellsToHide && i < cells.size(); i++) {
            int[] cell = cells.get(i);
            puzzle[cell[0]][cell[1]] = 0;
        }

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                board[row][col] = puzzle[row][col];
                given[row][col] = puzzle[row][col] != 0;
            }
        }
    }

    private boolean fillBoard(int row, int col, Random random) {
        if (row == SIZE) {
            return true;
        }
        int nextRow = col == SIZE - 1 ? row + 1 : row;
        int nextCol = col == SIZE - 1 ? 0 : col + 1;

        List<Integer> values = new ArrayList<>();
        for (int v = 1; v <= SIZE; v++) {
            values.add(v);
        }
        Collections.shuffle(values, random);
        for (int value : values) {
            if (canPlace(row, col, value)) {
                solution[row][col] = value;
                if (fillBoard(nextRow, nextCol, random)) {
                    return true;
                }
                solution[row][col] = 0;
            }
        }
        return false;
    }

    private boolean canPlace(int row, int col, int value) {
        for (int i = 0; i < SIZE; i++) {
            if (solution[row][i] == value || solution[i][col] == value) {
                return false;
            }
        }
        int boxRow = (row / SUBGRID) * SUBGRID;
        int boxCol = (col / SUBGRID) * SUBGRID;
        for (int r = boxRow; r < boxRow + SUBGRID; r++) {
            for (int c = boxCol; c < boxCol + SUBGRID; c++) {
                if (solution[r][c] == value) {
                    return false;
                }
            }
        }
        return true;
    }
}
