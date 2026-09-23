package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class HangmanSetup extends GameSetup {
    
    private int livesPerRound;
    private int roundsPerPlayer;
    private List<String> wordPool;

    @Override
    public void validate(int numPlayers) {
        int required = numPlayers * roundsPerPlayer;
        if (wordPool != null) {
            java.util.Set<String> unique = new java.util.HashSet<>();
            for (String w : wordPool) {
                if (w != null && !unique.add(w.trim().toUpperCase())) {
                    throw new IllegalArgumentException("Hangman: Duplicate words are not allowed: " + w);
                }
            }
        }
        if (wordPool == null || wordPool.size() < required) {
            throw new IllegalArgumentException(
                "Hangman: At least " + required + " words are required (players × rounds), but only " + (wordPool == null ? 0 : wordPool.size()) + " were provided."
            );
        }
    }
}