package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TriviaQuizSetup extends GameSetup {
    
    private int roundsPerPlayer;
    private List<QuestionDTO> questionPool;

    @Override
    public void validate(int numPlayers) {
        int required = numPlayers * roundsPerPlayer;
        if (questionPool == null || questionPool.size() < required) {
            throw new IllegalArgumentException(
                "Trivia_Quiz: Missing questions in the pool. At least " + required + " questions are required."
            );
        }
    }
}