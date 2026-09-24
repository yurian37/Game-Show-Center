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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SnapSolveSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> mediaPool = new ArrayList<>();
    private final java.util.Set<String> copyrightWarnings = new java.util.HashSet<>();

    private Spinner<Integer> roundsSpinner;
    private CheckBox battleRoyaleCheckBox;
    private CheckBox displacementCheck;
    private CheckBox swirlCheck;
    private CheckBox pixelateCheck;
    private CheckBox blurCheck;
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

        int curRounds = 2;
        boolean curBattleRoyale = false;
        List<String> curFilters = new ArrayList<>(Arrays.asList("displacement", "swirl", "pixelate", "blur"));

        if (currentSetup != null) {
            if (currentSetup.has("battleRoyale")) curBattleRoyale = currentSetup.get("battleRoyale").asBoolean(false);
            else if (currentSetup.has("battle_royale")) curBattleRoyale = currentSetup.get("battle_royale").asBoolean(false);

            if (currentSetup.has("rounds_per_player")) curRounds = currentSetup.get("rounds_per_player").asInt(2);
            else if (currentSetup.has("roundsPerPlayer")) curRounds = currentSetup.get("roundsPerPlayer").asInt(2);

            JsonNode filtersNode = currentSetup.has("selected_filters") ? currentSetup.get("selected_filters")
                    : (currentSetup.has("selectedFilters") ? currentSetup.get("selectedFilters") : null);
            if (filtersNode != null && filtersNode.isArray() && filtersNode.size() > 0) {
                curFilters.clear();
                for (JsonNode f : filtersNode) {
                    curFilters.add(f.asText());
                }
            }

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
        Label descLabel = new Label(I18n.get("game.editor.snapsolve.desc"));
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        // Battle Royale Toggle Row
        battleRoyaleCheckBox = new CheckBox(I18n.get("game.editor.battleroyale.check"));
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

        // Rounds per Player
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

        paramsRow.getChildren().addAll(roundsBox);

        // 3. FILTERS SELECTION BOX
        VBox filtersContainer = new VBox(8);
        Label filtersHeader = new Label(I18n.get("game.editor.snapsolve.filters_title"));
        filtersHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b; -fx-font-size: 12px;");

        HBox filterChecksBox = new HBox(16);
        filterChecksBox.setAlignment(Pos.CENTER_LEFT);

        displacementCheck = new CheckBox("〰️ Displacement");
        displacementCheck.setSelected(curFilters.contains("displacement"));
        displacementCheck.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 11px;");

        swirlCheck = new CheckBox("🌪️ Swirl");
        swirlCheck.setSelected(curFilters.contains("swirl"));
        swirlCheck.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 11px;");

        pixelateCheck = new CheckBox("▦ Pixelate");
        pixelateCheck.setSelected(curFilters.contains("pixelate"));
        pixelateCheck.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 11px;");

        blurCheck = new CheckBox("🌫️ Blur");
        blurCheck.setSelected(curFilters.contains("blur"));
        blurCheck.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 11px;");

        filterChecksBox.getChildren().addAll(displacementCheck, swirlCheck, pixelateCheck, blurCheck);
        filtersContainer.getChildren().addAll(filtersHeader, filterChecksBox);

        // 4. IMAGE POOL HEADER & CONTROLS
        HBox poolHeaderBox = new HBox(12);
        poolHeaderBox.setAlignment(Pos.CENTER_LEFT);

        Label poolTitle = new Label(I18n.get("game.editor.snapsolve.pool_label"));
        poolTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 13px;");

        poolCountLabel = new Label();
        updatePoolCountLabel(profiles);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button uploadBtn = new Button(I18n.get("game.editor.choose_files"));
        uploadBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6px 14px; -fx-background-radius: 8px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        uploadBtn.setOnAction(e -> {
            FileChooser chooser = FileChooserHelper.createChooser("Select Images for Snap Solve");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.webp, *.gif, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp"));
            List<File> files = chooser.showOpenMultipleDialog(uploadBtn.getScene().getWindow());
            if (files != null && !files.isEmpty()) {
                FileChooserHelper.updateLastDirectory(files.get(0));
                for (File f : files) {
                    mediaPool.add(f.toURI().toString());
                }
                refreshThumbnails();
                updatePoolCountLabel(profiles);
            }
        });

        poolHeaderBox.getChildren().addAll(poolTitle, poolCountLabel, spacer, uploadBtn);

        // 5. INPUT VIA URL
        HBox urlBox = new HBox(8);
        urlBox.setAlignment(Pos.CENTER_LEFT);

        TextField urlField = new TextField();
        urlField.setPromptText(I18n.get("game.editor.snapsolve.paste_url"));
        HBox.setHgrow(urlField, Priority.ALWAYS);
        urlField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; -fx-border-color: #475569; -fx-border-radius: 6px; -fx-padding: 6px; -fx-font-size: 11px;");

        Button addUrlBtn = new Button(I18n.get("game.editor.add_url"));
        addUrlBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6px 14px; -fx-background-radius: 6px; -fx-cursor: hand;");
        addUrlBtn.setOnAction(e -> {
            String u = urlField.getText().trim();
            if (!u.isEmpty()) {
                mediaPool.add(u);
                urlField.clear();
                refreshThumbnails();
                updatePoolCountLabel(profiles);
            }
        });
        urlField.setOnAction(e -> addUrlBtn.fire());

        urlBox.getChildren().addAll(urlField, addUrlBtn);

        // 6. THUMBNAILS FLOW
        thumbnailsFlow = new FlowPane(10, 10);
        thumbnailsFlow.setPadding(new Insets(10));
        thumbnailsFlow.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        ScrollPane scroll = new ScrollPane(thumbnailsFlow);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(220);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        refreshThumbnails();

        root.getChildren().addAll(descLabel, brCard, paramsRow, filtersContainer, new Separator(), poolHeaderBox, urlBox, scroll);
        return root;
    }

    private void updatePoolCountLabel(List<Competitor> profiles) {
        int compCount = (profiles != null && !profiles.isEmpty()) ? profiles.size() : 1;
        int rpp = roundsSpinner != null ? roundsSpinner.getValue() : 2;
        int required = compCount * rpp;

        poolCountLabel.setText(String.format("(%d configured / minimum %d required)", mediaPool.size(), required));
        if (mediaPool.size() >= required) {
            poolCountLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            poolCountLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    private void refreshThumbnails() {
        thumbnailsFlow.getChildren().clear();

        for (int i = 0; i < mediaPool.size(); i++) {
            final int index = i;
            String imgRef = mediaPool.get(i);

            StackPane tile = new StackPane();
            tile.setPrefSize(72, 72);
            tile.setStyle("-fx-background-color: #1e293b; -fx-border-color: #475569; -fx-border-radius: 6px; -fx-background-radius: 6px;");

            ImageView iv = new ImageView();
            iv.setFitWidth(68);
            iv.setFitHeight(68);
            iv.setPreserveRatio(true);
            tile.getChildren().add(iv);

            ImageLoaderHelper.loadImageAsync(imgRef, 68, 68, true, true,
                    iv::setImage,
                    () -> {
                        Label errLbl = new Label("🖼️");
                        errLbl.setStyle("-fx-font-size: 18px;");
                        tile.getChildren().setAll(errLbl);
                    }
            );

            Label numBadge = new Label("#" + (i + 1));
            numBadge.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-font-size: 9px; -fx-padding: 1px 4px; -fx-background-radius: 3px;");
            StackPane.setAlignment(numBadge, Pos.BOTTOM_LEFT);
            StackPane.setMargin(numBadge, new Insets(3));

            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 1px 5px; -fx-background-radius: 10px; -fx-cursor: hand;");
            StackPane.setAlignment(removeBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(removeBtn, new Insets(2));
            removeBtn.setOnAction(e -> {
                mediaPool.remove(index);
                refreshThumbnails();
                updatePoolCountLabel(null);
            });

            tile.getChildren().addAll(numBadge, removeBtn);

            if (copyrightWarnings.contains(mediaPool.get(i))) {
                Label warnBadge = new Label("⚠️");
                warnBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.9); -fx-text-fill: #000; -fx-font-size: 9px; -fx-padding: 1px 3px; -fx-background-radius: 3px; -fx-cursor: hand;");
                Tooltip.install(warnBadge, new Tooltip(I18n.get("settings.ai.copyright_badge")));
                StackPane.setAlignment(warnBadge, Pos.TOP_LEFT);
                StackPane.setMargin(warnBadge, new Insets(2));
                tile.getChildren().add(warnBadge);
            }

            thumbnailsFlow.getChildren().add(tile);
        }

        if (mediaPool.isEmpty()) {
            Label emptyLbl = new Label(I18n.get("game.editor.snapsolve.no_images"));
            emptyLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            thumbnailsFlow.getChildren().add(emptyLbl);
        }
    }

    @Override
    public ObjectNode getUpdatedSetup() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("game", "Snap_Solve");
        node.put("battleRoyale", battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());
        node.put("rounds_per_player", roundsSpinner != null ? roundsSpinner.getValue() : 2);

        ArrayNode filtersArray = node.putArray("selected_filters");
        if (displacementCheck != null && displacementCheck.isSelected()) filtersArray.add("displacement");
        if (swirlCheck != null && swirlCheck.isSelected()) filtersArray.add("swirl");
        if (pixelateCheck != null && pixelateCheck.isSelected()) filtersArray.add("pixelate");
        if (blurCheck != null && blurCheck.isSelected()) filtersArray.add("blur");

        ArrayNode poolArray = node.putArray("media_pool");
        for (String s : mediaPool) {
            poolArray.add(s);
        }

        if (!copyrightWarnings.isEmpty()) {
            ArrayNode warnArr = node.putArray("copyright_warnings");
            for (String w : copyrightWarnings) {
                warnArr.add(w);
            }
        }

        return node;
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
            return "Snap Solve: Configuration is null or invalid.";
        }

        boolean isBr = battleRoyale || (setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean(false))
                || (setupData.has("battle_royale") && setupData.get("battle_royale").asBoolean(false));

        int rpp = 2;
        if (setupData.has("rounds_per_player")) rpp = setupData.get("rounds_per_player").asInt(2);
        else if (setupData.has("roundsPerPlayer")) rpp = setupData.get("roundsPerPlayer").asInt(2);

        if (!isBr && rpp < 1) {
            return "Snap Solve: Rounds per player must be at least 1.";
        }

        JsonNode filtersNode = setupData.has("selected_filters") ? setupData.get("selected_filters")
                : (setupData.has("selectedFilters") ? setupData.get("selectedFilters") : null);
        if (filtersNode == null || !filtersNode.isArray() || filtersNode.size() == 0) {
            return "Snap Solve: At least one visual distortion filter must be selected.";
        }

        int compCount = (profiles != null && !profiles.isEmpty()) ? profiles.size() : 1;
        int requiredImages = isBr ? 1 : (compCount * rpp);

        JsonNode poolNode = setupData.has("media_pool") ? setupData.get("media_pool")
                : (setupData.has("mediaPool") ? setupData.get("mediaPool") : null);

        if (poolNode == null || !poolNode.isArray() || poolNode.size() == 0) {
            return isBr
                    ? "Snap Solve: At least 1 image is required in pool. None are configured."
                    : ("Snap Solve: At least " + requiredImages + " image(s) required in pool. None are configured.");
        }

        if (!isBr && poolNode.size() < requiredImages) {
            return "Snap Solve: At least " + requiredImages + " image(s) required for " + compCount + " contestant(s) (" + rpp + " round/player), but only " + poolNode.size() + " image(s) provided.";
        }

        // Image accessibility & offline verification
        for (int i = 0; i < poolNode.size(); i++) {
            String imgStr = poolNode.get(i).asText().trim();
            if (imgStr.isEmpty()) {
                return "Snap Solve image #" + (i + 1) + " contains an empty reference.";
            }

            boolean reachable = ImageLoaderHelper.isUrlReachable(imgStr);
            if (!reachable) {
                return "Cannot access Snap Solve image #" + (i + 1) + " (unreachable or offline). Please configure local offline images in Game Settings.";
            }
        }

        return null;
    }
}
