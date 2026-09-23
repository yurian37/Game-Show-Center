package com.gameshowcenter.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class MasterMatchRequest {
    
    private String gameMode;
    private int numPlayers;
    private List<GameSetup> setups;

    public void validateAll() {
        if (setups == null || setups.isEmpty()) {
            throw new IllegalArgumentException("At least one mini-game must be selected for the event.");
        }
        
        // Iteramos sobre todos los juegos seleccionados y ejecutamos sus validaciones
        for (GameSetup setup : setups) {
            setup.validate(this.numPlayers);
        }
    }
}