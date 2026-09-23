package com.gameshowcenter.backend.services;

import com.gameshowcenter.backend.dto.MasterMatchRequest;
import org.springframework.stereotype.Service;

@Service
public class MatchCompilerService {

    public String compileOfflineTemplate(MasterMatchRequest request) {
        // --- AQUÍ IRA LA LÓGICA DE NEGOCIO PESADA EN EL FUTURO ---
        
        System.out.println("Processing event: " + request.getGameMode());
        System.out.println("Number of players: " + request.getNumPlayers());
        System.out.println("Games to compile: " + request.getSetups().size());
        
        return "Simulation: The .zip file for the offline event has been successfully generated.";
    }
}