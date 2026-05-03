package com.touchgrass.bl.games;

import java.io.Serializable;

public record GameState(
        double ballX,
        double ballY,
        double paddle1Y,
        double paddle2Y,
        int scorePlayer1,
        int scorePlayer2) implements Serializable {
    private static final long serialVersionUID = 1L;
}
