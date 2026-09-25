package com.gameshowcenter.offline.games.editors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import com.gameshowcenter.offline.util.SvgEmoji;
import java.util.Set;

public class HangmanSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Spinner<Integer> livesSpinner;
    private Spinner<Integer> roundsSpinner;
    private CheckBox battleRoyaleCheckBox;
    private TextField wordsField;

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        VBox box = new VBox(12);

        boolean curBattleRoyale = false;
        if (currentSetup != null) {
            if (currentSetup.has("battleRoyale")) curBattleRoyale = currentSetup.get("battleRoyale").asBoolean(false);
            else if (currentSetup.has("battle_royale")) curBattleRoyale = currentSetup.get("battle_royale").asBoolean(false);
        }

        // Battle Royale Toggle Row
        battleRoyaleCheckBox = new CheckBox(I18n.get("game.editor.battleroyale.check"));
        SvgEmoji.setGraphic(battleRoyaleCheckBox, "swords", 16);
        battleRoyaleCheckBox.setSelected(curBattleRoyale);
        battleRoyaleCheckBox.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: 900; -fx-font-size: 12px; -fx-cursor: hand;");

        Label brHint = new Label(I18n.get("game.editor.battleroyale.hint"));
        brHint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-wrap-text: true;");

        VBox brCard = new VBox(4);
        brCard.setPadding(new Insets(8, 12, 8, 12));
        brCard.setStyle("-fx-background-color: rgba(245, 158, 11, 0.08); -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 8px; -fx-background-radius: 8px;");
        brCard.getChildren().addAll(battleRoyaleCheckBox, brHint);
        box.getChildren().add(brCard);

        HBox spinnersRow = new HBox(20);
        spinnersRow.setAlignment(Pos.CENTER_LEFT);

        // 1. Lives per Round Spinner
        HBox livesBox = new HBox(8);
        livesBox.setAlignment(Pos.CENTER_LEFT);
        Label lLabel = new Label(I18n.get("game.editor.hangman.lives"));
        lLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 13px;");

        int curLives = 6;
        if (currentSetup != null) {
            if (currentSetup.has("lives_per_round")) curLives = currentSetup.get("lives_per_round").asInt();
            else if (currentSetup.has("livesPerRound")) curLives = currentSetup.get("livesPerRound").asInt();
        }

        livesSpinner = new Spinner<>(1, 12, curLives, 1);
        livesSpinner.setEditable(true);
        livesSpinner.setPrefWidth(70);
        livesBox.getChildren().addAll(lLabel, livesSpinner);

        // 2. Rounds per Competitor Spinner
        HBox roundsBox = new HBox(8);
        roundsBox.setAlignment(Pos.CENTER_LEFT);
        Label rLabel = new Label(I18n.get("game.editor.rounds_per_player"));
        rLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 13px;");

        int curRounds = 3;
        if (currentSetup != null) {
            if (currentSetup.has("roundsPerPlayer")) curRounds = currentSetup.get("roundsPerPlayer").asInt();
            else if (currentSetup.has("rounds_per_player")) curRounds = currentSetup.get("rounds_per_player").asInt();
        }

        roundsSpinner = new Spinner<>(1, 10, curRounds, 1);
        roundsSpinner.setEditable(true);
        roundsSpinner.setPrefWidth(70);
        roundsSpinner.setDisable(curBattleRoyale);
        roundsBox.getChildren().addAll(rLabel, roundsSpinner);

        battleRoyaleCheckBox.setOnAction(e -> {
            roundsSpinner.setDisable(battleRoyaleCheckBox.isSelected());
        });

        spinnersRow.getChildren().addAll(livesBox, roundsBox);
        box.getChildren().add(spinnersRow);

        // 3. Word Pool Header Row (Label + Clean Duplicates Button)
        HBox wordsHeader = new HBox(12);
        wordsHeader.setAlignment(Pos.CENTER_LEFT);

        Label wordsLabel = new Label(I18n.get("game.editor.hangman.word_pool"));
        wordsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 13px;");

        Button cleanDupesBtn = new Button(I18n.get("game.editor.hangman.clean_duplicates", "Limpiar Duplicados"));
        SvgEmoji.setGraphic(cleanDupesBtn, "sparkles", 12);
        cleanDupesBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-font-size: 11px; -fx-padding: 3px 10px; -fx-background-radius: 6px; -fx-cursor: hand;");
        cleanDupesBtn.setVisible(false);

        wordsHeader.getChildren().addAll(wordsLabel, cleanDupesBtn);

        StringBuilder sb = new StringBuilder();
        JsonNode poolNode = (currentSetup != null && currentSetup.has("wordPool")) ? currentSetup.get("wordPool") :
            ((currentSetup != null && currentSetup.has("word_pool")) ? currentSetup.get("word_pool") : null);

        Set<String> initSeen = new LinkedHashSet<>();
        if (poolNode != null && poolNode.isArray() && poolNode.size() > 0) {
            for (JsonNode w : poolNode) {
                String norm = w.asText().trim().toUpperCase();
                if (!norm.isEmpty()) initSeen.add(norm);
            }
        } else {
            initSeen.addAll(List.of("CHAMPION", "VICTORY", "STUDIO", "ARENA", "SHOWCASE", "OFFLINE"));
        }

        for (String w : initSeen) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(w);
        }

        wordsField = new TextField(sb.toString());
        wordsField.getStyleClass().add("text-field-custom");

        Label wordsStatusLabel = new Label();
        wordsStatusLabel.setStyle("-fx-font-size: 11px;");

        Runnable checkDuplicatesAndCount = () -> {
            String text = wordsField.getText();
            Set<String> seen = new HashSet<>();
            List<String> dupes = new ArrayList<>();
            if (text != null && !text.isBlank()) {
                for (String w : text.split(",")) {
                    String norm = w.trim().toUpperCase();
                    if (!norm.isEmpty()) {
                        if (!seen.add(norm)) {
                            if (!dupes.contains(norm)) dupes.add(norm);
                        }
                    }
                }
            }

            int compCount = (profiles != null && !profiles.isEmpty()) ? profiles.size() : 2;
            int rounds = roundsSpinner != null ? roundsSpinner.getValue() : 3;
            int minWords = compCount * rounds;

            if (!dupes.isEmpty()) {
                wordsStatusLabel.setText(I18n.get("game.editor.hangman.duplicate_warning", String.join(", ", dupes)));
                wordsStatusLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-weight: bold; -fx-font-size: 12px;");
                cleanDupesBtn.setVisible(true);
            } else {
                wordsStatusLabel.setText(I18n.get("game.editor.hangman.unique_words_count", seen.size(), minWords));
                wordsStatusLabel.setStyle(seen.size() >= minWords ?
                    "-fx-text-fill: #10b981; -fx-font-size: 11px;" :
                    "-fx-text-fill: #f59e0b; -fx-font-size: 11px;");
                cleanDupesBtn.setVisible(false);
            }
        };

        cleanDupesBtn.setOnAction(e -> {
            String text = wordsField.getText();
            if (text != null && !text.isBlank()) {
                Set<String> unique = new LinkedHashSet<>();
                for (String w : text.split(",")) {
                    String norm = w.trim().toUpperCase();
                    if (!norm.isEmpty()) unique.add(norm);
                }
                wordsField.setText(String.join(", ", unique));
                checkDuplicatesAndCount.run();
            }
        });

        wordsField.textProperty().addListener((obs, o, n) -> checkDuplicatesAndCount.run());
        roundsSpinner.valueProperty().addListener((obs, o, n) -> checkDuplicatesAndCount.run());

        checkDuplicatesAndCount.run();

        box.getChildren().addAll(wordsHeader, wordsField, wordsStatusLabel);
        return box;
    }

    @Override
    public String validateSetup(List<Competitor> profiles) {
        return validateSetupData(getUpdatedSetup(), profiles);
    }

    @Override
    public String validateSetup(List<Competitor> profiles, boolean battleRoyale) {
        return validateSetupData(getUpdatedSetup(), profiles, battleRoyale);
    }

    @Override
    public String validateSetupData(JsonNode setupData, List<Competitor> profiles) {
        boolean br = setupData != null && (
            (setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean(false)) ||
            (setupData.has("battle_royale") && setupData.get("battle_royale").asBoolean(false))
        );
        return validateSetupData(setupData, profiles, br);
    }

    @Override
    public String validateSetupData(JsonNode setupData, List<Competitor> profiles, boolean battleRoyale) {
        if (setupData == null) return null;

        boolean isBr = battleRoyale || (setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean(false))
                || (setupData.has("battle_royale") && setupData.get("battle_royale").asBoolean(false));

        int compCount = (profiles != null && !profiles.isEmpty()) ? profiles.size() : 2;
        int rounds = 3;
        if (setupData.has("roundsPerPlayer")) rounds = setupData.get("roundsPerPlayer").asInt();
        else if (setupData.has("rounds_per_player")) rounds = setupData.get("rounds_per_player").asInt();

        int minWords = isBr ? 1 : (compCount * rounds);
        JsonNode poolNode = setupData.has("wordPool") ? setupData.get("wordPool") :
            (setupData.has("word_pool") ? setupData.get("word_pool") : null);

        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        if (poolNode != null && poolNode.isArray()) {
            for (JsonNode w : poolNode) {
                String norm = w.asText().trim().toUpperCase();
                if (!norm.isEmpty()) {
                    if (!seen.add(norm)) {
                        if (!duplicates.contains(norm)) duplicates.add(norm);
                    }
                }
            }
        }

        if (!duplicates.isEmpty()) {
            return I18n.get("game.editor.hangman.duplicate_words", String.join(", ", duplicates));
        }

        if (seen.size() < minWords) {
            return isBr
                    ? "Hangman setup invalid: Word pool is empty. At least 1 word is required."
                    : I18n.get("game.editor.hangman.pool_insufficient", seen.size(), minWords, compCount, rounds);
        }
        return null;
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "Hangman");
        root.put("battleRoyale", battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());
        root.put("lives_per_round", livesSpinner.getValue());
        root.put("livesPerRound", livesSpinner.getValue());
        root.put("roundsPerPlayer", roundsSpinner.getValue());
        ArrayNode arr = root.putArray("wordPool");
        Set<String> seen = new LinkedHashSet<>();
        for (String w : wordsField.getText().split(",")) {
            String trimmed = w.trim().toUpperCase();
            if (!trimmed.isEmpty() && seen.add(trimmed)) {
                arr.add(trimmed);
            }
        }
        root.set("word_pool", arr.deepCopy());
        return root;
    }
}
