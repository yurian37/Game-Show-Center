package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class FlipCoinFxStage extends VBox {

    // Allowed front-facing target rotation angles (excluding 90 deg and 270 deg edge cases)
    private static final int[] ALLOWED_ANGLES = {0, 20, 40, 140, 160, 180, 200, 220, 320, 340};

    private final List<Competitor> profiles;
    private final JsonNode setupData;
    private final Consumer<Competitor> onWinnerSelected;

    private StackPane coinNode;
    private ImageView coinImageView;
    private Label coinLabel;
    private Button spinBtn;
    private VBox winnerBanner;

    private boolean isSpinning = false;
    private double currentAngle = 0;
    private final Random random = new Random();

    private AudioClip flipAudioClip;
    private AudioClip winAudioClip;

    public FlipCoinFxStage(List<Competitor> profiles, JsonNode setupData, Consumer<Competitor> onWinnerSelected) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        this.setupData = setupData;
        this.onWinnerSelected = onWinnerSelected;

        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));

        // Load Audio Assets
        initAudioAssets();

        // 1. COIN 3D SPINNING NODE WITH EXTERNAL IMAGE ASSET
        coinNode = create3DCoinNode();

        // 2. WINNER BANNER (Initially Hidden)
        winnerBanner = new VBox(10);
        winnerBanner.setAlignment(Pos.CENTER);
        winnerBanner.setPadding(new Insets(16, 24, 16, 24));
        winnerBanner.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-background-radius: 20px; -fx-border-radius: 20px;", ThemeManager.getCardHex(), ThemeManager.getAccentHex()));
        winnerBanner.setVisible(false);

        // 3. SPIN BUTTON
        spinBtn = new Button(I18n.get("game.flipcoin.spin_btn"));
        spinBtn.setStyle(String.format("-fx-font-size: 16px; -fx-padding: 14px 36px; -fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-background-radius: 14px; -fx-cursor: hand;", ThemeManager.getAccentHex()));
        spinBtn.setOnAction(e -> startCoinSpin());

        getChildren().addAll(coinNode, winnerBanner, spinBtn);
    }

    private void initAudioAssets() {
        try {
            File flipFile = new File("assets/games/flipcoin/flip.mp3");
            if (!flipFile.exists()) flipFile = new File("template-offline/assets/games/flipcoin/flip.mp3");
            if (flipFile.exists()) {
                flipAudioClip = new AudioClip(flipFile.toURI().toString());
            }

            File winFile = new File("assets/games/flipcoin/win.mp3");
            if (!winFile.exists()) winFile = new File("template-offline/assets/games/flipcoin/win.mp3");
            if (winFile.exists()) {
                winAudioClip = new AudioClip(winFile.toURI().toString());
            }
        } catch (Exception ignored) {}
    }

    private StackPane create3DCoinNode() {
        StackPane coin = new StackPane();
        coin.setPrefSize(200, 200);
        coin.setMaxSize(200, 200);

        Circle outerRing = new Circle(100);
        outerRing.setStyle("-fx-fill: linear-gradient(to right, #fbbf24, #f59e0b, #d97706); -fx-stroke: #fef08a; -fx-stroke-width: 4px; -fx-effect: dropshadow(three-pass-box, rgba(245, 158, 11, 0.6), 24, 0, 0, 4);");

        coinImageView = new ImageView();
        coinImageView.setFitWidth(190);
        coinImageView.setFitHeight(190);
        coinImageView.setPreserveRatio(true);

        File coinFile = new File("assets/games/flipcoin/coin.png");
        if (!coinFile.exists()) coinFile = new File("template-offline/assets/games/flipcoin/coin.png");

        if (coinFile.exists()) {
            try {
                Image coinImg = new Image(coinFile.toURI().toString());
                coinImageView.setImage(coinImg);
            } catch (Exception ignored) {}
        }

        coinLabel = new Label(I18n.get("game.flipcoin.title"));
        coinLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: #fef08a; -fx-effect: dropshadow(one-pass-box, black, 4, 0, 0, 1);");

        VBox contentOverlay = new VBox(coinImageView, coinLabel);
        contentOverlay.setAlignment(Pos.CENTER);

        coin.getChildren().addAll(outerRing, contentOverlay);
        return coin;
    }

    private void startCoinSpin() {
        if (isSpinning || profiles.isEmpty()) return;

        isSpinning = true;
        spinBtn.setDisable(true);
        spinBtn.setText(I18n.get("game.flipcoin.flipping"));
        winnerBanner.setVisible(false);
        coinLabel.setText(I18n.get("game.roulette.spinning"));

        // Play Flip Sound Effect Asset via dedicated SoundManager
        if (flipAudioClip != null) {
            com.gameshowcenter.offline.sound.SoundManager.getInstance().playClip(flipAudioClip);
        }

        // 1. CALCULATE WEIGHTED ROULETTE SELECTION
        Competitor chosenWinner = calculateWeightedWinner();

        // 2. PICK ALLOWED FRONT-FACING TARGET ANGLE [0, 20, 40, 140, 160, 180, 200, 220, 320, 340]
        int chosenAngle = ALLOWED_ANGLES[random.nextInt(ALLOWED_ANGLES.length)];
        double baseRotation = Math.ceil(currentAngle / 360.0) * 360.0;
        double targetAngle = baseRotation + 1800.0 + chosenAngle;

        // 3. TRIGGER ROTATION TRANSITION ANIMATION (3 seconds)
        RotateTransition rt = new RotateTransition(Duration.seconds(3.0), coinNode);
        rt.setAxis(Rotate.Y_AXIS);
        rt.setFromAngle(currentAngle);
        rt.setToAngle(targetAngle);
        rt.setCycleCount(1);
        rt.setAutoReverse(false);

        rt.setOnFinished(e -> {
            isSpinning = false;
            currentAngle = targetAngle;
            spinBtn.setDisable(false);
            spinBtn.setText(I18n.get("game.roulette.spin_again"));
            coinLabel.setText(I18n.get("game.flipcoin.title"));

            // Play Win Fanfare Sound Asset via dedicated SoundManager
            if (winAudioClip != null) {
                com.gameshowcenter.offline.sound.SoundManager.getInstance().playClip(winAudioClip);
            }

            // Display Winner
            displayWinner(chosenWinner);

            if (onWinnerSelected != null && chosenWinner != null) {
                onWinnerSelected.accept(chosenWinner);
            }
        });

        rt.play();
    }

    private Competitor calculateWeightedWinner() {
        if (profiles.isEmpty()) return null;

        JsonNode weightsNode = (setupData != null && setupData.has("weights")) ? setupData.get("weights") : null;

        double totalWeight = 0;
        List<WeightedItem> items = new ArrayList<>();

        for (Competitor p : profiles) {
            double w = 1.0;
            if (weightsNode != null && weightsNode.has(p.getName())) {
                w = weightsNode.get(p.getName()).asDouble(1.0);
            }
            if (w <= 0) w = 1.0;

            totalWeight += w;
            items.add(new WeightedItem(p, w));
        }

        double randomVal = random.nextDouble() * totalWeight;
        double accumulated = 0;

        for (WeightedItem item : items) {
            accumulated += item.weight;
            if (randomVal < accumulated) {
                return item.competitor;
            }
        }

        return profiles.get(0);
    }

    private void displayWinner(Competitor winner) {
        if (winner == null) return;

        winnerBanner.getChildren().clear();

        Label badge = new Label(I18n.get("game.roulette.winner_chosen"));
        badge.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #fbbf24; -fx-letter-spacing: 1px;");

        HBox profileBox = new HBox(12);
        profileBox.setAlignment(Pos.CENTER);

        ImageView avatarView = new ImageView();
        avatarView.setFitWidth(52);
        avatarView.setFitHeight(52);
        avatarView.setPreserveRatio(true);

        if (winner.getAvatarPath() != null) {
            try {
                File file = new File(winner.getAvatarPath());
                if (!file.exists()) file = new File("template-offline/" + winner.getAvatarPath());

                if (file.exists()) {
                    avatarView.setImage(new Image(file.toURI().toString()));
                }
            } catch (Exception ignored) {}
        }

        VBox textInfo = new VBox(2);
        textInfo.setAlignment(Pos.CENTER_LEFT);

        Label nameL = new Label(winner.getName());
        nameL.setStyle("-fx-font-weight: 900; -fx-font-size: 20px; -fx-text-fill: #ffffff;");

        Label subL = new Label(I18n.get("game.flipcoin.luck_side"));
        subL.setStyle("-fx-font-size: 11px; -fx-text-fill: #fef08a;");

        textInfo.getChildren().addAll(nameL, subL);
        profileBox.getChildren().addAll(avatarView, textInfo);

        winnerBanner.getChildren().addAll(badge, profileBox);
        winnerBanner.setVisible(true);
    }

    private static class WeightedItem {
        Competitor competitor;
        double weight;

        WeightedItem(Competitor competitor, double weight) {
            this.competitor = competitor;
            this.weight = weight;
        }
    }
}
