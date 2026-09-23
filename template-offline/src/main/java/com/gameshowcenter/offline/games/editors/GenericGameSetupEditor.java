package com.gameshowcenter.offline.games.editors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class GenericGameSetupEditor implements IGameSetupEditor {

    private JsonNode setupNode;

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        this.setupNode = currentSetup;

        VBox box = new VBox(8);
        Label info = new Label(I18n.get("game.editor.generic.ready"));
        info.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        box.getChildren().add(info);
        return box;
    }

    @Override
    public JsonNode getUpdatedSetup() {
        if (setupNode == null) {
            setupNode = new ObjectMapper().createObjectNode();
        }
        return setupNode;
    }
}
