package com.gameshowcenter.offline.views;

import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.model.MatchConfig;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.util.List;

public class ProductionDeckFxView extends ScrollPane {

    public interface ProductionDeckListener {
        void onLaunchArena(MatchConfig config);
        void onBackToSettings();
    }

    private final MatchConfig config;
    private final ProductionDeckListener listener;
    private final VBox scoresContainer = new VBox(14);

    public ProductionDeckFxView(MatchConfig config, ProductionDeckListener listener) {
        this.config = config != null ? config : new MatchConfig();
        this.listener = listener;

        setFitToWidth(true);
        setFitToHeight(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(20));
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.prefWidthProperty().bind(widthProperty().subtract(48));
        contentBox.maxWidthProperty().bind(widthProperty().subtract(48));
        contentBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 20px;", ThemeManager.getCardRgbaString()));

        // Header Title
        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER);
        Label titleLabel = new Label(I18n.get("deck.title"));
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #f8fafc;");

        Label subLabel = new Label(I18n.get("deck.subtitle"));
        subLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(titleLabel, subLabel);

        // SCOREBOARD PANEL
        VBox scoreBoardPanel = new VBox(14);
        scoreBoardPanel.getStyleClass().add("panel-card-glow");
        scoreBoardPanel.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.98));
        scoreBoardPanel.setMaxWidth(Double.MAX_VALUE);
        scoreBoardPanel.setStyle(String.format("-fx-background-color: %s;", ThemeManager.getCardRgbaString()));

        Label scoreTitle = new Label(I18n.get("deck.scoreboard_title"));
        scoreTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #a5b4fc;");

        renderScoreControls();
        scoreBoardPanel.getChildren().addAll(scoreTitle, scoresContainer);

        // BOTTOM ACTION ROW
        HBox bottomActions = new HBox(16);
        bottomActions.setAlignment(Pos.CENTER_RIGHT);

        Button backBtn = new Button(I18n.get("deck.btn.back"));
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> {
            if (listener != null) listener.onBackToSettings();
        });

        Button launchArenaBtn = new Button(I18n.get("deck.btn.launch"));
        launchArenaBtn.getStyleClass().add("btn-primary");
        launchArenaBtn.setStyle("-fx-font-size: 15px; -fx-padding: 12px 30px;");
        launchArenaBtn.setOnAction(e -> {
            if (listener != null) listener.onLaunchArena(config);
        });

        bottomActions.getChildren().addAll(backBtn, launchArenaBtn);

        contentBox.getChildren().addAll(titleBox, scoreBoardPanel, bottomActions);

        StackPane centerWrap = new StackPane(contentBox);
        centerWrap.setAlignment(Pos.TOP_CENTER);
        centerWrap.setPadding(new Insets(16));
        setContent(centerWrap);
    }

    private void renderScoreControls() {
        scoresContainer.getChildren().clear();
        List<Competitor> profiles = config.getProfiles();
        List<String> presets = config.getScorePresets();

        for (Competitor c : profiles) {
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #111625; -fx-padding: 12px 18px; -fx-background-radius: 12px; -fx-border-color: #29334d; -fx-border-radius: 12px;");

            // Avatar Preview
            ImageView avatarView = new ImageView();
            avatarView.setFitWidth(40);
            avatarView.setFitHeight(40);
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

            Label nameL = new Label(c.getName());
            nameL.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-font-size: 14px;");
            nameL.setPrefWidth(160);

            Label scoreL = new Label(String.valueOf(config.getScore(c.getId())));
            scoreL.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
            scoreL.setPrefWidth(70);

            HBox scoreBtnBox = new HBox(8);
            scoreBtnBox.setAlignment(Pos.CENTER_LEFT);

            for (String preset : presets) {
                Button btn = new Button(preset);
                boolean isNegative = preset.startsWith("-");
                btn.getStyleClass().add(isNegative ? "btn-accent-rose" : "btn-accent-emerald");

                btn.setOnAction(e -> {
                    try {
                        int amount = Integer.parseInt(preset.replace("+", "").trim());
                        config.adjustScore(c.getId(), amount);
                        scoreL.setText(String.valueOf(config.getScore(c.getId())));
                    } catch (Exception ignored) {}
                });

                scoreBtnBox.getChildren().add(btn);
            }

            row.getChildren().addAll(avatarView, nameL, scoreL, scoreBtnBox);
            scoresContainer.getChildren().add(row);
        }
    }
}
