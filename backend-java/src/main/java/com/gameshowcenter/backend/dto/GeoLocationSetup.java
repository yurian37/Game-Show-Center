package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeoLocationSetup extends GameSetup {
    
    private int roundsPerPlayer;
    private int imagesPerRound;
    private List<LocationDTO> locations;

    @Override
    public void validate(int numPlayers) {
        int requiredLocations = numPlayers * roundsPerPlayer;
        if (locations == null || locations.size() < requiredLocations) {
            throw new IllegalArgumentException(
                "GeoLocation: Missing locations. At least " + requiredLocations + " locations are required."
            );
        }
        
        for (LocationDTO loc : locations) {
            if (loc.getImages() == null || loc.getImages().size() < imagesPerRound) {
                throw new IllegalArgumentException(
                    "GeoLocation: Location '" + loc.getLocationName() + "' must have at least " + imagesPerRound + " images."
                );
            }
        }
    }
}