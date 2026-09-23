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

import java.util.List;

public class FlipCoinSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ObjectNode weightsNode;

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        weightsNode = (currentSetup instanceof ObjectNode && currentSetup.has("weights")) ?
            (ObjectNode) currentSetup.get("weights") : objectMapper.createObjectNode();

        VBox box = new VBox(10);
        Label info = new Label(I18n.get("game.editor.flipcoin.prob_title"));
        info.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 13px;");
        box.getChildren().add(info);

        if (profiles != null && !profiles.isEmpty()) {
            for (Competitor c : profiles) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);

                Label nameL = new Label(c.getName() + ":");
                nameL.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-font-size: 13px;");
                nameL.setPrefWidth(140);

                double curVal = weightsNode.has(c.getName()) ? weightsNode.get(c.getName()).asDouble() : 1.0;
                Spinner<Double> spinner = new Spinner<>(0.1, 5.0, curVal, 0.1);
                spinner.setEditable(true);
                spinner.valueProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null) weightsNode.put(c.getName(), newV);
                });

                row.getChildren().addAll(nameL, spinner);
                box.getChildren().add(row);
            }
        } else {
            Label noProfilesLabel = new Label(I18n.get("game.editor.flipcoin.no_competitors"));
            noProfilesLabel.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            box.getChildren().add(noProfilesLabel);
        }

        return box;
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "Flip_Coin");
        root.set("weights", weightsNode);
        return root;
    }
}
