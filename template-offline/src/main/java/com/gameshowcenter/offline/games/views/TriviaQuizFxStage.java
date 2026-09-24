package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.*;

public class TriviaQuizFxStage extends VBox {

    public static class TriviaQuestion {
        private final String question;
        private final String answer;

        public TriviaQuestion(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }
    }

    private static final List<TriviaQuestion> DEFAULT_QUESTIONS = Arrays.asList(
            new TriviaQuestion("What is the capital of France?", "Paris"),
            new TriviaQuestion("Which planet is known as the Red Planet?", "Mars"),
            new TriviaQuestion("What is the largest ocean on Earth?", "Pacific Ocean"),
            new TriviaQuestion("Who painted the Mona Lisa?", "Leonardo da Vinci"),
            new TriviaQuestion("What element has the chemical symbol 'O'?", "Oxygen"),
            new TriviaQuestion("Which animal is the largest mammal in the world?", "Blue Whale"),
            new TriviaQuestion("What is the hardest natural substance on Earth?", "Diamond"),
            new TriviaQuestion("In which country can you find the Pyramids of Giza?", "Egypt"),
            new TriviaQuestion("What is the studio that developed Game Show Center?", "YuyiStudio"),
            new TriviaQuestion("How many continents are there on Earth?", "7"),
            new TriviaQuestion("What is the fastest land animal in the world?", "Cheetah"),
            new TriviaQuestion("Which gas do plants absorb during photosynthesis?", "Carbon Dioxide"));

    private final List<Competitor> profiles;
    private final JsonNode setupData;

    private final List<TriviaQuestion> initialPool = new ArrayList<>();
    private final List<TriviaQuestion> workingPool = new ArrayList<>();
    private TriviaQuestion currentQuestion = null;
    private boolean showAnswer = false;
    private int questionsCount = 1;

    private final boolean isBattleRoyale;
    private final int roundsPerPlayer;
    private final int numPlayers;
    private final int maxQuestions;

    private final Random random = new Random();

    private Label questionBadgeLabel;
    private Label poolInfoLabel;
    private VBox triviaCard;
    private Label questionPromptLabel;
    private Label answerLabel;
    private VBox answerCard;
    private Button showAnswerBtn;
    private Button nextQuestionBtn;

    private VBox completedBanner;
    private Label completedTitle;
    private Label completedMsg;
    private Button startNextRoundBtn;

    public TriviaQuizFxStage(List<Competitor> profiles, JsonNode setupData) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        this.setupData = setupData;

        // Parse Battle Royale
        boolean br = false;
        if (setupData != null) {
            if (setupData.has("battleRoyale")) br = setupData.get("battleRoyale").asBoolean(false);
            else if (setupData.has("battle_royale")) br = setupData.get("battle_royale").asBoolean(false);
        }
        this.isBattleRoyale = br;

        // Parse Rounds per Player & calculate limit
        int rpp = 3;
        if (setupData != null) {
            if (setupData.has("roundsPerPlayer"))
                rpp = setupData.get("roundsPerPlayer").asInt(3);
            else if (setupData.has("rounds_per_player"))
                rpp = setupData.get("rounds_per_player").asInt(3);
        }
        this.roundsPerPlayer = rpp;
        this.numPlayers = (!this.profiles.isEmpty()) ? this.profiles.size() : 1;

        setSpacing(18);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(16));

        // Parse Question Pool from JSON or fallback
        parseQuestionPool();

        this.maxQuestions = this.isBattleRoyale ? Math.max(1, initialPool.size()) : (this.numPlayers * this.roundsPerPlayer);

        // 1. BADGES
        VBox badgesBox = new VBox(6);
        badgesBox.setAlignment(Pos.CENTER);

        questionBadgeLabel = new Label();
        questionBadgeLabel.setStyle(String.format(
                "-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 4px 14px; -fx-background-radius: 12px;",
                ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary), ThemeManager.getButtonHex()));

        poolInfoLabel = new Label();
        poolInfoLabel.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: %s;",
                ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));

        badgesBox.getChildren().addAll(questionBadgeLabel, poolInfoLabel);

        // 2. MAIN TRIVIA CARD (Fluid responsive)
        triviaCard = new VBox(20);
        triviaCard.setAlignment(Pos.CENTER);
        triviaCard.setPadding(new Insets(24));
        triviaCard.prefWidthProperty().bind(widthProperty().multiply(0.92));
        triviaCard.setMaxWidth(Double.MAX_VALUE);
        triviaCard.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-width: 2px; -fx-background-radius: 24px; -fx-border-radius: 24px;",
                ThemeManager.getCardHex()));

        Label qHeaderLabel = new Label(I18n.get("game.common.question"));
        qHeaderLabel.setStyle(
                String.format("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-letter-spacing: 1px;",
                        ThemeManager.getAccentHex()));
        qHeaderLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        qHeaderLabel.setEllipsisString("");

        questionPromptLabel = new Label("");
        questionPromptLabel.setWrapText(true);
        questionPromptLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        questionPromptLabel.setEllipsisString("");
        questionPromptLabel.setStyle(String.format(
                "-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-wrap-text: true; -fx-alignment: center; -fx-text-alignment: center;",
                ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));
        questionPromptLabel.setMaxWidth(Double.MAX_VALUE);

        VBox qContainer = new VBox(6, qHeaderLabel, questionPromptLabel);
        qContainer.setAlignment(Pos.CENTER);
        qContainer.prefWidthProperty().bind(triviaCard.widthProperty().multiply(0.95));

        // Answer Sub-Card
        answerCard = new VBox(6);
        answerCard.setAlignment(Pos.CENTER);
        answerCard.prefWidthProperty().bind(triviaCard.widthProperty().multiply(0.90));
        answerCard.setMaxWidth(Double.MAX_VALUE);

        Label aHeaderLabel = new Label(I18n.get("game.common.correct_answer"));
        aHeaderLabel.setStyle(
                String.format("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-letter-spacing: 1px;",
                        ThemeManager.getAccentHex()));
        aHeaderLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        aHeaderLabel.setEllipsisString("");

        answerLabel = new Label("");
        answerLabel.setWrapText(true);
        answerLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        answerLabel.setEllipsisString("");
        answerLabel.setAlignment(Pos.CENTER);
        answerLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        answerCard.getChildren().addAll(aHeaderLabel, answerLabel);

        // Action Buttons Row
        HBox actionsRow = new HBox(14);
        actionsRow.setAlignment(Pos.CENTER);

        showAnswerBtn = new Button(I18n.get("game.trivia.show_answer"));
        showAnswerBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        showAnswerBtn.setEllipsisString("");
        showAnswerBtn.setStyle(String.format(
                "-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 10px 22px; -fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 10px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));
        showAnswerBtn.setOnAction(e -> handleShowAnswer());

        nextQuestionBtn = new Button(I18n.get("game.trivia.next_question"));
        nextQuestionBtn.setStyle(String.format(
                "-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 10px 22px; -fx-background-color: %s; -fx-text-fill: #ffffff; -fx-background-radius: 10px; -fx-cursor: hand;",
                ThemeManager.getAccentHex()));
        nextQuestionBtn.setOnAction(e -> handleNextClick());

        actionsRow.getChildren().addAll(showAnswerBtn, nextQuestionBtn);
        triviaCard.getChildren().addAll(qContainer, answerCard, actionsRow);

        // 3. COMPLETED BANNER (SHOWN WHEN QUESTIONS COUNT REACHES LIMIT)
        completedBanner = new VBox(12);
        completedBanner.setAlignment(Pos.CENTER);
        completedBanner.setPadding(new Insets(24));
        completedBanner.prefWidthProperty().bind(widthProperty().multiply(0.92));
        completedBanner.setMaxWidth(Double.MAX_VALUE);
        completedBanner.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-background-radius: 24px; -fx-border-radius: 24px;",
                ThemeManager.getCardHex(), ThemeManager.getAccentHex()));
        completedBanner.setVisible(false);
        completedBanner.setManaged(false);

        Label cIcon = new Label("🏁");
        cIcon.setStyle("-fx-font-size: 36px;");

        completedTitle = new Label(I18n.get("game.trivia.completed"));
        completedTitle.setStyle(String.format("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getAccentHex()));

        completedMsg = new Label("");
        completedMsg.setStyle(String.format(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-wrap-text: true; -fx-text-alignment: center;",
                ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));

        startNextRoundBtn = new Button(I18n.get("game.common.restart_round"));
        startNextRoundBtn.setStyle(String.format(
                "-fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 10px 24px; -fx-background-color: %s; -fx-text-fill: #ffffff; -fx-background-radius: 10px; -fx-cursor: hand;",
                ThemeManager.getAccentHex()));
        startNextRoundBtn.setOnAction(e -> startNewRoundCycle());

        completedBanner.getChildren().addAll(cIcon, completedTitle, completedMsg, startNextRoundBtn);

        getChildren().addAll(badgesBox, triviaCard, completedBanner);

        // Start first round cycle
        startNewRoundCycle();
    }

    private void parseQuestionPool() {
        JsonNode poolNode = (setupData != null && setupData.has("questionPool")) ? setupData.get("questionPool")
                : ((setupData != null && setupData.has("question_pool")) ? setupData.get("question_pool") : null);

        if (poolNode != null && poolNode.isArray() && poolNode.size() > 0) {
            for (JsonNode item : poolNode) {
                if (item.has("question") && item.has("answer")) {
                    String q = item.get("question").asText().trim();
                    String a = item.get("answer").asText().trim();
                    if (!q.isEmpty() && !a.isEmpty()) {
                        initialPool.add(new TriviaQuestion(q, a));
                    }
                }
            }
        }

        if (initialPool.isEmpty()) {
            initialPool.addAll(DEFAULT_QUESTIONS);
        }
    }

    private void startNewRoundCycle() {
        workingPool.clear();
        workingPool.addAll(initialPool);
        questionsCount = 1;

        completedBanner.setVisible(false);
        completedBanner.setManaged(false);
        triviaCard.setVisible(true);
        triviaCard.setManaged(true);

        handleNextQuestion();
    }

    private void handleNextClick() {
        if (questionsCount >= maxQuestions) {
            // Show match round completed banner
            triviaCard.setVisible(false);
            triviaCard.setManaged(false);

            String finishMsg = isBattleRoyale
                    ? String.format("¡Modo Battle Royale completado! Se han jugado todas las %d preguntas configuradas.", maxQuestions)
                    : String.format("All %d questions for this round have been played (%d player(s) × %d rounds/player).", maxQuestions, numPlayers, roundsPerPlayer);
            completedMsg.setText(finishMsg);
            completedBanner.setVisible(true);
            completedBanner.setManaged(true);
            return;
        }

        questionsCount++;
        handleNextQuestion();
    }

    private void handleNextQuestion() {
        if (workingPool.isEmpty()) {
            workingPool.addAll(initialPool);
        }

        int idx = random.nextInt(workingPool.size());
        currentQuestion = workingPool.remove(idx);
        showAnswer = false;

        String badge = isBattleRoyale
                ? String.format("⚔️ Battle Royale • Question %d of %d", Math.min(questionsCount, maxQuestions), maxQuestions)
                : String.format("❓ Trivia Quiz • Question %d of %d (%d round/player)", Math.min(questionsCount, maxQuestions), maxQuestions, roundsPerPlayer);
        questionBadgeLabel.setText(badge);
        poolInfoLabel
                .setText(String.format("Unseen Questions in Pool: %d of %d", workingPool.size(), initialPool.size()));

        if (currentQuestion != null) {
            questionPromptLabel.setText("\"" + currentQuestion.getQuestion() + "\"");
            answerLabel.setText(I18n.get("game.common.answer_hidden"));
            answerLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
            answerCard.setStyle(
                    "-fx-background-color: #0f121d; -fx-border-color: #334155; -fx-border-width: 1px; -fx-background-radius: 14px; -fx-border-radius: 14px; -fx-padding: 12px 18px;");
            showAnswerBtn.setVisible(true);
            showAnswerBtn.setManaged(true);

            if (questionsCount >= maxQuestions) {
                nextQuestionBtn.setText(I18n.get("game.common.finish_round"));
            } else {
                nextQuestionBtn.setText(I18n.get("game.trivia.next_question"));
            }
        }
    }

    private void handleShowAnswer() {
        showAnswer = true;
        if (currentQuestion != null) {
            answerLabel.setText("💡 " + currentQuestion.getAnswer());
            answerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #34d399;");
            answerCard.setStyle(
                    "-fx-background-color: rgba(16, 185, 129, 0.12); -fx-border-color: #10b981; -fx-border-width: 1px; -fx-background-radius: 14px; -fx-border-radius: 14px; -fx-padding: 12px 18px;");
            showAnswerBtn.setVisible(false);
            showAnswerBtn.setManaged(false);
        }
    }
}
