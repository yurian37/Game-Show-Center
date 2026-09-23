package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlipCoinSetup extends GameSetup {
    
    private Map<String, Double> weights;

    @Override
    public void validate(int numPlayers) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Flip_Coin: Player weights cannot be empty.");
        }
    }
}