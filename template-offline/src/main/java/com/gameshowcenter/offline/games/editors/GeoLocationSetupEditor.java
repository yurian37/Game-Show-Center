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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GeoLocationSetupEditor implements IGameSetupEditor {

    private static final String DND_IMAGE_PREFIX = "GEOLOC_IMG:";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class LocationItem {
        private String name;
        private final List<String> images = new ArrayList<>();

        public LocationItem(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getImages() {
            return images;
        }
    }

    private final List<LocationItem> locations = new ArrayList<>();
    private final java.util.Set<String> copyrightWarnings = new java.util.HashSet<>();
    private Spinner<Integer> roundsSpinner;
    private CheckBox battleRoyaleCheckBox;
    private ComboBox<Integer> imagesPerRoundCombo;
    private VBox locationsContainer;

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        locations.clear();
        copyrightWarnings.clear();

        if (currentSetup != null && currentSetup.has("copyright_warnings") && currentSetup.get("copyright_warnings").isArray()) {
            for (JsonNode w : currentSetup.get("copyright_warnings")) {
                copyrightWarnings.add(w.asText());
            }
        }

        // Load data from currentSetup
        int curRounds = 1;
        int curImagesPerRound = 3;
        boolean curBattleRoyale = false;

        if (currentSetup != null) {
            if (currentSetup.has("battleRoyale")) curBattleRoyale = currentSetup.get("battleRoyale").asBoolean(false);
            else if (currentSetup.has("battle_royale")) curBattleRoyale = currentSetup.get("battle_royale").asBoolean(false);

            if (currentSetup.has("rounds_per_player")) curRounds = currentSetup.get("rounds_per_player").asInt(1);
            else if (currentSetup.has("roundsPerPlayer")) curRounds = currentSetup.get("roundsPerPlayer").asInt(1);

            if (currentSetup.has("images_per_round")) curImagesPerRound = currentSetup.get("images_per_round").asInt(3);
            else if (currentSetup.has("imagesPerRound")) curImagesPerRound = currentSetup.get("imagesPerRound").asInt(3);

            JsonNode locsNode = currentSetup.has("locations") ? currentSetup.get("locations") : null;
            if (locsNode != null && locsNode.isArray()) {
                for (JsonNode locNode : locsNode) {
                    String locName = locNode.has("location_name") ? locNode.get("location_name").asText()
                            : (locNode.has("locationName") ? locNode.get("locationName").asText() : "Location");
                    LocationItem item = new LocationItem(locName);
                    if (locNode.has("images") && locNode.get("images").isArray()) {
                        for (JsonNode img : locNode.get("images")) {
                            String url = img.asText();
                            if (url != null && !url.isBlank()) {
                                item.getImages().add(url);
                            }
                        }
                    }
                    locations.add(item);
                }
            }
        }

        VBox root = new VBox(14);
        root.setPadding(new Insets(10));

        // 1. HEADER DESCRIPTION
        Label descLabel = new Label(I18n.get("game.editor.geolocation.desc"));
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

        // 2. SPINNERS ROW
        HBox spinnersRow = new HBox(24);
        spinnersRow.setAlignment(Pos.CENTER_LEFT);

        HBox roundsBox = new HBox(8);
        roundsBox.setAlignment(Pos.CENTER_LEFT);
        Label rLabel = new Label(I18n.get("game.editor.rounds_per_player"));
        rLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 12px;");
        roundsSpinner = new Spinner<>(1, 10, curRounds, 1);
        roundsSpinner.setEditable(true);
        roundsSpinner.setPrefWidth(75);
        roundsSpinner.setDisable(curBattleRoyale);
        roundsBox.getChildren().addAll(rLabel, roundsSpinner);

        battleRoyaleCheckBox.setOnAction(e -> {
            roundsSpinner.setDisable(battleRoyaleCheckBox.isSelected());
        });

        HBox imgCountBox = new HBox(8);
        imgCountBox.setAlignment(Pos.CENTER_LEFT);
        Label iLabel = new Label(I18n.get("game.editor.geolocation.min_images"));
        iLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a5b4fc; -fx-font-size: 12px;");
        imagesPerRoundCombo = new ComboBox<>();
        for (int k = 1; k <= 10; k++) {
            imagesPerRoundCombo.getItems().add(k);
        }
        imagesPerRoundCombo.setValue(Math.max(1, Math.min(10, curImagesPerRound)));
        imagesPerRoundCombo.setPrefWidth(75);
        imagesPerRoundCombo.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 6px; -fx-background-radius: 6px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        imagesPerRoundCombo.valueProperty().addListener((obs, oldV, newV) -> renderLocationCards());
        imgCountBox.getChildren().addAll(iLabel, imagesPerRoundCombo);

        spinnersRow.getChildren().addAll(roundsBox, imgCountBox);

        // 3. ADD NEW LOCATION FORM
        VBox addSection = new VBox(8);
        Label addTitle = new Label(I18n.get("game.editor.geolocation.add_location"));
        addTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #f59e0b;");

        HBox addRow = new HBox(10);
        addRow.setAlignment(Pos.CENTER_LEFT);

        TextField newLocInput = new TextField();
        newLocInput.setPromptText(I18n.get("game.editor.geolocation.name_placeholder"));
        newLocInput.setPrefWidth(320);
        newLocInput.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 8px; -fx-padding: 8px 12px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));

        Button addLocBtn = new Button(I18n.get("game.editor.add_location"));
        addLocBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));

        Runnable handleAdd = () -> {
            String name = newLocInput.getText().trim();
            if (name.isEmpty()) return;
            locations.add(new LocationItem(name));
            newLocInput.clear();
            renderLocationCards();
        };
        addLocBtn.setOnAction(e -> handleAdd.run());
        newLocInput.setOnAction(e -> handleAdd.run());

        addRow.getChildren().addAll(newLocInput, addLocBtn);
        addSection.getChildren().addAll(addTitle, addRow);

        // 4. LOCATIONS LIST CONTAINER
        locationsContainer = new VBox(12);

        ScrollPane scroll = new ScrollPane(locationsContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(380);
        scroll.setMaxHeight(420);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        renderLocationCards();

        root.getChildren().addAll(descLabel, brCard, spinnersRow, new Separator(), addSection, new Separator(), scroll);
        return root;
    }

    private void renderLocationCards() {
        if (locationsContainer == null) return;
        locationsContainer.getChildren().clear();

        int minReq = imagesPerRoundCombo != null && imagesPerRoundCombo.getValue() != null ? imagesPerRoundCombo.getValue() : 3;

        if (locations.isEmpty()) {
            Label emptyLbl = new Label(I18n.get("game.editor.geolocation.no_locations"));
            emptyLbl.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 20px;");
            locationsContainer.getChildren().add(emptyLbl);
            return;
        }

        for (int i = 0; i < locations.size(); i++) {
            final int locIndex = i;
            LocationItem loc = locations.get(i);

            VBox card = new VBox(10);
            card.setPadding(new Insets(12));
            card.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.12); -fx-border-radius: 12px; -fx-background-radius: 12px;",
                    ThemeManager.getCardHex()));

            // Header of location card: Name edit + status badge + delete button
            HBox cardHeader = new HBox(10);
            cardHeader.setAlignment(Pos.CENTER_LEFT);

            TextField nameField = new TextField(loc.getName());
            nameField.setStyle(String.format("-fx-background-color: transparent; -fx-text-fill: %s; -fx-font-weight: 900; -fx-font-size: 13px; -fx-border-color: transparent transparent #6366f1 transparent; -fx-border-width: 0 0 1.5px 0;",
                    ThemeManager.getTextOnCardPrimaryHex()));
            nameField.textProperty().addListener((obs, oldV, newV) -> loc.setName(newV));
            HBox.setHgrow(nameField, Priority.ALWAYS);

            int count = loc.getImages().size();
            boolean isComplete = count >= minReq;
            Label statusBadge = new Label(isComplete ? count + " / " + minReq + " min" : count + " / " + minReq + " min (Need " + (minReq - count) + " more)");
            SvgEmoji.setGraphic(statusBadge, isComplete ? "check" : "warning", 10);
            statusBadge.setStyle(isComplete
                    ? "-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3px 8px; -fx-background-radius: 6px; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 6px;"
                    : "-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3px 8px; -fx-background-radius: 6px; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 6px;");

            Button delLocBtn = new Button(I18n.get("game.editor.delete"));
            delLocBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-background-radius: 6px; -fx-cursor: hand;");
            delLocBtn.setOnAction(e -> {
                locations.remove(locIndex);
                renderLocationCards();
            });

            Label dragHint = new Label(I18n.get("game.editor.geolocation.drag_hint"));
            dragHint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-style: italic;");

            cardHeader.getChildren().addAll(nameField, statusBadge, delLocBtn);

            // Images Flow Container
            FlowPane imagesFlow = new FlowPane(8, 8);
            imagesFlow.setAlignment(Pos.CENTER_LEFT);

            // Add File Button Tile
            Button addFilesBtn = new Button(I18n.get("game.editor.choose_files"));
            addFilesBtn.setPrefSize(110, 75);
            addFilesBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #6366f1; -fx-border-style: dashed; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-text-alignment: center;",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
            addFilesBtn.setOnAction(e -> {
                FileChooser chooser = FileChooserHelper.createChooser("Select Clue Images for " + loc.getName());
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.webp, *.gif, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp"));
                List<File> files = chooser.showOpenMultipleDialog(addFilesBtn.getScene().getWindow());
                if (files != null && !files.isEmpty()) {
                    for (File f : files) {
                        loc.getImages().add(f.toURI().toString());
                    }
                    renderLocationCards();
                }
            });

            // DnD onto Add Files Button (Drop existing thumbnail to move to end, or drop files from OS to add at end)
            addFilesBtn.setOnDragOver(e -> {
                Dragboard db = e.getDragboard();
                if (db.hasString() && db.getString().startsWith(DND_IMAGE_PREFIX)) {
                    e.acceptTransferModes(TransferMode.MOVE);
                    addFilesBtn.setStyle(String.format(
                            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #a5b4fc; -fx-border-style: solid; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-text-alignment: center;",
                            ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
                } else if (db.hasFiles()) {
                    e.acceptTransferModes(TransferMode.COPY);
                    addFilesBtn.setStyle(String.format(
                            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #34d399; -fx-border-style: solid; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-text-alignment: center;",
                            ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
                }
                e.consume();
            });

            addFilesBtn.setOnDragExited(e -> {
                addFilesBtn.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #6366f1; -fx-border-style: dashed; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-text-alignment: center;",
                        ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
                e.consume();
            });

            addFilesBtn.setOnDragDropped(e -> {
                Dragboard db = e.getDragboard();
                boolean success = false;
                if (db.hasString() && db.getString().startsWith(DND_IMAGE_PREFIX)) {
                    try {
                        String[] parts = db.getString().substring(DND_IMAGE_PREFIX.length()).split(":");
                        int srcLocIdx = Integer.parseInt(parts[0]);
                        int srcImgIdx = Integer.parseInt(parts[1]);
                        if (srcLocIdx >= 0 && srcLocIdx < locations.size()) {
                            LocationItem srcLoc = locations.get(srcLocIdx);
                            if (srcImgIdx >= 0 && srcImgIdx < srcLoc.getImages().size()) {
                                String moved = srcLoc.getImages().remove(srcImgIdx);
                                loc.getImages().add(moved);
                                success = true;
                                renderLocationCards();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                } else if (db.hasFiles()) {
                    List<File> files = db.getFiles();
                    for (File f : files) {
                        String n = f.getName().toLowerCase();
                        if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                                || n.endsWith(".webp") || n.endsWith(".gif") || n.endsWith(".bmp")) {
                            loc.getImages().add(f.toURI().toString());
                            success = true;
                        }
                    }
                    if (success) {
                        renderLocationCards();
                    }
                }
                e.setDropCompleted(success);
                e.consume();
            });

            imagesFlow.getChildren().add(addFilesBtn);

            // ImagesFlow background drop handling (drop files or thumbnails to end)
            imagesFlow.setOnDragOver(e -> {
                Dragboard db = e.getDragboard();
                if (db.hasFiles() || (db.hasString() && db.getString().startsWith(DND_IMAGE_PREFIX))) {
                    e.acceptTransferModes(TransferMode.ANY);
                }
                e.consume();
            });

            imagesFlow.setOnDragDropped(e -> {
                if (e.isDropCompleted()) return;
                Dragboard db = e.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    List<File> files = db.getFiles();
                    for (File f : files) {
                        String n = f.getName().toLowerCase();
                        if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                                || n.endsWith(".webp") || n.endsWith(".gif") || n.endsWith(".bmp")) {
                            loc.getImages().add(f.toURI().toString());
                            success = true;
                        }
                    }
                    if (success) {
                        renderLocationCards();
                    }
                } else if (db.hasString() && db.getString().startsWith(DND_IMAGE_PREFIX)) {
                    try {
                        String[] parts = db.getString().substring(DND_IMAGE_PREFIX.length()).split(":");
                        int srcLocIdx = Integer.parseInt(parts[0]);
                        int srcImgIdx = Integer.parseInt(parts[1]);
                        if (srcLocIdx >= 0 && srcLocIdx < locations.size()) {
                            LocationItem srcLoc = locations.get(srcLocIdx);
                            if (srcImgIdx >= 0 && srcImgIdx < srcLoc.getImages().size()) {
                                String moved = srcLoc.getImages().remove(srcImgIdx);
                                loc.getImages().add(moved);
                                success = true;
                                renderLocationCards();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                e.setDropCompleted(success);
                e.consume();
            });

            // Render existing image thumbnails with drag-and-drop reordering
            for (int j = 0; j < loc.getImages().size(); j++) {
                final int imgIndex = j;
                String imgUrl = loc.getImages().get(j);

                StackPane thumbPane = new StackPane();
                thumbPane.setPrefSize(75, 75);
                thumbPane.setMaxSize(75, 75);
                thumbPane.setCursor(Cursor.MOVE);
                Tooltip.install(thumbPane, new Tooltip(I18n.get("game.editor.geolocation.drag_tooltip")));
                thumbPane.setStyle("-fx-background-color: #090c14; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8px; -fx-background-radius: 8px;");

                ImageView iv = new ImageView();
                iv.setFitWidth(75);
                iv.setFitHeight(75);
                iv.setPreserveRatio(true);
                iv.setMouseTransparent(true);
                thumbPane.getChildren().add(iv);

                ImageLoaderHelper.loadImageAsync(imgUrl, 75, 75, true, true,
                        iv::setImage,
                        () -> {
                            Label errLbl = new Label();
                            SvgEmoji.setGraphic(errLbl, "image", 20);
                            thumbPane.getChildren().setAll(errLbl);
                        }
                );

                Label numBadge = new Label("#" + (j + 1));
                numBadge.setStyle("-fx-background-color: rgba(0,0,0,0.75); -fx-text-fill: #ffffff; -fx-font-size: 9px; -fx-font-weight: 900; -fx-padding: 1px 4px; -fx-background-radius: 4px;");
                numBadge.setMouseTransparent(true);
                StackPane.setAlignment(numBadge, Pos.BOTTOM_LEFT);
                StackPane.setMargin(numBadge, new Insets(3));

                Label dragGrip = new Label("⋮⋮");
                dragGrip.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 0 4px; -fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 4px;");
                dragGrip.setMouseTransparent(true);
                StackPane.setAlignment(dragGrip, Pos.TOP_CENTER);
                StackPane.setMargin(dragGrip, new Insets(3, 0, 0, 0));

                Button delImgBtn = new Button();
                SvgEmoji.setGraphic(delImgBtn, "close", 8);
                delImgBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 9px; -fx-font-weight: 900; -fx-padding: 1px 5px; -fx-background-radius: 10px; -fx-cursor: hand;");
                StackPane.setAlignment(delImgBtn, Pos.TOP_RIGHT);
                StackPane.setMargin(delImgBtn, new Insets(3));
                delImgBtn.setOnAction(e -> {
                    loc.getImages().remove(imgIndex);
                    renderLocationCards();
                });

                thumbPane.getChildren().addAll(numBadge, dragGrip, delImgBtn);

                if (copyrightWarnings.contains(imgUrl)) {
                    Label warnBadge = new Label();
                    SvgEmoji.setGraphic(warnBadge, "warning", 9);
                    warnBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.9); -fx-text-fill: #000; -fx-font-size: 9px; -fx-padding: 1px 3px; -fx-background-radius: 3px; -fx-cursor: hand;");
                    Tooltip.install(warnBadge, new Tooltip(I18n.get("settings.ai.copyright_badge")));
                    StackPane.setAlignment(warnBadge, Pos.TOP_LEFT);
                    StackPane.setMargin(warnBadge, new Insets(3));
                    thumbPane.getChildren().add(warnBadge);
                }

                // Drag Source Events
                thumbPane.setOnDragDetected(e -> {
                    Dragboard db = thumbPane.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(DND_IMAGE_PREFIX + locIndex + ":" + imgIndex);
                    db.setContent(cc);

                    SnapshotParameters snapParams = new SnapshotParameters();
                    snapParams.setFill(Color.TRANSPARENT);
                    db.setDragView(thumbPane.snapshot(snapParams, null));

                    thumbPane.setOpacity(0.35);
                    e.consume();
                });

                thumbPane.setOnDragDone(e -> {
                    thumbPane.setOpacity(1.0);
                    e.consume();
                });

                // Drag Target Over Events (Visual indicator for dropping before/after)
                thumbPane.setOnDragOver(e -> {
                    Dragboard db = e.getDragboard();
                    if (db.hasString() && db.getString().startsWith(DND_IMAGE_PREFIX)) {
                        e.acceptTransferModes(TransferMode.MOVE);
                        double mouseX = e.getX();
                        boolean dropBefore = mouseX < (thumbPane.getWidth() / 2.0);
                        if (dropBefore) {
                            thumbPane.setStyle("-fx-background-color: #090c14; -fx-border-color: #6366f1 rgba(255,255,255,0.2) rgba(255,255,255,0.2) #6366f1; -fx-border-width: 1 1 1 4; -fx-border-radius: 8px; -fx-background-radius: 8px;");
                        } else {
                            thumbPane.setStyle("-fx-background-color: #090c14; -fx-border-color: rgba(255,255,255,0.2) #6366f1 #6366f1 rgba(255,255,255,0.2); -fx-border-width: 1 4 1 1; -fx-border-radius: 8px; -fx-background-radius: 8px;");
                        }
                    }
                    e.consume();
                });

                thumbPane.setOnDragExited(e -> {
                    thumbPane.setStyle("-fx-background-color: #090c14; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8px; -fx-background-radius: 8px;");
                    e.consume();
                });

                // Drag Target Dropped Event
                thumbPane.setOnDragDropped(e -> {
                    Dragboard db = e.getDragboard();
                    boolean success = false;
                    if (db.hasString() && db.getString().startsWith(DND_IMAGE_PREFIX)) {
                        try {
                            String[] parts = db.getString().substring(DND_IMAGE_PREFIX.length()).split(":");
                            int srcLocIdx = Integer.parseInt(parts[0]);
                            int srcImgIdx = Integer.parseInt(parts[1]);

                            if (srcLocIdx >= 0 && srcLocIdx < locations.size()) {
                                LocationItem srcLoc = locations.get(srcLocIdx);
                                if (srcImgIdx >= 0 && srcImgIdx < srcLoc.getImages().size()) {
                                    double mouseX = e.getX();
                                    boolean dropBefore = mouseX < (thumbPane.getWidth() / 2.0);
                                    int targetInsertIndex = imgIndex + (dropBefore ? 0 : 1);

                                    if (srcLocIdx == locIndex) {
                                        if (srcImgIdx != imgIndex) {
                                            String moved = loc.getImages().remove(srcImgIdx);
                                            if (srcImgIdx < targetInsertIndex) {
                                                targetInsertIndex--;
                                            }
                                            if (targetInsertIndex < 0) targetInsertIndex = 0;
                                            if (targetInsertIndex > loc.getImages().size()) targetInsertIndex = loc.getImages().size();
                                            loc.getImages().add(targetInsertIndex, moved);
                                            success = true;
                                            renderLocationCards();
                                        }
                                    } else {
                                        String moved = srcLoc.getImages().remove(srcImgIdx);
                                        if (targetInsertIndex < 0) targetInsertIndex = 0;
                                        if (targetInsertIndex > loc.getImages().size()) targetInsertIndex = loc.getImages().size();
                                        loc.getImages().add(targetInsertIndex, moved);
                                        success = true;
                                        renderLocationCards();
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    e.setDropCompleted(success);
                    e.consume();
                });

                imagesFlow.getChildren().add(thumbPane);
            }

            // URL input row
            HBox urlRow = new HBox(8);
            urlRow.setAlignment(Pos.CENTER_LEFT);
            TextField urlInput = new TextField();
            urlInput.setPromptText(I18n.get("game.editor.paste_image_url"));
            urlInput.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 6px; -fx-padding: 4px 8px;",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
            HBox.setHgrow(urlInput, Priority.ALWAYS);

            Button addUrlBtn = new Button(I18n.get("game.editor.add_url"));
            addUrlBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 6px; -fx-cursor: hand;");
            Runnable handleAddUrl = () -> {
                String u = urlInput.getText().trim();
                if (u.isEmpty()) return;
                loc.getImages().add(u);
                urlInput.clear();
                renderLocationCards();
            };
            addUrlBtn.setOnAction(e -> handleAddUrl.run());
            urlInput.setOnAction(e -> handleAddUrl.run());

            urlRow.getChildren().addAll(urlInput, addUrlBtn);

            card.getChildren().addAll(cardHeader, dragHint, imagesFlow, urlRow);
            locationsContainer.getChildren().add(card);
        }
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "GeoLocation");
        root.put("battleRoyale", battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());
        root.put("rounds_per_player", roundsSpinner != null ? roundsSpinner.getValue() : 1);
        root.put("images_per_round", imagesPerRoundCombo != null && imagesPerRoundCombo.getValue() != null ? imagesPerRoundCombo.getValue() : 3);

        ArrayNode locsArr = root.putArray("locations");
        for (LocationItem item : locations) {
            ObjectNode locNode = objectMapper.createObjectNode();
            locNode.put("location_name", item.getName());
            ArrayNode imgArr = locNode.putArray("images");
            for (String img : item.getImages()) {
                imgArr.add(img);
            }
            locsArr.add(locNode);
        }

        if (!copyrightWarnings.isEmpty()) {
            ArrayNode warnArr = root.putArray("copyright_warnings");
            for (String w : copyrightWarnings) {
                warnArr.add(w);
            }
        }

        return root;
    }

    public int getImagesPerRound() {
        return imagesPerRoundCombo != null && imagesPerRoundCombo.getValue() != null ? imagesPerRoundCombo.getValue() : 3;
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
            return "GeoLocation configuration is missing.";
        }

        boolean isBr = battleRoyale || (setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean(false))
                || (setupData.has("battle_royale") && setupData.get("battle_royale").asBoolean(false));

        int rpp = 1;
        if (setupData.has("rounds_per_player")) rpp = setupData.get("rounds_per_player").asInt(1);
        else if (setupData.has("roundsPerPlayer")) rpp = setupData.get("roundsPerPlayer").asInt(1);

        int minImages = isBr ? 1 : 3;
        if (!isBr) {
            if (setupData.has("images_per_round")) minImages = setupData.get("images_per_round").asInt(3);
            else if (setupData.has("imagesPerRound")) minImages = setupData.get("imagesPerRound").asInt(3);
        }

        int compCount = (profiles != null && !profiles.isEmpty()) ? profiles.size() : 1;
        int requiredLocations = isBr ? 1 : (compCount * rpp);

        JsonNode locsNode = setupData.has("locations") ? setupData.get("locations") : null;
        if (locsNode == null || !locsNode.isArray() || locsNode.size() == 0) {
            return isBr
                    ? "GeoLocation requires at least 1 configured location. None are added."
                    : ("GeoLocation requires at least " + requiredLocations + " configured location(s). None are added.");
        }

        if (!isBr && locsNode.size() < requiredLocations) {
            return "GeoLocation requires at least " + requiredLocations + " location(s) for " + compCount + " competitor(s) (" + rpp + " round/player), but only " + locsNode.size() + " location(s) are configured.";
        }

        // Validate each location has enough images & check offline image accessibility
        for (int i = 0; i < locsNode.size(); i++) {
            JsonNode loc = locsNode.get(i);
            String locName = loc.has("location_name") ? loc.get("location_name").asText()
                    : (loc.has("locationName") ? loc.get("locationName").asText() : ("Location #" + (i + 1)));

            JsonNode imgs = loc.has("images") ? loc.get("images") : null;
            if (imgs == null || !imgs.isArray() || imgs.size() < minImages) {
                int count = (imgs != null && imgs.isArray()) ? imgs.size() : 0;
                return battleRoyale
                        ? ("Location '" + locName + "' has no images. At least 1 image is required.")
                        : ("Location '" + locName + "' has only " + count + " image(s). At least " + minImages + " images are required per round.");
            }

            // Image accessibility & offline verification
            for (JsonNode imgNode : imgs) {
                String imgStr = imgNode.asText().trim();
                if (imgStr.isEmpty()) {
                    return "Location '" + locName + "' contains an empty image reference.";
                }

                // If local file path or file URI
                if (imgStr.startsWith("file:")) {
                    try {
                        File localFile = new File(new URI(imgStr));
                        if (!localFile.exists()) {
                            return "Cannot access offline image for '" + locName + "': File not found (" + localFile.getName() + "). Please configure local offline images in Game Settings.";
                        }
                    } catch (Exception ex) {
                        return "Invalid local file path for '" + locName + "': " + imgStr;
                    }
                } else if (!imgStr.startsWith("http://") && !imgStr.startsWith("https://")) {
                    File localFile = ImageLoaderHelper.resolveLocalFile(imgStr);
                    if (localFile == null || !localFile.exists()) {
                        return "Cannot access offline image for '" + locName + "': File not found (" + imgStr + "). Please configure local offline images in Game Settings.";
                    }
                } else {
                    // It's an HTTP/HTTPS URL. Check if reachable
                    boolean reachable = ImageLoaderHelper.isUrlReachable(imgStr);
                    if (!reachable) {
                        return "Cannot access online image for '" + locName + "' (unreachable or offline). Please connect to the internet or configure local offline images in Game Settings.";
                    }
                }
            }
        }

        return null;
    }
}
