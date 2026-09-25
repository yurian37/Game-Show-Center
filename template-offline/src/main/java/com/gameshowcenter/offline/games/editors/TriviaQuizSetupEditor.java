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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import com.gameshowcenter.offline.util.SvgEmoji;

public class TriviaQuizSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Spinner<Integer> roundsSpinner;
    private CheckBox battleRoyaleCheckBox;
    private final List<QuestionItem> questionItems = new ArrayList<>();
    private VBox questionsListPanel;

    private static class QuestionItem {
        TextField questionField;
        TextField answerField;
    }

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        VBox box = new VBox(12);
        String textColor = palette != null ? ThemeManager.getContrastTextColor(palette.bgCard) : ThemeManager.getTextPrimaryHex();

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

        HBox roundsRow = new HBox(12);
        roundsRow.setAlignment(Pos.CENTER_LEFT);
        Label rLabel = new Label(I18n.get("game.editor.rounds_per_player"));
        rLabel.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));

        int curRounds = (currentSetup != null && currentSetup.has("roundsPerPlayer")) ? currentSetup.get("roundsPerPlayer").asInt() : 3;
        roundsSpinner = new Spinner<>(1, 10, curRounds, 1);
        roundsSpinner.setEditable(true);
        roundsSpinner.setDisable(curBattleRoyale);

        battleRoyaleCheckBox.setOnAction(e -> {
            roundsSpinner.setDisable(battleRoyaleCheckBox.isSelected());
        });

        roundsRow.getChildren().addAll(rLabel, roundsSpinner);
        box.getChildren().add(roundsRow);

        // Header for Questions List
        HBox qHeader = new HBox(12);
        qHeader.setAlignment(Pos.CENTER_LEFT);

        Label qTitle = new Label(I18n.get("game.editor.trivia.pool_title"));
        qTitle.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));

        Button addQBtn = new Button(I18n.get("game.editor.trivia.add_question"));
        addQBtn.getStyleClass().add("btn-accent-emerald");
        addQBtn.setOnAction(e -> addQuestionRow(null));

        qHeader.getChildren().addAll(qTitle, addQBtn);
        box.getChildren().add(qHeader);

        questionsListPanel = new VBox(10);

        questionItems.clear();
        JsonNode poolNode = (currentSetup != null && currentSetup.has("questionPool")) ? currentSetup.get("questionPool") :
            ((currentSetup != null && currentSetup.has("question_pool")) ? currentSetup.get("question_pool") : null);

        if (poolNode != null && poolNode.isArray() && poolNode.size() > 0) {
            for (JsonNode qNode : poolNode) {
                addQuestionRow(qNode);
            }
        } else {
            ObjectNode sample1 = objectMapper.createObjectNode();
            sample1.put("question", "What studio developed Game Show Center?");
            sample1.put("answer", "YuyiStudio");
            addQuestionRow(sample1);
        }

        ScrollPane questionsScrollPane = new ScrollPane(questionsListPanel);
        questionsScrollPane.setFitToWidth(true);
        questionsScrollPane.setPrefHeight(220);
        questionsScrollPane.setMaxHeight(260);
        questionsScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        box.getChildren().add(questionsScrollPane);
        return box;
    }

    private void addQuestionRow(JsonNode qNode) {
        QuestionItem q = new QuestionItem();

        VBox rowCard = new VBox(8);
        rowCard.getStyleClass().add("sponsor-card");
        rowCard.setAlignment(Pos.CENTER_LEFT);

        String defaultQ = qNode != null && qNode.has("question") ? qNode.get("question").asText() : "New Question?";
        String defaultA = qNode != null && qNode.has("answer") ? qNode.get("answer").asText() : "Answer";

        q.questionField = new TextField(defaultQ);
        q.questionField.getStyleClass().add("text-field-custom");
        HBox.setHgrow(q.questionField, Priority.ALWAYS);

        q.answerField = new TextField(defaultA);
        q.answerField.getStyleClass().add("text-field-custom");
        HBox.setHgrow(q.answerField, Priority.ALWAYS);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label qL = new Label(I18n.get("game.common.question") + ":");
        qL.setStyle("-fx-font-weight: bold; -fx-text-fill: #818cf8;");

        Button delBtn = new Button();
        SvgEmoji.setGraphic(delBtn, "close", 8);
        delBtn.getStyleClass().add("btn-accent-rose");
        delBtn.setOnAction(e -> {
            questionItems.remove(q);
            questionsListPanel.getChildren().remove(rowCard);
        });

        topRow.getChildren().addAll(qL, q.questionField, delBtn);

        HBox ansRow = new HBox(8);
        ansRow.setAlignment(Pos.CENTER_LEFT);
        Label aL = new Label(I18n.get("game.editor.trivia.answer_label"));
        aL.setStyle("-fx-font-weight: bold; -fx-text-fill: #34d399;");
        ansRow.getChildren().addAll(aL, q.answerField);

        rowCard.getChildren().addAll(topRow, ansRow);

        questionItems.add(q);
        questionsListPanel.getChildren().add(rowCard);
    }

    @Override
    public String validateSetup(List<Competitor> profiles) {
        return validateSetup(profiles, battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());
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

        int minQuestions = isBr ? 1 : (compCount * rounds);
        int questionCount = 0;
        JsonNode poolNode = setupData.has("questionPool") ? setupData.get("questionPool") :
            (setupData.has("question_pool") ? setupData.get("question_pool") : null);

        if (poolNode != null && poolNode.isArray()) {
            for (JsonNode qNode : poolNode) {
                String qText = "";
                if (qNode.isObject() && qNode.has("question")) {
                    qText = qNode.get("question").asText().trim();
                } else if (qNode.isTextual()) {
                    qText = qNode.asText().trim();
                }
                if (!qText.isEmpty()) {
                    questionCount++;
                }
            }
        }

        if (questionCount < minQuestions) {
            return isBr
                    ? "Trivia Quiz setup invalid: Question pool is empty. At least 1 question is required."
                    : I18n.get("game.editor.triviaquiz.pool_insufficient", questionCount, minQuestions, compCount, rounds);
        }
        return null;
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "Trivia_Quiz");
        root.put("battleRoyale", battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());
        int rpp = roundsSpinner.getValue();
        root.put("roundsPerPlayer", rpp);
        root.put("rounds_per_player", rpp);

        ArrayNode qArr = root.putArray("questionPool");
        for (QuestionItem item : questionItems) {
            String qText = item.questionField.getText().trim();
            String aText = item.answerField.getText().trim();
            if (!qText.isEmpty()) {
                ObjectNode qObj = objectMapper.createObjectNode();
                qObj.put("question", qText);
                qObj.put("answer", aText);
                qArr.add(qObj);
            }
        }
        root.set("question_pool", qArr.deepCopy());

        return root;
    }
}
