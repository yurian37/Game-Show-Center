package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SnapSolveSetup extends GameSetup {

    private int roundsPerPlayer = 2;
    private List<String> selectedFilters;
    private List<String> mediaPool;

    @Override
    public void validate(int numPlayers) {
        if (roundsPerPlayer < 1) {
            throw new IllegalArgumentException("Snap Solve: Rounds per player must be at least 1.");
        }

        if (selectedFilters == null || selectedFilters.isEmpty()) {
            throw new IllegalArgumentException("Snap Solve: At least one distortion filter must be selected.");
        }

        int required = numPlayers * roundsPerPlayer;
        if (mediaPool == null || mediaPool.size() < required) {
            int actual = mediaPool != null ? mediaPool.size() : 0;
            throw new IllegalArgumentException(
                "Snap Solve: At least " + required + " media files required in pool (" +
                numPlayers + " player(s) × " + roundsPerPlayer + " round(s)), but only " + actual + " provided."
            );
        }
    }
}
