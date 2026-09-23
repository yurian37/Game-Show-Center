package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaPoolSetup extends GameSetup {
    
    private int roundsPerPlayer;
    private List<String> mediaPool;

    @Override
    public void validate(int numPlayers) {
        int required = numPlayers * roundsPerPlayer;
        if (mediaPool == null || mediaPool.size() < required) {
            throw new IllegalArgumentException(
                "Media Module: Missing files in media pool. At least " + required + " media files are required."
            );
        }
    }
}