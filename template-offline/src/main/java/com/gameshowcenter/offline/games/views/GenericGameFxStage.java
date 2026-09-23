package com.gameshowcenter.offline.games.views;

import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.model.GameDescriptor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class GenericGameFxStage extends VBox {

    public GenericGameFxStage(List<Competitor> competitors, GameDescriptor descriptor) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(16);
        this.setPadding(new Insets(24));
        this.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 16px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-padding: 30px;", ThemeManager.getCardHex()));

        Label icon = new Label("🎯");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label(descriptor != null ? descriptor.getName() : "Custom Minigame");
        title.setStyle(String.format("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: %s;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));

        Label desc = new Label(descriptor != null && descriptor.getDescription() != null ?
                descriptor.getDescription() : "Data-driven game setup ready to play.");
        desc.setWrapText(true);
        desc.setStyle(String.format("-fx-font-size: 14px; -fx-text-fill: %s; -fx-text-alignment: center;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));

        Label hint = new Label(I18n.get("game.generic.hint"));
        hint.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s; -fx-font-weight: bold;", ThemeManager.getAccentHex()));

        this.getChildren().addAll(icon, title, desc, hint);
    }
}
