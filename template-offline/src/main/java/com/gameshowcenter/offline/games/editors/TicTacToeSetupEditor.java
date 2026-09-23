package com.gameshowcenter.offline.games.editors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class TicTacToeSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ComboBox<String> teamBox;
    private Slider timerSlider;
    private Label timerValueLabel;

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        VBox rootBox = new VBox(12);
        rootBox.setAlignment(Pos.CENTER_LEFT);

        String textColor = palette != null ? ThemeManager.getContrastTextColor(palette.bgCard) : ThemeManager.getTextPrimaryHex();

        HBox teamRow = new HBox(12);
        teamRow.setAlignment(Pos.CENTER_LEFT);

        Label tLabel = new Label(I18n.get("game.editor.tictactoe.team_turn"));
        tLabel.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));

        teamBox = new ComboBox<>();
        teamBox.getItems().addAll("red", "blue", "random");
        String currentVal = "random";
        if (currentSetup != null) {
            if (currentSetup.has("starting_team")) currentVal = currentSetup.get("starting_team").asText();
            else if (currentSetup.has("startingTeam")) currentVal = currentSetup.get("startingTeam").asText();
        }
        teamBox.setValue(currentVal);
        teamRow.getChildren().addAll(tLabel, teamBox);

        HBox sliderRow = new HBox(12);
        sliderRow.setAlignment(Pos.CENTER_LEFT);

        Label sLabel = new Label(I18n.get("game.editor.tictactoe.turn_time"));
        sLabel.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));

        double defaultTimer = 1.0;
        if (currentSetup != null) {
            if (currentSetup.has("turnTimer")) defaultTimer = currentSetup.get("turnTimer").asDouble();
            else if (currentSetup.has("turn_time")) defaultTimer = currentSetup.get("turn_time").asDouble();
        }

        timerSlider = new Slider(0.1, 5.0, defaultTimer);
        timerSlider.setBlockIncrement(0.1);
        timerSlider.setMajorTickUnit(1.0);
        timerSlider.setMinorTickCount(9);
        timerSlider.setShowTickMarks(true);
        timerSlider.setPrefWidth(220);

        timerValueLabel = new Label(String.format("%.1f sec", timerSlider.getValue()));
        timerValueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 13px;");

        timerSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double rounded = Math.round(newVal.doubleValue() * 10.0) / 10.0;
            timerValueLabel.setText(String.format("%.1f sec", rounded));
        });

        sliderRow.getChildren().addAll(sLabel, timerSlider, timerValueLabel);

        rootBox.getChildren().addAll(teamRow, sliderRow);
        return rootBox;
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "TicTacToe");
        root.put("startingTeam", teamBox.getValue());
        root.put("starting_team", teamBox.getValue());
        double roundedTimer = Math.round(timerSlider.getValue() * 10.0) / 10.0;
        root.put("turnTimer", roundedTimer);
        root.put("turn_time", roundedTimer);
        return root;
    }
}
