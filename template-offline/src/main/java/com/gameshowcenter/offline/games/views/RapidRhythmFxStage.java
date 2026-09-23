package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RapidRhythmFxStage extends VBox {

    public static class TrackData {
        public String id;
        public String name;
        public String audioUrl;
        public String answer;
        public int startTime = 0;
        public int spanTime = 30;
        public int endTime = 30;
        public int duration = 30;

        public TrackData(String id, String name, String audioUrl, String answer, int startTime, int endTime, int duration) {
            this.id = id;
            this.name = name;
            this.audioUrl = audioUrl;
            this.answer = answer;
            this.startTime = startTime;
            this.endTime = endTime;
            this.spanTime = Math.max(1, endTime - startTime);
            this.duration = duration;
        }
    }

    private static final List<TrackData> DEFAULT_TRACKS = List.of(
            new TrackData("track_1", "Synthwave Groove #1", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "SoundHelix Track 1", 0, 30, 372),
            new TrackData("track_2", "Electropop Beat #2", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "SoundHelix Track 2", 0, 30, 423),
            new TrackData("track_3", "Chill Lounge Anthem #3", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "SoundHelix Track 3", 0, 30, 340),
            new TrackData("track_4", "Funky Bassline #4", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "SoundHelix Track 4", 0, 30, 302)
    );

    private final List<Competitor> profiles;
    private final JsonNode setupData;

    private final List<TrackData> initialPool = new ArrayList<>();
    private final List<TrackData> workingPool = new ArrayList<>();
    private TrackData currentTrack;
    private int roundNumber = 1;
    private boolean isMatchFinished = false;

    private final int roundsPerPlayer;
    private final int numPlayers;
    private final int totalMatchRounds;
    private final Random random = new Random();

    // Audio Playback Engine
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private double currentSeconds = 0;
    private double volume = 0.8;
    private boolean isAnswerRevealed = false;

    // UI Nodes
    private Label roundBadgeLabel;
    private Label poolInfoLabel;
    private StackPane dynamicContainer;

    // Active Play UI Nodes
    private VBox activePlayBox;
    private VBox matchFinishedBox;

    // Rule 1: Mystery Answer Card
    private Label answerTextLabel;
    private Label answerStatusDesc;
    private Button revealAnswerBtn;
    private VBox answerCardBox;

    // Rule 2 & 3: Audio Player Visuals & Equalizer
    private HBox equalizerBox;
    private List<Rectangle> equalizerBars = new ArrayList<>();
    private Timeline equalizerAnimation;
    private ProgressBar progressBar;
    private Label timeElapsedLabel;
    private Label segmentLabel;
    private Button playPauseBtn;
    private Button replayBtn;
    private Slider volumeSlider;
    private Button nextSongBtn;

    public RapidRhythmFxStage(List<Competitor> profiles, JsonNode setupData) {
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

        parseTrackPool();

        // 1. TOP STATUS BADGES
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER);

        roundBadgeLabel = new Label(String.format(I18n.get("game.rhythm.title_song"), 1, totalMatchRounds));
        roundBadgeLabel.setStyle(String.format(
                "-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #f59e0b; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 20px; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 20px;",
                ThemeManager.getAccentHex()));

        poolInfoLabel = new Label(String.format(I18n.get("game.rhythm.unseen_pool"), workingPool.size()));
        poolInfoLabel.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6px 12px; -fx-background-radius: 20px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 20px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));

        topBar.getChildren().addAll(roundBadgeLabel, poolInfoLabel);
        getChildren().add(topBar);

        // 2. MAIN DYNAMIC CONTAINER
        dynamicContainer = new StackPane();
        dynamicContainer.setAlignment(Pos.CENTER);
        getChildren().add(dynamicContainer);

        buildActivePlayBox();
        buildMatchFinishedBox();

        // Pick first track
        pickFirstTrack();
        updateUI();
    }

    private void parseTrackPool() {
        initialPool.clear();
        if (setupData != null) {
            JsonNode tracksNode = setupData.has("tracks") ? setupData.get("tracks")
                    : (setupData.has("media_pool") ? setupData.get("media_pool") : null);

            if (tracksNode != null && tracksNode.isArray()) {
                for (int i = 0; i < tracksNode.size(); i++) {
                    JsonNode t = tracksNode.get(i);
                    if (t.isObject()) {
                        String u = t.has("audioUrl") ? t.get("audioUrl").asText() : "";
                        String name = t.has("name") ? t.get("name").asText() : "Track #" + (i + 1);
                        String ans = t.has("answer") ? t.get("answer").asText() : "Answer #" + (i + 1);
                        int start = t.has("startTime") ? t.get("startTime").asInt(0) : 0;
                        int end = t.has("endTime") ? t.get("endTime").asInt(30) : 30;
                        int dur = t.has("duration") ? t.get("duration").asInt(30) : 30;
                        if (!u.isBlank()) {
                            initialPool.add(new TrackData("track_" + i, name, u, ans, start, end, dur));
                        }
                    } else if (t.isTextual()) {
                        String u = t.asText();
                        if (!u.isBlank()) {
                            initialPool.add(new TrackData("track_" + i, "Track #" + (i + 1), u, "Song #" + (i + 1), 0, 30, 30));
                        }
                    }
                }
            }
        }

        if (initialPool.isEmpty()) {
            initialPool.addAll(DEFAULT_TRACKS);
        }

        workingPool.clear();
        workingPool.addAll(initialPool);
    }

    private void pickFirstTrack() {
        if (workingPool.isEmpty()) workingPool.addAll(initialPool);
        int idx = random.nextInt(workingPool.size());
        currentTrack = workingPool.remove(idx);
        roundNumber = 1;
        isMatchFinished = false;
        isAnswerRevealed = false;
        loadMediaForCurrentTrack();
    }

    // =========================================================================
    // BUILD ACTIVE PLAY VIEW
    // =========================================================================
    private void buildActivePlayBox() {
        activePlayBox = new VBox(14);
        activePlayBox.setAlignment(Pos.CENTER);
        activePlayBox.setMaxWidth(680);
        activePlayBox.setPrefWidth(680);

        // --- RULE 1: MYSTERY ANSWER CARD ---
        answerCardBox = new VBox(10);
        answerCardBox.setAlignment(Pos.CENTER);
        answerCardBox.setPadding(new Insets(16));
        answerCardBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 16, 0, 0, 6);",
                ThemeManager.getCardHex()));

        HBox topClueBar = new HBox();
        topClueBar.setAlignment(Pos.CENTER_LEFT);

        Label clueTag = new Label(I18n.get("game.rhythm.track_clue"));
        clueTag.setStyle("-fx-background-color: rgba(99, 102, 241, 0.2); -fx-text-fill: #a5b4fc; -fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 3px 8px; -fx-background-radius: 6px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        segmentLabel = new Label("⏱️ 0s ➔ 30s");
        segmentLabel.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #f59e0b; -fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 3px 8px; -fx-background-radius: 6px;");

        topClueBar.getChildren().addAll(clueTag, spacer, segmentLabel);

        // Answer Display Box
        VBox answerInnerBox = new VBox(4);
        answerInnerBox.setAlignment(Pos.CENTER);
        answerInnerBox.setMinHeight(75);
        answerInnerBox.setPadding(new Insets(10));
        answerInnerBox.setStyle("-fx-background-color: #090c14; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 12px; -fx-background-radius: 12px;");

        answerTextLabel = new Label("🔒 ••••••••••••••••••••");
        answerTextLabel.setWrapText(true);
        answerTextLabel.setAlignment(Pos.CENTER);
        answerTextLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        answerTextLabel.setMaxWidth(Double.MAX_VALUE);
        answerTextLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        answerTextLabel.setEllipsisString("");
        answerTextLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #94a3b8;");

        answerStatusDesc = new Label(I18n.get("game.rhythm.answer_hidden_desc"));
        answerStatusDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        answerStatusDesc.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        answerStatusDesc.setEllipsisString("");

        answerInnerBox.getChildren().addAll(answerTextLabel, answerStatusDesc);

        // Reveal Toggle Button
        revealAnswerBtn = new Button(I18n.get("game.common.reveal_answer"));
        revealAnswerBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 8px 24px; -fx-background-radius: 10px; -fx-cursor: hand;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        revealAnswerBtn.setOnAction(e -> toggleAnswerReveal());

        answerCardBox.getChildren().addAll(topClueBar, answerInnerBox, revealAnswerBtn);

        // --- RULE 2 & 3: AUDIO PLAYER CARD ---
        VBox playerBox = new VBox(14);
        playerBox.setAlignment(Pos.CENTER);
        playerBox.setPadding(new Insets(16));
        playerBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 14, 0, 0, 4);",
                ThemeManager.getCardHex()));

        // Equalizer visualizer
        equalizerBox = new HBox(4);
        equalizerBox.setAlignment(Pos.BOTTOM_CENTER);
        equalizerBox.setPrefHeight(60);
        equalizerBox.setPadding(new Insets(8));
        equalizerBox.setStyle("-fx-background-color: #090c14; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 12px; -fx-background-radius: 12px;");

        equalizerBars.clear();
        for (int i = 0; i < 24; i++) {
            Rectangle bar = new Rectangle(12, 6);
            bar.setArcWidth(4);
            bar.setArcHeight(4);
            bar.setFill(Color.web("#334155"));
            equalizerBars.add(bar);
            equalizerBox.getChildren().add(bar);
        }

        setupEqualizerAnimation();

        // Progress bar and time readout
        VBox progressContainer = new VBox(4);
        HBox timeLabelsRow = new HBox();
        timeElapsedLabel = new Label("0s / 30s");
        timeElapsedLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        Region prgSpacer = new Region();
        HBox.setHgrow(prgSpacer, Priority.ALWAYS);

        Label statusLbl = new Label(I18n.get("game.rhythm.playback_segment"));
        statusLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #818cf8;");
        timeLabelsRow.getChildren().addAll(timeElapsedLabel, prgSpacer, statusLbl);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: #6366f1;");

        progressContainer.getChildren().addAll(timeLabelsRow, progressBar);

        // Control Buttons: Replay, Play/Pause, Volume
        HBox controlsRow = new HBox(16);
        controlsRow.setAlignment(Pos.CENTER);

        replayBtn = new Button(I18n.get("game.rhythm.btn_replay"));
        replayBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 10px; -fx-cursor: hand;");
        replayBtn.setOnAction(e -> handleReplay());

        playPauseBtn = new Button(I18n.get("game.rhythm.btn_play"));
        playPauseBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 10px 28px; -fx-background-radius: 12px; -fx-cursor: hand;");
        playPauseBtn.setOnAction(e -> togglePlayPause());

        HBox volBox = new HBox(6);
        volBox.setAlignment(Pos.CENTER);
        Label volIcon = new Label("🔊");
        volIcon.setStyle("-fx-font-size: 12px;");
        volumeSlider = new Slider(0, 1, volume);
        volumeSlider.setPrefWidth(90);
        volumeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            volume = newV.doubleValue();
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volume);
            }
        });
        volBox.getChildren().addAll(volIcon, volumeSlider);

        controlsRow.getChildren().addAll(replayBtn, playPauseBtn, volBox);

        playerBox.getChildren().addAll(equalizerBox, progressContainer, controlsRow);

        // Next Song Button
        nextSongBtn = new Button(I18n.get("game.rhythm.btn_next_song"));
        nextSongBtn.setPrefWidth(680);
        nextSongBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 14px; -fx-font-weight: 900; -fx-padding: 13px 28px; -fx-background-radius: 12px; -fx-cursor: hand;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        nextSongBtn.setOnAction(e -> handleNextSong());

        activePlayBox.getChildren().addAll(answerCardBox, playerBox, nextSongBtn);
    }

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

        Label finTitle = new Label(I18n.get("game.rhythm.completed"));
        finTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #fbbf24;");

        Label finDesc = new Label(String.format(I18n.get("game.rhythm.all_rounds_concluded"), totalMatchRounds, numPlayers, roundsPerPlayer));
        finDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1; -fx-text-alignment: center;");
        finDesc.setWrapText(true);
        finDesc.setMaxWidth(480);

        Label footer = new Label(I18n.get("game.common.ready_next_game"));
        footer.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-background-color: #0f172a; -fx-padding: 8px 16px; -fx-background-radius: 10px;");

        matchFinishedBox.getChildren().addAll(cup, finTitle, finDesc, footer);
    }

    private void setupEqualizerAnimation() {
        equalizerAnimation = new Timeline(new KeyFrame(Duration.millis(120), e -> {
            if (isPlaying) {
                for (int i = 0; i < equalizerBars.size(); i++) {
                    double h = 8 + random.nextDouble() * 40;
                    equalizerBars.get(i).setHeight(h);
                    equalizerBars.get(i).setFill(Color.web(i % 2 == 0 ? "#6366f1" : "#a855f7"));
                }
            } else {
                for (Rectangle bar : equalizerBars) {
                    bar.setHeight(6);
                    bar.setFill(Color.web("#334155"));
                }
            }
        }));
        equalizerAnimation.setCycleCount(Timeline.INDEFINITE);
    }

    private void loadMediaForCurrentTrack() {
        stopMediaPlayer();

        if (currentTrack == null || currentTrack.audioUrl == null) return;

        try {
            Media media = new Media(currentTrack.audioUrl);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volume);

            int start = currentTrack.startTime;
            int end = currentTrack.endTime;
            int span = Math.max(1, end - start);

            mediaPlayer.setStartTime(Duration.seconds(start));
            mediaPlayer.setStopTime(Duration.seconds(end));

            mediaPlayer.currentTimeProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) {
                    currentSeconds = newV.toSeconds();
                    double elapsedInSpan = Math.max(0, Math.min(span, currentSeconds - start));
                    double progress = elapsedInSpan / span;
                    progressBar.setProgress(progress);
                    timeElapsedLabel.setText(String.format("%.0fs / %ds", elapsedInSpan, span));
                }
            });

            // Rule 3: End of media -> reset to start time and pause (no loop)
            mediaPlayer.setOnEndOfMedia(() -> {
                isPlaying = false;
                mediaPlayer.pause();
                mediaPlayer.seek(Duration.seconds(start));
                progressBar.setProgress(0);
                timeElapsedLabel.setText(String.format("0s / %ds", span));
                updatePlayPauseButton();
            });

            mediaPlayer.setOnError(() -> {
                isPlaying = false;
                updatePlayPauseButton();
            });

        } catch (Exception ex) {
            System.err.println("Could not initialize MediaPlayer for " + currentTrack.name + ": " + ex.getMessage());
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null || currentTrack == null) return;

        if (isPlaying) {
            com.gameshowcenter.offline.sound.SoundManager.getInstance().pauseActiveMedia();
            isPlaying = false;
        } else {
            com.gameshowcenter.offline.sound.SoundManager.getInstance().playMedia(mediaPlayer);
            isPlaying = true;
        }
        updatePlayPauseButton();
    }

    private void handleReplay() {
        if (mediaPlayer == null || currentTrack == null) return;
        mediaPlayer.seek(Duration.seconds(currentTrack.startTime));
        com.gameshowcenter.offline.sound.SoundManager.getInstance().playMedia(mediaPlayer);
        isPlaying = true;
        updatePlayPauseButton();
    }

    private void updatePlayPauseButton() {
        if (isPlaying) {
            playPauseBtn.setText(I18n.get("game.rhythm.btn_pause"));
            playPauseBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 10px 28px; -fx-background-radius: 12px; -fx-cursor: hand;");
            if (equalizerAnimation != null) equalizerAnimation.play();
        } else {
            playPauseBtn.setText(I18n.get("game.rhythm.btn_play"));
            playPauseBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 10px 28px; -fx-background-radius: 12px; -fx-cursor: hand;");
        }
    }

    private void toggleAnswerReveal() {
        isAnswerRevealed = !isAnswerRevealed;
        updateAnswerCard();
    }

    private void updateAnswerCard() {
        if (isAnswerRevealed) {
            answerTextLabel.setText("✨ " + (currentTrack != null ? currentTrack.answer : "No answer"));
            answerTextLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #34d399;");
            answerStatusDesc.setText(I18n.get("game.common.answer_revealed"));
            answerStatusDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            revealAnswerBtn.setText(I18n.get("game.common.hide_answer"));
            revealAnswerBtn.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 8px 24px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-border-color: rgba(52, 211, 153, 0.3); -fx-border-radius: 10px;");
        } else {
            answerTextLabel.setText("🔒 ••••••••••••••••••••");
            answerTextLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #94a3b8;");
            answerStatusDesc.setText(I18n.get("game.rhythm.answer_hidden_desc"));
            answerStatusDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
            revealAnswerBtn.setText(I18n.get("game.common.reveal_answer"));
            revealAnswerBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 8px 24px; -fx-background-radius: 10px; -fx-cursor: hand;",
                    ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        }
    }

    private void handleNextSong() {
        stopMediaPlayer();

        if (roundNumber >= totalMatchRounds) {
            isMatchFinished = true;
            updateUI();
            return;
        }

        if (workingPool.isEmpty()) {
            workingPool.addAll(initialPool);
        }
        int idx = random.nextInt(workingPool.size());
        currentTrack = workingPool.remove(idx);
        roundNumber++;
        isAnswerRevealed = false;

        updateUI();
        loadMediaForCurrentTrack();
    }

    private void stopMediaPlayer() {
        isPlaying = false;
        if (equalizerAnimation != null) equalizerAnimation.stop();
        com.gameshowcenter.offline.sound.SoundManager.getInstance().stopAll();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        updatePlayPauseButton();
    }

    private void updateUI() {
        roundBadgeLabel.setText(String.format(I18n.get("game.rhythm.title_song"), Math.min(roundNumber, totalMatchRounds), totalMatchRounds));
        poolInfoLabel.setText(String.format(I18n.get("game.rhythm.unseen_pool"), workingPool.size()));

        dynamicContainer.getChildren().clear();

        if (isMatchFinished) {
            dynamicContainer.getChildren().add(matchFinishedBox);
            return;
        }

        dynamicContainer.getChildren().add(activePlayBox);

        if (currentTrack != null) {
            segmentLabel.setText(String.format("⏱️ %ds ➔ %ds (%ds Clip)", currentTrack.startTime, currentTrack.endTime, currentTrack.spanTime));
            timeElapsedLabel.setText(String.format("0s / %ds", currentTrack.spanTime));
            progressBar.setProgress(0);
        }

        updateAnswerCard();
        nextSongBtn.setText(roundNumber >= totalMatchRounds ? "🏁 Finish Match" : "Next Song ➔");
    }
}
