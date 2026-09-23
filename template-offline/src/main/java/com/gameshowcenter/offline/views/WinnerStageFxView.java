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
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WinnerStageFxView extends ScrollPane {

    public interface WinnerStageListener {
        void onRestartMatch();
        void onReturnHome();
    }

    public WinnerStageFxView(MatchConfig config, WinnerStageListener listener) {
        setFitToWidth(true);
        setFitToHeight(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox contentBox = new VBox(24);
        contentBox.setPadding(new Insets(24));
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.prefWidthProperty().bind(widthProperty().subtract(48));
        contentBox.maxWidthProperty().bind(widthProperty().subtract(48));
        contentBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 20px;", ThemeManager.getCardRgbaString()));

        // Celebration Trophy & Title
        Label trophyIcon = new Label("👑");
        trophyIcon.setStyle("-fx-font-size: 64px; -fx-effect: dropshadow(three-pass-box, rgba(245, 158, 11, 0.6), 20, 0, 0, 4);");

        Text mainTitle = new Text(I18n.get("winner.title"));
        mainTitle.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-fill: linear-gradient(to right, #fbbf24, #f59e0b, #d97706);");

        Label subtitle = new Label(I18n.get("winner.subtitle"));
        subtitle.setStyle(String.format("-fx-font-size: 14px; -fx-text-fill: %s;", ThemeManager.getTextOnMainBoxSecondaryHex()));

        // SORT COMPETITORS BY SCORE
        List<Competitor> sortedProfiles = new ArrayList<>(config != null ? config.getProfiles() : new ArrayList<>());
        sortedProfiles.sort(Comparator.comparingInt((Competitor c) -> config != null ? config.getScore(c.getId()) : 0).reversed());

        // 1. PROMINENT WINNER SPOTLIGHT CARD (CIRCULAR AVATAR + WINNER NAME ONLY)
        VBox winnerSpotlightCard = new VBox(12);
        winnerSpotlightCard.getStyleClass().add("podium-gold");
        winnerSpotlightCard.setAlignment(Pos.CENTER);
        winnerSpotlightCard.setPadding(new Insets(24));
        winnerSpotlightCard.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.70));
        winnerSpotlightCard.setMaxWidth(Double.MAX_VALUE);

        if (!sortedProfiles.isEmpty()) {
            int topScore = config != null ? config.getScore(sortedProfiles.get(0).getId()) : 0;
            List<Competitor> topWinners = new ArrayList<>();
            for (Competitor c : sortedProfiles) {
                int sc = config != null ? config.getScore(c.getId()) : 0;
                if (sc == topScore) {
                    topWinners.add(c);
                } else {
                    break;
                }
            }

            boolean isTie = topWinners.size() > 1;
            Label winnerCrownBadge = new Label(isTie ? I18n.get("winner.tie_label") : I18n.get("winner.champion_label"));
            winnerCrownBadge.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #fbbf24; -fx-letter-spacing: 1px;");

            HBox avatarsBox = new HBox(20);
            avatarsBox.setAlignment(Pos.CENTER);

            for (Competitor winner : topWinners) {
                VBox singleWinnerBox = new VBox(8);
                singleWinnerBox.setAlignment(Pos.CENTER);

                // Circular Cropped Avatar Image
                StackPane avatarFrame = new StackPane();
                avatarFrame.setAlignment(Pos.CENTER);

                ImageView winnerAvatarView = new ImageView();
                int avatarDim = isTie ? 90 : 110;
                winnerAvatarView.setFitWidth(avatarDim);
                winnerAvatarView.setFitHeight(avatarDim);
                winnerAvatarView.setPreserveRatio(false);

                // Circular Clip
                Circle clipCircle = new Circle(avatarDim / 2.0, avatarDim / 2.0, avatarDim / 2.0);
                winnerAvatarView.setClip(clipCircle);

                if (winner.getAvatarPath() != null) {
                    try {
                        File file = new File(winner.getAvatarPath());
                        if (!file.exists()) file = new File("template-offline/" + winner.getAvatarPath());

                        if (file.exists()) {
                            winnerAvatarView.setImage(new Image(file.toURI().toString()));
                        }
                    } catch (Exception ignored) {}
                }

                // Outer Circular Ring Border
                Circle borderRing = new Circle(avatarDim / 2.0 + 3);
                borderRing.setStyle("-fx-fill: transparent; -fx-stroke: linear-gradient(to bottom, #fbbf24, #d97706); -fx-stroke-width: 4px; -fx-effect: dropshadow(three-pass-box, rgba(251, 191, 36, 0.8), 12, 0, 0, 2);");

                avatarFrame.getChildren().addAll(borderRing, winnerAvatarView);

                Label winnerName = new Label(winner.getName());
                winnerName.setStyle(String.format("-fx-font-weight: 900; -fx-font-size: %s; -fx-text-fill: %s; -fx-wrap-text: true; -fx-alignment: center; -fx-text-alignment: center;", isTie ? "20px" : "26px", ThemeManager.getTextOnCardPrimaryHex()));

                singleWinnerBox.getChildren().addAll(avatarFrame, winnerName);
                avatarsBox.getChildren().add(singleWinnerBox);
            }

            Label winnerScoreLabel = new Label(topScore + " PTS");
            winnerScoreLabel.setStyle(String.format("-fx-font-weight: 900; -fx-font-size: 32px; -fx-text-fill: %s;", ThemeManager.getAccentHex()));

            winnerSpotlightCard.getChildren().addAll(winnerCrownBadge, avatarsBox, winnerScoreLabel);
        }

        // 2. LEADERBOARD TABLE FOR REMAINING COMPETITORS (NO IMAGES, NAME AND SCORE ONLY)
        VBox leaderboardPanel = new VBox(12);
        leaderboardPanel.getStyleClass().add("panel-card");
        leaderboardPanel.setStyle(String.format("-fx-background-color: %s;", ThemeManager.getCardRgbaString()));
        leaderboardPanel.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.90));
        leaderboardPanel.setMaxWidth(Double.MAX_VALUE);
        leaderboardPanel.setAlignment(Pos.TOP_CENTER);

        Label leaderboardTitle = new Label("📊 " + I18n.get("winner.leaderboard.title"));
        leaderboardTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;", ThemeManager.getTextOnCardPrimaryHex()));
        leaderboardPanel.getChildren().add(leaderboardTitle);

        VBox leaderboardRows = new VBox(8);
        leaderboardRows.setAlignment(Pos.TOP_CENTER);

        int topScore = !sortedProfiles.isEmpty() && config != null ? config.getScore(sortedProfiles.get(0).getId()) : 0;
        int winnersCount = 0;
        for (Competitor c : sortedProfiles) {
            if ((config != null ? config.getScore(c.getId()) : 0) == topScore) {
                winnersCount++;
            } else {
                break;
            }
        }

        if (sortedProfiles.size() > winnersCount) {
            for (int i = winnersCount; i < sortedProfiles.size(); i++) {
                Competitor c = sortedProfiles.get(i);
                int score = config != null ? config.getScore(c.getId()) : 0;
                int rank = i + 1;

                HBox row = new HBox(16);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle(String.format("-fx-background-color: %s; -fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 10px;", ThemeManager.getButtonHex()));

                Label rankLabel = new Label("#" + rank);
                rankLabel.setStyle(String.format("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: %s;", ThemeManager.getTextOnButtonPrimaryHex()));
                rankLabel.setPrefWidth(45);

                Label nameLabel = new Label(c.getName());
                nameLabel.setStyle(String.format("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: %s;", ThemeManager.getTextOnButtonPrimaryHex()));
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                Label scoreLabel = new Label(score + " PTS");
                scoreLabel.setStyle(String.format("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: %s;", ThemeManager.getAccentHex()));

                row.getChildren().addAll(rankLabel, nameLabel, scoreLabel);
                leaderboardRows.getChildren().add(row);
            }
        } else {
            Label singlePlayerNote = new Label(I18n.get("winner.no_more_competitors"));
            singlePlayerNote.setStyle(String.format("-fx-text-fill: %s; -fx-font-style: italic;", ThemeManager.getTextOnCardSecondaryHex()));
            leaderboardRows.getChildren().add(singlePlayerNote);
        }

        leaderboardPanel.getChildren().add(leaderboardRows);

        // 3. ACTION BUTTONS
        HBox actionsBox = new HBox(16);
        actionsBox.setAlignment(Pos.CENTER);

        Button restartBtn = new Button("🔄 " + I18n.get("winner.btn.play_again"));
        restartBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-padding: 12px 24px; -fx-background-radius: 12px; -fx-cursor: hand;", ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        restartBtn.setOnAction(e -> {
            if (listener != null) listener.onRestartMatch();
        });

        Button homeBtn = new Button(I18n.get("winner.btn.menu"));
        homeBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-padding: 12px 24px; -fx-background-radius: 12px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        homeBtn.setOnAction(e -> {
            if (listener != null) listener.onReturnHome();
        });

        actionsBox.getChildren().addAll(restartBtn, homeBtn);

        contentBox.getChildren().addAll(trophyIcon, mainTitle, subtitle, winnerSpotlightCard, leaderboardPanel, actionsBox);

        StackPane centerWrap = new StackPane(contentBox);
        centerWrap.setAlignment(Pos.TOP_CENTER);
        centerWrap.setPadding(new Insets(16));
        setContent(centerWrap);
    }
}
