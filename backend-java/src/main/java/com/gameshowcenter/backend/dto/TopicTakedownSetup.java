package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TopicTakedownSetup extends GameSetup {
    
    private int numCategories;
    private int questionsPerCategory;
    private List<CategoryDTO> categories;

    @Override
    public void validate(int numPlayers) {
        if (numCategories < 1 || numCategories > 3) {
            throw new IllegalArgumentException("Topic_Takedown: Online mode limits categories between 1 and 3.");
        }
        if (questionsPerCategory < 1 || questionsPerCategory > 4) {
            throw new IllegalArgumentException("Topic_Takedown: Online mode limits questions per category between 1 and 4.");
        }

        if (categories == null || categories.size() != numCategories) {
            throw new IllegalArgumentException("Topic_Takedown: The number of categories received does not match the setup configuration.");
        }
        
        for (CategoryDTO cat : categories) {
            if (cat.getQuestions() == null || cat.getQuestions().size() != questionsPerCategory) {
                throw new IllegalArgumentException(
                    "Topic_Takedown: The category '" + cat.getCategoryName() + "' must have exactly " + questionsPerCategory + " questions."
                );
            }
            for (int i = 0; i < cat.getQuestions().size(); i++) {
                QuestionDTO q = cat.getQuestions().get(i);
                if (q.getQuestion() == null || q.getQuestion().trim().isEmpty() ||
                    q.getAnswer() == null || q.getAnswer().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        "Topic_Takedown: Question #" + (i + 1) + " in category '" + cat.getCategoryName() + "' must have non-empty question and answer text."
                    );
                }
            }
        }
    }
}