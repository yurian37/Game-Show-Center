package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.util.ImageLoaderHelper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.util.*;

public class GuessCharacterFxStage extends VBox {

    private static final List<String> DEFAULT_CHARACTERS = Arrays.asList(
            "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1563089145-599997674d42?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1608889175123-8ee362201f81?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1569003339405-ea396a5a8a90?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1563245372-f21724e3856d?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1589254065878-42c9da997008?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1535223289827-42f1e9919769?auto=format&fit=crop&w=1000&q=80"
    );

    private final List<Competitor> profiles;
    private final JsonNode setupData;

    private final List<String> initialPool = new ArrayList<>();
    private final List<String> workingPool = new ArrayList<>();
    private String currentImage = "";
    private boolean isRoundActive = false;
    private int roundNumber = 1;
    private boolean isMatchFinished = false;

    private final int roundsPerPlayer;
    private final int numPlayers;
    private final int totalMatchRounds;
    private final boolean enableTimer;
    private final int timerInitialDuration;
    private final Random random = new Random();

    // Timer State
    private int timeRemaining = 30;
    private Timeline countdownTimeline;

    // Digital Assets (0-9 and colon)
    private final Image[] digitImages = new Image[10];
    private Image colonImage;

    // UI Nodes
    private Label roundBadgeLabel;
    private Label poolInfoLabel;

    private StackPane dynamicContainer;
    private VBox waitingBox;
    private VBox activePlayBox;
    private VBox matchFinishedBox;

    // Active Play UI Nodes
    private StackPane imageViewport;
    private ImageView imageView;
    private ProgressIndicator loader;
    private Label imageCounterBadge;
    private HBox digitalTimerBox;
    private Button nextImageBtn;

    public GuessCharacterFxStage(List<Competitor> profiles, JsonNode setupData) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        this.setupData = setupData;

        int rpp = 3;
        boolean timerOn = true;
        int timerSecs = 30;

        if (setupData != null) {
            if (setupData.has("rounds_per_player")) rpp = setupData.get("rounds_per_player").asInt(3);
            else if (setupData.has("roundsPerPlayer")) rpp = setupData.get("roundsPerPlayer").asInt(3);

            if (setupData.has("enable_timer")) timerOn = setupData.get("enable_timer").asBoolean(true);
            else if (setupData.has("enableTimer")) timerOn = setupData.get("enableTimer").asBoolean(true);

            if (setupData.has("timer_seconds")) timerSecs = setupData.get("timer_seconds").asInt(30);
            else if (setupData.has("timerSeconds")) timerSecs = setupData.get("timerSeconds").asInt(30);
        }

        this.roundsPerPlayer = rpp;
        this.enableTimer = timerOn;
        this.timerInitialDuration = timerSecs;
        this.numPlayers = (!this.profiles.isEmpty()) ? this.profiles.size() : 1;
        this.totalMatchRounds = this.numPlayers * this.roundsPerPlayer;
        this.timeRemaining = this.timerInitialDuration;

        setSpacing(14);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(12));

        loadDigitAssets();
        parseCharacterPool();

        // 1. TOP STATUS BADGES
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER);

        roundBadgeLabel = new Label(String.format(I18n.get("game.guesscharacter.title_image"), 1, totalMatchRounds));
        roundBadgeLabel.setStyle(String.format(
                "-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #f59e0b; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 20px; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 20px;",
                ThemeManager.getAccentHex()));

        poolInfoLabel = new Label(String.format(I18n.get("game.guesscharacter.unseen_pool"), workingPool.size()));
        poolInfoLabel.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6px 12px; -fx-background-radius: 20px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 20px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));

        topBar.getChildren().addAll(roundBadgeLabel, poolInfoLabel);
        getChildren().add(topBar);

        // 2. MAIN DYNAMIC CONTAINER
        dynamicContainer = new StackPane();
        dynamicContainer.setAlignment(Pos.CENTER);
        getChildren().add(dynamicContainer);

        // Build View States
        buildWaitingBox();
        buildActivePlayBox();
        buildMatchFinishedBox();

        // Pick first character
        pickFirstCharacter();
        updateUI();
    }

    private void loadDigitAssets() {
        try {
            File colonFile = new File("assets/games/guessthecharacter/colon.png");
            if (!colonFile.exists()) colonFile = new File("template-offline/assets/games/guessthecharacter/colon.png");
            if (!colonFile.exists()) colonFile = new File("template-offline/assets/games/zeromargin/colon.png");
            if (colonFile.exists()) {
                colonImage = new Image(colonFile.toURI().toString());
            }

            for (int i = 0; i <= 9; i++) {
                File digitFile = new File("assets/games/guessthecharacter/digits/" + i + ".png");
                if (!digitFile.exists()) digitFile = new File("template-offline/assets/games/guessthecharacter/digits/" + i + ".png");
                if (!digitFile.exists()) digitFile = new File("template-offline/assets/games/zeromargin/digits/" + i + ".png");
                if (digitFile.exists()) {
                    digitImages[i] = new Image(digitFile.toURI().toString());
                }
            }
        } catch (Exception ex) {
            System.err.println("Could not load digital clock assets: " + ex.getMessage());
        }
    }

    private void parseCharacterPool() {
        initialPool.clear();
        if (setupData != null) {
            JsonNode poolNode = setupData.has("media_pool") ? setupData.get("media_pool")
                    : (setupData.has("mediaPool") ? setupData.get("mediaPool") : null);
            if (poolNode != null && poolNode.isArray()) {
                for (JsonNode item : poolNode) {
                    String s = item.asText();
                    if (s != null && !s.isBlank()) initialPool.add(s);
                }
            }
        }
        if (initialPool.isEmpty()) {
            initialPool.addAll(DEFAULT_CHARACTERS);
        }
        workingPool.clear();
        workingPool.addAll(initialPool);
    }

    private void pickFirstCharacter() {
        if (workingPool.isEmpty()) workingPool.addAll(initialPool);
        int idx = random.nextInt(workingPool.size());
        currentImage = workingPool.remove(idx);
        isRoundActive = false;
        roundNumber = 1;
        isMatchFinished = false;
        timeRemaining = timerInitialDuration;
    }

    // =========================================================================
    // 1. RULE 1: WAITING SCREEN (Hidden before starting turn)
    // =========================================================================
    private void buildWaitingBox() {
        waitingBox = new VBox(16);
        waitingBox.setAlignment(Pos.CENTER);
        waitingBox.setPrefSize(680, 380);
        waitingBox.setMaxSize(680, 380);
        waitingBox.setMinSize(680, 380);
        waitingBox.setPadding(new Insets(24));
        waitingBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-width: 2px; -fx-border-radius: 24px; -fx-background-radius: 24px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 20, 0, 0, 8);",
                ThemeManager.getCardHex()));

        Label iconLbl = new Label("🎭");
        iconLbl.setStyle("-fx-font-size: 54px;");

        Label roundTitle = new Label(String.format(I18n.get("game.common.turn_n_of_m"), 1, totalMatchRounds));
        roundTitle.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getAccentHex()));

        Label mainPrompt = new Label(I18n.get("game.guesscharacter.ready_title"));
        mainPrompt.setStyle(String.format("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-text-alignment: center;",
                ThemeManager.getTextOnCardPrimaryHex()));
        mainPrompt.setWrapText(true);
        mainPrompt.setAlignment(Pos.CENTER);
        mainPrompt.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        mainPrompt.setEllipsisString("");

        Label subPrompt = new Label(enableTimer
                ? String.format(I18n.get("game.guesscharacter.ready_desc_timer"), timerInitialDuration)
                : I18n.get("game.guesscharacter.ready_desc_notimer"));
        subPrompt.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s; -fx-text-alignment: center;",
                ThemeManager.getTextOnCardSecondaryHex()));
        subPrompt.setWrapText(true);
        subPrompt.setAlignment(Pos.CENTER);
        subPrompt.setMaxWidth(480);
        subPrompt.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        subPrompt.setEllipsisString("");

        Button startTurnBtn = new Button(I18n.get("game.common.start_turn"));
        startTurnBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 14px; -fx-font-weight: 900; -fx-padding: 12px 36px; -fx-background-radius: 14px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        startTurnBtn.setOnAction(e -> {
            isRoundActive = true;
            timeRemaining = timerInitialDuration;
            updateUI();
            loadImage(currentImage);
            startTimer();
        });

        waitingBox.getChildren().addAll(iconLbl, roundTitle, mainPrompt, subPrompt, startTurnBtn);
    }

    // =========================================================================
    // 2. ACTIVE ROUND PLAY BOX (Fixed Image Viewport + Digital Timer + Controls)
    // =========================================================================
    private void buildActivePlayBox() {
        activePlayBox = new VBox(12);
        activePlayBox.setAlignment(Pos.CENTER);
        activePlayBox.setMaxWidth(680);

        // 2.1 IMAGE VIEWPORT (Fixed specific dimension: 680x380)
        imageViewport = new StackPane();
        imageViewport.setPrefSize(680, 380);
        imageViewport.setMaxSize(680, 380);
        imageViewport.setMinSize(680, 380);
        imageViewport.setStyle(String.format(
                "-fx-background-color: #090c14; -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
                ThemeManager.getCardHex()));

        Rectangle clip = new Rectangle(680, 380);
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        imageViewport.setClip(clip);

        imageView = new ImageView();
        imageView.setFitWidth(680);
        imageView.setFitHeight(380);
        imageView.setPreserveRatio(false);

        loader = new ProgressIndicator();
        loader.setMaxSize(40, 40);

        // Image Counter Badge (Bottom-Center)
        imageCounterBadge = new Label("🎭 1 / " + totalMatchRounds);
        imageCounterBadge.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.88); -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-font-size: 13px; -fx-padding: 6px 18px; -fx-background-radius: 14px; -fx-border-color: rgba(255, 255, 255, 0.25); -fx-border-radius: 14px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 8, 0, 0, 2);");
        StackPane.setAlignment(imageCounterBadge, Pos.BOTTOM_CENTER);
        StackPane.setMargin(imageCounterBadge, new Insets(14));

        imageViewport.getChildren().addAll(imageView, loader, imageCounterBadge);

        // Digital Timer Box (Positioned outside image frame, at top of activePlayBox)
        digitalTimerBox = new HBox(3);
        digitalTimerBox.setAlignment(Pos.CENTER);
        digitalTimerBox.setMaxWidth(Region.USE_PREF_SIZE);
        digitalTimerBox.setStyle("-fx-background-color: rgba(11, 14, 23, 0.9); -fx-padding: 6px 20px; -fx-background-radius: 16px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 16px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 4);");

        // 2.2 BUTTON (Rule 4: Next Image Manual Advance)
        nextImageBtn = new Button(I18n.get("game.common.next_image"));
        nextImageBtn.setPrefWidth(680);
        nextImageBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 14px; -fx-font-weight: 900; -fx-padding: 13px 28px; -fx-background-radius: 12px; -fx-cursor: hand;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        nextImageBtn.setOnAction(e -> handleNextImage());

        activePlayBox.getChildren().addAll(digitalTimerBox, imageViewport, nextImageBtn);
    }

    // =========================================================================
    // 3. MATCH COMPLETED BANNER
    // =========================================================================
    private void buildMatchFinishedBox() {
        matchFinishedBox = new VBox(16);
        matchFinishedBox.setAlignment(Pos.CENTER);
        matchFinishedBox.setPrefSize(680, 380);
        matchFinishedBox.setMaxSize(680, 380);
        matchFinishedBox.setMinSize(680, 380);
        matchFinishedBox.setPadding(new Insets(24));
        matchFinishedBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: #f59e0b; -fx-border-width: 2px; -fx-border-radius: 24px; -fx-background-radius: 24px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
                ThemeManager.getCardHex()));

        Label cup = new Label("🏆");
        cup.setStyle("-fx-font-size: 54px;");

        Label finTitle = new Label(I18n.get("game.guesscharacter.completed"));
        finTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #fbbf24;");

        Label finDesc = new Label(String.format(I18n.get("game.guesscharacter.all_rounds_concluded"), totalMatchRounds, numPlayers, roundsPerPlayer));
        finDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1; -fx-text-alignment: center;");
        finDesc.setWrapText(true);
        finDesc.setMaxWidth(480);

        Label footer = new Label(I18n.get("game.common.ready_next_game"));
        footer.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-background-color: #0f172a; -fx-padding: 8px 16px; -fx-background-radius: 10px;");

        matchFinishedBox.getChildren().addAll(cup, finTitle, finDesc, footer);
    }

    private void updateUI() {
        roundBadgeLabel.setText(String.format(I18n.get("game.guesscharacter.title_image"), Math.min(roundNumber, totalMatchRounds), totalMatchRounds));
        poolInfoLabel.setText(String.format(I18n.get("game.guesscharacter.unseen_pool"), workingPool.size()));

        dynamicContainer.getChildren().clear();

        if (isMatchFinished) {
            dynamicContainer.getChildren().add(matchFinishedBox);
            return;
        }

        if (!isRoundActive) {
            if (waitingBox.getChildren().size() >= 2) {
                ((Label) waitingBox.getChildren().get(1)).setText(String.format(I18n.get("game.common.turn_n_of_m"), roundNumber, totalMatchRounds));
            }
            dynamicContainer.getChildren().add(waitingBox);
            return;
        }

        dynamicContainer.getChildren().add(activePlayBox);

        imageCounterBadge.setText("🎭 " + roundNumber + " / " + totalMatchRounds);
        nextImageBtn.setText(roundNumber >= totalMatchRounds ? I18n.get("game.common.finish_match") : I18n.get("game.common.next_image"));

        renderDigitalTimer();
    }

    private void renderDigitalTimer() {
        if (!enableTimer) {
            digitalTimerBox.setVisible(false);
            digitalTimerBox.setManaged(false);
            return;
        }
        digitalTimerBox.setVisible(true);
        digitalTimerBox.setManaged(true);
        digitalTimerBox.getChildren().clear();

        int mins = timeRemaining / 60;
        int secs = timeRemaining % 60;

        String minStr = mins < 10 ? "0" + mins : String.valueOf(mins);
        String secStr = secs < 10 ? "0" + secs : String.valueOf(secs);

        boolean isTimesUp = (timeRemaining == 0);
        if (isTimesUp) {
            digitalTimerBox.setStyle("-fx-background-color: rgba(239, 68, 68, 0.25); -fx-padding: 6px 12px; -fx-background-radius: 14px; -fx-border-color: #ef4444; -fx-border-radius: 14px; -fx-effect: dropshadow(three-pass-box, rgba(239,68,68,0.7), 12, 0, 0, 2);");
        } else if (timeRemaining <= 5) {
            digitalTimerBox.setStyle("-fx-background-color: rgba(245, 158, 11, 0.25); -fx-padding: 6px 12px; -fx-background-radius: 14px; -fx-border-color: #f59e0b; -fx-border-radius: 14px; -fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.6), 10, 0, 0, 2);");
        } else {
            digitalTimerBox.setStyle("-fx-background-color: rgba(11, 14, 23, 0.9); -fx-padding: 6px 12px; -fx-background-radius: 14px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 14px;");
        }

        // Minutes
        for (char c : minStr.toCharArray()) {
            int d = c - '0';
            digitalTimerBox.getChildren().add(createDigitView(d));
        }

        // Colon
        if (colonImage != null) {
            ImageView civ = new ImageView(colonImage);
            civ.setFitHeight(22);
            civ.setPreserveRatio(true);
            HBox.setMargin(civ, new Insets(0, 2, 0, 2));
            digitalTimerBox.getChildren().add(civ);
        } else {
            Label cl = new Label(":");
            cl.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #ffffff;");
            digitalTimerBox.getChildren().add(cl);
        }

        // Seconds
        for (char c : secStr.toCharArray()) {
            int d = c - '0';
            digitalTimerBox.getChildren().add(createDigitView(d));
        }
    }

    private Node createDigitView(int digit) {
        if (digit >= 0 && digit <= 9 && digitImages[digit] != null) {
            ImageView iv = new ImageView(digitImages[digit]);
            iv.setFitHeight(28);
            iv.setPreserveRatio(true);
            return iv;
        } else {
            Label lbl = new Label(String.valueOf(digit));
            lbl.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #ffffff; -fx-font-family: monospace;");
            return lbl;
        }
    }

    private void startTimer() {
        stopTimer();
        if (!enableTimer) return;

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            if (timeRemaining > 0) {
                timeRemaining--;
                renderDigitalTimer();
            } else {
                // Rule 3: Time ends, timer stops at 00:00 without auto-changing the image
                stopTimer();
                renderDigitalTimer();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void stopTimer() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void loadImage(String url) {
        loader.setVisible(true);
        imageView.setImage(null);

        ImageLoaderHelper.loadImageAsync(url, 680, 380, true, true,
                img -> {
                    loader.setVisible(false);
                    imageView.setImage(img);
                },
                () -> loader.setVisible(false)
        );
    }

    private void handleNextImage() {
        stopTimer();

        if (roundNumber >= totalMatchRounds) {
            // Rule 5: Conclude game
            isMatchFinished = true;
            updateUI();
            return;
        }

        if (workingPool.isEmpty()) {
            workingPool.addAll(initialPool);
        }
        int idx = random.nextInt(workingPool.size());
        currentImage = workingPool.remove(idx); // Rule: Non-repeating
        roundNumber++;
        timeRemaining = timerInitialDuration;

        updateUI();
        loadImage(currentImage);
        startTimer();
    }
}
