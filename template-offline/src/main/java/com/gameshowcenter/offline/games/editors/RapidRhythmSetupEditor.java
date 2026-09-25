package com.gameshowcenter.offline.games.editors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.util.FileChooserHelper;
import com.gameshowcenter.offline.util.ImageLoaderHelper;
import com.gameshowcenter.offline.util.SvgEmoji;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RapidRhythmSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<TrackEntry> trackEntries = new ArrayList<>();

    private Spinner<Integer> roundsSpinner;
    private VBox tracksListPanel;
    private Label poolCountLabel;

    // Active Preview MediaPlayer
    private MediaPlayer previewPlayer;
    private Button currentPreviewBtn;
    private CheckBox battleRoyaleCheckBox;

    public static class TrackEntry {
        public String id;
        public String audioUrl;
        public int duration = 30;
        public TextField nameField;
        public TextField answerField;
        public Spinner<Integer> startSpinner;
        public Spinner<Integer> endSpinner;
        public Label spanInfoLabel;
        public Button previewBtn;
    }

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        stopPreview();
        trackEntries.clear();

        int curRounds = 1;
        boolean curBattleRoyale = false;

        if (currentSetup != null) {
            if (currentSetup.has("battleRoyale")) curBattleRoyale = currentSetup.get("battleRoyale").asBoolean(false);
            else if (currentSetup.has("battle_royale")) curBattleRoyale = currentSetup.get("battle_royale").asBoolean(false);

            if (currentSetup.has("rounds_per_player")) curRounds = currentSetup.get("rounds_per_player").asInt(1);
            else if (currentSetup.has("roundsPerPlayer")) curRounds = currentSetup.get("roundsPerPlayer").asInt(1);
        }

        VBox root = new VBox(14);
        root.setPadding(new Insets(10));

        // 1. HEADER DESCRIPTION
        Label descLabel = new Label(I18n.get("game.editor.rhythm.desc"));
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        // Battle Royale Toggle Row
        battleRoyaleCheckBox = new CheckBox(I18n.get("game.editor.battleroyale.check"));
        SvgEmoji.setGraphic(battleRoyaleCheckBox, "swords", 16);
        battleRoyaleCheckBox.setSelected(curBattleRoyale);
        battleRoyaleCheckBox.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: 900; -fx-font-size: 12px; -fx-cursor: hand;");

        Label brHint = new Label(I18n.get("game.editor.battleroyale.hint"));
        brHint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-wrap-text: true;");

        VBox brCard = new VBox(4);
        brCard.setPadding(new Insets(8, 12, 8, 12));
        brCard.setStyle("-fx-background-color: rgba(245, 158, 11, 0.08); -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 8px; -fx-background-radius: 8px;");
        brCard.getChildren().addAll(battleRoyaleCheckBox, brHint);

        // 2. PARAMETERS ROW
        HBox paramsRow = new HBox(20);
        paramsRow.setAlignment(Pos.CENTER_LEFT);

        HBox roundsBox = new HBox(8);
        roundsBox.setAlignment(Pos.CENTER_LEFT);
        Label rLabel = new Label(I18n.get("game.editor.rounds_per_player"));
        rLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 12px;");
        roundsSpinner = new Spinner<>(1, 20, curRounds, 1);
        roundsSpinner.setEditable(true);
        roundsSpinner.setPrefWidth(75);
        roundsSpinner.setDisable(curBattleRoyale);
        roundsBox.getChildren().addAll(rLabel, roundsSpinner);

        battleRoyaleCheckBox.setOnAction(e -> {
            roundsSpinner.setDisable(battleRoyaleCheckBox.isSelected());
        });

        paramsRow.getChildren().add(roundsBox);

        // 3. TRACK POOL HEADER
        HBox poolHeader = new HBox(12);
        poolHeader.setAlignment(Pos.CENTER_LEFT);

        Label poolTitle = new Label(I18n.get("game.editor.rhythm.pool_title"));
        poolTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #f59e0b;");

        poolCountLabel = new Label(String.format(I18n.get("game.editor.rhythm.tracks_count"), 0));
        poolCountLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        poolHeader.getChildren().addAll(poolTitle, poolCountLabel);

        // 4. ADD CONTROLS ROW
        HBox addControlsRow = new HBox(10);
        addControlsRow.setAlignment(Pos.CENTER_LEFT);

        Button chooseFilesBtn = new Button(I18n.get("game.editor.choose_files"));
        chooseFilesBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8px 14px; -fx-background-radius: 8px; -fx-border-color: #6366f1; -fx-border-style: dashed; -fx-border-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        chooseFilesBtn.setOnAction(e -> {
            FileChooser chooser = FileChooserHelper.createChooser("Select Rapid Rhythm Audio Files");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files (*.mp3, *.wav, *.m4a, *.aac, *.ogg)", "*.mp3", "*.wav", "*.m4a", "*.aac", "*.ogg"));
            List<File> files = chooser.showOpenMultipleDialog(chooseFilesBtn.getScene().getWindow());
            if (files != null && !files.isEmpty()) {
                for (File f : files) {
                    addTrackRow(f.toURI().toString(), f.getName().replaceFirst("[.][^.]+$", ""), f.getName().replaceFirst("[.][^.]+$", ""), 0, 30, 30);
                }
                updatePoolCount();
            }
        });

        TextField urlInput = new TextField();
        urlInput.setPromptText(I18n.get("game.editor.rhythm.paste_audio_url"));
        urlInput.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8px; -fx-padding: 8px 12px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        HBox.setHgrow(urlInput, Priority.ALWAYS);

        TextField ansInput = new TextField();
        ansInput.setPromptText(I18n.get("game.editor.rhythm.answer_mandatory_placeholder"));
        ansInput.setStyle("-fx-background-color: #0f121d; -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-border-color: rgba(52, 211, 153, 0.4); -fx-border-radius: 8px; -fx-padding: 8px 12px; -fx-font-weight: bold;");
        ansInput.setPrefWidth(160);

        Button addUrlBtn = new Button(I18n.get("game.editor.add_url"));
        addUrlBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8px 14px; -fx-background-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));

        Runnable handleAddUrl = () -> {
            String u = urlInput.getText().trim();
            String a = ansInput.getText().trim();
            if (u.isEmpty()) return;
            if (a.isEmpty()) a = "Song #" + (trackEntries.size() + 1);
            addTrackRow(u, "Track #" + (trackEntries.size() + 1), a, 0, 30, 30);
            urlInput.clear();
            ansInput.clear();
            updatePoolCount();
        };
        addUrlBtn.setOnAction(e -> handleAddUrl.run());
        urlInput.setOnAction(e -> handleAddUrl.run());
        ansInput.setOnAction(e -> handleAddUrl.run());

        addControlsRow.getChildren().addAll(chooseFilesBtn, urlInput, ansInput, addUrlBtn);

        // 5. TRACKS LIST CONTAINER
        tracksListPanel = new VBox(10);
        tracksListPanel.setPadding(new Insets(10));
        tracksListPanel.setStyle("-fx-background-color: #090c14; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 12px; -fx-background-radius: 12px;");

        ScrollPane scroll = new ScrollPane(tracksListPanel);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(320);
        scroll.setMaxHeight(360);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Parse initial setup tracks
        if (currentSetup != null) {
            JsonNode tracksNode = currentSetup.has("tracks") ? currentSetup.get("tracks")
                    : (currentSetup.has("media_pool") ? currentSetup.get("media_pool") : null);

            if (tracksNode != null && tracksNode.isArray() && tracksNode.size() > 0) {
                for (JsonNode t : tracksNode) {
                    if (t.isObject()) {
                        String u = t.has("audioUrl") ? t.get("audioUrl").asText() : "";
                        String name = t.has("name") ? t.get("name").asText() : "Track";
                        String ans = t.has("answer") ? t.get("answer").asText() : "";
                        int start = t.has("startTime") ? t.get("startTime").asInt(0) : 0;
                        int end = t.has("endTime") ? t.get("endTime").asInt(30) : 30;
                        int dur = t.has("duration") ? t.get("duration").asInt(30) : 30;
                        if (!u.isBlank()) {
                            addTrackRow(u, name, ans, start, end, dur);
                        }
                    } else if (t.isTextual()) {
                        String u = t.asText();
                        if (!u.isBlank()) {
                            addTrackRow(u, "Track", "Answer", 0, 30, 30);
                        }
                    }
                }
            }
        }

        // If empty, add standard defaults
        if (trackEntries.isEmpty()) {
            addTrackRow("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "Synthwave Groove #1", "SoundHelix Track 1", 0, 30, 372);
            addTrackRow("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "Electropop Beat #2", "SoundHelix Track 2", 0, 30, 423);
        }

        updatePoolCount();

        root.getChildren().addAll(descLabel, brCard, paramsRow, new Separator(), poolHeader, addControlsRow, scroll);
        return root;
    }

    private void addTrackRow(String url, String name, String answer, int start, int end, int duration) {
        TrackEntry entry = new TrackEntry();
        entry.id = "track_" + System.currentTimeMillis() + "_" + trackEntries.size();
        entry.audioUrl = url;
        entry.duration = Math.max(1, duration);

        VBox card = new VBox(8);
        card.setPadding(new Insets(10));
        card.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 10px; -fx-background-radius: 10px;",
                ThemeManager.getCardHex()));

        // TOP ROW: INDEX, NAME, PREVIEW, DELETE
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label numLbl = new Label("#" + (trackEntries.size() + 1));
        numLbl.setStyle("-fx-background-color: rgba(168, 85, 247, 0.2); -fx-text-fill: #c084fc; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-radius: 6px;");

        entry.nameField = new TextField(name);
        entry.nameField.setPromptText(I18n.get("game.editor.rhythm.track_title_placeholder"));
        entry.nameField.setStyle("-fx-background-color: #0f121d; -fx-text-fill: #a5b4fc; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6px; -fx-padding: 4px 8px;");
        HBox.setHgrow(entry.nameField, Priority.ALWAYS);

        entry.previewBtn = new Button(I18n.get("game.editor.preview"));
        entry.previewBtn.setStyle("-fx-background-color: rgba(99, 102, 241, 0.25); -fx-text-fill: #a5b4fc; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 5px 10px; -fx-background-radius: 6px; -fx-cursor: hand;");
        entry.previewBtn.setOnAction(e -> togglePreview(entry));

        Button delBtn = new Button();
        SvgEmoji.setGraphic(delBtn, "close", 10);
        delBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #ef4444; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 5px 8px; -fx-background-radius: 6px; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            stopPreview();
            trackEntries.remove(entry);
            tracksListPanel.getChildren().remove(card);
            updatePoolCount();
        });

        topRow.getChildren().addAll(numLbl, entry.nameField, entry.previewBtn, delBtn);

        // MIDDLE ROW: MANDATORY ANSWER
        HBox ansRow = new HBox(8);
        ansRow.setAlignment(Pos.CENTER_LEFT);

        Label aLbl = new Label(I18n.get("game.editor.rhythm.answer_field_label"));
        aLbl.setStyle("-fx-text-fill: #34d399; -fx-font-weight: 900; -fx-font-size: 11px;");

        entry.answerField = new TextField(answer);
        entry.answerField.setPromptText(I18n.get("game.editor.rhythm.answer_placeholder"));
        entry.answerField.setStyle("-fx-background-color: #0f121d; -fx-text-fill: #6ee7b7; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: rgba(52, 211, 153, 0.3); -fx-border-radius: 6px; -fx-padding: 4px 8px;");
        HBox.setHgrow(entry.answerField, Priority.ALWAYS);

        ansRow.getChildren().addAll(aLbl, entry.answerField);

        // BOTTOM ROW: TIME RANGE SPINNERS (START TIME & END TIME)
        HBox timeRow = new HBox(12);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        timeRow.setStyle("-fx-background-color: #0f121d; -fx-padding: 6px 10px; -fx-background-radius: 6px;");

        Label startLbl = new Label(I18n.get("game.editor.rhythm.start_secs"));
        startLbl.setStyle("-fx-text-fill: #a5b4fc; -fx-font-size: 10px; -fx-font-weight: bold;");
        entry.startSpinner = new Spinner<>(0, Math.max(0, entry.duration - 1), Math.min(start, entry.duration - 1), 1);
        entry.startSpinner.setPrefWidth(65);
        entry.startSpinner.setEditable(true);

        Label endLbl = new Label(I18n.get("game.editor.rhythm.end_secs"));
        endLbl.setStyle("-fx-text-fill: #c084fc; -fx-font-size: 10px; -fx-font-weight: bold;");
        entry.endSpinner = new Spinner<>(1, Math.max(1, entry.duration), Math.max(start + 1, Math.min(end, entry.duration)), 1);
        entry.endSpinner.setPrefWidth(65);
        entry.endSpinner.setEditable(true);

        entry.spanInfoLabel = new Label();
        entry.spanInfoLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 10px; -fx-font-weight: bold;");
        updateSpanInfo(entry);

        entry.startSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && entry.endSpinner.getValue() != null && entry.endSpinner.getValue() <= newV) {
                entry.endSpinner.getValueFactory().setValue(Math.min(entry.duration, newV + 1));
            }
            updateSpanInfo(entry);
        });

        entry.endSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && entry.startSpinner.getValue() != null && entry.startSpinner.getValue() >= newV) {
                entry.startSpinner.getValueFactory().setValue(Math.max(0, newV - 1));
            }
            updateSpanInfo(entry);
        });

        timeRow.getChildren().addAll(startLbl, entry.startSpinner, endLbl, entry.endSpinner, entry.spanInfoLabel);

        card.getChildren().addAll(topRow, ansRow, timeRow);
        trackEntries.add(entry);
        tracksListPanel.getChildren().add(card);

        // Async read duration if possible
        detectAudioDuration(entry);
    }

    private void updateSpanInfo(TrackEntry entry) {
        if (entry.spanInfoLabel != null && entry.startSpinner != null && entry.endSpinner != null) {
            int s = entry.startSpinner.getValue() != null ? entry.startSpinner.getValue() : 0;
            int e = entry.endSpinner.getValue() != null ? entry.endSpinner.getValue() : 30;
            int span = Math.max(0, e - s);
            entry.spanInfoLabel.setText(I18n.get("game.editor.rhythm.segment_prefix") + s + "s - " + e + "s (" + span + "s clip)");
        }
    }

    private void detectAudioDuration(TrackEntry entry) {
        new Thread(() -> {
            try {
                Media media = new Media(entry.audioUrl);
                MediaPlayer tempPlayer = new MediaPlayer(media);
                tempPlayer.setOnReady(() -> {
                    double durSeconds = media.getDuration().toSeconds();
                    if (durSeconds > 0 && !Double.isNaN(durSeconds)) {
                        int dur = (int) Math.round(durSeconds);
                        entry.duration = dur;
                        Platform.runLater(() -> {
                            int curStart = entry.startSpinner.getValue() != null ? entry.startSpinner.getValue() : 0;
                            int curEnd = entry.endSpinner.getValue() != null ? entry.endSpinner.getValue() : Math.min(30, dur);

                            SpinnerValueFactory.IntegerSpinnerValueFactory startFactory =
                                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Math.max(0, dur - 1), Math.min(curStart, dur - 1));
                            entry.startSpinner.setValueFactory(startFactory);

                            SpinnerValueFactory.IntegerSpinnerValueFactory endFactory =
                                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, dur, Math.max(curStart + 1, Math.min(curEnd, dur)));
                            entry.endSpinner.setValueFactory(endFactory);

                            updateSpanInfo(entry);
                        });
                    }
                    tempPlayer.dispose();
                });
                tempPlayer.setOnError(tempPlayer::dispose);
            } catch (Exception ignored) {}
        }).start();
    }

    private void togglePreview(TrackEntry entry) {
        if (previewPlayer != null && currentPreviewBtn == entry.previewBtn) {
            stopPreview();
            return;
        }

        stopPreview();

        try {
            Media media = new Media(entry.audioUrl);
            previewPlayer = new MediaPlayer(media);
            currentPreviewBtn = entry.previewBtn;

            int startSec = entry.startSpinner.getValue() != null ? entry.startSpinner.getValue() : 0;
            int endSec = entry.endSpinner.getValue() != null ? entry.endSpinner.getValue() : 30;

            previewPlayer.setStartTime(Duration.seconds(startSec));
            previewPlayer.setStopTime(Duration.seconds(endSec));

            previewPlayer.setOnEndOfMedia(() -> stopPreview());
            previewPlayer.setOnError(() -> stopPreview());

            previewPlayer.play();
            com.gameshowcenter.offline.sound.SoundManager.getInstance().playMedia(previewPlayer);
            entry.previewBtn.setText(I18n.get("game.editor.stop"));
            entry.previewBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 5px 10px; -fx-background-radius: 6px; -fx-cursor: hand;");
        } catch (Exception ex) {
            stopPreview();
        }
    }

    private void stopPreview() {
        com.gameshowcenter.offline.sound.SoundManager.getInstance().stopAll();
        if (previewPlayer != null) {
            try {
                previewPlayer.stop();
                previewPlayer.dispose();
            } catch (Exception ignored) {}
            previewPlayer = null;
        }
        if (currentPreviewBtn != null) {
            currentPreviewBtn.setText(I18n.get("game.editor.preview"));
            currentPreviewBtn.setStyle("-fx-background-color: rgba(99, 102, 241, 0.25); -fx-text-fill: #a5b4fc; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 5px 10px; -fx-background-radius: 6px; -fx-cursor: hand;");
            currentPreviewBtn = null;
        }
    }

    private void updatePoolCount() {
        if (poolCountLabel != null) {
            poolCountLabel.setText(String.format(I18n.get("game.editor.rhythm.tracks_count"), trackEntries.size()));
        }
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "Rapid_Rhythm");
        root.put("battleRoyale", battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());
        int rpp = roundsSpinner != null ? roundsSpinner.getValue() : 1;
        root.put("rounds_per_player", rpp);
        root.put("roundsPerPlayer", rpp);

        ArrayNode tracksArr = root.putArray("tracks");
        ArrayNode mediaArr = root.putArray("media_pool");

        for (TrackEntry t : trackEntries) {
            ObjectNode tObj = objectMapper.createObjectNode();
            tObj.put("id", t.id);
            tObj.put("name", t.nameField.getText().trim().isEmpty() ? "Track" : t.nameField.getText().trim());
            tObj.put("audioUrl", t.audioUrl);
            tObj.put("answer", t.answerField.getText().trim());

            int start = t.startSpinner != null && t.startSpinner.getValue() != null ? t.startSpinner.getValue() : 0;
            int end = t.endSpinner != null && t.endSpinner.getValue() != null ? t.endSpinner.getValue() : 30;

            tObj.put("startTime", start);
            tObj.put("endTime", end);
            tObj.put("spanTime", Math.max(1, end - start));
            tObj.put("duration", t.duration);

            tracksArr.add(tObj);
            mediaArr.add(t.audioUrl);
        }

        return root;
    }

    @Override
    public String validateSetup(List<Competitor> profiles) {
        return validateSetupData(getUpdatedSetup(), profiles);
    }

    @Override
    public String validateSetup(List<Competitor> profiles, boolean battleRoyale) {
        return validateSetupData(getUpdatedSetup(), profiles, battleRoyale);
    }

    @Override
    public String validateSetupData(JsonNode setupData, List<Competitor> profiles) {
        boolean br = setupData != null && (
            (setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean(false)) ||
            (setupData.has("battle_royale") && setupData.get("battle_royale").asBoolean(false))
        );
        return validateSetupData(setupData, profiles, br);
    }

    @Override
    public String validateSetupData(JsonNode setupData, List<Competitor> profiles, boolean battleRoyale) {
        if (setupData == null) {
            return "Rapid Rhythm configuration is missing.";
        }

        boolean isBr = battleRoyale || (setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean(false))
                || (setupData.has("battle_royale") && setupData.get("battle_royale").asBoolean(false));

        int rpp = 1;
        if (setupData.has("rounds_per_player")) rpp = setupData.get("rounds_per_player").asInt(1);
        else if (setupData.has("roundsPerPlayer")) rpp = setupData.get("roundsPerPlayer").asInt(1);

        int compCount = (profiles != null && !profiles.isEmpty()) ? profiles.size() : 1;
        int requiredTracks = isBr ? 1 : (compCount * rpp);

        JsonNode tracksNode = setupData.has("tracks") ? setupData.get("tracks")
                : (setupData.has("media_pool") ? setupData.get("media_pool") : null);

        if (tracksNode == null || !tracksNode.isArray() || tracksNode.size() == 0) {
            return isBr
                    ? "Rapid Rhythm requires at least 1 audio track. None are added."
                    : ("Rapid Rhythm requires at least " + requiredTracks + " audio track(s). None are added.");
        }

        if (!isBr && tracksNode.size() < requiredTracks) {
            return "Rapid Rhythm requires at least " + requiredTracks + " track(s) for " + compCount + " competitor(s) (" + rpp + " round/player), but only " + tracksNode.size() + " track(s) are configured.";
        }

        // Validate each track has non-empty answer and valid range
        for (int i = 0; i < tracksNode.size(); i++) {
            JsonNode tNode = tracksNode.get(i);
            String u = "";
            String answer = "";

            if (tNode.isObject()) {
                u = tNode.has("audioUrl") ? tNode.get("audioUrl").asText().trim() : "";
                answer = tNode.has("answer") ? tNode.get("answer").asText().trim() : "";
            } else if (tNode.isTextual()) {
                u = tNode.asText().trim();
                answer = "Answer";
            }

            if (u.isEmpty()) {
                return "Rapid Rhythm: Track #" + (i + 1) + " contains an empty audio source URL.";
            }

            if (answer.isEmpty()) {
                return "Rapid Rhythm: Track #" + (i + 1) + " is missing a mandatory Answer. Please provide an answer for every track.";
            }

            // Accessibility check
            if (u.startsWith("file:")) {
                try {
                    File localFile = new File(new URI(u));
                    if (!localFile.exists()) {
                        return "Cannot access offline audio track #" + (i + 1) + ": File not found (" + localFile.getName() + ").";
                    }
                } catch (Exception ex) {
                    return "Invalid local file path for track #" + (i + 1) + ": " + u;
                }
            } else if (!u.startsWith("http://") && !u.startsWith("https://")) {
                File localFile = ImageLoaderHelper.resolveLocalFile(u);
                if (localFile == null || !localFile.exists()) {
                    return "Cannot access offline audio track #" + (i + 1) + ": File not found (" + u + ").";
                }
            } else {
                boolean reachable = ImageLoaderHelper.isUrlReachable(u);
                if (!reachable) {
                    return "Cannot access online audio track #" + (i + 1) + " (unreachable or offline). Please check internet connection or select local audio files.";
                }
            }
        }

        return null;
    }
}
