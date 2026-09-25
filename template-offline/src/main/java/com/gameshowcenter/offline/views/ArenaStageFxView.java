package com.gameshowcenter.offline.views;

import com.gameshowcenter.offline.games.GameStageRegistry;
import com.gameshowcenter.offline.games.drivers.IGameArenaDriver;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.model.GameDescriptor;
import com.gameshowcenter.offline.model.MatchConfig;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.util.SvgEmoji;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArenaStageFxView extends ScrollPane {

    public interface ArenaStageListener {
        void onEndMatch(MatchConfig config);
        void onOpenSettings();
    }

    private final MatchConfig config;
    private final ArenaStageListener listener;

    private String selectedCompetitorId = null;
    private int currentGameIndex = 0;

    private final Map<String, VBox> competitorCards = new HashMap<>();
    private final Map<String, Label> scoreLabels = new HashMap<>();
    private final Map<String, Label> statusBadgeLabels = new HashMap<>();

    private Label activeGameTitle;
    private Label gameCounterLabel;
    private VBox gameAreaContainer;
    private StackPane rootWrapper;
    private Label selectedPlayerIndicatorLabel;

    public ArenaStageFxView(MatchConfig config, ArenaStageListener listener) {
        this.config = config != null ? config : new MatchConfig();
        this.listener = listener;

        setFitToWidth(true);
        setFitToHeight(false);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox contentBox = new VBox(16);
        contentBox.setPadding(new Insets(16));
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setFillWidth(true);
        contentBox.prefWidthProperty().bind(widthProperty().subtract(48));
        contentBox.maxWidthProperty().bind(widthProperty().subtract(48));
        contentBox.setStyle(String.format("-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 20px; -fx-background-radius: 20px;", ThemeManager.getMainBoxHex()));

        rootWrapper = new StackPane(contentBox);
        rootWrapper.setAlignment(Pos.TOP_CENTER);
        rootWrapper.setPadding(new Insets(16));

        // Enable smooth, responsive mouse wheel scrolling on any monitor size
        setOnScroll(event -> {
            if (event.getDeltaY() != 0) {
                double contentH = rootWrapper.getBoundsInLocal().getHeight();
                double viewH = getViewportBounds().getHeight();
                if (contentH > viewH) {
                    double delta = event.getDeltaY();
                    double step = (delta / (contentH - viewH)) * 2.5;
                    setVvalue(Math.max(getVmin(), Math.min(getVmax(), getVvalue() - step)));
                    event.consume();
                }
            }
        });

        List<Competitor> profiles = this.config.getProfiles();
        if (profiles != null && !profiles.isEmpty()) {
            selectedCompetitorId = profiles.get(0).getId(); // Select first player by default
        }

        // =========================================================================
        // 1. TOP ROW: JUGADORES Y SCORE PERSONAL (SELECTABLE CARDS CON SCROLL ADAPTATIVO)
        // =========================================================================
        ScrollPane topPlayersScroll = new ScrollPane();
        topPlayersScroll.setFitToHeight(true);
        topPlayersScroll.setFitToWidth(profiles.size() <= 6);
        topPlayersScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        topPlayersScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        topPlayersScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        topPlayersScroll.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.98));
        topPlayersScroll.setMaxWidth(Double.MAX_VALUE);

        HBox topPlayersRow = new HBox(12);
        topPlayersRow.setAlignment(profiles.size() <= 4 ? Pos.CENTER : Pos.CENTER_LEFT);
        topPlayersRow.setPadding(new Insets(2, 4, 8, 4));
        if (profiles.size() <= 6) {
            topPlayersRow.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.96));
        }

        topPlayersScroll.setContent(topPlayersRow);

        topPlayersScroll.setOnScroll(event -> {
            if (event.getDeltaY() != 0 || event.getDeltaX() != 0) {
                double delta = event.getDeltaY() != 0 ? event.getDeltaY() : event.getDeltaX();
                double contentW = topPlayersRow.getBoundsInLocal().getWidth();
                double viewW = topPlayersScroll.getViewportBounds().getWidth();
                if (contentW > viewW) {
                    double step = (delta / (contentW - viewW)) * 2.5;
                    topPlayersScroll.setHvalue(Math.max(topPlayersScroll.getHmin(), Math.min(topPlayersScroll.getHmax(), topPlayersScroll.getHvalue() - step)));
                    event.consume();
                }
            }
        });

        topPlayersRow.setFillHeight(true);

        double cardW = profiles.size() > 8 ? 105 : (profiles.size() > 4 ? 130 : 155);
        double maxCompetitorNameH = 0;
        javafx.scene.text.Font nameFont = javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, profiles.size() > 8 ? 11 : 13);
        for (Competitor c : profiles) {
            javafx.scene.text.Text t = new javafx.scene.text.Text(c.getName() != null ? c.getName() : "");
            t.setFont(nameFont);
            t.setWrappingWidth(cardW - 16);
            double h = t.getLayoutBounds().getHeight();
            if (h > maxCompetitorNameH) maxCompetitorNameH = h;
        }
        double uniformPlayerCardH = Math.max(130.0, (profiles.size() > 8 ? 32 : 40) + maxCompetitorNameH + 52 + 20);

        for (Competitor c : profiles) {
            VBox scoreCard = new VBox(4);
            scoreCard.setAlignment(Pos.CENTER);
            scoreCard.setMinWidth(profiles.size() > 8 ? 95 : (profiles.size() > 4 ? 115 : 135));
            scoreCard.setPrefWidth(cardW);
            scoreCard.setMinHeight(uniformPlayerCardH);
            scoreCard.setPrefHeight(uniformPlayerCardH);
            if (profiles.size() <= 6) {
                HBox.setHgrow(scoreCard, Priority.ALWAYS);
            }
            scoreCard.setStyle(String.format("-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 8px 10px;", ThemeManager.getCardHex()));

            // Avatar Preview
            ImageView avatarView = new ImageView();
            avatarView.setFitWidth(profiles.size() > 8 ? 32 : 40);
            avatarView.setFitHeight(profiles.size() > 8 ? 32 : 40);
            avatarView.setPreserveRatio(true);

            if (c.getAvatarPath() != null) {
                try {
                    File file = new File(c.getAvatarPath());
                    if (!file.exists()) file = new File("template-offline/" + c.getAvatarPath());

                    if (file.exists()) {
                        avatarView.setImage(new Image(file.toURI().toString()));
                    }
                } catch (Exception ignored) {}
            }

            Label nameLabel = new Label(c.getName());
            nameLabel.setStyle(String.format("-fx-font-weight: 900; -fx-text-fill: %s; -fx-font-size: %s;", ThemeManager.getTextOnCardPrimaryHex(), profiles.size() > 8 ? "11px" : "13px"));
            nameLabel.setWrapText(true);
            nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            nameLabel.setAlignment(Pos.CENTER);
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            nameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            nameLabel.setEllipsisString("");

            Label scoreLabel = new Label(String.valueOf(this.config.getScore(c.getId())));
            scoreLabel.setStyle(String.format("-fx-font-size: %s; -fx-font-weight: 900; -fx-text-fill: %s;", profiles.size() > 8 ? "18px" : "22px", ThemeManager.getAccentHex()));
            scoreLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            scoreLabel.setEllipsisString("");
            scoreLabels.put(c.getId(), scoreLabel);

            Label statusBadge = new Label(I18n.get("arena.player.click_to_select"));
            statusBadge.setStyle(String.format("-fx-font-size: 9px; -fx-text-fill: %s;", ThemeManager.getTextOnCardSecondaryHex()));
            statusBadge.setWrapText(true);
            statusBadge.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            statusBadge.setAlignment(Pos.CENTER);
            statusBadge.setMaxWidth(Double.MAX_VALUE);
            statusBadge.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            statusBadge.setEllipsisString("");
            statusBadgeLabels.put(c.getId(), statusBadge);

            scoreCard.getChildren().addAll(avatarView, nameLabel, scoreLabel, statusBadge);

            scoreCard.setOnMouseClicked(e -> selectCompetitor(c.getId()));
            competitorCards.put(c.getId(), scoreCard);
            topPlayersRow.getChildren().add(scoreCard);
        }

        // =========================================================================
        // 2. MIDDLE ROW: BOTONES PARA AGREGAR PUNTOS A LOS JUGADORES
        // =========================================================================
        VBox middleScoreControlPanel = new VBox(10);
        middleScoreControlPanel.setPadding(new Insets(10, 16, 10, 16));
        middleScoreControlPanel.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.98));
        middleScoreControlPanel.setMaxWidth(Double.MAX_VALUE);
        middleScoreControlPanel.setStyle(String.format("-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;", ThemeManager.getCardHex()));
        middleScoreControlPanel.setAlignment(Pos.CENTER);

        HBox scoreHeaderRow = new HBox(12);
        scoreHeaderRow.setAlignment(Pos.CENTER);

        Label pointTitle = new Label(I18n.get("arena.scoreboard.title"));
        SvgEmoji.setGraphic(pointTitle, "lightning", 16);
        pointTitle.setStyle(String.format("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-letter-spacing: 1px;", ThemeManager.getAccentHex()));
        pointTitle.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        pointTitle.setEllipsisString("");

        selectedPlayerIndicatorLabel = new Label("");
        selectedPlayerIndicatorLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #38bdf8; -fx-background-color: rgba(56, 189, 248, 0.18); -fx-padding: 2px 10px; -fx-background-radius: 6px;");
        selectedPlayerIndicatorLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        selectedPlayerIndicatorLabel.setEllipsisString("");

        scoreHeaderRow.getChildren().addAll(pointTitle, selectedPlayerIndicatorLabel);

        HBox scoreControlsRow = new HBox(12);
        scoreControlsRow.setAlignment(Pos.CENTER);

        HBox scoreBtnBox = new HBox(10);
        scoreBtnBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(scoreBtnBox, Priority.ALWAYS);

        List<String> presets = this.config.getScorePresets();
        for (String preset : presets) {
            Button btn = new Button(preset);
            boolean isNegative = preset.startsWith("-");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            btn.setEllipsisString("");
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-font-size: 13px; -fx-padding: 8px 16px; -fx-background-radius: 10px; -fx-cursor: hand;", isNegative ? "#f43f5e" : ThemeManager.getAccentHex(), isNegative ? "#ffffff" : ThemeManager.getTextOnAccentPrimaryHex()));

            btn.setOnAction(e -> {
                if (selectedCompetitorId != null) {
                    try {
                        int amount = Integer.parseInt(preset.replace("+", "").trim());
                        this.config.adjustScore(selectedCompetitorId, amount);

                        if (scoreLabels.containsKey(selectedCompetitorId)) {
                            scoreLabels.get(selectedCompetitorId).setText(String.valueOf(this.config.getScore(selectedCompetitorId)));
                        }
                    } catch (Exception ignored) {}
                }
            });

            scoreBtnBox.getChildren().add(btn);
        }

        Button rouletteArenaBtn = new Button(I18n.get("game.roulette.title"));
        SvgEmoji.setGraphic(rouletteArenaBtn, "slot-machine", 16);
        rouletteArenaBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 8px 18px; -fx-background-radius: 10px; -fx-border-color: rgba(245, 158, 11, 0.5); -fx-border-radius: 10px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getAccentHex()));
        rouletteArenaBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        rouletteArenaBtn.setEllipsisString("");
        rouletteArenaBtn.setOnAction(e -> {
            StackPane overlay = createRouletteOverlay(rootWrapper);
            if (!rootWrapper.getChildren().contains(overlay)) {
                rootWrapper.getChildren().add(overlay);
            }
        });

        Button finishMatchBtn = new Button(I18n.get("arena.btn.finish_tournament"));
        finishMatchBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 8px 20px; -fx-background-radius: 10px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        finishMatchBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        finishMatchBtn.setEllipsisString("");
        finishMatchBtn.setOnAction(e -> {
            StackPane overlay = createConfirmationOverlay(rootWrapper);
            if (!rootWrapper.getChildren().contains(overlay)) {
                rootWrapper.getChildren().add(overlay);
            }
        });

        scoreControlsRow.getChildren().addAll(scoreBtnBox, rouletteArenaBtn, finishMatchBtn);
        middleScoreControlPanel.getChildren().addAll(scoreHeaderRow, scoreControlsRow);

        updateSelectionStyles();

        // =========================================================================
        // 3. BOTTOM ROW: EL JUEGO (APROVECHANDO LA CAPACIDAD DE PANTALLA)
        // =========================================================================
        VBox bottomGamePanel = new VBox(14);
        bottomGamePanel.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.98));
        bottomGamePanel.setMaxWidth(Double.MAX_VALUE);
        bottomGamePanel.setPadding(new Insets(16));
        bottomGamePanel.setStyle(String.format("-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;", ThemeManager.getCardHex()));
        VBox.setVgrow(bottomGamePanel, Priority.ALWAYS);

        HBox gameNavigationHeader = new HBox(12);
        gameNavigationHeader.setAlignment(Pos.CENTER);

        Button prevGameBtn = new Button(I18n.get("arena.btn.prev"));
        prevGameBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 16px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        prevGameBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        prevGameBtn.setEllipsisString("");
        prevGameBtn.setOnAction(e -> switchGame(-1));

        activeGameTitle = new Label(I18n.get("arena.header.game") + ": ");
        SvgEmoji.setGraphic(activeGameTitle, "gamepad", 18);
        activeGameTitle.setStyle(String.format("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: %s;", ThemeManager.getAccentHex()));
        activeGameTitle.setWrapText(true);
        activeGameTitle.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        activeGameTitle.setEllipsisString("");

        Button rulesBtn = new Button(I18n.get("arena.btn.rules"));
        SvgEmoji.setGraphic(rulesBtn, "book", 16);
        rulesBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 16px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        rulesBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        rulesBtn.setEllipsisString("");
        rulesBtn.setOnAction(e -> {
            if (rootWrapper != null) {
                StackPane overlay = createInstructionsOverlay(rootWrapper);
                if (!rootWrapper.getChildren().contains(overlay)) {
                    rootWrapper.getChildren().add(overlay);
                }
            }
        });

        gameCounterLabel = new Label("1 / 1");
        gameCounterLabel.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: %s;", ThemeManager.getTextOnCardSecondaryHex()));
        gameCounterLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        gameCounterLabel.setEllipsisString("");

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);

        Button nextGameBtn = new Button(I18n.get("arena.btn.next"));
        nextGameBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 16px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        nextGameBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        nextGameBtn.setEllipsisString("");
        nextGameBtn.setOnAction(e -> switchGame(1));

        gameNavigationHeader.getChildren().addAll(prevGameBtn, activeGameTitle, rulesBtn, gameCounterLabel, nextGameBtn);

        gameAreaContainer = new VBox(16);
        gameAreaContainer.setAlignment(Pos.CENTER);
        gameAreaContainer.setPadding(new Insets(16));
        VBox.setVgrow(gameAreaContainer, Priority.ALWAYS);

        updateActiveGameDisplay();

        bottomGamePanel.getChildren().addAll(gameNavigationHeader, gameAreaContainer);

        contentBox.getChildren().addAll(topPlayersScroll, middleScoreControlPanel, bottomGamePanel);

        setContent(rootWrapper);
        ThemeManager.applyTextScale(this, ThemeManager.getFontScale());
    }

    private StackPane createConfirmationOverlay(StackPane rootPane) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.82);");
        overlay.setAlignment(Pos.CENTER);

        // Compact Square Dimensions with dynamic font scaling
        VBox modalCard = new VBox(16);
        double scale = ThemeManager.getFontScale();
        modalCard.setPrefWidth(Math.max(350, 350 * scale));
        modalCard.setMaxWidth(Math.max(480, 480 * scale));
        modalCard.setMinHeight(Region.USE_COMPUTED_SIZE);
        modalCard.setAlignment(Pos.CENTER);
        modalCard.setPadding(new Insets(24));
        modalCard.setStyle(String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
            ThemeManager.getCardHex(),
            ThemeManager.getAccentHex()
        ));

        // Trophy Icon Box (Loading from assets/icons/trophy_dialog.png)
        File trophyIconFile = new File("assets/icons/trophy_dialog.png");
        if (!trophyIconFile.exists()) trophyIconFile = new File("template-offline/assets/icons/trophy_dialog.png");

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(70, 70);
        iconCircle.setMaxSize(70, 70);
        iconCircle.setStyle(String.format("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: %s; -fx-border-radius: 18px; -fx-background-radius: 18px;", ThemeManager.getAccentHex()));

        if (trophyIconFile.exists()) {
            try {
                ImageView imgView = new ImageView(new Image(trophyIconFile.toURI().toString()));
                imgView.setFitWidth(56);
                imgView.setFitHeight(56);
                imgView.setPreserveRatio(true);
                iconCircle.getChildren().add(imgView);
            } catch (Exception e) {
                Node trophyIcon = SvgEmoji.create("trophy", 32);
                iconCircle.getChildren().add(trophyIcon);
            }
        } else {
            Node trophyIcon = SvgEmoji.create("trophy", 32);
            iconCircle.getChildren().add(trophyIcon);
        }

        VBox textGroup = new VBox(6);
        textGroup.setAlignment(Pos.CENTER);

        Label titleL = new Label(I18n.get("arena.confirm.finish_title"));
        titleL.setStyle(String.format("-fx-font-size: 17px; -fx-font-weight: 900; -fx-text-fill: %s;", ThemeManager.getTextOnCardPrimaryHex()));

        Label descL = new Label(I18n.get("arena.confirm.finish_desc"));
        descL.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s; -fx-text-alignment: center;", ThemeManager.getTextOnCardSecondaryHex()));
        descL.setWrapText(true);

        textGroup.getChildren().addAll(titleL, descL);

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER);

        File cancelIconFile = new File("assets/icons/button_cancel.png");
        if (!cancelIconFile.exists()) cancelIconFile = new File("template-offline/assets/icons/button_cancel.png");

        File confirmIconFile = new File("assets/icons/button_confirm.png");
        if (!confirmIconFile.exists()) confirmIconFile = new File("template-offline/assets/icons/button_confirm.png");

        Button cancelBtn = new Button(I18n.get("arena.confirm.cancel"));
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);
        cancelBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 10px 16px; -fx-background-radius: 10px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 10px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));

        if (cancelIconFile.exists()) {
            try {
                ImageView cancelImg = new ImageView(new Image(cancelIconFile.toURI().toString()));
                cancelImg.setFitWidth(18);
                cancelImg.setFitHeight(18);
                cancelImg.setPreserveRatio(true);
                cancelBtn.setGraphic(cancelImg);
            } catch (Exception ignored) {}
        }
        cancelBtn.setOnAction(ev -> rootPane.getChildren().remove(overlay));

        Button confirmBtn = new Button(I18n.get("arena.confirm.yes"));
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(confirmBtn, Priority.ALWAYS);
        confirmBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 10px 16px; -fx-background-radius: 10px; -fx-cursor: hand;", ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));

        if (confirmIconFile.exists()) {
            try {
                ImageView confirmImg = new ImageView(new Image(confirmIconFile.toURI().toString()));
                confirmImg.setFitWidth(18);
                confirmImg.setFitHeight(18);
                confirmImg.setPreserveRatio(true);
                confirmBtn.setGraphic(confirmImg);
            } catch (Exception ignored) {}
        }
        confirmBtn.setOnAction(ev -> {
            rootPane.getChildren().remove(overlay);
            if (this.listener != null) this.listener.onEndMatch(this.config);
        });

        btnBox.getChildren().addAll(cancelBtn, confirmBtn);

        modalCard.getChildren().addAll(iconCircle, textGroup, btnBox);
        overlay.getChildren().add(modalCard);
        ThemeManager.applyTextScale(overlay, ThemeManager.getFontScale());

        return overlay;
    }

    private void switchGame(int delta) {
        List<GameDescriptor> games = this.config.getSelectedGames();
        if (games == null || games.isEmpty()) return;

        // Stop all active audio / music from the previous game
        com.gameshowcenter.offline.sound.SoundManager.getInstance().stopAll();

        // Notify previous driver of exit
        if (currentGameIndex >= 0 && currentGameIndex < games.size()) {
            GameDescriptor prevGame = games.get(currentGameIndex);
            IGameArenaDriver prevDriver = GameStageRegistry.getDriver(prevGame.getName());
            if (prevDriver != null) {
                prevDriver.onGameExited(prevGame, this.config);
            }
        }

        currentGameIndex += delta;
        if (currentGameIndex < 0) currentGameIndex = games.size() - 1;
        if (currentGameIndex >= games.size()) currentGameIndex = 0;

        setVvalue(0.0);
        updateActiveGameDisplay();
    }

    private StackPane createInstructionsOverlay(StackPane rootPane) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        overlay.setAlignment(Pos.CENTER);

        List<GameDescriptor> games = this.config.getSelectedGames();
        final GameDescriptor activeGame = (games != null && currentGameIndex >= 0 && currentGameIndex < games.size())
            ? games.get(currentGameIndex)
            : null;

        VBox modalCard = new VBox(16);
        double scale = ThemeManager.getFontScale();
        modalCard.setPrefWidth(Math.max(560, 560 * scale));
        modalCard.setMaxWidth(Math.max(700, 700 * scale));
        modalCard.setMinHeight(Region.USE_COMPUTED_SIZE);
        modalCard.setMaxHeight(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() * 0.90);
        modalCard.setAlignment(Pos.TOP_CENTER);
        modalCard.setPadding(new Insets(24));
        modalCard.setStyle(String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
            ThemeManager.getCardHex(),
            ThemeManager.getAccentHex()
        ));

        String gameName = activeGame != null ? activeGame.getName() : "Game";
        Label titleLabel = new Label(gameName.toUpperCase() + " - " + I18n.get("arena.btn.rules"));
        SvgEmoji.setGraphic(titleLabel, "book", 18);
        titleLabel.setStyle(String.format("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: %s;", ThemeManager.getTextOnCardPrimaryHex()));

        HBox langRow = new HBox(8);
        langRow.setAlignment(Pos.CENTER);

        final String[] selectedLang = { I18n.getLanguage() };

        Label fallbackNotice = new Label("");
        fallbackNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.15); -fx-padding: 4px 10px; -fx-background-radius: 6px;");
        fallbackNotice.setVisible(false);
        fallbackNotice.setManaged(false);

        VBox contentList = new VBox(10);
        contentList.setPadding(new Insets(8));

        Label descLabel = new Label("");
        descLabel.setStyle(String.format("-fx-font-size: 13px; -fx-text-fill: %s; -fx-font-style: italic;", ThemeManager.getTextOnCardSecondaryHex()));
        descLabel.setWrapText(true);

        ScrollPane scrollContent = new ScrollPane();
        scrollContent.setContent(contentList);
        scrollContent.setFitToWidth(true);
        scrollContent.setPrefHeight(260);
        scrollContent.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 12px;");

        List<Button> langButtons = new ArrayList<>();
        Runnable refreshView = () -> {
            if (activeGame == null) {
                descLabel.setText(I18n.get("arena.no_game_active"));
                contentList.getChildren().clear();
                return;
            }

            String current = selectedLang[0];
            descLabel.setText(activeGame.getDescription(current));

            String rawInstructions = activeGame.getInstructions(current);
            boolean hasNativeTranslation = false;
            if (activeGame.getTranslations().containsKey(current.toLowerCase())) {
                GameDescriptor.Translation tr = activeGame.getTranslations().get(current.toLowerCase());
                if (tr != null && tr.instructions != null && !tr.instructions.trim().isEmpty()) {
                    hasNativeTranslation = true;
                }
            }

            boolean isFallback = !current.equalsIgnoreCase("en") && !hasNativeTranslation;
            if (isFallback) {
                fallbackNotice.setText("ℹ️ " + (current.equals("es") ? "Instrucciones en inglés (idioma no traducido)" : "Showing English instructions (fallback)"));
                fallbackNotice.setVisible(true);
                fallbackNotice.setManaged(true);
            } else {
                fallbackNotice.setVisible(false);
                fallbackNotice.setManaged(false);
            }

            contentList.getChildren().clear();
            if (rawInstructions == null || rawInstructions.trim().isEmpty()) {
                Label emptyLbl = new Label(current.equals("es") ? "No hay instrucciones disponibles." : "No instructions available.");
                emptyLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
                contentList.getChildren().add(emptyLbl);
            } else {
                String[] lines = rawInstructions.split("\n");
                int stepNum = 1;
                for (String line : lines) {
                    String clean = line.trim();
                    if (clean.isEmpty()) continue;
                    clean = clean.replaceFirst("^\\d+[\\.\\-]\\s*", "");

                    HBox itemRow = new HBox(10);
                    itemRow.setAlignment(Pos.CENTER_LEFT);
                    itemRow.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-padding: 8px 12px; -fx-background-radius: 8px;");

                    Label badge = new Label(String.valueOf(stepNum++));
                    badge.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: 900; -fx-min-width: 22px; -fx-min-height: 22px; -fx-alignment: center; -fx-background-radius: 11px;",
                        ThemeManager.getAccentHex(),
                        ThemeManager.getTextOnAccentPrimaryHex()
                    ));

                    Label stepLabel = new Label(clean);
                    stepLabel.setStyle(String.format("-fx-font-size: 13px; -fx-text-fill: %s;", ThemeManager.getTextOnCardPrimaryHex()));
                    stepLabel.setWrapText(true);
                    HBox.setHgrow(stepLabel, Priority.ALWAYS);

                    itemRow.getChildren().addAll(badge, stepLabel);
                    contentList.getChildren().add(itemRow);
                }
            }

            for (Button btn : langButtons) {
                String code = (String) btn.getUserData();
                if (code.equalsIgnoreCase(current)) {
                    btn.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 8px; -fx-cursor: hand;",
                        ThemeManager.getAccentHex(),
                        ThemeManager.getTextOnAccentPrimaryHex()
                    ));
                } else {
                    btn.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 8px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 8px; -fx-cursor: hand;",
                        ThemeManager.getButtonHex(),
                        ThemeManager.getTextOnButtonPrimaryHex()
                    ));
                }
            }
        };

        for (I18n.LanguageOption opt : I18n.SUPPORTED_LANGUAGES) {
            Button lBtn = new Button(opt.code.toUpperCase());
            SvgEmoji.setGraphic(lBtn, opt.flag, 16);
            lBtn.setUserData(opt.code);
            lBtn.setOnAction(e -> {
                selectedLang[0] = opt.code;
                refreshView.run();
            });
            langButtons.add(lBtn);
            langRow.getChildren().add(lBtn);
        }

        refreshView.run();

        Button closeBtn = new Button(I18n.get("arena.confirm.cancel"));
        closeBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8px 24px; -fx-background-radius: 10px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        closeBtn.setOnAction(e -> rootPane.getChildren().remove(overlay));

        modalCard.getChildren().addAll(titleLabel, langRow, fallbackNotice, descLabel, scrollContent, closeBtn);
        overlay.getChildren().add(modalCard);
        ThemeManager.applyTextScale(overlay, ThemeManager.getFontScale());

        return overlay;
    }

    private void updateActiveGameDisplay() {
        // Stop any running sound when updating game display
        com.gameshowcenter.offline.sound.SoundManager.getInstance().stopAll();

        List<GameDescriptor> games = this.config.getSelectedGames();
        gameAreaContainer.getChildren().clear();

        if (games != null && !games.isEmpty()) {
            GameDescriptor activeGame = games.get(currentGameIndex);
            activeGameTitle.setText(I18n.get("arena.header.game") + ": " + activeGame.getName().toUpperCase());
            SvgEmoji.setGraphic(activeGameTitle, "gamepad", 18);
            gameCounterLabel.setText(String.format("%d / %d", currentGameIndex + 1, games.size()));

            Region stageNode = com.gameshowcenter.offline.games.GameStageRegistry.createStage(
                activeGame,
                config,
                winner -> {
                    if (winner != null) selectCompetitor(winner.getId());
                }
            );
            if (stageNode != null) {
                VBox.setVgrow(stageNode, Priority.ALWAYS);
                stageNode.setMaxWidth(Double.MAX_VALUE);
                stageNode.setMaxHeight(Double.MAX_VALUE);
                gameAreaContainer.getChildren().add(stageNode);
                ThemeManager.applyTextScale(stageNode, ThemeManager.getFontScale());
            }
        } else {
            activeGameTitle.setText(I18n.get("arena.title"));
            SvgEmoji.setGraphic(activeGameTitle, "gamepad", 18);
            gameCounterLabel.setText("0 / 0");

            Label emptyLabel = new Label(I18n.get("arena.no_games"));
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            gameAreaContainer.getChildren().add(emptyLabel);
        }
    }

    private void selectCompetitor(String competitorId) {
        this.selectedCompetitorId = competitorId;
        updateSelectionStyles();
    }

    private void updateSelectionStyles() {
        String selectedName = "";
        for (Map.Entry<String, VBox> entry : competitorCards.entrySet()) {
            String cId = entry.getKey();
            VBox card = entry.getValue();
            Label badge = statusBadgeLabels.get(cId);

            if (cId.equals(selectedCompetitorId)) {
                card.getStyleClass().setAll("score-badge-selected");
                if (badge != null) {
                    badge.setText(I18n.get("arena.player.selected"));
                    badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-background-color: rgba(56, 189, 248, 0.2); -fx-padding: 2px 8px; -fx-background-radius: 6px;");
                }
                for (Competitor c : config.getProfiles()) {
                    if (c.getId().equals(cId)) {
                        selectedName = c.getName();
                        break;
                    }
                }
            } else {
                card.getStyleClass().setAll("score-badge");
                if (badge != null) {
                    badge.setText(I18n.get("arena.player.click_to_select"));
                    badge.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                }
            }
        }

        if (selectedPlayerIndicatorLabel != null) {
            if (!selectedName.isEmpty()) {
                selectedPlayerIndicatorLabel.setText("● " + selectedName.toUpperCase());
                selectedPlayerIndicatorLabel.setVisible(true);
            } else {
                selectedPlayerIndicatorLabel.setVisible(false);
            }
        }
    }

    public void refreshScores() {
        for (Competitor c : config.getProfiles()) {
            if (scoreLabels.containsKey(c.getId())) {
                scoreLabels.get(c.getId()).setText(String.valueOf(config.getScore(c.getId())));
            }
        }
    }

    private StackPane createRouletteOverlay(StackPane rootPane) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        overlay.setAlignment(Pos.CENTER);

        VBox modalCard = new VBox(14);
        modalCard.setMaxWidth(520);
        modalCard.setAlignment(Pos.CENTER);
        modalCard.setPadding(new Insets(20));
        modalCard.setStyle(String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-border-radius: 24px; -fx-background-radius: 24px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 24, 0, 0, 8);",
            ThemeManager.getMainBoxHex(), ThemeManager.getAccentHex()
        ));

        Label titleLabel = new Label(I18n.get("game.roulette.title"));
        SvgEmoji.setGraphic(titleLabel, "slot-machine", 18);
        titleLabel.setStyle(String.format("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: %s;", ThemeManager.getAccentHex()));

        Label subLabel = new Label(I18n.get("arena.roulette.desc", "Sorteo en vivo de participantes con probabilidades iguales (1:1)."));
        subLabel.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s; -fx-font-weight: bold;", ThemeManager.getTextOnCardSecondaryHex()));

        com.gameshowcenter.offline.games.views.RouletteFxStage rouletteStage =
            new com.gameshowcenter.offline.games.views.RouletteFxStage(
                this.config.getProfiles(),
                null,
                winner -> {
                    if (winner != null) {
                        selectCompetitor(winner.getId());
                    }
                }
            );
        rouletteStage.setMaxWidth(Double.MAX_VALUE);

        Button closeBtn = new Button(I18n.get("arena.confirm.cancel"));
        SvgEmoji.setGraphic(closeBtn, "close", 14);
        closeBtn.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8px 24px; -fx-background-radius: 10px; -fx-cursor: hand;",
            ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()
        ));
        closeBtn.setOnAction(e -> {
            com.gameshowcenter.offline.sound.SoundManager.getInstance().stopAll();
            rootPane.getChildren().remove(overlay);
        });

        modalCard.getChildren().addAll(titleLabel, subLabel, rouletteStage, closeBtn);
        overlay.getChildren().add(modalCard);
        ThemeManager.applyTextScale(overlay, ThemeManager.getFontScale());

        return overlay;
    }
}
