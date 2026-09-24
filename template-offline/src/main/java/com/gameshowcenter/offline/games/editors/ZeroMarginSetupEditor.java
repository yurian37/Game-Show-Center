package com.gameshowcenter.offline.games.editors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ZeroMarginSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Spinner<Integer> roundsSpinner;
    private CheckBox battleRoyaleCheckBox;
    private final List<TextField> targetTimeFields = new ArrayList<>();
    private HBox timesGridPanel;

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        VBox box = new VBox(12);
        String textColor = palette != null ? ThemeManager.getContrastTextColor(palette.bgCard) : ThemeManager.getTextPrimaryHex();

        // Battle Royale Toggle
        boolean curBR = currentSetup != null && currentSetup.has("battleRoyale") && currentSetup.get("battleRoyale").asBoolean();
        battleRoyaleCheckBox = new CheckBox(I18n.get("game.editor.battleroyale.check"));
        battleRoyaleCheckBox.setSelected(curBR);
        battleRoyaleCheckBox.setStyle(String.format("-fx-font-weight: 900; -fx-text-fill: #f59e0b; -fx-font-size: 13px; -fx-cursor: hand;"));

        Label brHint = new Label(I18n.get("game.editor.battleroyale.hint"));
        brHint.setWrapText(true);
        brHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");

        VBox brCard = new VBox(4, battleRoyaleCheckBox, brHint);
        brCard.setStyle("-fx-background-color: rgba(245, 158, 11, 0.08); -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px 12px;");
        box.getChildren().add(brCard);

        HBox roundsRow = new HBox(12);
        roundsRow.setAlignment(Pos.CENTER_LEFT);
        Label rLabel = new Label(I18n.get("game.editor.rounds_per_player"));
        rLabel.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));

        int curRounds = 3;
        if (currentSetup != null) {
            if (currentSetup.has("roundsPerPlayer")) curRounds = currentSetup.get("roundsPerPlayer").asInt();
            else if (currentSetup.has("rounds_per_player")) curRounds = currentSetup.get("rounds_per_player").asInt();
        }
        if (curRounds <= 0) curRounds = 1;

        roundsSpinner = new Spinner<>();
        roundsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, curRounds, 1));
        roundsSpinner.setEditable(true);
        roundsSpinner.setDisable(curBR);

        battleRoyaleCheckBox.selectedProperty().addListener((obs, oldV, isBr) -> {
            roundsSpinner.setDisable(isBr);
        });

        roundsRow.getChildren().addAll(rLabel, roundsSpinner);
        box.getChildren().add(roundsRow);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label info = new Label(I18n.get("game.editor.zeromargin.pool_title"));
        info.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));

        Button addBtn = new Button(I18n.get("game.editor.zeromargin.add_target"));
        addBtn.getStyleClass().add("btn-accent-emerald");
        addBtn.setOnAction(e -> addTimeField("5.0"));

        header.getChildren().addAll(info, addBtn);
        box.getChildren().add(header);

        timesGridPanel = new HBox(10);
        timesGridPanel.setAlignment(Pos.CENTER_LEFT);

        targetTimeFields.clear();
        JsonNode poolNode = (currentSetup != null && currentSetup.has("targetTimesPool")) ? currentSetup.get("targetTimesPool") :
            ((currentSetup != null && currentSetup.has("target_times_pool")) ? currentSetup.get("target_times_pool") : null);

        if (poolNode != null && poolNode.isArray() && poolNode.size() > 0) {
            for (JsonNode t : poolNode) {
                double val = t.asDouble();
                if (val > 0) addTimeField(String.valueOf(val));
            }
        }
        if (targetTimeFields.isEmpty()) {
            addTimeField("5.0");
            addTimeField("10.0");
            addTimeField("15.0");
        }

        box.getChildren().add(timesGridPanel);
        return box;
    }

    private void addTimeField(String valStr) {
        HBox itemBox = new HBox(4);
        itemBox.setAlignment(Pos.CENTER_LEFT);

        TextField tf = new TextField(valStr);
        tf.getStyleClass().add("text-field-custom");
        tf.setPrefWidth(80);

        // Strict numeric filtering: no text, no zero, no negative values
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                tf.setText(oldVal);
            }
        });

        tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                try {
                    double d = Double.parseDouble(tf.getText().trim());
                    if (d <= 0) tf.setText("1.0");
                } catch (Exception e) {
                    tf.setText("1.0");
                }
            }
        });

        targetTimeFields.add(tf);

        Button delBtn = new Button("✕");
        delBtn.getStyleClass().add("btn-accent-rose");
        delBtn.setOnAction(e -> {
            if (targetTimeFields.size() <= 1) return;
            targetTimeFields.remove(tf);
            timesGridPanel.getChildren().remove(itemBox);
        });

        itemBox.getChildren().addAll(tf, delBtn);
        timesGridPanel.getChildren().add(itemBox);
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "Zero_Margin");
        root.put("battleRoyale", battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());
        int r = 1;
        try {
            r = Math.max(1, roundsSpinner.getValue());
        } catch (Exception ignored) {}
        root.put("roundsPerPlayer", r);

        ArrayNode arr = root.putArray("targetTimesPool");
        for (TextField tf : targetTimeFields) {
            String val = tf.getText().trim();
            if (!val.isEmpty()) {
                try {
                    double d = Double.parseDouble(val);
                    if (d > 0) {
                        arr.add(d);
                    }
                } catch (Exception ignored) {}
            }
        }
        if (arr.size() == 0) {
            arr.add(5.0);
            arr.add(10.0);
            arr.add(15.0);
        }
        return root;
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
        boolean br = setupData != null && setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean();
        return validateSetupData(setupData, profiles, br);
    }

    @Override
    public String validateSetupData(JsonNode setupData, List<Competitor> profiles, boolean battleRoyale) {
        if (setupData == null) return "Zero Margin setup is missing.";

        boolean isBr = battleRoyale || (setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean());

        JsonNode poolNode = setupData.has("targetTimesPool") ? setupData.get("targetTimesPool") :
            (setupData.has("target_times_pool") ? setupData.get("target_times_pool") : null);

        if (poolNode == null || !poolNode.isArray() || poolNode.size() == 0) {
            return "Zero Margin target times pool is empty. Please add target times.";
        }

        for (JsonNode t : poolNode) {
            if (t.asDouble() <= 0) {
                return "Zero Margin setup invalid: Target times must be greater than 0.";
            }
        }
        return null;
    }
}
