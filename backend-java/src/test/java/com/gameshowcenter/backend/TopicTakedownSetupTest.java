package com.gameshowcenter.backend;

import com.gameshowcenter.backend.dto.CategoryDTO;
import com.gameshowcenter.backend.dto.QuestionDTO;
import com.gameshowcenter.backend.dto.TopicTakedownSetup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TopicTakedownSetupTest {

    private QuestionDTO createQuestion(String q, String a) {
        QuestionDTO question = new QuestionDTO();
        question.setQuestion(q);
        question.setAnswer(a);
        return question;
    }

    private CategoryDTO createCategory(String name, int qCount) {
        CategoryDTO cat = new CategoryDTO();
        cat.setCategoryName(name);
        List<QuestionDTO> list = new ArrayList<>();
        for (int i = 0; i < qCount; i++) {
            list.add(createQuestion("Question " + (i + 1), "Answer " + (i + 1)));
        }
        cat.setQuestions(list);
        return cat;
    }

    @Test
    void testValidOnlineSetup() {
        TopicTakedownSetup setup = new TopicTakedownSetup();
        setup.setNumCategories(3);
        setup.setQuestionsPerCategory(4);

        List<CategoryDTO> cats = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            cats.add(createCategory("Cat " + (i + 1), 4));
        }
        setup.setCategories(cats);

        assertDoesNotThrow(() -> setup.validate(2));
    }

    @Test
    void testExceedsCategoryLimit() {
        TopicTakedownSetup setup = new TopicTakedownSetup();
        setup.setNumCategories(4); // Exceeds online limit of 3
        setup.setQuestionsPerCategory(3);

        List<CategoryDTO> cats = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cats.add(createCategory("Cat " + (i + 1), 3));
        }
        setup.setCategories(cats);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setup.validate(2));
        assertTrue(ex.getMessage().contains("Online mode limits categories between 1 and 3"));
    }

    @Test
    void testExceedsQuestionsPerCategoryLimit() {
        TopicTakedownSetup setup = new TopicTakedownSetup();
        setup.setNumCategories(2);
        setup.setQuestionsPerCategory(5); // Exceeds online limit of 4

        List<CategoryDTO> cats = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            cats.add(createCategory("Cat " + (i + 1), 5));
        }
        setup.setCategories(cats);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setup.validate(2));
        assertTrue(ex.getMessage().contains("Online mode limits questions per category between 1 and 4"));
    }

    @Test
    void testNonRectangularCategoryFails() {
        TopicTakedownSetup setup = new TopicTakedownSetup();
        setup.setNumCategories(2);
        setup.setQuestionsPerCategory(3);

        List<CategoryDTO> cats = new ArrayList<>();
        cats.add(createCategory("Cat 1", 3));
        cats.add(createCategory("Cat 2", 2)); // only 2 questions instead of 3
        setup.setCategories(cats);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setup.validate(2));
        assertTrue(ex.getMessage().contains("must have exactly 3 questions"));
    }

    @Test
    void testBlankQuestionFails() {
        TopicTakedownSetup setup = new TopicTakedownSetup();
        setup.setNumCategories(1);
        setup.setQuestionsPerCategory(1);

        List<CategoryDTO> cats = new ArrayList<>();
        CategoryDTO cat = new CategoryDTO();
        cat.setCategoryName("Cat 1");
        List<QuestionDTO> questions = new ArrayList<>();
        questions.add(createQuestion("   ", "Valid Answer"));
        cat.setQuestions(questions);
        cats.add(cat);
        setup.setCategories(cats);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setup.validate(2));
        assertTrue(ex.getMessage().contains("must have non-empty question and answer text"));
    }
}
