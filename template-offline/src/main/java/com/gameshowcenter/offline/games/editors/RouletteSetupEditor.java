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
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouletteSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ObjectNode weightsNode;
    private final Map<String, Label> percentLabels = new HashMap<>();
    private List<Competitor> activeProfiles;

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        this.activeProfiles = profiles;
        weightsNode = (currentSetup instanceof ObjectNode && currentSetup.has("weights")) ?
            (ObjectNode) currentSetup.get("weights") : objectMapper.createObjectNode();

        VBox box = new VBox(12);
        Label info = new Label(I18n.get("game.editor.roulette.title"));
        info.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 13px;");

        Label subInfo = new Label(I18n.get("game.editor.roulette.desc"));
        subInfo.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        box.getChildren().addAll(info, subInfo);
        percentLabels.clear();

        if (profiles != null && !profiles.isEmpty()) {
            for (Competitor c : profiles) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);

                Label nameL = new Label(c.getName() + ":");
                nameL.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-font-size: 13px;");
                nameL.setPrefWidth(130);

                double curVal = weightsNode.has(c.getName()) ? weightsNode.get(c.getName()).asDouble() : 1.0;
                if (curVal <= 0.0) curVal = 1.0;
                weightsNode.put(c.getName(), curVal);

                Spinner<Double> spinner = new Spinner<>(0.1, 5.0, curVal, 0.1);
                spinner.setEditable(true);
                spinner.setPrefWidth(90);

                Label percentLabel = new Label("100%");
                percentLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-background-color: rgba(56, 189, 248, 0.15); -fx-padding: 3px 8px; -fx-background-radius: 6px;");
                percentLabels.put(c.getName(), percentLabel);

                spinner.valueProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null) {
                        weightsNode.put(c.getName(), newV);
                        updatePercentages();
                    }
                });

                row.getChildren().addAll(nameL, spinner, percentLabel);
                box.getChildren().add(row);
            }
            updatePercentages();
        } else {
            Label noProfilesLabel = new Label(I18n.get("game.editor.roulette.no_competitors"));
            noProfilesLabel.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            box.getChildren().add(noProfilesLabel);
        }

        return box;
    }

    private void updatePercentages() {
        if (activeProfiles == null || activeProfiles.isEmpty()) return;

        double total = 0;
        for (Competitor c : activeProfiles) {
            double w = weightsNode.has(c.getName()) ? weightsNode.get(c.getName()).asDouble(1.0) : 1.0;
            total += w;
        }

        if (total <= 0) total = 1.0;

        for (Competitor c : activeProfiles) {
            double w = weightsNode.has(c.getName()) ? weightsNode.get(c.getName()).asDouble(1.0) : 1.0;
            double pct = (w / total) * 100.0;
            Label l = percentLabels.get(c.getName());
            if (l != null) {
                l.setText(String.format("%.1f%%", pct));
            }
        }
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "Roulette");
        root.set("weights", weightsNode);
        return root;
    }
}
