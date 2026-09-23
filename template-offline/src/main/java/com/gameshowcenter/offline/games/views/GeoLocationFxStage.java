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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.util.*;

public class GeoLocationFxStage extends VBox {

    public static class LocationEntry {
        private final String name;
        private final List<String> images;

        public LocationEntry(String name, List<String> images) {
            this.name = name;
            this.images = images != null ? images : new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public List<String> getImages() {
            return images;
        }
    }

    private static final List<LocationEntry> DEFAULT_LOCATIONS = Arrays.asList(
            new LocationEntry("Chichén Itzá, México", Arrays.asList(
                    "https://images.unsplash.com/photo-1518638150340-f706e86654de?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1568402102990-bc541580b59f?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1599837565318-67429bde7162?auto=format&fit=crop&w=1200&q=80")),
            new LocationEntry("Coliseo Romano, Italia", Arrays.asList(
                    "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1515542622106-78bda8ba0e5b?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1529260830199-42c24126f198?auto=format&fit=crop&w=1200&q=80")),
            new LocationEntry("Machu Picchu, Perú", Arrays.asList(
                    "https://images.unsplash.com/photo-1526392060635-9d6019884377?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1587595431973-160d0d94add1?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1509299349698-dd22323b5963?auto=format&fit=crop&w=1200&q=80")),
            new LocationEntry("Torre Eiffel (París), Francia", Arrays.asList(
                    "https://images.unsplash.com/photo-1511739001486-6bfe10ce785f?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1549144511-f099e773c147?auto=format&fit=crop&w=1200&q=80")),
            new LocationEntry("Taj Mahal (Agra), India", Arrays.asList(
                    "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1585506942812-e72b29cef752?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1548013146-72479768bada?auto=format&fit=crop&w=1200&q=80")),
            new LocationEntry("Gran Muralla China, China", Arrays.asList(
                    "https://images.unsplash.com/photo-1508804185872-d7badad00f7d?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1547981609-4b6bfe67ca0b?auto=format&fit=crop&w=1200&q=80")),
            new LocationEntry("Estatua de la Libertad (Nueva York), EE. UU.", Arrays.asList(
                    "https://images.unsplash.com/photo-1605130284535-11dd9eedc58a?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1543783207-ec64e4d95325?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1503572327579-b5c6afe5c5c5?auto=format&fit=crop&w=1200&q=80")),
            new LocationEntry("Cristo Redentor (Río de Janeiro), Brasil", Arrays.asList(
                    "https://images.unsplash.com/photo-1598970434795-0c54fe7c0648?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1516306580123-e6e52b1b7b5f?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?auto=format&fit=crop&w=1200&q=80")),
            new LocationEntry("Pirámides de Guiza, Egipto", Arrays.asList(
                    "https://images.unsplash.com/photo-1503177119275-0aa32b3a9368?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1539650116574-8efeb43e2750?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1568322445389-f64ac2515020?auto=format&fit=crop&w=1200&q=80")),
            new LocationEntry("Ópera de Sídney, Australia", Arrays.asList(
                    "https://images.unsplash.com/photo-1624138784614-87fd1b6528f8?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1523428096881-5bd79d04330f?auto=format&fit=crop&w=1200&q=80"))
    );

    private final List<Competitor> profiles;
    private final JsonNode setupData;

    private final List<LocationEntry> initialPool = new ArrayList<>();
    private final List<LocationEntry> workingPool = new ArrayList<>();
    private LocationEntry currentLocation = null;
    private int currentImageIndex = 0;
    private boolean isRoundActive = false;
    private boolean isAnswerRevealed = false;
    private int roundNumber = 1;
    private boolean isMatchFinished = false;

    private final int roundsPerPlayer;
    private final int numPlayers;
    private final int totalMatchRounds;
    private final Random random = new Random();

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
    private Label answerLabel;
    private Button nextImageBtn;
    private Button revealAnswerBtn;

    public GeoLocationFxStage(List<Competitor> profiles, JsonNode setupData) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        this.setupData = setupData;

        int rpp = 1;
        if (setupData != null) {
            if (setupData.has("rounds_per_player")) rpp = setupData.get("rounds_per_player").asInt(1);
            else if (setupData.has("roundsPerPlayer")) rpp = setupData.get("roundsPerPlayer").asInt(1);
        }
        this.roundsPerPlayer = rpp;
        this.numPlayers = (!this.profiles.isEmpty()) ? this.profiles.size() : 1;
        this.totalMatchRounds = this.numPlayers * this.roundsPerPlayer;

        setSpacing(14);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(12));

        parseLocationsPool();

        // 1. TOP STATUS BADGES
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER);

        roundBadgeLabel = new Label(String.format(I18n.get("game.geolocation.title_round"), 1, totalMatchRounds));
        roundBadgeLabel.setStyle(String.format(
                "-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #f59e0b; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 20px; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 20px;",
                ThemeManager.getAccentHex()));

        poolInfoLabel = new Label(String.format(I18n.get("game.geolocation.unseen_locations"), workingPool.size()));
        poolInfoLabel.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6px 12px; -fx-background-radius: 20px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 20px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));

        topBar.getChildren().addAll(roundBadgeLabel, poolInfoLabel);
        getChildren().add(topBar);

        // 2. MAIN DYNAMIC CONTAINER (Holds Waiting Box, Active Play Box, or Match Finished Box)
        dynamicContainer = new StackPane();
        dynamicContainer.setAlignment(Pos.CENTER);
        getChildren().add(dynamicContainer);

        // Build View States
        buildWaitingBox();
        buildActivePlayBox();
        buildMatchFinishedBox();

        // Pick first place
        pickFirstLocation();
        updateUI();
    }

    private void parseLocationsPool() {
        initialPool.clear();
        if (setupData != null && setupData.has("locations") && setupData.get("locations").isArray()) {
            for (JsonNode locNode : setupData.get("locations")) {
                String name = locNode.has("location_name") ? locNode.get("location_name").asText()
                        : (locNode.has("locationName") ? locNode.get("locationName").asText() : "Unknown Location");
                List<String> images = new ArrayList<>();
                if (locNode.has("images") && locNode.get("images").isArray()) {
                    for (JsonNode img : locNode.get("images")) {
                        String s = img.asText();
                        if (s != null && !s.isBlank()) images.add(s);
                    }
                }
                if (!images.isEmpty()) {
                    initialPool.add(new LocationEntry(name, images));
                }
            }
        }
        if (initialPool.isEmpty()) {
            initialPool.addAll(DEFAULT_LOCATIONS);
        }
        workingPool.clear();
        workingPool.addAll(initialPool);
    }

    private void pickFirstLocation() {
        if (workingPool.isEmpty()) workingPool.addAll(initialPool);
        int idx = random.nextInt(workingPool.size());
        currentLocation = workingPool.remove(idx);
        currentImageIndex = 0;
        isRoundActive = false;
        isAnswerRevealed = false;
        roundNumber = 1;
        isMatchFinished = false;
    }

    // =========================================================================
    // 1. RULE 1: WAITING SCREEN (Hidden before starting round)
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

        Label iconLbl = new Label("🧭");
        iconLbl.setStyle("-fx-font-size: 54px;");

        Label roundTitle = new Label(String.format(I18n.get("game.common.round_n_of_m"), 1, totalMatchRounds));
        roundTitle.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getAccentHex()));

        Label mainPrompt = new Label(I18n.get("game.geolocation.ready_title"));
        mainPrompt.setStyle(String.format("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getTextOnCardPrimaryHex()));

        Label subPrompt = new Label(I18n.get("game.geolocation.ready_desc"));
        subPrompt.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s; -fx-text-alignment: center;",
                ThemeManager.getTextOnCardSecondaryHex()));
        subPrompt.setWrapText(true);
        subPrompt.setMaxWidth(480);

        Button startRoundBtn = new Button(I18n.get("game.common.start_round"));
        startRoundBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 14px; -fx-font-weight: 900; -fx-padding: 12px 36px; -fx-background-radius: 14px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        startRoundBtn.setOnAction(e -> {
            isRoundActive = true;
            isAnswerRevealed = false;
            currentImageIndex = 0;
            updateUI();
            loadImage(currentLocation.getImages().get(currentImageIndex));
        });

        waitingBox.getChildren().addAll(iconLbl, roundTitle, mainPrompt, subPrompt, startRoundBtn);
    }

    // =========================================================================
    // 2. ACTIVE ROUND PLAY BOX (Image Viewport + Answer Box + Controls)
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

        // Rounded clip for viewport
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

        // Image Counter Badge (Over image: Number of image / Total images per round)
        imageCounterBadge = new Label("📷 1 / 3");
        imageCounterBadge.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.88); -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-font-size: 13px; -fx-padding: 6px 18px; -fx-background-radius: 14px; -fx-border-color: rgba(255, 255, 255, 0.25); -fx-border-radius: 14px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 8, 0, 0, 2);");
        StackPane.setAlignment(imageCounterBadge, Pos.BOTTOM_CENTER);
        StackPane.setMargin(imageCounterBadge, new Insets(14));

        imageViewport.getChildren().addAll(imageView, loader, imageCounterBadge);

        // 2.2 ANSWER DISPLAY BOX
        VBox answerBoxWrap = new VBox(4);
        answerBoxWrap.setAlignment(Pos.CENTER);
        answerBoxWrap.setPrefWidth(680);
        answerBoxWrap.setMaxWidth(680);
        answerBoxWrap.setPadding(new Insets(10, 16, 10, 16));
        answerBoxWrap.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 14px; -fx-background-radius: 14px;",
                ThemeManager.getCardHex()));

        Label ansTitle = new Label(I18n.get("game.geolocation.mystery_answer"));
        ansTitle.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: #94a3b8; -fx-letter-spacing: 1px;");

        answerLabel = new Label(I18n.get("game.common.answer_hidden"));
        answerLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #64748b; -fx-padding: 6px 14px;");

        answerBoxWrap.getChildren().addAll(ansTitle, answerLabel);

        // 2.3 BUTTONS ROW
        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPrefWidth(680);
        btnRow.setMaxWidth(680);

        nextImageBtn = new Button(I18n.get("game.common.next_image"));
        nextImageBtn.setPrefWidth(334);
        nextImageBtn.setStyle(String.format(
                "-fx-background-color: #4f46e5; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 20px; -fx-background-radius: 12px; -fx-cursor: hand;",
                ThemeManager.getButtonHex()));
        nextImageBtn.setOnAction(e -> handleNextImage());

        revealAnswerBtn = new Button(I18n.get("game.common.reveal_answer"));
        revealAnswerBtn.setPrefWidth(334);
        revealAnswerBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 20px; -fx-background-radius: 12px; -fx-cursor: hand;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        revealAnswerBtn.setOnAction(e -> handleRevealOrNextRound());

        btnRow.getChildren().addAll(nextImageBtn, revealAnswerBtn);

        activePlayBox.getChildren().addAll(imageViewport, answerBoxWrap, btnRow);
    }

    // =========================================================================
    // 3. MATCH FINISHED BANNER
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
                "-fx-background-color: %s; -fx-border-color: #f59e0b; -fx-border-width: 2px; -fx-border-radius: 24px; -fx-background-radius: 24px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
                ThemeManager.getCardHex()));

        Label cup = new Label("🏆");
        cup.setStyle("-fx-font-size: 54px;");

        Label finTitle = new Label(I18n.get("game.geolocation.completed"));
        finTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #fbbf24;");

        Label finDesc = new Label(String.format(I18n.get("game.geolocation.all_rounds_concluded"), totalMatchRounds, numPlayers, roundsPerPlayer));
        finDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1; -fx-text-alignment: center;");
        finDesc.setWrapText(true);
        finDesc.setMaxWidth(480);

        Label footer = new Label(I18n.get("game.common.ready_next_game"));
        footer.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-background-color: #0f172a; -fx-padding: 8px 16px; -fx-background-radius: 10px;");

        matchFinishedBox.getChildren().addAll(cup, finTitle, finDesc, footer);
    }

    private void updateUI() {
        roundBadgeLabel.setText(String.format(I18n.get("game.geolocation.title_round"), Math.min(roundNumber, totalMatchRounds), totalMatchRounds));
        poolInfoLabel.setText(String.format(I18n.get("game.geolocation.unseen_locations"), workingPool.size()));

        dynamicContainer.getChildren().clear();

        if (isMatchFinished) {
            dynamicContainer.getChildren().add(matchFinishedBox);
            return;
        }

        if (!isRoundActive) {
            // Update labels on waiting screen
            if (waitingBox.getChildren().size() >= 2) {
                ((Label) waitingBox.getChildren().get(1)).setText(String.format(I18n.get("game.common.round_n_of_m"), roundNumber, totalMatchRounds));
            }
            dynamicContainer.getChildren().add(waitingBox);
            return;
        }

        dynamicContainer.getChildren().add(activePlayBox);

        // Update Active Play controls
        int totalImages = currentLocation != null ? currentLocation.getImages().size() : 1;
        boolean isLast = currentImageIndex >= totalImages - 1;

        // Display exact Number of image / Total images per round
        imageCounterBadge.setText(String.format("📷 %d / %d", currentImageIndex + 1, totalImages));

        // Rule 3 & 4: Next Image vs Last Image
        if (isLast) {
            nextImageBtn.setText(I18n.get("game.common.last_image"));
            nextImageBtn.setDisable(true);
            nextImageBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 20px; -fx-background-radius: 12px; -fx-opacity: 0.7;");
        } else {
            nextImageBtn.setText(I18n.get("game.common.next_image"));
            nextImageBtn.setDisable(false);
            nextImageBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 20px; -fx-background-radius: 12px; -fx-cursor: hand;");
        }

        // Rule 2: Reveal Answer vs Next Round
        if (isAnswerRevealed) {
            answerLabel.setText("🏛️ " + (currentLocation != null ? currentLocation.getName() : "Unknown"));
            answerLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 900; -fx-text-fill: #34d399; -fx-background-color: rgba(16, 185, 129, 0.15); -fx-padding: 6px 20px; -fx-background-radius: 10px; -fx-border-color: rgba(16, 185, 129, 0.4); -fx-border-radius: 10px;");

            revealAnswerBtn.setText(roundNumber >= totalMatchRounds ? I18n.get("game.common.finish_match") : I18n.get("game.common.next_round"));
            revealAnswerBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 20px; -fx-background-radius: 12px; -fx-cursor: hand;");
        } else {
            answerLabel.setText(I18n.get("game.common.answer_hidden"));
            answerLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #64748b; -fx-padding: 6px 14px;");

            revealAnswerBtn.setText(I18n.get("game.common.reveal_answer"));
            revealAnswerBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 12px 20px; -fx-background-radius: 12px; -fx-cursor: hand;",
                    ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
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
        if (currentLocation == null) return;
        if (currentImageIndex < currentLocation.getImages().size() - 1) {
            currentImageIndex++;
            updateUI();
            loadImage(currentLocation.getImages().get(currentImageIndex));
        }
    }

    private void handleRevealOrNextRound() {
        if (!isAnswerRevealed) {
            isAnswerRevealed = true;
            updateUI();
        } else {
            // Next Round
            if (roundNumber >= totalMatchRounds) {
                isMatchFinished = true;
                updateUI();
                return;
            }

            if (workingPool.isEmpty()) {
                workingPool.addAll(initialPool);
            }
            int idx = random.nextInt(workingPool.size());
            currentLocation = workingPool.remove(idx); // Rule: No duplicate place
            currentImageIndex = 0;
            isAnswerRevealed = false;
            isRoundActive = false; // Rule 1: returns to waiting screen
            roundNumber++;
            updateUI();
        }
    }
}
