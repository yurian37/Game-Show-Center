package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class RouletteFxStage extends VBox {

    private static final String[] SLICE_COLORS = {
        "#6366f1", "#ec4899", "#f59e0b", "#10b981", "#8b5cf6", "#06b6d4", "#f97316", "#14b8a6", "#e11d48", "#84cc16"
    };

    private static final String[] SLICE_BORDER_COLORS = {
        "#818cf8", "#f472b6", "#fbbf24", "#34d399", "#a78bfa", "#22d3ee", "#fb923c", "#2dd4bf", "#fb7185", "#a3e635"
    };

    private final List<Competitor> allProfiles;
    private final List<Competitor> activeProfiles = new ArrayList<>();
    private final JsonNode setupData;
    private final Consumer<Competitor> onWinnerSelected;

    private StackPane wheelContainer;
    private Group wheelGroup;
    private final List<Arc> sliceArcs = new ArrayList<>();
    private final List<SliceInfo> sliceInfoList = new ArrayList<>();

    private FlowPane chipsPane;
    private Label chipsCounterLabel;
    private Button spinBtn;
    private VBox winnerBanner;
    private Label statusLabel;

    private boolean isSpinning = false;
    private double currentRotation = 0;
    private final Random random = new Random();

    private AudioClip spinAudioClip;
    private AudioClip winAudioClip;

    public RouletteFxStage(List<Competitor> profiles, JsonNode setupData, Consumer<Competitor> onWinnerSelected) {
        this.allProfiles = profiles != null ? new ArrayList<>(profiles) : new ArrayList<>();
        this.activeProfiles.addAll(this.allProfiles);
        this.setupData = setupData;
        this.onWinnerSelected = onWinnerSelected;

        setSpacing(14);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(12));

        // 1. Initialize Audio Assets
        initAudioAssets();

        // 2. Status / Info Header
        statusLabel = new Label("🎰 " + I18n.get("game.roulette.title"));
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #a5b4fc; -fx-letter-spacing: 1px;");

        // 3. Contestant Chips / Removal Panel
        VBox chipsBox = createContestantChipsPanel();

        // 4. Build Vector Roulette Wheel
        wheelContainer = createRouletteWheelNode();

        // 5. Winner Banner (Initially Hidden)
        winnerBanner = new VBox(6);
        winnerBanner.setAlignment(Pos.CENTER);
        winnerBanner.setPadding(new Insets(10, 20, 10, 20));
        winnerBanner.setStyle(String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-background-radius: 16px; -fx-border-radius: 16px;",
            ThemeManager.getCardHex(), ThemeManager.getAccentHex()
        ));
        winnerBanner.setVisible(false);

        // 6. Spin Button
        spinBtn = new Button(I18n.get("game.roulette.spin_btn"));
        spinBtn.setStyle(String.format(
            "-fx-font-size: 14px; -fx-padding: 10px 28px; -fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-background-radius: 12px; -fx-cursor: hand;",
            ThemeManager.getAccentHex()
        ));
        spinBtn.setOnAction(e -> startRouletteSpin());

        getChildren().addAll(statusLabel, chipsBox, wheelContainer, winnerBanner, spinBtn);
    }

    private void initAudioAssets() {
        try {
            File spinFile = new File("assets/games/roulette/flip.mp3");
            if (!spinFile.exists()) spinFile = new File("template-offline/assets/games/roulette/flip.mp3");
            if (!spinFile.exists()) spinFile = new File("template-offline/assets/games/flipcoin/flip.mp3");
            if (spinFile.exists()) {
                spinAudioClip = new AudioClip(spinFile.toURI().toString());
            }

            File winFile = new File("assets/games/roulette/win.mp3");
            if (!winFile.exists()) winFile = new File("template-offline/assets/games/roulette/win.mp3");
            if (!winFile.exists()) winFile = new File("template-offline/assets/games/flipcoin/win.mp3");
            if (winFile.exists()) {
                winAudioClip = new AudioClip(winFile.toURI().toString());
            }
        } catch (Exception ignored) {}
    }

    private VBox createContestantChipsPanel() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(460);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 12px; -fx-background-radius: 12px;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER);

        chipsCounterLabel = new Label();
        chipsCounterLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        Label minNotice = new Label("• Mínimo 2 para girar");
        minNotice.setStyle("-fx-font-size: 10px; -fx-text-fill: #f59e0b; -fx-font-weight: bold;");

        header.getChildren().addAll(chipsCounterLabel, minNotice);

        chipsPane = new FlowPane(6, 6);
        chipsPane.setAlignment(Pos.CENTER);
        chipsPane.setPrefWrapLength(440);

        refreshChips();

        box.getChildren().addAll(header, chipsPane);
        return box;
    }

    private void refreshChips() {
        chipsPane.getChildren().clear();
        chipsCounterLabel.setText(String.format("Participantes (%d / %d activos)", activeProfiles.size(), allProfiles.size()));

        for (Competitor c : allProfiles) {
            boolean isActive = activeProfiles.contains(c);
            boolean canRemove = activeProfiles.size() > 2;

            HBox chip = new HBox(6);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setPadding(new Insets(3, 8, 3, 8));
            chip.setStyle(isActive
                ? "-fx-background-color: #1e293b; -fx-border-color: #3b82f6; -fx-border-radius: 8px; -fx-background-radius: 8px;"
                : "-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-opacity: 0.6;");

            Label nameL = new Label(c.getName());
            nameL.setStyle(isActive
                ? "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;"
                : "-fx-font-size: 11px; -fx-font-weight: normal; -fx-text-fill: #64748b;");

            Button actionBtn = new Button(isActive ? "✕" : "➕");
            actionBtn.setStyle(isActive
                ? "-fx-background-color: transparent; -fx-text-fill: #f43f5e; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 0 2 0 2; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: #10b981; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 0 2 0 2; -fx-cursor: hand;");

            if (isActive && !canRemove) {
                actionBtn.setDisable(true);
                actionBtn.setTooltip(new Tooltip("Se requieren al menos 2 concursantes para girar la ruleta."));
            }

            actionBtn.setOnAction(e -> {
                if (isSpinning) return;
                if (isActive) {
                    if (activeProfiles.size() > 2) {
                        activeProfiles.remove(c);
                        winnerBanner.setVisible(false);
                        refreshChips();
                        rebuildWheel();
                    }
                } else {
                    activeProfiles.add(c);
                    winnerBanner.setVisible(false);
                    refreshChips();
                    rebuildWheel();
                }
            });

            chip.getChildren().addAll(nameL, actionBtn);
            chipsPane.getChildren().add(chip);
        }
    }

    private void rebuildWheel() {
        wheelGroup.getChildren().clear();
        sliceArcs.clear();
        sliceInfoList.clear();
        currentRotation = 0;
        wheelGroup.setRotate(0);
        calculateAndBuildSlices(130);
    }

    private StackPane createRouletteWheelNode() {
        StackPane root = new StackPane();
        root.setPrefSize(280, 280);
        root.setMaxSize(280, 280);

        // Outer Glow Ring
        Circle outerGlow = new Circle(138);
        outerGlow.setFill(Color.TRANSPARENT);
        outerGlow.setStroke(Color.web("#fbbf24", 0.4));
        outerGlow.setStrokeWidth(3);
        outerGlow.setEffect(new DropShadow(18, Color.web("#f59e0b", 0.5)));

        // Outer Metallic Rim
        Circle outerRim = new Circle(134);
        RadialGradient rimGradient = new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#1e293b")),
            new Stop(0.85, Color.web("#0f172a")),
            new Stop(0.96, Color.web("#f59e0b")),
            new Stop(1.0, Color.web("#d97706"))
        );
        outerRim.setFill(rimGradient);
        outerRim.setStroke(Color.web("#fef08a"));
        outerRim.setStrokeWidth(2.5);

        // Rotating Wheel Group
        wheelGroup = new Group();
        sliceArcs.clear();
        sliceInfoList.clear();

        calculateAndBuildSlices(130);

        // Center Hub
        Circle centerHubOuter = new Circle(34);
        centerHubOuter.setFill(Color.web("#0f172a"));
        centerHubOuter.setStroke(Color.web("#fbbf24"));
        centerHubOuter.setStrokeWidth(3);
        centerHubOuter.setEffect(new DropShadow(8, Color.BLACK));

        Circle centerHubInner = new Circle(24);
        RadialGradient hubGrad = new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#fbbf24")),
            new Stop(0.7, Color.web("#d97706")),
            new Stop(1.0, Color.web("#78350f"))
        );
        centerHubInner.setFill(hubGrad);

        Label centerIcon = new Label("🎯");
        centerIcon.setStyle("-fx-font-size: 18px;");

        StackPane centerStack = new StackPane(centerHubOuter, centerHubInner, centerIcon);

        // Top pointer indicator
        Label pointer = new Label("▼");
        pointer.setStyle("-fx-font-size: 24px; -fx-text-fill: #f59e0b; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 6, 0, 0, 2);");
        StackPane.setAlignment(pointer, Pos.TOP_CENTER);
        pointer.setTranslateY(-14);

        root.getChildren().addAll(outerGlow, outerRim, wheelGroup, centerStack, pointer);
        return root;
    }

    private void calculateAndBuildSlices(double radius) {
        if (activeProfiles.isEmpty()) return;

        double totalWeight = activeProfiles.size();
        double currentAngle = 0;

        for (int i = 0; i < activeProfiles.size(); i++) {
            Competitor comp = activeProfiles.get(i);
            double sliceAngle = (1.0 / totalWeight) * 360.0;

            String fillColorHex = SLICE_COLORS[i % SLICE_COLORS.length];
            String strokeColorHex = SLICE_BORDER_COLORS[i % SLICE_BORDER_COLORS.length];

            Arc sliceArc = new Arc();
            sliceArc.setCenterX(0);
            sliceArc.setCenterY(0);
            sliceArc.setRadiusX(radius);
            sliceArc.setRadiusY(radius);
            sliceArc.setStartAngle(currentAngle);
            sliceArc.setLength(sliceAngle);
            sliceArc.setType(ArcType.ROUND);
            sliceArc.setFill(Color.web(fillColorHex));
            sliceArc.setStroke(Color.web(strokeColorHex));
            sliceArc.setStrokeWidth(1.8);
            sliceArc.setStrokeType(StrokeType.INSIDE);

            sliceArcs.add(sliceArc);
            wheelGroup.getChildren().add(sliceArc);

            // Text Label
            double midAngleDeg = currentAngle + (sliceAngle / 2.0);
            double midAngleRad = Math.toRadians(midAngleDeg);
            double labelRadius = radius * 0.65;

            double labelX = labelRadius * Math.cos(midAngleRad);
            double labelY = -labelRadius * Math.sin(midAngleRad);

            String displayName = comp.getName();
            if (displayName.length() > 9) {
                displayName = displayName.substring(0, 8) + "…";
            }

            Text sliceText = new Text(displayName);
            sliceText.setFont(Font.font("Segoe UI", FontWeight.BLACK, 11));
            sliceText.setFill(Color.WHITE);
            sliceText.setEffect(new DropShadow(4, Color.BLACK));

            sliceText.setX(labelX - (sliceText.getLayoutBounds().getWidth() / 2.0));
            sliceText.setY(labelY + (sliceText.getLayoutBounds().getHeight() / 4.0));

            double textRotation = -midAngleDeg;
            if (midAngleDeg > 90 && midAngleDeg < 270) {
                textRotation += 180;
            }
            sliceText.getTransforms().add(new Rotate(textRotation, labelX, labelY));

            wheelGroup.getChildren().add(sliceText);

            sliceInfoList.add(new SliceInfo(comp, 1.0, currentAngle, sliceAngle, midAngleDeg));
            currentAngle += sliceAngle;
        }
    }

    private void startRouletteSpin() {
        if (isSpinning || activeProfiles.size() < 2) return;
        isSpinning = true;
        spinBtn.setDisable(true);
        winnerBanner.setVisible(false);

        if (spinAudioClip != null) {
            com.gameshowcenter.offline.sound.SoundManager.getInstance().playClip(spinAudioClip);
        }

        SliceInfo winningSlice = calculateWeightedWinner();
        Competitor chosenWinner = winningSlice != null ? winningSlice.competitor : (activeProfiles.isEmpty() ? null : activeProfiles.get(0));

        double winningMidAngle = winningSlice != null ? winningSlice.midAngleDeg : 90.0;
        double targetLandingBase = (90.0 - winningMidAngle);
        while (targetLandingBase < 0) targetLandingBase += 360.0;

        double sliceHalfSpan = winningSlice != null ? (winningSlice.spanAngle * 0.35) : 10.0;
        double randomOffset = (random.nextDouble() * 2.0 - 1.0) * sliceHalfSpan;

        double baseFullSpins = Math.ceil(currentRotation / 360.0) * 360.0 + 2160.0;
        double targetAngle = baseFullSpins + targetLandingBase + randomOffset;

        RotateTransition rt = new RotateTransition(Duration.seconds(3.5), wheelGroup);
        rt.setAxis(Rotate.Z_AXIS);
        rt.setFromAngle(-currentRotation);
        rt.setToAngle(-targetAngle);
        rt.setInterpolator(Interpolator.SPLINE(0.15, 0.9, 0.2, 1.0));
        rt.setCycleCount(1);
        rt.setAutoReverse(false);

        rt.setOnFinished(e -> {
            isSpinning = false;
            currentRotation = targetAngle;
            spinBtn.setDisable(false);
            spinBtn.setText(I18n.get("game.roulette.spin_again"));

            if (winAudioClip != null) {
                com.gameshowcenter.offline.sound.SoundManager.getInstance().playClip(winAudioClip);
            }

            displayWinner(chosenWinner);

            if (onWinnerSelected != null && chosenWinner != null) {
                onWinnerSelected.accept(chosenWinner);
            }
        });

        rt.play();
    }

    private SliceInfo calculateWeightedWinner() {
        if (sliceInfoList.isEmpty()) return null;
        int idx = random.nextInt(sliceInfoList.size());
        return sliceInfoList.get(idx);
    }

    private void displayWinner(Competitor winner) {
        if (winner == null) return;

        winnerBanner.getChildren().clear();

        Label badge = new Label(I18n.get("game.roulette.winner_chosen"));
        badge.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #fbbf24; -fx-letter-spacing: 1px;");

        HBox profileBox = new HBox(8);
        profileBox.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(winner.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #ffffff;");

        profileBox.getChildren().add(nameLabel);
        winnerBanner.getChildren().addAll(badge, profileBox);
        winnerBanner.setVisible(true);
    }

    public static class SliceInfo {
        public Competitor competitor;
        public double weight;
        public double startAngle;
        public double spanAngle;
        public double midAngleDeg;

        public SliceInfo(Competitor competitor, double weight, double startAngle, double spanAngle, double midAngleDeg) {
            this.competitor = competitor;
            this.weight = weight;
            this.startAngle = startAngle;
            this.spanAngle = spanAngle;
            this.midAngleDeg = midAngleDeg;
        }
    }
}
