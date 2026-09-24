package com.gameshowcenter.offline.games;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.scene.Node;

import java.util.List;

public interface IGameSetupEditor {

    /**
     * Builds and returns the UI editor panel for this minijuego.
     */
    Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette);

    /**
     * Gets the latest JsonNode configuration from the UI controls.
     */
    JsonNode getUpdatedSetup();

    /**
     * Validates the configuration before saving.
     * @return error message if invalid, or null if valid.
     */
    default String validateSetup(List<Competitor> profiles) {
        return validateSetupData(getUpdatedSetup(), profiles);
    }

    default String validateSetup(List<Competitor> profiles, boolean battleRoyale) {
        return validateSetupData(getUpdatedSetup(), profiles, battleRoyale);
    }

    default String validateSetupData(JsonNode setupData, List<Competitor> profiles) {
        boolean br = setupData != null && (
            (setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean(false)) ||
            (setupData.has("battle_royale") && setupData.get("battle_royale").asBoolean(false))
        );
        return validateSetupData(setupData, profiles, br);
    }

    default String validateSetupData(JsonNode setupData, List<Competitor> profiles, boolean battleRoyale) {
        return null;
    }
}
