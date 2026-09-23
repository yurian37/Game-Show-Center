package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ZeroMarginSetup extends GameSetup {
    
    private int roundsPerPlayer;
    private List<Double> targetTimesPool;

    @Override
    public void validate(int numPlayers) {
        if (targetTimesPool == null || targetTimesPool.isEmpty()) {
            throw new IllegalArgumentException("Zero_Margin: Target times pool cannot be empty.");
        }
    }
}