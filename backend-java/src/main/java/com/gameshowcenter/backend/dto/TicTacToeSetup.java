package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TicTacToeSetup extends GameSetup {
    
    private String startingTeam;

    @Override
    public void validate(int numPlayers) {
        if (startingTeam == null || (!startingTeam.equals("red") && !startingTeam.equals("blue") && !startingTeam.equals("random"))) {
            throw new IllegalArgumentException("TicTacToe: Invalid starting team. Must be 'red', 'blue', or 'random'.");
        }
    }
}