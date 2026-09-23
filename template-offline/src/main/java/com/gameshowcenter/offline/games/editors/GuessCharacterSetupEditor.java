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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GuessCharacterSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> mediaPool = new ArrayList<>();
    private final java.util.Set<String> copyrightWarnings = new java.util.HashSet<>();

    private Spinner<Integer> roundsSpinner;
    private CheckBox enableTimerCheckBox;
    private Spinner<Integer> timerSecondsSpinner;
    private FlowPane thumbnailsFlow;
    private Label poolCountLabel;

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        mediaPool.clear();
        copyrightWarnings.clear();

        if (currentSetup != null && currentSetup.has("copyright_warnings") && currentSetup.get("copyright_warnings").isArray()) {
            for (JsonNode w : currentSetup.get("copyright_warnings")) {
                copyrightWarnings.add(w.asText());
            }
        }

        int curRounds = 3;
        boolean curEnableTimer = true;
        int curTimerSecs = 30;

        if (currentSetup != null) {
            if (currentSetup.has("rounds_per_player")) curRounds = currentSetup.get("rounds_per_player").asInt(3);
            else if (currentSetup.has("roundsPerPlayer")) curRounds = currentSetup.get("roundsPerPlayer").asInt(3);

            if (currentSetup.has("enable_timer")) curEnableTimer = currentSetup.get("enable_timer").asBoolean(true);
            else if (currentSetup.has("enableTimer")) curEnableTimer = currentSetup.get("enableTimer").asBoolean(true);

            if (currentSetup.has("timer_duration_seconds")) curTimerSecs = currentSetup.get("timer_duration_seconds").asInt(30);
            else if (currentSetup.has("timerDurationSeconds")) curTimerSecs = currentSetup.get("timerDurationSeconds").asInt(30);

            JsonNode poolNode = currentSetup.has("media_pool") ? currentSetup.get("media_pool")
                    : (currentSetup.has("mediaPool") ? currentSetup.get("mediaPool") : null);
            if (poolNode != null && poolNode.isArray()) {
                for (JsonNode item : poolNode) {
                    String url = item.asText();
                    if (url != null && !url.isBlank()) {
                        mediaPool.add(url);
                    }
                }
            }
        }

        VBox root = new VBox(14);
        root.setPadding(new Insets(10));

        // 1. HEADER DESCRIPTION
        Label descLabel = new Label(I18n.get("game.editor.guesscharacter.desc"));
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        // 2. PARAMETERS ROW
        HBox paramsRow = new HBox(20);
        paramsRow.setAlignment(Pos.CENTER_LEFT);

        // Rounds per Player
        HBox roundsBox = new HBox(8);
        roundsBox.setAlignment(Pos.CENTER_LEFT);
        Label rLabel = new Label(I18n.get("game.editor.rounds_per_player"));
        rLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 12px;");
        roundsSpinner = new Spinner<>(1, 20, curRounds, 1);
        roundsSpinner.setEditable(true);
        roundsSpinner.setPrefWidth(75);
        roundsBox.getChildren().addAll(rLabel, roundsSpinner);

        // Set Timer Checkbox
        enableTimerCheckBox = new CheckBox(I18n.get("game.editor.guesscharacter.set_timer"));
        enableTimerCheckBox.setSelected(curEnableTimer);
        enableTimerCheckBox.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");

        // Timer Seconds
        HBox timerSecsBox = new HBox(8);
        timerSecsBox.setAlignment(Pos.CENTER_LEFT);
        Label tLabel = new Label(I18n.get("game.editor.duration_seconds"));
        tLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 12px;");
        timerSecondsSpinner = new Spinner<>(5, 300, curTimerSecs, 5);
        timerSecondsSpinner.setEditable(true);
        timerSecondsSpinner.setPrefWidth(80);
        timerSecsBox.getChildren().addAll(tLabel, timerSecondsSpinner);

        timerSecsBox.visibleProperty().bind(enableTimerCheckBox.selectedProperty());
        timerSecsBox.managedProperty().bind(enableTimerCheckBox.selectedProperty());

        paramsRow.getChildren().addAll(roundsBox, enableTimerCheckBox, timerSecsBox);

        // 3. IMAGE POOL SECTION HEADER
        HBox poolHeader = new HBox(12);
        poolHeader.setAlignment(Pos.CENTER_LEFT);

        Label poolTitle = new Label(I18n.get("game.editor.guesscharacter.pool_title"));
        poolTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #f59e0b;");

        poolCountLabel = new Label("(" + mediaPool.size() + " images)");
        poolCountLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        poolHeader.getChildren().addAll(poolTitle, poolCountLabel);

        // 4. ADD URL / CHOOSE FILES ROW
        HBox addControlsRow = new HBox(10);
        addControlsRow.setAlignment(Pos.CENTER_LEFT);

        Button chooseFilesBtn = new Button(I18n.get("game.editor.choose_files"));
        chooseFilesBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8px 14px; -fx-background-radius: 8px; -fx-border-color: #6366f1; -fx-border-style: dashed; -fx-border-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        chooseFilesBtn.setOnAction(e -> {
            FileChooser chooser = FileChooserHelper.createChooser("Select Character Clue Images");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.webp, *.gif, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp"));
            List<File> files = chooser.showOpenMultipleDialog(chooseFilesBtn.getScene().getWindow());
            if (files != null && !files.isEmpty()) {
                for (File f : files) {
                    mediaPool.add(f.toURI().toString());
                }
                renderThumbnails();
            }
        });

        TextField urlInput = new TextField();
        urlInput.setPromptText(I18n.get("game.editor.guesscharacter.paste_url"));
        urlInput.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8px; -fx-padding: 8px 12px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        HBox.setHgrow(urlInput, Priority.ALWAYS);

        Button addUrlBtn = new Button(I18n.get("game.editor.add_url"));
        addUrlBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8px 14px; -fx-background-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));

        Runnable handleAddUrl = () -> {
            String u = urlInput.getText().trim();
            if (u.isEmpty()) return;
            mediaPool.add(u);
            urlInput.clear();
            renderThumbnails();
        };
        addUrlBtn.setOnAction(e -> handleAddUrl.run());
        urlInput.setOnAction(e -> handleAddUrl.run());

        addControlsRow.getChildren().addAll(chooseFilesBtn, urlInput, addUrlBtn);

        // 5. THUMBNAILS GRID
        thumbnailsFlow = new FlowPane(10, 10);
        thumbnailsFlow.setAlignment(Pos.TOP_LEFT);
        thumbnailsFlow.setPadding(new Insets(10));
        thumbnailsFlow.setStyle("-fx-background-color: #090c14; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 12px; -fx-background-radius: 12px;");

        ScrollPane scroll = new ScrollPane(thumbnailsFlow);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);
        scroll.setMaxHeight(320);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        renderThumbnails();

        root.getChildren().addAll(descLabel, paramsRow, new Separator(), poolHeader, addControlsRow, scroll);
        return root;
    }

    private void renderThumbnails() {
        if (thumbnailsFlow == null) return;
        thumbnailsFlow.getChildren().clear();
        if (poolCountLabel != null) poolCountLabel.setText("(" + mediaPool.size() + " images)");

        if (mediaPool.isEmpty()) {
            Label emptyLbl = new Label(I18n.get("game.editor.guesscharacter.pool_empty"));
            emptyLbl.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 20px;");
            thumbnailsFlow.getChildren().add(emptyLbl);
            return;
        }

        for (int i = 0; i < mediaPool.size(); i++) {
            final int index = i;
            String imgUrl = mediaPool.get(i);

            StackPane thumbCard = new StackPane();
            thumbCard.setPrefSize(85, 85);
            thumbCard.setMaxSize(85, 85);
            thumbCard.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 10px; -fx-background-radius: 10px;",
                    ThemeManager.getCardHex()));

            ImageView iv = new ImageView();
            iv.setFitWidth(85);
            iv.setFitHeight(85);
            iv.setPreserveRatio(true);
            thumbCard.getChildren().add(iv);

            ImageLoaderHelper.loadImageAsync(imgUrl, 85, 85, true, true,
                    iv::setImage,
                    () -> {
                        Label err = new Label("🎭");
                        err.setStyle("-fx-font-size: 24px;");
                        thumbCard.getChildren().setAll(err);
                    }
            );

            Label numBadge = new Label("#" + (i + 1));
            numBadge.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #ffffff; -fx-font-size: 9px; -fx-font-weight: 900; -fx-padding: 2px 5px; -fx-background-radius: 4px;");
            StackPane.setAlignment(numBadge, Pos.BOTTOM_LEFT);
            StackPane.setMargin(numBadge, new Insets(3));

            Button delBtn = new Button("✕");
            delBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 9px; -fx-font-weight: 900; -fx-padding: 1px 5px; -fx-background-radius: 10px; -fx-cursor: hand;");
            StackPane.setAlignment(delBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(delBtn, new Insets(3));
            delBtn.setOnAction(e -> {
                mediaPool.remove(index);
                renderThumbnails();
            });

            thumbCard.getChildren().addAll(numBadge, delBtn);

            if (copyrightWarnings.contains(imgUrl)) {
                Label warnBadge = new Label("⚠️");
                warnBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.9); -fx-text-fill: #000; -fx-font-size: 9px; -fx-padding: 1px 3px; -fx-background-radius: 3px; -fx-cursor: hand;");
                Tooltip.install(warnBadge, new Tooltip(I18n.get("settings.ai.copyright_badge")));
                StackPane.setAlignment(warnBadge, Pos.TOP_LEFT);
                StackPane.setMargin(warnBadge, new Insets(3));
                thumbCard.getChildren().add(warnBadge);
            }

            thumbnailsFlow.getChildren().add(thumbCard);
        }
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "Guess_Character");
        root.put("rounds_per_player", roundsSpinner != null ? roundsSpinner.getValue() : 3);
        root.put("enable_timer", enableTimerCheckBox != null ? enableTimerCheckBox.isSelected() : true);
        root.put("timer_seconds", timerSecondsSpinner != null ? timerSecondsSpinner.getValue() : 30);

        ArrayNode arr = root.putArray("media_pool");
        for (String item : mediaPool) {
            arr.add(item);
        }

        if (!copyrightWarnings.isEmpty()) {
            ArrayNode warnArr = root.putArray("copyright_warnings");
            for (String w : copyrightWarnings) {
                warnArr.add(w);
            }
        }

        return root;
    }


    @Override
    public String validateSetup(List<Competitor> profiles) {
        return validateSetupData(getUpdatedSetup(), profiles);
    }

    @Override
    public String validateSetupData(JsonNode setupData, List<Competitor> profiles) {
        if (setupData == null) {
            return "Guess Character configuration is missing.";
        }

        int rpp = 3;
        if (setupData.has("rounds_per_player")) rpp = setupData.get("rounds_per_player").asInt(3);
        else if (setupData.has("roundsPerPlayer")) rpp = setupData.get("roundsPerPlayer").asInt(3);

        int compCount = (profiles != null && !profiles.isEmpty()) ? profiles.size() : 1;
        int requiredImages = compCount * rpp;

        JsonNode poolNode = setupData.has("media_pool") ? setupData.get("media_pool")
                : (setupData.has("mediaPool") ? setupData.get("mediaPool") : null);

        if (poolNode == null || !poolNode.isArray() || poolNode.size() == 0) {
            return "Guess Character requires at least " + requiredImages + " character image(s). None are added.";
        }

        if (poolNode.size() < requiredImages) {
            return "Guess Character requires at least " + requiredImages + " image(s) for " + compCount + " competitor(s) (" + rpp + " round/player), but only " + poolNode.size() + " image(s) are configured.";
        }

        // Image accessibility & offline verification
        for (int i = 0; i < poolNode.size(); i++) {
            String imgStr = poolNode.get(i).asText().trim();
            if (imgStr.isEmpty()) {
                return "Character image #" + (i + 1) + " contains an empty reference.";
            }

            if (imgStr.startsWith("file:")) {
                try {
                    File localFile = new File(new URI(imgStr));
                    if (!localFile.exists()) {
                        return "Cannot access offline character image #" + (i + 1) + ": File not found (" + localFile.getName() + "). Please configure local offline images in Game Settings.";
                    }
                } catch (Exception ex) {
                    return "Invalid local file path for image #" + (i + 1) + ": " + imgStr;
                }
            } else if (!imgStr.startsWith("http://") && !imgStr.startsWith("https://")) {
                File localFile = ImageLoaderHelper.resolveLocalFile(imgStr);
                if (localFile == null || !localFile.exists()) {
                    return "Cannot access offline character image #" + (i + 1) + ": File not found (" + imgStr + "). Please configure local offline images in Game Settings.";
                }
            } else {
                // Online URL
                boolean reachable = ImageLoaderHelper.isUrlReachable(imgStr);
                if (!reachable) {
                    return "Cannot access online character image #" + (i + 1) + " (unreachable or offline). Please connect to the internet or configure local offline images in Game Settings.";
                }
            }
        }

        return null;
    }
}
