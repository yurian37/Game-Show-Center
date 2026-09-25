package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import com.gameshowcenter.offline.util.SvgEmoji;

import java.io.File;
import java.util.*;

public class HangmanFxStage extends VBox {

    private final List<Competitor> profiles;
    private final JsonNode setupData;
    private final List<String> initialWordPool = new ArrayList<>();
    private final List<String> workingWordPool = new ArrayList<>();

    private final boolean isBattleRoyale;
    private final int maxLives;
    private final int roundsPerPlayer;
    private final int totalRounds;
    private int currentRoundNumber = 1;

    private String currentWord = "";
    private final Set<Character> guessedLetters = new HashSet<>();
    private final Set<Character> usedLetters = new LinkedHashSet<>();
    private int livesLeft;

    private Image heartFilledImg;
    private Image heartEmptyImg;

    private Label roundCounterLabel;
    private HBox heartsBar;
    private HBox wordTilesContainer;
    private Label wordInfoLabel;

    private VBox usedLettersBox;
    private Label usedLettersTitleLabel;
    private FlowPane usedLettersFlow;

    private VBox inputControlsBox;
    private TextField letterInput;
    private TextField fullWordInput;

    private VBox endBanner;
    private Label endBannerIcon;
    private Label endBannerTitle;
    private Label endBannerMsg;
    private Button nextRoundBtn;
    private Label matchCompletedLabel;

    public HangmanFxStage(List<Competitor> profiles, JsonNode setupData) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        this.setupData = setupData;

        // Parse Max Lives
        int lives = 6;
        if (setupData != null) {
            if (setupData.has("lives_per_round")) lives = setupData.get("lives_per_round").asInt(6);
            else if (setupData.has("livesPerRound")) lives = setupData.get("livesPerRound").asInt(6);
        }
        this.maxLives = lives;

        // Parse Battle Royale
        boolean br = false;
        if (setupData != null) {
            if (setupData.has("battleRoyale")) br = setupData.get("battleRoyale").asBoolean(false);
            else if (setupData.has("battle_royale")) br = setupData.get("battle_royale").asBoolean(false);
        }
        this.isBattleRoyale = br;

        // Parse Rounds per Player
        int rpp = 1;
        if (setupData != null) {
            if (setupData.has("rounds_per_player")) rpp = setupData.get("rounds_per_player").asInt(1);
            else if (setupData.has("roundsPerPlayer")) rpp = setupData.get("roundsPerPlayer").asInt(1);
        }
        this.roundsPerPlayer = rpp;

        int numPlayers = (!this.profiles.isEmpty()) ? this.profiles.size() : 1;

        // Load Heart Images
        loadHeartImages();

        // Parse Word Pool
        JsonNode poolNode = (setupData != null && setupData.has("wordPool")) ? setupData.get("wordPool") :
            ((setupData != null && setupData.has("word_pool")) ? setupData.get("word_pool") : null);

        if (poolNode != null && poolNode.isArray() && poolNode.size() > 0) {
            Set<String> seenWords = new LinkedHashSet<>();
            for (JsonNode w : poolNode) {
                String val = w.asText().trim().toUpperCase();
                if (!val.isEmpty()) seenWords.add(val);
            }
            initialWordPool.addAll(seenWords);
        }

        if (initialWordPool.isEmpty()) {
            initialWordPool.addAll(Arrays.asList("CHAMPION", "VICTORY", "STUDIO", "ARENA", "SHOWCASE", "OFFLINE"));
        }

        this.totalRounds = this.isBattleRoyale ? Math.max(1, initialWordPool.size()) : (numPlayers * this.roundsPerPlayer);

        setSpacing(18);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(16));

        // 1. HEARTS LIVES BAR & ROUND COUNTER
        VBox heartsContainer = new VBox(6);
        heartsContainer.setAlignment(Pos.CENTER);

        roundCounterLabel = new Label();
        roundCounterLabel.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 3px 12px; -fx-background-radius: 10px;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary), ThemeManager.getButtonHex()));

        Label heartsTitle = new Label(I18n.get("game.hangman.lives_remaining"));
        heartsTitle.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-letter-spacing: 1px;", ThemeManager.getAccentHex()));

        heartsBar = new HBox(8);
        heartsBar.setAlignment(Pos.CENTER);
        heartsBar.setStyle(String.format("-fx-background-color: %s; -fx-padding: 10px 20px; -fx-background-radius: 16px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px;", ThemeManager.getCardHex()));

        heartsContainer.getChildren().addAll(roundCounterLabel, heartsTitle, heartsBar);

        // 2. WORD TILES CONTAINER
        wordTilesContainer = new HBox(10);
        wordTilesContainer.setAlignment(Pos.CENTER);

        wordInfoLabel = new Label("");
        wordInfoLabel.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 4px 12px; -fx-background-radius: 12px;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary), ThemeManager.getButtonHex()));

        // 2.5 USED LETTERS CONTAINER
        usedLettersBox = new VBox(8);
        usedLettersBox.setAlignment(Pos.CENTER);
        usedLettersBox.prefWidthProperty().bind(widthProperty().multiply(0.92));
        usedLettersBox.setMaxWidth(Double.MAX_VALUE);
        usedLettersBox.setStyle(String.format("-fx-background-color: %s; -fx-padding: 10px 16px; -fx-background-radius: 14px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 14px;", ThemeManager.getCardHex()));

        HBox usedLettersHeader = new HBox(8);
        usedLettersHeader.setAlignment(Pos.CENTER);

        usedLettersTitleLabel = new Label();
        usedLettersTitleLabel.setStyle(String.format("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: %s; -fx-letter-spacing: 1px;", ThemeManager.getAccentHex()));

        usedLettersHeader.getChildren().add(usedLettersTitleLabel);

        usedLettersFlow = new FlowPane();
        usedLettersFlow.setAlignment(Pos.CENTER);
        usedLettersFlow.setHgap(8);
        usedLettersFlow.setVgap(8);

        usedLettersBox.getChildren().addAll(usedLettersHeader, usedLettersFlow);

        // 3. INPUT CONTROLS BOX (Fluid responsive)
        inputControlsBox = new VBox(12);
        inputControlsBox.setAlignment(Pos.CENTER);
        inputControlsBox.prefWidthProperty().bind(widthProperty().multiply(0.92));
        inputControlsBox.setMaxWidth(Double.MAX_VALUE);

        HBox inputsRow = new HBox(16);
        inputsRow.setAlignment(Pos.CENTER);
        inputsRow.prefWidthProperty().bind(inputControlsBox.widthProperty());

        // Letter Input Box
        VBox letterBox = new VBox(6);
        letterBox.setStyle(String.format("-fx-background-color: %s; -fx-padding: 14px; -fx-background-radius: 14px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 14px;", ThemeManager.getCardHex()));
        letterBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(letterBox, Priority.ALWAYS);

        Label letterLabel = new Label(I18n.get("game.hangman.guess_letter"));
        letterLabel.setStyle(String.format("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: %s;", ThemeManager.getAccentHex()));

        HBox letterForm = new HBox(8);
        letterInput = new TextField();
        letterInput.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 8px; -fx-padding: 6px 10px;", ThemeManager.getButtonHex(), ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));
        letterInput.setPrefWidth(60);
        letterInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                String lastChar = newVal.substring(newVal.length() - 1);
                if (!newVal.equals(lastChar)) {
                    letterInput.setText(lastChar);
                }
            }
        });

        Button submitLetterBtn = new Button(I18n.get("game.hangman.submit_letter"));
        submitLetterBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 8px 14px; -fx-background-radius: 8px; -fx-cursor: hand;", ThemeManager.getAccentHex()));
        submitLetterBtn.setOnAction(e -> handleLetterSubmit());

        letterForm.getChildren().addAll(letterInput, submitLetterBtn);
        letterBox.getChildren().addAll(letterLabel, letterForm);

        // Full Word Input Box
        VBox fullWordBox = new VBox(6);
        fullWordBox.setStyle(String.format("-fx-background-color: %s; -fx-padding: 14px; -fx-background-radius: 14px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 14px;", ThemeManager.getCardHex()));
        fullWordBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fullWordBox, Priority.ALWAYS);

        Label wordLabel = new Label(I18n.get("game.hangman.solve_entire_word"));
        wordLabel.setStyle(String.format("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: %s;", ThemeManager.getAccentHex()));

        HBox wordForm = new HBox(8);
        fullWordInput = new TextField();
        fullWordInput.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 8px; -fx-padding: 6px 10px;", ThemeManager.getButtonHex(), ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));
        HBox.setHgrow(fullWordInput, Priority.ALWAYS);

        Button submitWordBtn = new Button(I18n.get("game.hangman.solve_word_btn"));
        submitWordBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-padding: 8px 14px; -fx-background-radius: 8px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));
        submitWordBtn.setOnAction(e -> handleFullWordSubmit());

        wordForm.getChildren().addAll(fullWordInput, submitWordBtn);
        fullWordBox.getChildren().addAll(wordLabel, wordForm);

        inputsRow.getChildren().addAll(letterBox, fullWordBox);
        inputControlsBox.getChildren().add(inputsRow);

        // 4. END BANNER & NEXT ROUND BUTTON (HIDDEN DURING PLAY)
        endBanner = new VBox(10);
        endBanner.setAlignment(Pos.CENTER);
        endBanner.setPadding(new Insets(16, 24, 16, 24));
        endBanner.prefWidthProperty().bind(widthProperty().multiply(0.92));
        endBanner.setMaxWidth(Double.MAX_VALUE);
        endBanner.setVisible(false);

        endBannerIcon = new Label();
        SvgEmoji.setGraphic(endBannerIcon, "crown", 36);

        endBannerTitle = new Label(I18n.get("game.hangman.round_victory"));
        endBannerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #ffffff;");

        endBannerMsg = new Label("");
        endBannerMsg.setStyle(String.format("-fx-font-size: 13px; -fx-text-fill: %s; -fx-wrap-text: true; -fx-alignment: center;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));

        nextRoundBtn = new Button(I18n.get("game.common.next_round"));
        SvgEmoji.setGraphic(nextRoundBtn, "arrow-right", 16);
        nextRoundBtn.setStyle(String.format("-fx-font-size: 14px; -fx-padding: 10px 28px; -fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-background-radius: 12px; -fx-cursor: hand;", ThemeManager.getAccentHex()));
        nextRoundBtn.setOnAction(e -> {
            if (currentRoundNumber < totalRounds) {
                currentRoundNumber++;
                startRound();
            }
        });

        matchCompletedLabel = new Label(I18n.get("game.hangman.match_completed"));
        SvgEmoji.setGraphic(matchCompletedLabel, "trophy", 16);
        matchCompletedLabel.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 8px 20px; -fx-background-radius: 12px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 12px;", ThemeManager.getAccentHex(), ThemeManager.getCardHex()));
        matchCompletedLabel.setVisible(false);

        endBanner.getChildren().addAll(endBannerIcon, endBannerTitle, endBannerMsg, nextRoundBtn, matchCompletedLabel);

        getChildren().addAll(heartsContainer, wordTilesContainer, wordInfoLabel, usedLettersBox, inputControlsBox, endBanner);

        // START FIRST ROUND
        workingWordPool.addAll(initialWordPool);
        currentRoundNumber = 1;
        startRound();
    }

    private void loadHeartImages() {
        try {
            File filledFile = new File("assets/games/hangman/heart_filled.png");
            if (!filledFile.exists()) filledFile = new File("template-offline/assets/games/hangman/heart_filled.png");

            if (filledFile.exists()) {
                heartFilledImg = new Image(filledFile.toURI().toString());
            }

            File emptyFile = new File("assets/games/hangman/heart_empty.png");
            if (!emptyFile.exists()) emptyFile = new File("template-offline/assets/games/hangman/heart_empty.png");

            if (emptyFile.exists()) {
                heartEmptyImg = new Image(emptyFile.toURI().toString());
            }
        } catch (Exception ignored) {}
    }

    private void startRound() {
        if (workingWordPool.isEmpty()) {
            workingWordPool.addAll(initialWordPool);
        }

        // Pick random word and remove from pool
        Random rand = new Random();
        int idx = rand.nextInt(workingWordPool.size());
        currentWord = workingWordPool.remove(idx).toUpperCase().trim();

        guessedLetters.clear();
        usedLetters.clear();
        livesLeft = maxLives;
        letterInput.setText("");
        fullWordInput.setText("");

        if (isBattleRoyale) {
            roundCounterLabel.setText(String.format("Battle Royale • Word %d of %d", currentRoundNumber, totalRounds));
            SvgEmoji.setGraphic(roundCounterLabel, "swords", 12);
        } else {
            roundCounterLabel.setText(String.format("Round %d of %d (%d round/player)", currentRoundNumber, totalRounds, roundsPerPlayer));
            roundCounterLabel.setGraphic(null);
        }

        inputControlsBox.setVisible(true);
        endBanner.setVisible(false);

        renderHearts();
        renderWordTiles(false);
        updateWordInfo();
        renderUsedLetters();
    }

    private void renderHearts() {
        heartsBar.getChildren().clear();
        for (int i = 0; i < maxLives; i++) {
            boolean isFilled = i < livesLeft;

            if (isFilled && heartFilledImg != null) {
                ImageView iv = new ImageView(heartFilledImg);
                iv.setFitWidth(32);
                iv.setFitHeight(32);
                iv.setPreserveRatio(true);
                heartsBar.getChildren().add(iv);
            } else if (!isFilled && heartEmptyImg != null) {
                ImageView iv = new ImageView(heartEmptyImg);
                iv.setFitWidth(32);
                iv.setFitHeight(32);
                iv.setPreserveRatio(true);
                iv.setOpacity(0.4);
                heartsBar.getChildren().add(iv);
            } else {
                Label heart = new Label();
                SvgEmoji.setGraphic(heart, "heart", 22);
                if (!isFilled) heart.setOpacity(0.3);
                heartsBar.getChildren().add(heart);
            }
        }
    }

    private void renderWordTiles(boolean revealAll) {
        wordTilesContainer.getChildren().clear();

        for (char ch : currentWord.toCharArray()) {
            boolean isGuessed = guessedLetters.contains(ch) || revealAll;

            VBox tile = new VBox();
            tile.setPrefSize(44, 52);
            tile.setAlignment(Pos.CENTER);

            if (isGuessed) {
                boolean isErrorReveal = revealAll && !guessedLetters.contains(ch);
                tile.setStyle(isErrorReveal ?
                    "-fx-background-color: rgba(244, 63, 94, 0.2); -fx-border-color: #f43f5e; -fx-border-width: 2px; -fx-background-radius: 12px; -fx-border-radius: 12px;" :
                    "-fx-background-color: rgba(99, 102, 241, 0.3); -fx-border-color: #6366f1; -fx-border-width: 2px; -fx-background-radius: 12px; -fx-border-radius: 12px;");

                Label textL = new Label(String.valueOf(ch));
                textL.setStyle(isErrorReveal ? "-fx-font-weight: 900; -fx-font-size: 22px; -fx-text-fill: #fda4af;" : "-fx-font-weight: 900; -fx-font-size: 22px; -fx-text-fill: #ffffff;");
                tile.getChildren().add(textL);
            } else {
                tile.setStyle(String.format("-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-width: 2px; -fx-background-radius: 12px; -fx-border-radius: 12px;", ThemeManager.getButtonHex()));
                Label textL = new Label("_");
                textL.setStyle(String.format("-fx-font-weight: 900; -fx-font-size: 22px; -fx-text-fill: %s;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));
                tile.getChildren().add(textL);
            }

            wordTilesContainer.getChildren().add(tile);
        }
    }

    private void updateWordInfo() {
        wordInfoLabel.setText(String.format("Word Length: %d letters • Words Remaining in Pool: %d", currentWord.length(), workingWordPool.size()));
    }

    private void renderUsedLetters() {
        String title = I18n.get("game.hangman.used_letters", "LETRAS USADAS / USED LETTERS");
        usedLettersTitleLabel.setText(String.format("%s (%d)", title, usedLetters.size()));
        SvgEmoji.setGraphic(usedLettersTitleLabel, "letters-case", 14);

        usedLettersFlow.getChildren().clear();

        if (usedLetters.isEmpty()) {
            Label emptyLbl = new Label(I18n.get("game.hangman.no_letters_used", "Ninguna letra usada aún • No letters used yet"));
            emptyLbl.setStyle(String.format("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: %s;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));
            usedLettersFlow.getChildren().add(emptyLbl);
        } else {
            List<Character> sortedLetters = new ArrayList<>(usedLetters);
            Collections.sort(sortedLetters);

            for (char ch : sortedLetters) {
                boolean isCorrect = currentWord.indexOf(ch) >= 0;

                HBox chip = new HBox(5);
                chip.setAlignment(Pos.CENTER);
                chip.setPadding(new Insets(3, 8, 3, 8));

                if (isCorrect) {
                    chip.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-border-color: rgba(16, 185, 129, 0.5); -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
                    Label letterLbl = new Label(String.valueOf(ch));
                    letterLbl.setStyle("-fx-font-weight: 900; -fx-font-size: 12px; -fx-text-fill: #6ee7b7;");
                    Label markLbl = new Label();
                    SvgEmoji.setGraphic(markLbl, "check", 10);
                    chip.getChildren().addAll(letterLbl, markLbl);
                } else {
                    chip.setStyle("-fx-background-color: rgba(244, 63, 94, 0.2); -fx-border-color: rgba(244, 63, 94, 0.4); -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
                    Label letterLbl = new Label(String.valueOf(ch));
                    letterLbl.setStyle("-fx-font-weight: 900; -fx-font-size: 12px; -fx-text-fill: #fda4af;");
                    Label markLbl = new Label();
                    SvgEmoji.setGraphic(markLbl, "close", 10);
                    chip.getChildren().addAll(letterLbl, markLbl);
                }

                usedLettersFlow.getChildren().add(chip);
            }
        }
    }

    private void handleLetterSubmit() {
        String raw = letterInput.getText().trim().toUpperCase();
        letterInput.setText("");
        if (raw.isEmpty()) return;

        char letter = raw.charAt(0);

        if (usedLetters.contains(letter)) {
            // Deduct 1 life for repeated letter
            livesLeft--;
            renderHearts();

            if (livesLeft <= 0) {
                showLossBanner(String.format("Out of hearts! The word was \"%s\".", currentWord));
            }
            return;
        }

        // Register as used letter
        usedLetters.add(letter);
        renderUsedLetters();

        if (currentWord.indexOf(letter) < 0) {
            // Deduct 1 life for wrong letter
            livesLeft--;
            renderHearts();

            if (livesLeft <= 0) {
                showLossBanner(String.format("Out of hearts! The word was \"%s\".", currentWord));
            }
            return;
        }

        // Correct new letter
        guessedLetters.add(letter);
        renderWordTiles(false);

        // Check if word is complete
        boolean complete = true;
        for (char ch : currentWord.toCharArray()) {
            if (!guessedLetters.contains(ch)) {
                complete = false;
                break;
            }
        }

        if (complete) {
            showWinBanner(String.format("CONGRATULATIONS! You solved the word with %d heart(s) remaining!", livesLeft));
        }
    }

    private void handleFullWordSubmit() {
        String attempt = fullWordInput.getText().trim().toUpperCase();
        fullWordInput.setText("");
        if (attempt.isEmpty()) return;

        if (attempt.equals(currentWord)) {
            // WIN: Reveal all letters and mark as used
            for (char ch : currentWord.toCharArray()) {
                guessedLetters.add(ch);
                usedLetters.add(ch);
            }
            renderWordTiles(false);
            renderUsedLetters();
            showWinBanner(String.format("BRILLIANT! You guessed the entire word correctly with %d heart(s) remaining!", livesLeft));
        } else {
            // LOSS: Lose ALL hearts immediately
            livesLeft = 0;
            renderHearts();
            showLossBanner(String.format("Incorrect full word guess! You lost all hearts. The word was \"%s\".", currentWord));
        }
    }

    private void showWinBanner(String message) {
        inputControlsBox.setVisible(false);

        endBannerIcon.setText("");
        SvgEmoji.setGraphic(endBannerIcon, "crown", 36);
        endBannerTitle.setText(I18n.get("game.hangman.round_victory"));
        endBannerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #34d399;");
        endBannerMsg.setText(message);
        endBanner.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-border-color: #10b981; -fx-border-width: 2px; -fx-background-radius: 20px; -fx-border-radius: 20px;");

        boolean isLastRound = currentRoundNumber >= totalRounds;
        nextRoundBtn.setVisible(!isLastRound);
        nextRoundBtn.setManaged(!isLastRound);
        matchCompletedLabel.setVisible(isLastRound);
        matchCompletedLabel.setManaged(isLastRound);
        if (isLastRound) {
            matchCompletedLabel.setText(I18n.get("game.hangman.match_completed") + String.format(" (%d/%d)", currentRoundNumber, totalRounds));
        }

        endBanner.setVisible(true);
    }

    private void showLossBanner(String message) {
        inputControlsBox.setVisible(false);
        renderWordTiles(true); // Reveal full word in red

        endBannerIcon.setText("");
        SvgEmoji.setGraphic(endBannerIcon, "skull", 36);
        endBannerTitle.setText(I18n.get("game.hangman.round_over"));
        endBannerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #f43f5e;");
        endBannerMsg.setText(message);
        endBanner.setStyle("-fx-background-color: rgba(244, 63, 94, 0.15); -fx-border-color: #f43f5e; -fx-border-width: 2px; -fx-background-radius: 20px; -fx-border-radius: 20px;");

        boolean isLastRound = currentRoundNumber >= totalRounds;
        nextRoundBtn.setVisible(!isLastRound);
        nextRoundBtn.setManaged(!isLastRound);
        matchCompletedLabel.setVisible(isLastRound);
        matchCompletedLabel.setManaged(isLastRound);
        if (isLastRound) {
            matchCompletedLabel.setText(I18n.get("game.hangman.match_completed") + String.format(" (%d/%d)", currentRoundNumber, totalRounds));
        }

        endBanner.setVisible(true);
    }
}
