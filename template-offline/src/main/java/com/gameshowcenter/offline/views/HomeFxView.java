package com.gameshowcenter.offline.views;

import com.gameshowcenter.offline.MainApp;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeFxView extends ScrollPane {

    public interface HomeListener {
        void onStartClicked();
    }

    private final VBox contentBox;
    private final List<VBox> featureCards = new ArrayList<>();
    private final Button startBtn;
    private final Button customizeThemeBtn;
    private final MainApp mainApp;
    private final Label subtitleLabel;
    private final Label descLabel;

    public HomeFxView(MainApp app, HomeListener listener) {
        this.mainApp = app;
        setFitToWidth(true);
        setFitToHeight(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // FLUID MAIN CONTAINER (Adapts automatically to screen width & height)
        contentBox = new VBox(24);
        contentBox.setSpacing(24);
        contentBox.setPadding(new Insets(32, 28, 32, 28));
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getStyleClass().add("panel-card-glow");

        // Fluid responsive width binding
        contentBox.prefWidthProperty().bind(widthProperty().subtract(64));
        contentBox.maxWidthProperty().bind(widthProperty().subtract(64));
        contentBox.setMinWidth(500);

        // 1. HERO LOGO & TITLES
        ImageView logoView = null;
        File logoFile = new File("assets/logo/logo.png");
        if (!logoFile.exists())
            logoFile = new File("template-offline/assets/logo/logo.png");

        if (logoFile.exists()) {
            try {
                Image logoImg = new Image(logoFile.toURI().toString());
                logoView = new ImageView(logoImg);
                logoView.setFitHeight(95);
                logoView.setPreserveRatio(true);
            } catch (Exception ignored) {
            }
        }

        Text titleText = new Text("GAME SHOW CENTER");
        titleText.getStyleClass().add("app-logo-text");
        titleText.setStyle("-fx-font-size: 38px; -fx-font-weight: 900;");

        subtitleLabel = new Label(I18n.get("home.subtitle"));
        subtitleLabel.setStyle(String.format(
                "-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-letter-spacing: 2px;",
                ThemeManager.getAccentHex()));

        descLabel = new Label(I18n.get("home.description"));
        descLabel.setStyle(String.format(
                "-fx-font-size: 13px; -fx-text-fill: %s; -fx-wrap-text: true; -fx-alignment: center; -fx-text-alignment: center; -fx-max-width: 620px;",
                ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));

        VBox heroSection = new VBox(10);
        heroSection.setAlignment(Pos.CENTER);
        if (logoView != null) {
            heroSection.getChildren().addAll(logoView, titleText, subtitleLabel, descLabel);
        } else {
            Label emojiBadge = new Label("🎯");
            emojiBadge.setStyle("-fx-font-size: 54px;");
            heroSection.getChildren().addAll(emojiBadge, titleText, subtitleLabel, descLabel);
        }

        // 2. FEATURE HIGHLIGHTS (4 CARDS GRID)
        HBox featureGrid = new HBox(14);
        featureGrid.setAlignment(Pos.CENTER);
        featureGrid.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.95));
        featureGrid.maxWidthProperty().bind(contentBox.widthProperty().multiply(0.95));

        VBox card1 = createFeatureCard("⚡", I18n.get("home.card.offline.title"), I18n.get("home.card.offline.desc"));
        VBox card2 = createFeatureCard("👥", I18n.get("home.card.modes.title"), I18n.get("home.card.modes.desc"));
        VBox card3 = createFeatureCard("🏆", I18n.get("home.card.arena.title"), I18n.get("home.card.arena.desc"));

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        
        featureCards.add(card1);
        featureCards.add(card2);
        featureCards.add(card3);
        
        featureGrid.getChildren().addAll(card1, card2, card3);

        // 3. ACTION BUTTONS ROW (PROMINENT START + CLEAN THEME CUSTOMIZER BUTTON)
        startBtn = new Button(I18n.get("home.btn.start"));
        startBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-size: 15px; -fx-font-weight: 900; -fx-border-color: rgba(255, 255, 255, 0.3); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 14px 36px; -fx-cursor: hand;",
                ThemeManager.getAccentHex()));
        startBtn.setOnAction(e -> {
            if (listener != null)
                listener.onStartClicked();
        });

        customizeThemeBtn = new Button("⚙️ " + I18n.get("home.btn.theme"));
        customizeThemeBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 13px 24px; -fx-cursor: hand;",
                ThemeManager.getCardHex(), ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));
        customizeThemeBtn.setOnAction(e -> {
            if (mainApp != null) {
                ThemeCustomizerDialog.show(mainApp.getPrimaryStage(), mainApp::refreshTheme);
            }
        });

        HBox actionsRow = new HBox(16, startBtn, customizeThemeBtn);
        actionsRow.setAlignment(Pos.CENTER);

        // ASSEMBLE CONTENT
        contentBox.getChildren().addAll(heroSection, featureGrid, actionsRow);

        // Apply dynamic initial styles
        updateViewStyles();

        // Center Container Wrap
        StackPane centerWrap = new StackPane(contentBox);
        centerWrap.setAlignment(Pos.TOP_CENTER);
        centerWrap.setPadding(new Insets(16));
        setContent(centerWrap);
    }

    private void updateViewStyles() {
        String mainBoxHex = ThemeManager.getMainBoxHex();
        String cardHex = ThemeManager.getCardHex();
        String accentHex = ThemeManager.getAccentHex();
        String textOnMainBoxPrimaryHex = ThemeManager.getTextOnMainBoxPrimaryHex();
        String textOnMainBoxSecondaryHex = ThemeManager.getTextOnMainBoxSecondaryHex();
        String textOnCardPrimaryHex = ThemeManager.getTextOnCardPrimaryHex();
        String textOnCardSecondaryHex = ThemeManager.getTextOnCardSecondaryHex();
        String textOnAccentPrimaryHex = ThemeManager.getTextOnAccentPrimaryHex();

        // Apply color to Main Container
        contentBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-radius: 20px; -fx-background-radius: 20px;",
                mainBoxHex));

        if (subtitleLabel != null) {
            subtitleLabel.setStyle(String.format(
                    "-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-letter-spacing: 2px;",
                    accentHex));
        }

        if (descLabel != null) {
            descLabel.setStyle(String.format(
                    "-fx-font-size: 13px; -fx-text-fill: %s; -fx-wrap-text: true; -fx-alignment: center; -fx-text-alignment: center; -fx-max-width: 620px;",
                    textOnMainBoxSecondaryHex));
        }

        // Apply color to Feature Cards (calculated against cardHex!)
        for (VBox card : featureCards) {
            card.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;",
                    cardHex));
            if (card.getChildren().size() >= 3) {
                javafx.scene.Node titleNode = card.getChildren().get(1);
                if (titleNode instanceof Label) {
                    titleNode.setStyle(String.format("-fx-font-weight: 900; -fx-text-fill: %s; -fx-font-size: 12px;", textOnCardPrimaryHex));
                }
                javafx.scene.Node descNode = card.getChildren().get(2);
                if (descNode instanceof Label) {
                    descNode.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 10px; -fx-wrap-text: true; -fx-alignment: center; -fx-text-alignment: center;", textOnCardSecondaryHex));
                }
            }
        }

        if (startBtn != null) {
            startBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 15px; -fx-font-weight: 900; -fx-border-color: rgba(255, 255, 255, 0.3); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 14px 36px; -fx-cursor: hand;",
                    accentHex, textOnAccentPrimaryHex));
        }

        if (customizeThemeBtn != null) {
            customizeThemeBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 13px 24px; -fx-cursor: hand;",
                    cardHex, textOnCardPrimaryHex));
        }
    }

    private VBox createFeatureCard(String emoji, String title, String desc) {
        VBox card = new VBox(6);
        card.getStyleClass().add("sponsor-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 12, 16, 12));

        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 26px;");

        Label titleL = new Label(title);
        titleL.setStyle(String.format("-fx-font-weight: 900; -fx-text-fill: %s; -fx-font-size: 12px;",
                ThemeManager.getTextOnCardPrimaryHex()));

        Label descL = new Label(desc);
        descL.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 10px; -fx-wrap-text: true; -fx-alignment: center; -fx-text-alignment: center;",
                ThemeManager.getTextOnCardSecondaryHex()));

        card.getChildren().addAll(icon, titleL, descL);
        return card;
    }
}
