package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.util.ImageLoaderHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;

import java.util.*;

public class SnapSolveFxStage extends VBox {

    private static final List<String> DEFAULT_POOL = Arrays.asList(
            "https://images.unsplash.com/photo-1546182990-dffeafbe841d?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1564349683136-77e08dba1ef6?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1508974239320-0a029497e820?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1563245372-f21724e3856d?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1589254065878-42c9da997008?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1535223289827-42f1e9919769?auto=format&fit=crop&w=1000&q=80",
            "https://images.unsplash.com/photo-1574063413132-355dbfd83e23?auto=format&fit=crop&w=1000&q=80"
    );

    private final List<Competitor> profiles;
    private final JsonNode setupData;

    private final List<String> initialPool = new ArrayList<>();
    private final List<String> workingPool = new ArrayList<>();
    private final List<String> allowedFilters = new ArrayList<>();

    private String currentImage = "";
    private String currentFilter = "swirl";
    private int difficulty = 0; // 0: Max, 1: Med, 2: Easy, 3: Original
    private boolean isWaiting = true;
    private int roundNumber = 1;
    private boolean isMatchFinished = false;

    private final boolean isBattleRoyale;
    private final int roundsPerPlayer;
    private final int numPlayers;
    private final int totalMatchRounds;
    private final Random random = new Random();

    // Cache base image loaded from network or local file
    private Image rawBaseImage = null;

    // UI Nodes
    private Label roundBadgeLabel;
    private Label poolInfoLabel;
    private StackPane dynamicContainer;

    private VBox waitingBox;
    private VBox activePlayBox;
    private VBox matchFinishedBox;

    // Active Play UI Nodes
    private Label filterNameBadge;
    private Label diffBadge;
    private StackPane imageViewport;
    private ImageView imageView;
    private ProgressIndicator loader;
    private Label imageCounterBadge;
    private Button continueGuessingBtn;
    private Button nextImageBtn;

    public SnapSolveFxStage(List<Competitor> profiles, JsonNode setupData) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        this.setupData = setupData;

        int rpp = 2;
        boolean br = false;
        if (setupData != null) {
            if (setupData.has("battleRoyale")) br = setupData.get("battleRoyale").asBoolean(false);
            else if (setupData.has("battle_royale")) br = setupData.get("battle_royale").asBoolean(false);

            if (setupData.has("rounds_per_player")) rpp = setupData.get("rounds_per_player").asInt(2);
            else if (setupData.has("roundsPerPlayer")) rpp = setupData.get("roundsPerPlayer").asInt(2);
        }

        this.isBattleRoyale = br;
        this.roundsPerPlayer = rpp;
        this.numPlayers = (!this.profiles.isEmpty()) ? this.profiles.size() : 1;

        setSpacing(14);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(12));

        parseFiltersAndPool();

        this.totalMatchRounds = this.isBattleRoyale ? Math.max(1, initialPool.size()) : (this.numPlayers * this.roundsPerPlayer);

        // 1. TOP STATUS BADGES
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER);

        String badgeTitle = String.format(I18n.get("game.snapsolve.title_round"), 1, totalMatchRounds);
        if (isBattleRoyale) {
            badgeTitle = "⚔️ BR • " + badgeTitle;
        }
        roundBadgeLabel = new Label(badgeTitle);
        roundBadgeLabel.setStyle(String.format(
                "-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #f59e0b; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 20px; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 20px;",
                ThemeManager.getAccentHex()));

        poolInfoLabel = new Label(String.format(I18n.get("game.snapsolve.remaining_pool"), workingPool.size()));
        poolInfoLabel.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6px 12px; -fx-background-radius: 20px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 20px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));

        topBar.getChildren().addAll(roundBadgeLabel, poolInfoLabel);
        getChildren().add(topBar);

        // 2. MAIN DYNAMIC CONTAINER
        dynamicContainer = new StackPane();
        dynamicContainer.setAlignment(Pos.CENTER);
        getChildren().add(dynamicContainer);

        buildWaitingBox();
        buildActivePlayBox();
        buildMatchFinishedBox();

        showWaitingBox();
    }

    private void parseFiltersAndPool() {
        allowedFilters.clear();
        if (setupData != null) {
            JsonNode filtersNode = setupData.has("selected_filters") ? setupData.get("selected_filters")
                    : (setupData.has("selectedFilters") ? setupData.get("selectedFilters") : null);
            if (filtersNode != null && filtersNode.isArray() && filtersNode.size() > 0) {
                for (JsonNode f : filtersNode) {
                    allowedFilters.add(f.asText());
                }
            }
        }
        if (allowedFilters.isEmpty()) {
            allowedFilters.addAll(Arrays.asList("displacement", "swirl", "pixelate", "blur"));
        }

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
            initialPool.addAll(DEFAULT_POOL);
        }

        workingPool.clear();
        workingPool.addAll(initialPool);
    }

    // =========================================================================
    // 1. PHASE 0: WAITING SCREEN (Pause / Player Selection)
    // =========================================================================
    private void buildWaitingBox() {
        waitingBox = new VBox(16);
        waitingBox.setAlignment(Pos.CENTER);
        double scale = ThemeManager.getFontScale();
        waitingBox.setPrefWidth(Math.max(680, 680 * scale));
        waitingBox.setMaxWidth(Double.MAX_VALUE);
        waitingBox.setMinHeight(Region.USE_COMPUTED_SIZE);
        waitingBox.setPadding(new Insets(24));
        waitingBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-width: 2px; -fx-border-radius: 24px; -fx-background-radius: 24px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 20, 0, 0, 8);",
                ThemeManager.getCardHex()));

        Label iconLbl = new Label("⚡");
        iconLbl.setStyle("-fx-font-size: 54px;");

        Label roundTitle = new Label(String.format(I18n.get("game.common.turn_n_of_m"), 1, totalMatchRounds));
        roundTitle.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s;", ThemeManager.getAccentHex()));

        Label mainPrompt = new Label(I18n.get("game.snapsolve.ready_prompt"));
        mainPrompt.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: white; -fx-text-alignment: center;");
        mainPrompt.setWrapText(true);
        mainPrompt.setAlignment(Pos.CENTER);
        mainPrompt.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        mainPrompt.setEllipsisString("");

        Label subPrompt = new Label(I18n.get("game.snapsolve.ready_desc"));
        subPrompt.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
        subPrompt.setWrapText(true);
        subPrompt.setAlignment(Pos.CENTER);
        subPrompt.setMaxWidth(520);
        subPrompt.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        subPrompt.setEllipsisString("");

        Button startTurnBtn = new Button(I18n.get("game.common.start_turn"));
        startTurnBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-font-size: 13px; -fx-padding: 12px 32px; -fx-background-radius: 14px; -fx-cursor: hand;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        startTurnBtn.setOnAction(e -> startActiveTurn());

        waitingBox.getChildren().addAll(iconLbl, roundTitle, mainPrompt, subPrompt, startTurnBtn);
    }

    private void showWaitingBox() {
        isWaiting = true;
        String badge = String.format(I18n.get("game.snapsolve.title_round"), roundNumber, totalMatchRounds);
        if (isBattleRoyale) badge = "⚔️ BR • " + badge;
        roundBadgeLabel.setText(badge);
        poolInfoLabel.setText(String.format(I18n.get("game.snapsolve.remaining_pool"), workingPool.size()));

        if (waitingBox.getChildren().size() > 1 && waitingBox.getChildren().get(1) instanceof Label rLabel) {
            rLabel.setText(String.format(I18n.get("game.common.turn_n_of_m"), roundNumber, totalMatchRounds));
        }

        dynamicContainer.getChildren().setAll(waitingBox);
    }

    // =========================================================================
    // 2. PHASE 1-5: ACTIVE TURN SCREEN
    // =========================================================================
    private void buildActivePlayBox() {
        activePlayBox = new VBox(12);
        activePlayBox.setAlignment(Pos.CENTER);
        activePlayBox.setMaxWidth(680);

        // Status row (Filter badge + Difficulty badge)
        HBox statusRow = new HBox(12);
        statusRow.setAlignment(Pos.CENTER);

        filterNameBadge = new Label();
        filterNameBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 12px; -fx-border-color: #334155; -fx-border-radius: 12px;");

        diffBadge = new Label(I18n.get("game.snapsolve.diff_max"));
        diffBadge.setStyle("-fx-background-color: rgba(244, 63, 94, 0.2); -fx-text-fill: #f43f5e; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 12px; -fx-border-color: rgba(244, 63, 94, 0.4); -fx-border-radius: 12px;");

        statusRow.getChildren().addAll(filterNameBadge, diffBadge);

        // Viewport 680x380
        imageViewport = new StackPane();
        imageViewport.setPrefSize(680, 380);
        imageViewport.setMaxSize(680, 380);
        imageViewport.setMinSize(680, 380);
        imageViewport.setStyle("-fx-background-color: #090c15; -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);");
        imageViewport.setAlignment(Pos.CENTER);

        imageView = new ImageView();
        imageView.setFitWidth(680);
        imageView.setFitHeight(380);
        imageView.setPreserveRatio(false);

        loader = new ProgressIndicator();
        loader.setMaxSize(50, 50);

        imageCounterBadge = new Label(String.format(I18n.get("game.common.turn_n_of_m"), 1, totalMatchRounds));
        imageCounterBadge.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 4px 16px; -fx-background-radius: 20px; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 20px;");
        StackPane.setAlignment(imageCounterBadge, Pos.BOTTOM_CENTER);
        StackPane.setMargin(imageCounterBadge, new Insets(12));

        imageViewport.getChildren().addAll(imageView, loader, imageCounterBadge);

        // Action Buttons
        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER);

        continueGuessingBtn = new Button(I18n.get("game.snapsolve.guess_med"));
        continueGuessingBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(continueGuessingBtn, Priority.ALWAYS);
        continueGuessingBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #0f172a; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 12px 18px; -fx-background-radius: 14px; -fx-cursor: hand;");
        continueGuessingBtn.setOnAction(e -> handleContinueGuessing());

        nextImageBtn = new Button(I18n.get("game.common.next_image"));
        nextImageBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nextImageBtn, Priority.ALWAYS);
        nextImageBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 12px 18px; -fx-background-radius: 14px; -fx-cursor: hand;", ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        nextImageBtn.setOnAction(e -> handleNextImage());

        actionsRow.getChildren().addAll(continueGuessingBtn, nextImageBtn);

        activePlayBox.getChildren().addAll(statusRow, imageViewport, actionsRow);
    }

    private void startActiveTurn() {
        if (workingPool.isEmpty()) workingPool.addAll(initialPool);
        int idx = random.nextInt(workingPool.size());
        currentImage = workingPool.remove(idx);

        // Pick random filter from allowed filters
        currentFilter = allowedFilters.get(random.nextInt(allowedFilters.size()));
        difficulty = 0; // Maximum difficulty
        isWaiting = false;

        loader.setVisible(true);
        imageView.setImage(null);
        imageView.setEffect(null);

        updateFilterLabels();

        // Load image asynchronously using ImageLoaderHelper
        ImageLoaderHelper.loadImageAsync(currentImage, 680, 380, false, true,
                base -> {
                    this.rawBaseImage = base;
                    loader.setVisible(false);
                    applyCurrentFilter();
                },
                () -> loader.setVisible(false)
        );

        imageCounterBadge.setText(String.format(I18n.get("game.common.turn_n_of_m"), roundNumber, totalMatchRounds));
        dynamicContainer.getChildren().setAll(activePlayBox);
    }

    private void handleContinueGuessing() {
        if (difficulty < 3) {
            difficulty++;
            updateFilterLabels();
            applyCurrentFilter();
        }
    }

    private void handleNextImage() {
        if (roundNumber >= totalMatchRounds) {
            showMatchFinished();
            return;
        }

        roundNumber++;
        showWaitingBox();
    }

    private void updateFilterLabels() {
        String fName = switch (currentFilter) {
            case "displacement" -> "〰️ Displacement";
            case "swirl" -> "🌪️ Swirl";
            case "pixelate" -> "▦ Pixelate";
            case "blur" -> "🌫️ Blur";
            default -> "⚡ Distortion";
        };
        filterNameBadge.setText(String.format(I18n.get("game.snapsolve.filter_badge"), fName));

        switch (difficulty) {
            case 0 -> {
                diffBadge.setText(I18n.get("game.snapsolve.diff_max"));
                diffBadge.setStyle("-fx-background-color: rgba(244, 63, 94, 0.2); -fx-text-fill: #f43f5e; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 12px; -fx-border-color: rgba(244, 63, 94, 0.4); -fx-border-radius: 12px;");
                continueGuessingBtn.setText(I18n.get("game.snapsolve.guess_med"));
                continueGuessingBtn.setDisable(false);
            }
            case 1 -> {
                diffBadge.setText(I18n.get("game.snapsolve.diff_med"));
                diffBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.2); -fx-text-fill: #f59e0b; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 12px; -fx-border-color: rgba(245, 158, 11, 0.4); -fx-border-radius: 12px;");
                continueGuessingBtn.setText(I18n.get("game.snapsolve.guess_easy"));
                continueGuessingBtn.setDisable(false);
            }
            case 2 -> {
                diffBadge.setText(I18n.get("game.snapsolve.diff_easy"));
                diffBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10b981; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 12px; -fx-border-color: rgba(16, 185, 129, 0.4); -fx-border-radius: 12px;");
                continueGuessingBtn.setText(I18n.get("game.snapsolve.guess_orig"));
                continueGuessingBtn.setDisable(false);
            }
            default -> {
                diffBadge.setText(I18n.get("game.snapsolve.diff_original"));
                diffBadge.setStyle("-fx-background-color: rgba(99, 102, 241, 0.2); -fx-text-fill: #a5b4fc; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 12px; -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-radius: 12px;");
                continueGuessingBtn.setText(I18n.get("game.snapsolve.orig_visible"));
                continueGuessingBtn.setDisable(true);
            }
        }

        if (roundNumber >= totalMatchRounds) {
            nextImageBtn.setText(I18n.get("game.common.finish_match"));
        } else {
            nextImageBtn.setText(I18n.get("game.common.next_image"));
        }
    }

    private void applyCurrentFilter() {
        if (rawBaseImage == null) return;

        if (difficulty >= 3) {
            // Original Image
            imageView.setEffect(null);
            imageView.setImage(rawBaseImage);
            return;
        }

        switch (currentFilter) {
            case "blur" -> {
                imageView.setImage(rawBaseImage);
                double r = (difficulty == 0) ? 42.0 : (difficulty == 1) ? 20.0 : 7.0;
                imageView.setEffect(new GaussianBlur(r));
            }
            case "pixelate" -> {
                imageView.setEffect(null);
                int blockSize = (difficulty == 0) ? 38 : (difficulty == 1) ? 20 : 9;
                imageView.setImage(createPixelatedImage(rawBaseImage, blockSize));
            }
            case "swirl" -> {
                imageView.setEffect(null);
                double angle = (difficulty == 0) ? 4.5 * Math.PI : (difficulty == 1) ? 2.3 * Math.PI : 0.9 * Math.PI;
                imageView.setImage(createSwirledImage(rawBaseImage, angle));
            }
            case "displacement" -> {
                imageView.setEffect(null);
                double amp = (difficulty == 0) ? 40.0 : (difficulty == 1) ? 20.0 : 8.0;
                double period = (difficulty == 0) ? 18.0 : (difficulty == 1) ? 30.0 : 50.0;
                imageView.setImage(createDisplacedImage(rawBaseImage, amp, period));
            }
            default -> {
                imageView.setEffect(null);
                imageView.setImage(rawBaseImage);
            }
        }
    }

    // --- Image Processing Helpers ---

    private Image createPixelatedImage(Image src, int blockSize) {
        int w = (int) src.getWidth();
        int h = (int) src.getHeight();
        if (w <= 0 || h <= 0 || blockSize <= 1) return src;
        PixelReader reader = src.getPixelReader();
        if (reader == null) return src;

        WritableImage dst = new WritableImage(w, h);
        PixelWriter writer = dst.getPixelWriter();

        for (int y = 0; y < h; y += blockSize) {
            for (int x = 0; x < w; x += blockSize) {
                int sampleX = Math.min(x + blockSize / 2, w - 1);
                int sampleY = Math.min(y + blockSize / 2, h - 1);
                int argb = reader.getArgb(sampleX, sampleY);
                int bw = Math.min(blockSize, w - x);
                int bh = Math.min(blockSize, h - y);

                for (int dy = 0; dy < bh; dy++) {
                    for (int dx = 0; dx < bw; dx++) {
                        writer.setArgb(x + dx, y + dy, argb);
                    }
                }
            }
        }
        return dst;
    }

    private Image createSwirledImage(Image src, double maxAngle) {
        int w = (int) src.getWidth();
        int h = (int) src.getHeight();
        if (w <= 0 || h <= 0 || maxAngle <= 0.01) return src;
        PixelReader reader = src.getPixelReader();
        if (reader == null) return src;

        WritableImage dst = new WritableImage(w, h);
        PixelWriter writer = dst.getPixelWriter();
        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.min(w, h) / 1.8;
        double radiusSq = radius * radius;

        for (int y = 0; y < h; y++) {
            double dy = y - cy;
            double dySq = dy * dy;
            for (int x = 0; x < w; x++) {
                double dx = x - cx;
                double distSq = dx * dx + dySq;
                if (distSq < radiusSq) {
                    double dist = Math.sqrt(distSq);
                    double factor = 1.0 - (dist / radius);
                    double angle = Math.atan2(dy, dx) + maxAngle * factor * factor;
                    int srcX = (int) Math.round(cx + dist * Math.cos(angle));
                    int srcY = (int) Math.round(cy + dist * Math.sin(angle));
                    if (srcX >= 0 && srcX < w && srcY >= 0 && srcY < h) {
                        writer.setArgb(x, y, reader.getArgb(srcX, srcY));
                    } else {
                        writer.setArgb(x, y, reader.getArgb(x, y));
                    }
                } else {
                    writer.setArgb(x, y, reader.getArgb(x, y));
                }
            }
        }
        return dst;
    }

    private Image createDisplacedImage(Image src, double amplitude, double period) {
        int w = (int) src.getWidth();
        int h = (int) src.getHeight();
        if (w <= 0 || h <= 0 || amplitude <= 0.5) return src;
        PixelReader reader = src.getPixelReader();
        if (reader == null) return src;

        WritableImage dst = new WritableImage(w, h);
        PixelWriter writer = dst.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int shiftX = (int) Math.round(amplitude * Math.sin((y / period) * Math.PI * 2.0));
                int shiftY = (int) Math.round((amplitude * 0.7) * Math.cos((x / period) * Math.PI * 2.0));
                int srcX = Math.min(w - 1, Math.max(0, x + shiftX));
                int srcY = Math.min(h - 1, Math.max(0, y + shiftY));
                writer.setArgb(x, y, reader.getArgb(srcX, srcY));
            }
        }
        return dst;
    }

    // =========================================================================
    // 3. MATCH FINISHED SCREEN
    // =========================================================================
    private void buildMatchFinishedBox() {
        matchFinishedBox = new VBox(16);
        matchFinishedBox.setAlignment(Pos.CENTER);
        double scale = ThemeManager.getFontScale();
        matchFinishedBox.setPrefWidth(Math.max(680, 680 * scale));
        matchFinishedBox.setMaxWidth(Double.MAX_VALUE);
        matchFinishedBox.setMinHeight(Region.USE_COMPUTED_SIZE);
        matchFinishedBox.setPadding(new Insets(24));
        matchFinishedBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(245, 158, 11, 0.5); -fx-border-width: 2px; -fx-border-radius: 24px; -fx-background-radius: 24px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
                ThemeManager.getCardHex()));

        Label trophyLbl = new Label("🏆");
        trophyLbl.setStyle("-fx-font-size: 54px;");

        Label title = new Label(I18n.get("game.snapsolve.completed"));
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #f59e0b;");

        String finDescText = isBattleRoyale
                ? String.format("¡Modo Battle Royale completado! Las %d imágenes configuradas aparecieron en la arena.", totalMatchRounds)
                : String.format(I18n.get("game.snapsolve.all_rounds_concluded"), totalMatchRounds, numPlayers, roundsPerPlayer);
        Label desc = new Label(finDescText);
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #e2e8f0; -fx-text-alignment: center;");
        desc.setWrapText(true);
        desc.setMaxWidth(520);

        matchFinishedBox.getChildren().addAll(trophyLbl, title, desc);
    }

    private void showMatchFinished() {
        isMatchFinished = true;
        dynamicContainer.getChildren().setAll(matchFinishedBox);
    }
}
