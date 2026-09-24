package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import javafx.scene.layout.*;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class ZeroMarginFxStage extends VBox {

    public static class TurnResult {
        public int round;
        public Competitor player;
        public double target;
        public double stopped;
        public double diff;
        public double absDiff;

        public TurnResult(int round, Competitor player, double target, double stopped) {
            this.round = round;
            this.player = player;
            this.target = target;
            this.stopped = stopped;
            this.diff = stopped - target;
            this.absDiff = Math.abs(this.diff);
        }
    }

    private static final List<Double> DEFAULT_POOL = Arrays.asList(5.0, 10.0, 15.0, 7.5, 20.0);

    private final List<Competitor> profiles;
    private final JsonNode setupData;
    private final Consumer<Competitor> onWinnerSelected;

    private final List<Double> targetTimesPool = new ArrayList<>();
    private final List<TurnResult> playerResults = new ArrayList<>();

    private boolean isBattleRoyale = false;
    private int roundsPerPlayer = 3;
    private int currentRound = 1;
    private int currentPlayerIndex = 0;

    private double roundTargetTime = 5.0;
    private double lastTargetTime = -1.0;

    private String timerState = "idle"; // "idle", "running", "stopped", "match_completed"
    private long startTimeNanos = 0;
    private double elapsedSeconds = 0.0;
    private double stoppedSeconds = 0.0;
    private AnimationTimer timerLoop;

    private Image colonImg;
    private final Image[] digitImgs = new Image[10];

    private Label roundBadgeLabel;
    private Label turnBadgeLabel;
    private CheckBox hiddenCheckBox;
    private Label targetTimeLabel;
    private HBox timerDisplayBox;

    private VBox mainGameCard;
    private VBox resultBox;
    private Label resultLabel;
    private Label resultSubText;

    private Button startStopBtn;
    private Button nextTurnBtn;

    private VBox completedBanner;
    private Label completedTitle;
    private Label completedMsg;
    private Button startNextRoundBtn;

    private final Random random = new Random();

    private final EventHandler<KeyEvent> sceneKeyFilter = event -> {
        if (getParent() == null || getScene() == null || !isVisible()) {
            return;
        }
        if (event.getCode() == KeyCode.ENTER) {
            if ("idle".equals(timerState) || "running".equals(timerState)) {
                event.consume();
                handleStartStop();
            } else if ("stopped".equals(timerState)) {
                event.consume();
                handleNextTurn();
            }
        }
    };

    public ZeroMarginFxStage(List<Competitor> profiles, JsonNode setupData) {
        this(profiles, setupData, null);
    }

    public ZeroMarginFxStage(List<Competitor> profiles, JsonNode setupData, Consumer<Competitor> onWinnerSelected) {
        this.profiles = profiles != null && !profiles.isEmpty() ? profiles :
            Arrays.asList(new Competitor("p1", "Player 1", null), new Competitor("p2", "Player 2", null));
        this.setupData = setupData;
        this.onWinnerSelected = onWinnerSelected;

        setSpacing(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(16));

        // Load PNG Digit Assets
        loadAssets();

        // Parse Setup Data
        parseSetup();

        // 1. BADGES & HIDDEN CHECKBOX
        VBox badgesBox = new VBox(6);
        badgesBox.setAlignment(Pos.CENTER);

        roundBadgeLabel = new Label();
        roundBadgeLabel.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 4px 14px; -fx-background-radius: 12px;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary), ThemeManager.getButtonHex()));

        turnBadgeLabel = new Label();
        turnBadgeLabel.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #ffffff; -fx-background-color: %s; -fx-padding: 4px 14px; -fx-background-radius: 12px;", ThemeManager.getAccentHex()));

        hiddenCheckBox = new CheckBox("🙈 Hidden");
        hiddenCheckBox.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-cursor: hand;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));

        HBox turnRow = new HBox(12, turnBadgeLabel, hiddenCheckBox);
        turnRow.setAlignment(Pos.CENTER);

        badgesBox.getChildren().addAll(roundBadgeLabel, turnRow);

        // 2. MAIN GAME CARD
        mainGameCard = new VBox(18);
        mainGameCard.setAlignment(Pos.CENTER);
        mainGameCard.setPadding(new Insets(24));
        mainGameCard.setMaxWidth(580);
        mainGameCard.setStyle(String.format("-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-width: 2px; -fx-background-radius: 24px; -fx-border-radius: 24px;", ThemeManager.getCardHex()));

        Label tHeaderLabel = new Label(I18n.get("game.zeromargin.target_time"));
        tHeaderLabel.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-letter-spacing: 1px;", ThemeManager.getAccentHex()));

        targetTimeLabel = new Label("🎯 5.00s");
        targetTimeLabel.setStyle(String.format("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 6px 20px; -fx-background-radius: 16px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary), ThemeManager.getButtonHex()));

        VBox targetBox = new VBox(4, tHeaderLabel, targetTimeLabel);
        targetBox.setAlignment(Pos.CENTER);

        Label timerHeaderLabel = new Label(I18n.get("game.zeromargin.stopwatch"));
        timerHeaderLabel.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: %s;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));

        timerDisplayBox = new HBox(4);
        timerDisplayBox.setAlignment(Pos.CENTER);
        timerDisplayBox.setStyle(String.format("-fx-background-color: %s; -fx-padding: 12px 24px; -fx-background-radius: 18px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 18px;", ThemeManager.getMainBoxHex()));

        VBox timerContainer = new VBox(6, timerHeaderLabel, timerDisplayBox);
        timerContainer.setAlignment(Pos.CENTER);

        // Result Sub-Box (Stopped time & error diff)
        resultBox = new VBox(6);
        resultBox.setAlignment(Pos.CENTER);
        resultBox.setPrefWidth(440);
        resultBox.setVisible(false);
        resultBox.setManaged(false);

        Label rHeaderLabel = new Label(I18n.get("game.zeromargin.result_diff"));
        rHeaderLabel.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-letter-spacing: 1px;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));

        resultLabel = new Label();
        resultSubText = new Label();
        resultSubText.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));

        resultBox.getChildren().addAll(rHeaderLabel, resultLabel, resultSubText);

        // Action Buttons
        startStopBtn = new Button(I18n.get("game.zeromargin.start_timer"));
        startStopBtn.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 32px; -fx-background-color: %s; -fx-text-fill: #ffffff; -fx-background-radius: 12px; -fx-cursor: hand;", ThemeManager.getAccentHex()));
        startStopBtn.setOnAction(e -> handleStartStop());

        nextTurnBtn = new Button(I18n.get("game.common.next_turn") + " (↵ Enter)");
        nextTurnBtn.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 32px; -fx-background-color: %s; -fx-text-fill: #ffffff; -fx-background-radius: 12px; -fx-cursor: hand;", ThemeManager.getAccentHex()));
        nextTurnBtn.setVisible(false);
        nextTurnBtn.setManaged(false);
        nextTurnBtn.setOnAction(e -> handleNextTurn());

        HBox actionsRow = new HBox(12, startStopBtn, nextTurnBtn);
        actionsRow.setAlignment(Pos.CENTER);

        mainGameCard.getChildren().addAll(targetBox, timerContainer, resultBox, actionsRow);

        // 3. MATCH COMPLETED BANNER
        completedBanner = new VBox(12);
        completedBanner.setAlignment(Pos.CENTER);
        completedBanner.setPadding(new Insets(24));
        completedBanner.setMaxWidth(540);
        completedBanner.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-background-radius: 24px; -fx-border-radius: 24px;", ThemeManager.getCardHex(), ThemeManager.getAccentHex()));
        completedBanner.setVisible(false);
        completedBanner.setManaged(false);

        Label cIcon = new Label("🏁");
        cIcon.setStyle("-fx-font-size: 36px;");

        completedTitle = new Label(I18n.get("game.zeromargin.completed"));
        completedTitle.setStyle(String.format("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: %s;", ThemeManager.getAccentHex()));

        completedMsg = new Label("");
        completedMsg.setStyle(String.format("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-wrap-text: true; -fx-text-alignment: center;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));

        startNextRoundBtn = new Button(I18n.get("game.common.restart_round"));
        startNextRoundBtn.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 10px 24px; -fx-background-color: %s; -fx-text-fill: #ffffff; -fx-background-radius: 12px; -fx-cursor: hand;", ThemeManager.getAccentHex()));
        startNextRoundBtn.setOnAction(e -> startMatch());

        completedBanner.getChildren().addAll(cIcon, completedTitle, completedMsg, startNextRoundBtn);

        getChildren().addAll(badgesBox, mainGameCard, completedBanner);

        // Keyboard listener attachment
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, sceneKeyFilter);
            }
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, sceneKeyFilter);
            }
        });

        // Life-cycle cleanup: when stage node is removed from gameAreaContainer
        parentProperty().addListener((obs, oldP, newP) -> {
            if (newP == null) {
                Scene sc = getScene();
                if (sc != null) {
                    sc.removeEventFilter(KeyEvent.KEY_PRESSED, sceneKeyFilter);
                }
                if (timerLoop != null) {
                    timerLoop.stop();
                }
            }
        });

        // Animation Timer Loop setup
        initTimerLoop();

        // Start Match
        startMatch();
    }

    private void loadAssets() {
        try {
            File cFile = new File("assets/games/zeromargin/colon.png");
            if (!cFile.exists()) cFile = new File("template-offline/assets/games/zeromargin/colon.png");
            if (cFile.exists()) colonImg = new Image(cFile.toURI().toString());

            for (int i = 0; i <= 9; i++) {
                File dFile = new File("assets/games/zeromargin/digits/" + i + ".png");
                if (!dFile.exists()) dFile = new File("template-offline/assets/games/zeromargin/digits/" + i + ".png");
                if (dFile.exists()) digitImgs[i] = new Image(dFile.toURI().toString());
            }
        } catch (Exception ignored) {}
    }

    private void parseSetup() {
        if (setupData != null && setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean()) {
            this.isBattleRoyale = true;
        }

        int rpp = 3;
        if (setupData != null) {
            if (setupData.has("roundsPerPlayer")) rpp = setupData.get("roundsPerPlayer").asInt(3);
            else if (setupData.has("rounds_per_player")) rpp = setupData.get("rounds_per_player").asInt(3);
        }
        this.roundsPerPlayer = rpp;

        JsonNode poolNode = (setupData != null && setupData.has("targetTimesPool")) ? setupData.get("targetTimesPool") :
            ((setupData != null && setupData.has("target_times_pool")) ? setupData.get("target_times_pool") : null);

        if (poolNode != null && poolNode.isArray() && poolNode.size() > 0) {
            for (JsonNode t : poolNode) {
                double val = t.asDouble(0.0);
                if (val > 0) targetTimesPool.add(val);
            }
        }

        if (targetTimesPool.isEmpty()) {
            targetTimesPool.addAll(DEFAULT_POOL);
        }

        if (isBattleRoyale) {
            this.roundsPerPlayer = Math.max(1, targetTimesPool.size());
        }
    }

    private double pickTargetTime(double lastTime) {
        if (targetTimesPool.isEmpty()) return 5.0;
        double chosen = targetTimesPool.get(random.nextInt(targetTimesPool.size()));
        for (int i = 0; i < 3; i++) {
            if (Math.abs(chosen - lastTime) > 0.001) break;
            chosen = targetTimesPool.get(random.nextInt(targetTimesPool.size()));
        }
        return chosen;
    }

    private void initTimerLoop() {
        timerLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if ("running".equals(timerState)) {
                    double elapsed = (now - startTimeNanos) / 1_000_000_000.0;
                    if (elapsed >= 900.0) { // Max 15 minutes limit
                        elapsedSeconds = 900.0;
                        stopTimer(900.0);
                        return;
                    }
                    elapsedSeconds = elapsed;
                    renderDigitalDisplay(elapsedSeconds);
                }
            }
        };
    }

    private void startMatch() {
        currentRound = 1;
        currentPlayerIndex = 0;
        lastTargetTime = -1.0;
        roundTargetTime = isBattleRoyale ? targetTimesPool.get(0) : pickTargetTime(-1.0);
        playerResults.clear();

        completedBanner.setVisible(false);
        completedBanner.setManaged(false);
        mainGameCard.setVisible(true);
        mainGameCard.setManaged(true);

        resetTurnState();
    }

    private void resetTurnState() {
        timerState = "idle";
        elapsedSeconds = 0.0;
        stoppedSeconds = 0.0;

        roundBadgeLabel.setText(isBattleRoyale
                ? String.format("⚔️ BR • Ronda %d de %d (Objetivo #%d)", currentRound, roundsPerPlayer, currentRound)
                : String.format("⏱️ Zero Margin • Round %d of %d", currentRound, roundsPerPlayer));

        Competitor activeComp = profiles.get(currentPlayerIndex);
        turnBadgeLabel.setText(String.format("Active Turn: %s (%d of %d)", activeComp.getName(), currentPlayerIndex + 1, profiles.size()));

        targetTimeLabel.setText(String.format("🎯 %.2fs", roundTargetTime));

        renderDigitalDisplay(0.0);

        resultBox.setVisible(false);
        resultBox.setManaged(false);

        startStopBtn.setText(I18n.get("game.zeromargin.start_timer"));
        startStopBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 32px; -fx-background-color: linear-gradient(to right, #10b981, #059669); -fx-text-fill: #ffffff;");
        startStopBtn.setVisible(true);
        startStopBtn.setManaged(true);

        nextTurnBtn.setVisible(false);
        nextTurnBtn.setManaged(false);
    }

    private void handleStartStop() {
        if ("idle".equals(timerState)) {
            timerState = "running";
            startTimeNanos = System.nanoTime();
            startStopBtn.setText(I18n.get("game.zeromargin.stop_timer"));
            startStopBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 32px; -fx-background-color: linear-gradient(to right, #f43f5e, #e11d48); -fx-text-fill: #ffffff;");
            timerLoop.start();
        } else if ("running".equals(timerState)) {
            stopTimer(elapsedSeconds);
        }
    }

    private void stopTimer(double finalElapsed) {
        timerLoop.stop();
        timerState = "stopped";
        stoppedSeconds = finalElapsed;

        renderDigitalDisplay(stoppedSeconds);

        double diff = stoppedSeconds - roundTargetTime;
        double absDiff = Math.abs(diff);
        boolean isTooSlow = diff > 0;

        String styleClass = absDiff < 0.2 ?
            "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-border-color: #10b981; -fx-text-fill: #34d399;" :
            (absDiff < 0.6 ?
                "-fx-background-color: rgba(245, 158, 11, 0.2); -fx-border-color: #f59e0b; -fx-text-fill: #fcd34d;" :
                "-fx-background-color: rgba(244, 63, 94, 0.2); -fx-border-color: #f43f5e; -fx-text-fill: #fda4af;");

        String statusMsg = absDiff < 0.2 ? "🎯 AMAZING PRECISION!" : (isTooSlow ? "⌛ TOO SLOW!" : "⚡ TOO FAST!");
        String signStr = isTooSlow ? "+" : "";

        resultLabel.setText(String.format("%s • Missed by %s%.2fs", statusMsg, signStr, diff));
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-padding: 8px 16px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-width: 1px; " + styleClass);

        resultSubText.setText(String.format("Stopped at %.2fs (Target: %.2fs)", stoppedSeconds, roundTargetTime));

        resultBox.setVisible(true);
        resultBox.setManaged(true);

        startStopBtn.setVisible(false);
        startStopBtn.setManaged(false);

        nextTurnBtn.setVisible(true);
        nextTurnBtn.setManaged(true);
    }

    private void handleNextTurn() {
        Competitor activeComp = profiles.get(currentPlayerIndex);
        playerResults.add(new TurnResult(currentRound, activeComp, roundTargetTime, stoppedSeconds));

        // Check if more competitors in current round
        if (currentPlayerIndex < profiles.size() - 1) {
            currentPlayerIndex++;
            resetTurnState();
        } else {
            // Round completed for all competitors
            if (currentRound < roundsPerPlayer) {
                currentRound++;
                currentPlayerIndex = 0;
                lastTargetTime = roundTargetTime;
                roundTargetTime = isBattleRoyale 
                        ? targetTimesPool.get(currentRound - 1) 
                        : pickTargetTime(lastTargetTime);
                resetTurnState();
            } else {
                // MATCH COMPLETED
                mainGameCard.setVisible(false);
                mainGameCard.setManaged(false);

                completedMsg.setText(isBattleRoyale
                        ? String.format("⚔️ ¡BATTLE ROYALE COMPLETADO! Todos los tiempos objetivo (%d) fueron presentados en la arena.", roundsPerPlayer)
                        : String.format("All %d rounds for all %d competitor(s) have been completed.", roundsPerPlayer, profiles.size()));

                completedBanner.setVisible(true);
                completedBanner.setManaged(true);

                // Determine winner (lowest average absolute error diff)
                Map<String, List<Double>> stats = new HashMap<>();
                for (TurnResult res : playerResults) {
                    stats.computeIfAbsent(res.player.getId(), k -> new ArrayList<>()).add(res.absDiff);
                }

                Competitor bestComp = profiles.get(0);
                double lowestAvg = Double.MAX_VALUE;

                for (Competitor c : profiles) {
                    List<Double> diffs = stats.get(c.getId());
                    if (diffs != null && !diffs.isEmpty()) {
                        double avg = diffs.stream().mapToDouble(Double::doubleValue).average().orElse(Double.MAX_VALUE);
                        if (avg < lowestAvg) {
                            lowestAvg = avg;
                            bestComp = c;
                        }
                    }
                }

                if (onWinnerSelected != null) {
                    onWinnerSelected.accept(bestComp);
                }
            }
        }
    }

    private void renderDigitalDisplay(double timeInSec) {
        timerDisplayBox.getChildren().clear();

        if (hiddenCheckBox != null && hiddenCheckBox.isSelected() && "running".equals(timerState)) {
            Label hiddenLabel = new Label(I18n.get("game.zeromargin.hidden_timer"));
            hiddenLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #fcd34d; -fx-padding: 8px 16px;");
            timerDisplayBox.getChildren().add(hiddenLabel);
            return;
        }

        int sec = (int) Math.floor(timeInSec);
        int centis = (int) Math.floor((timeInSec - sec) * 100);

        String secStr = sec < 10 ? "0" + sec : String.valueOf(sec);
        String csStr = centis < 10 ? "0" + centis : String.valueOf(centis);

        // Seconds digits
        for (char ch : secStr.toCharArray()) {
            int d = Character.getNumericValue(ch);
            if (d >= 0 && d <= 9 && digitImgs[d] != null) {
                ImageView iv = new ImageView(digitImgs[d]);
                iv.setFitHeight(52);
                iv.setPreserveRatio(true);
                timerDisplayBox.getChildren().add(iv);
            } else {
                Label l = new Label(String.valueOf(ch));
                l.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #a5b4fc;");
                timerDisplayBox.getChildren().add(l);
            }
        }

        // Colon
        if (colonImg != null) {
            ImageView iv = new ImageView(colonImg);
            iv.setFitHeight(44);
            iv.setPreserveRatio(true);
            timerDisplayBox.getChildren().add(iv);
        } else {
            Label l = new Label(":");
            l.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #a5b4fc;");
            timerDisplayBox.getChildren().add(l);
        }

        // Centiseconds digits
        for (char ch : csStr.toCharArray()) {
            int d = Character.getNumericValue(ch);
            if (d >= 0 && d <= 9 && digitImgs[d] != null) {
                ImageView iv = new ImageView(digitImgs[d]);
                iv.setFitHeight(52);
                iv.setPreserveRatio(true);
                timerDisplayBox.getChildren().add(iv);
            } else {
                Label l = new Label(String.valueOf(ch));
                l.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #f43f5e;");
                timerDisplayBox.getChildren().add(l);
            }
        }
    }
}
