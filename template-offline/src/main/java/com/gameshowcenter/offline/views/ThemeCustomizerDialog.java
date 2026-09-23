package com.gameshowcenter.offline.views;

import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.theme.ImageSourcesConfigManager;
import com.gameshowcenter.offline.theme.RagDocumentsConfigManager;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.util.FileChooserHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.Rectangle2D;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class ThemeCustomizerDialog {

    public static void show(Stage parentStage, Runnable onApplyCallback) {
        if (I18n.isSwitchingLocked()) {
            return;
        }
        Stage dialog = new Stage();
        dialog.initOwner(parentStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(I18n.get("theme.dialog.title"));
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        dialog.setMinWidth(750);
        dialog.setMinHeight(520);
        dialog.setMaxWidth(Math.min(1050, screenBounds.getWidth() * 0.95));
        dialog.setMaxHeight(Math.min(760, screenBounds.getHeight() * 0.90));

        // Header
        Label titleLabel = new Label(I18n.get("theme.header.title"));
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #f8fafc;");
        
        Label subtitleLabel = new Label(I18n.get("theme.header.subtitle"));
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        subtitleLabel.setWrapText(true);

        VBox titleBox = new VBox(4, titleLabel, subtitleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button topCloseBtn = new Button("✕");
        topCloseBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-weight: 900; -fx-font-size: 14px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 6px 12px;");
        topCloseBtn.setOnAction(e -> dialog.close());

        HBox headerBox = new HBox(12, titleBox, topCloseBtn);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // TabPane for organized settings
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        // TAB 2 Color Pickers Declarations (needed early for Tab 1 sync)
        ColorPicker appBgColorPicker = new ColorPicker(
            ThemeManager.getCustomAppBgColor() != null ? ThemeManager.getCustomAppBgColor() : ThemeManager.getCurrentPalette().bgApp
        );

        ColorPicker mainBoxColorPicker = new ColorPicker(
            ThemeManager.getCustomMainBoxColor() != null ? ThemeManager.getCustomMainBoxColor() : ThemeManager.getCurrentPalette().bgMainBox
        );

        ColorPicker cardColorPicker = new ColorPicker(
            ThemeManager.getCustomCardColor() != null ? ThemeManager.getCustomCardColor() : ThemeManager.getCurrentPalette().bgCard
        );

        ColorPicker accentColorPicker = new ColorPicker(
            ThemeManager.getCustomAccentColor() != null ? ThemeManager.getCustomAccentColor() : ThemeManager.getCurrentPalette().accent
        );

        Label imgPathLabel = new Label(
            ThemeManager.getCustomBackgroundImagePath() != null ? ThemeManager.getCustomBackgroundImagePath() : I18n.get("theme.label.no_img")
        );
        imgPathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        // Helper to sync pickers with active palette
        Runnable syncPickersWithCurrentTheme = () -> {
            appBgColorPicker.setValue(ThemeManager.getCustomAppBgColor() != null ? ThemeManager.getCustomAppBgColor() : ThemeManager.getCurrentPalette().bgApp);
            mainBoxColorPicker.setValue(ThemeManager.getCustomMainBoxColor() != null ? ThemeManager.getCustomMainBoxColor() : ThemeManager.getCurrentPalette().bgMainBox);
            cardColorPicker.setValue(ThemeManager.getCustomCardColor() != null ? ThemeManager.getCustomCardColor() : ThemeManager.getCurrentPalette().bgCard);
            accentColorPicker.setValue(ThemeManager.getCustomAccentColor() != null ? ThemeManager.getCustomAccentColor() : ThemeManager.getCurrentPalette().accent);
        };

        // REAL-TIME HANDLERS: Applied immediately on change!
        appBgColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ThemeManager.setCustomBackgroundImagePath(null);
                imgPathLabel.setText(I18n.get("theme.label.no_img"));
                ThemeManager.setCustomAppBgColor(newVal);
                if (onApplyCallback != null) onApplyCallback.run();
            }
        });

        mainBoxColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ThemeManager.setCustomMainBoxColor(newVal);
                if (onApplyCallback != null) onApplyCallback.run();
            }
        });

        cardColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ThemeManager.setCustomCardColor(newVal);
                if (onApplyCallback != null) onApplyCallback.run();
            }
        });

        accentColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ThemeManager.setCustomAccentColor(newVal);
                if (onApplyCallback != null) onApplyCallback.run();
            }
        });

        // TAB 1: 20 PALETTES + CUSTOM THEMES
        Tab palettesTab = new Tab(I18n.get("theme.tab.palettes"));
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        Map<String, ThemeManager.Palette> palettes = ThemeManager.getPalettesMap();
        int col = 0, row = 0;

        for (Map.Entry<String, ThemeManager.Palette> entry : palettes.entrySet()) {
            String name = entry.getKey();
            ThemeManager.Palette pal = entry.getValue();

            VBox card = new VBox(8);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: #141929; -fx-border-color: #1e293b; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-cursor: hand;");

            HBox cardHeader = new HBox(8);
            cardHeader.setAlignment(Pos.CENTER_LEFT);

            Label nameLbl = new Label((pal.isCustom ? "⭐ " : "") + name);
            nameLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);

            cardHeader.getChildren().add(nameLbl);

            if (pal.isCustom) {
                Button delBtn = new Button("🗑️");
                delBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-padding: 3 7; -fx-background-radius: 6; -fx-cursor: hand;");
                delBtn.setOnAction(e -> {
                    e.consume();
                    ThemeManager.deleteCustomPalette(name);
                    if (onApplyCallback != null) onApplyCallback.run();
                    dialog.close();
                    ThemeCustomizerDialog.show(parentStage, onApplyCallback);
                });
                cardHeader.getChildren().add(delBtn);
            }

            HBox swatches = new HBox(6);
            swatches.setAlignment(Pos.CENTER_LEFT);
            swatches.getChildren().addAll(
                createSwatch(pal.bgApp, I18n.get("theme.swatch.app_bg")),
                createSwatch(pal.bgMainBox, I18n.get("theme.swatch.main_box")),
                createSwatch(pal.bgCard, I18n.get("theme.swatch.card")),
                createSwatch(pal.accent, I18n.get("theme.swatch.accent"))
            );

            card.getChildren().addAll(cardHeader, swatches);

            card.setOnMouseClicked(e -> {
                ThemeManager.setPalette(name);
                ThemeManager.setCustomAppBgColor(null);
                ThemeManager.setCustomMainBoxColor(null);
                ThemeManager.setCustomCardColor(null);
                ThemeManager.setCustomAccentColor(null);
                ThemeManager.setCustomBackgroundImagePath(null);
                imgPathLabel.setText(I18n.get("theme.label.no_img"));
                syncPickersWithCurrentTheme.run();
                if (onApplyCallback != null) onApplyCallback.run();
                dialog.close();
            });

            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1e253b; -fx-border-color: #6366f1; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-cursor: hand;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #141929; -fx-border-color: #1e293b; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-cursor: hand;"));

            grid.add(card, col, row);
            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
        }

        scrollPane.setContent(grid);
        palettesTab.setContent(scrollPane);

        // TAB 2: CUSTOM BACKGROUND & BOXES
        Tab customTab = new Tab(I18n.get("theme.tab.custom"));
        VBox customBox = new VBox(20);
        customBox.setPadding(new Insets(20));

        // Section 1: Background (Color vs Image)
        VBox bgSection = new VBox(10);
        Label bgSectionTitle = new Label(I18n.get("theme.section.bg_title"));
        bgSectionTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        Button chooseImgBtn = new Button(I18n.get("theme.btn.choose_img"));
        chooseImgBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        chooseImgBtn.setOnAction(e -> {
            FileChooser chooser = FileChooserHelper.createChooser(I18n.get("theme.chooser.img_title"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.get("theme.chooser.img_filter"), "*.png", "*.jpg", "*.jpeg"));
            File file = FileChooserHelper.showOpenDialog(chooser, dialog);
            if (file != null) {
                ThemeManager.setCustomBackgroundImagePath(file.getAbsolutePath());
                imgPathLabel.setText(file.getAbsolutePath());
                if (onApplyCallback != null) onApplyCallback.run();
            }
        });

        Button clearImgBtn = new Button(I18n.get("theme.btn.clear_img"));
        clearImgBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");
        clearImgBtn.setOnAction(e -> {
            ThemeManager.setCustomBackgroundImagePath(null);
            imgPathLabel.setText(I18n.get("theme.label.no_img"));
            if (onApplyCallback != null) onApplyCallback.run();
        });

        HBox imgBtnBox = new HBox(10, chooseImgBtn, clearImgBtn);
        imgBtnBox.setAlignment(Pos.CENTER_LEFT);

        Label solidBgLbl = new Label(I18n.get("theme.label.solid_bg"));
        solidBgLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 12px;");
        HBox appBgBox = new HBox(16, solidBgLbl, appBgColorPicker);
        appBgBox.setAlignment(Pos.CENTER_LEFT);

        bgSection.getChildren().addAll(bgSectionTitle, appBgBox, imgBtnBox, imgPathLabel);

        // Section 2: Independent Main Box, Text Box and Accent Colors
        VBox boxesSection = new VBox(12);
        Label boxesTitle = new Label(I18n.get("theme.section.boxes_title"));
        boxesTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #a855f7;");

        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(20);
        colorGrid.setVgap(12);

        Label lbl1 = new Label(I18n.get("theme.label.box_main"));
        lbl1.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold;");
        Label lbl2 = new Label(I18n.get("theme.label.box_cards"));
        lbl2.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold;");
        Label lbl3 = new Label(I18n.get("theme.label.box_accent"));
        lbl3.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold;");

        colorGrid.add(lbl1, 0, 0);
        colorGrid.add(mainBoxColorPicker, 1, 0);
        colorGrid.add(lbl2, 0, 1);
        colorGrid.add(cardColorPicker, 1, 1);
        colorGrid.add(lbl3, 0, 2);
        colorGrid.add(accentColorPicker, 1, 2);

        boxesSection.getChildren().addAll(boxesTitle, colorGrid);

        // Section 3: Save as Named Custom Theme
        VBox saveSection = new VBox(10);
        Label saveTitle = new Label(I18n.get("theme.section.save_title"));
        saveTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        TextField saveNameField = new TextField();
        saveNameField.setPromptText(I18n.get("theme.field.theme_name"));
        saveNameField.setStyle("-fx-background-color: #141929; -fx-text-fill: #ffffff; -fx-border-color: #334155; -fx-border-radius: 8px; -fx-padding: 6px 12px;");

        Button savePaletteBtn = new Button(I18n.get("theme.btn.save_theme"));
        savePaletteBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 7px 16px; -fx-background-radius: 8px; -fx-cursor: hand;");
        savePaletteBtn.setOnAction(e -> {
            String name = saveNameField.getText();
            if (name != null && !name.trim().isEmpty()) {
                ThemeManager.setCustomBackgroundImagePath(null);
                ThemeManager.saveCustomPalette(
                    name.trim(),
                    appBgColorPicker.getValue(),
                    mainBoxColorPicker.getValue(),
                    cardColorPicker.getValue(),
                    accentColorPicker.getValue()
                );
                if (onApplyCallback != null) onApplyCallback.run();
                dialog.close();
            }
        });

        HBox saveBox = new HBox(10, saveNameField, savePaletteBtn);
        saveBox.setAlignment(Pos.CENTER_LEFT);
        saveSection.getChildren().addAll(saveTitle, saveBox);

        customBox.getChildren().addAll(bgSection, new Separator(), boxesSection, new Separator(), saveSection);
        ScrollPane customScroll = new ScrollPane(customBox);
        customScroll.setFitToWidth(true);
        customScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        customTab.setContent(customScroll);

        // TAB 3: ACCESSIBILITY & FONT SCALING
        Tab accessibilityTab = new Tab(I18n.get("theme.tab.accessibility", "👁️ Accesibilidad"));
        VBox accessBox = new VBox(16);
        accessBox.setPadding(new Insets(20));

        Label accessTitle = new Label(I18n.get("theme.accessibility.title", "TAMAÑO GENERAL DEL TEXTO (ACCESIBILIDAD)"));
        accessTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #38bdf8;");

        Label accessDesc = new Label(I18n.get("theme.accessibility.desc", "Ajusta la escala del texto para agrandar o achicar todo el contenido de la aplicación simultáneamente, mejorando la legibilidad."));
        accessDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        accessDesc.setWrapText(true);

        // Current scale indicator badge
        Label scaleBadge = new Label(Math.round(ThemeManager.getFontScale() * 100) + "%");
        scaleBadge.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-padding: 4px 14px; -fx-background-radius: 12px; -fx-font-size: 13px;");

        Label scaleTitle = new Label(I18n.get("theme.accessibility.scale", "Escala de Texto:"));
        scaleTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        HBox scaleHeader = new HBox(12, scaleTitle, scaleBadge);
        scaleHeader.setAlignment(Pos.CENTER_LEFT);

        // Quick A- / Default / A+ buttons
        Button shrinkBtn = new Button("🔍 " + I18n.get("theme.accessibility.shrink", "Achicar (A-)"));
        shrinkBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 8px; -fx-cursor: hand;");

        Button defaultScaleBtn = new Button("↺ " + I18n.get("theme.accessibility.default", "Predeterminado (100%)"));
        defaultScaleBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 8px; -fx-cursor: hand;");

        Button enlargeBtn = new Button("🔎 " + I18n.get("theme.accessibility.enlarge", "Agrandar (A+)"));
        enlargeBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 8px; -fx-cursor: hand;");

        HBox quickButtons = new HBox(10, shrinkBtn, defaultScaleBtn, enlargeBtn);
        quickButtons.setAlignment(Pos.CENTER_LEFT);

        // Smooth Slider
        Slider fontSlider = new Slider(0.75, 1.60, ThemeManager.getFontScale());
        fontSlider.setBlockIncrement(0.05);
        fontSlider.setMajorTickUnit(0.25);
        fontSlider.setShowTickMarks(true);
        fontSlider.setPrefWidth(420);

        // Preset Chips
        HBox presetChips = new HBox(8);
        presetChips.setAlignment(Pos.CENTER_LEFT);
        double[] presetValues = {0.75, 0.90, 1.00, 1.15, 1.30, 1.50};
        String[] presetLabels = {
            I18n.get("theme.accessibility.preset_compact", "75% (Compacto)"),
            I18n.get("theme.accessibility.preset_reduced", "90% (Reducido)"),
            I18n.get("theme.accessibility.preset_normal", "100% (Normal)"),
            I18n.get("theme.accessibility.preset_comfortable", "115% (Cómodo)"),
            I18n.get("theme.accessibility.preset_large", "130% (Grande)"),
            I18n.get("theme.accessibility.preset_huge", "150% (Extra Grande)")
        };

        // Interactive Live Preview Box
        VBox previewBox = new VBox(10);
        previewBox.setStyle("-fx-background-color: #141929; -fx-border-color: #334155; -fx-border-radius: 12px; -fx-padding: 16px; -fx-background-radius: 12px;");
        Label prevTitle = new Label("🎯 " + I18n.get("theme.accessibility.prev_title", "Vista Previa de Lectura"));
        prevTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: #f8fafc;");
        Label prevDesc = new Label(I18n.get("theme.accessibility.prev_desc", "Todo el texto de los menús, pantallas y juegos se adaptará inmediatamente a este tamaño."));
        prevDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-wrap-text: true;");
        Button prevBtn = new Button(I18n.get("theme.accessibility.prev_btn", "Botón de Demostración"));
        prevBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 8px 18px; -fx-background-radius: 8px;");
        previewBox.getChildren().addAll(prevTitle, prevDesc, prevBtn);

        // Scaling logic handler
        java.util.function.Consumer<Double> updateScale = (newScale) -> {
            double clamped = Math.max(0.70, Math.min(1.80, Math.round(newScale * 100.0) / 100.0));
            ThemeManager.setFontScale(clamped);
            fontSlider.setValue(clamped);
            scaleBadge.setText(Math.round(clamped * 100) + "%");
            if (dialog.getScene() != null) {
                ThemeManager.applyTextScale(dialog.getScene().getRoot(), clamped);
                Rectangle2D scrBounds = Screen.getPrimary().getVisualBounds();
                if (dialog.getHeight() > scrBounds.getHeight() * 0.90) {
                    dialog.setHeight(scrBounds.getHeight() * 0.90);
                }
            }
            if (onApplyCallback != null) onApplyCallback.run();
        };

        fontSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && Math.abs(newVal.doubleValue() - ThemeManager.getFontScale()) >= 0.01) {
                updateScale.accept(newVal.doubleValue());
            }
        });

        shrinkBtn.setOnAction(e -> updateScale.accept(ThemeManager.getFontScale() - 0.10));
        defaultScaleBtn.setOnAction(e -> updateScale.accept(1.00));
        enlargeBtn.setOnAction(e -> updateScale.accept(ThemeManager.getFontScale() + 0.10));

        for (int i = 0; i < presetValues.length; i++) {
            double pVal = presetValues[i];
            String pLbl = presetLabels[i];
            Button chip = new Button(pLbl);
            chip.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-padding: 5px 10px; -fx-background-radius: 12px; -fx-cursor: hand;");
            chip.setOnAction(e -> updateScale.accept(pVal));
            presetChips.getChildren().add(chip);
        }

        accessBox.getChildren().addAll(
            accessTitle,
            accessDesc,
            scaleHeader,
            fontSlider,
            quickButtons,
            presetChips,
            new Separator(),
            previewBox
        );
        ScrollPane accessScroll = new ScrollPane(accessBox);
        accessScroll.setFitToWidth(true);
        accessScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        accessibilityTab.setContent(accessScroll);

        // TAB 4: AI IMAGE SOURCES CONFIGURATION (5 fixed repos, max 10 local folders, max 2 school URLs, internet toggle)
        Tab aiSourcesTab = createAiSourcesTab(parentStage, dialog);

        // TAB 5: RAG KNOWLEDGE DOCUMENTS (PDF only, max 10)
        Tab ragDocumentsTab = createRagDocumentsTab(parentStage, dialog);

        tabPane.getTabs().addAll(palettesTab, customTab, accessibilityTab, aiSourcesTab, ragDocumentsTab);

        // FOOTER ACTIONS
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button resetBtn = new Button(I18n.get("theme.btn.reset"));
        resetBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 10; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> {
            ThemeManager.setPalette("Midnight Indigo");
            ThemeManager.setCustomAppBgColor(null);
            ThemeManager.setCustomMainBoxColor(null);
            ThemeManager.setCustomCardColor(null);
            ThemeManager.setCustomAccentColor(null);
            ThemeManager.setCustomBackgroundImagePath(null);
            ThemeManager.setFontScale(1.0);
            fontSlider.setValue(1.0);
            scaleBadge.setText("100%");
            syncPickersWithCurrentTheme.run();
            imgPathLabel.setText(I18n.get("theme.label.no_img"));
            if (dialog.getScene() != null) {
                ThemeManager.applyTextScale(dialog.getScene().getRoot(), 1.0);
            }
            if (onApplyCallback != null) onApplyCallback.run();
        });

        Button closeBtn = new Button(I18n.get("theme.btn.done"));
        closeBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-padding: 10 24; -fx-background-radius: 10; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        footer.getChildren().addAll(resetBtn, closeBtn);

        BorderPane rootLayout = new BorderPane();
        rootLayout.setPadding(new Insets(20));
        rootLayout.setStyle("-fx-background-color: #0b0e17;");
        rootLayout.setTop(headerBox);
        BorderPane.setMargin(headerBox, new Insets(0, 0, 14, 0));
        rootLayout.setCenter(tabPane);
        rootLayout.setBottom(footer);
        BorderPane.setMargin(footer, new Insets(14, 0, 0, 0));

        Scene scene = new Scene(rootLayout, 850, Math.min(680, screenBounds.getHeight() * 0.88));
        scene.setFill(Color.web("#0b0e17"));
        
        // Link Master Stylesheet so TabPane and other controls use dark modern styling
        URL cssUrl = ThemeCustomizerDialog.class.getResource("/styles/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Apply initial accessibility scale to dialog
        ThemeManager.applyTextScale(rootLayout, ThemeManager.getFontScale());

        dialog.setScene(scene);
        dialog.show();
    }

    private static Circle createSwatch(Color color, String tooltipText) {
        Circle circle = new Circle(8);
        circle.setFill(color);
        circle.setStroke(Color.web("#334155"));
        circle.setStrokeWidth(1);
        Tooltip.install(circle, new Tooltip(tooltipText));
        return circle;
    }

    private static Tab createAiSourcesTab(Stage parentStage, Stage dialog) {
        Tab tab = new Tab(I18n.get("theme.tab.ai_sources"));

        VBox rootBox = new VBox(20);
        rootBox.setPadding(new Insets(20));
        rootBox.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(rootBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // 1. Header
        VBox titleBox = new VBox(4);
        Label titleLbl = new Label(I18n.get("settings.ai.sources.tab_title"));
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #38bdf8; -fx-letter-spacing: 0.5px;");
        Label subtitleLbl = new Label(I18n.get("settings.ai.sources.tab_subtitle"));
        subtitleLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        subtitleLbl.setWrapText(true);
        titleBox.getChildren().addAll(titleLbl, subtitleLbl);

        // 2. Toggle Switch for Internet Search
        VBox toggleCard = new VBox(8);
        toggleCard.setPadding(new Insets(14));
        toggleCard.setStyle("-fx-background-color: #141929; -fx-border-color: #334155; -fx-border-radius: 12px; -fx-background-radius: 12px;");

        CheckBox internetToggle = new CheckBox(I18n.get("settings.ai.sources.toggle_internet"));
        internetToggle.setSelected(ImageSourcesConfigManager.isInternetSearchEnabled());
        internetToggle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-cursor: hand;");

        Label toggleDesc = new Label(I18n.get("settings.ai.sources.toggle_internet_desc"));
        toggleDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        toggleDesc.setWrapText(true);

        toggleCard.getChildren().addAll(internetToggle, toggleDesc);

        // 2.5 Strictness Threshold Card (Base 10: 1 to 10)
        VBox strictnessCard = new VBox(10);
        strictnessCard.setPadding(new Insets(14));
        strictnessCard.setStyle("-fx-background-color: #141929; -fx-border-color: #334155; -fx-border-radius: 12px; -fx-background-radius: 12px;");

        HBox strictnessHeader = new HBox(12);
        strictnessHeader.setAlignment(Pos.CENTER_LEFT);

        Label strictnessTitle = new Label("🎯 " + I18n.get("settings.ai.sources.strictness_title", "UMBRAL DE ESTRICTEZ DE BÚSQUEDA (BASE 10)"));
        strictnessTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #f59e0b;");
        HBox.setHgrow(strictnessTitle, Priority.ALWAYS);

        int currentStrictness = ImageSourcesConfigManager.getStrictness();
        Label strictnessBadge = new Label(I18n.get("settings.ai.sources.strictness_level", "Nivel:") + " " + currentStrictness + " / 10");
        strictnessBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #fef08a; -fx-background-color: #78350f; -fx-padding: 3 10; -fx-background-radius: 10;");

        strictnessHeader.getChildren().addAll(strictnessTitle, strictnessBadge);

        Slider strictnessSlider = new Slider(1, 10, currentStrictness);
        strictnessSlider.setBlockIncrement(1);
        strictnessSlider.setMajorTickUnit(1);
        strictnessSlider.setMinorTickCount(0);
        strictnessSlider.setSnapToTicks(true);
        strictnessSlider.setShowTickMarks(true);
        strictnessSlider.setShowTickLabels(true);
        strictnessSlider.setStyle("-fx-cursor: hand;");

        Label strictnessDesc = new Label(I18n.get("settings.ai.sources.strictness_desc",
            "1 = Estrictez baja (Acepta cualquier imagen relevante, solo filtros NSFW estrictos) — 10 = Sumamente estricto (Máxima precisión y calidad visual obligatoria)."));
        strictnessDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        strictnessDesc.setWrapText(true);

        strictnessSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = (int) Math.round(newV.doubleValue());
            strictnessBadge.setText(I18n.get("settings.ai.sources.strictness_level", "Nivel:") + " " + val + " / 10");
            ImageSourcesConfigManager.setStrictness(val);
        });

        strictnessCard.getChildren().addAll(strictnessHeader, strictnessSlider, strictnessDesc);

        // 3. Fixed Internet Repositories (5 sources)
        VBox fixedSection = new VBox(10);
        HBox fixedHeader = new HBox(8);
        fixedHeader.setAlignment(Pos.CENTER_LEFT);
        Label fixedTitle = new Label(I18n.get("settings.ai.sources.fixed_repos_title"));
        fixedTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #818cf8;");
        Label fixedLockBadge = new Label(I18n.get("settings.ai.sources.fixed_repos_locked"));
        fixedLockBadge.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1; -fx-background-color: #1e253b; -fx-padding: 2 8; -fx-background-radius: 6;");
        fixedHeader.getChildren().addAll(fixedTitle, fixedLockBadge);

        GridPane fixedGrid = new GridPane();
        fixedGrid.setHgap(10);
        fixedGrid.setVgap(8);

        int col = 0, row = 0;
        for (ImageSourcesConfigManager.FixedInternetSource fis : ImageSourcesConfigManager.FIXED_INTERNET_SOURCES) {
            VBox card = new VBox(4);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: #162033; -fx-border-color: #1e293b; -fx-border-radius: 10px; -fx-background-radius: 10px;");
            card.setPrefWidth(330);

            HBox cardTop = new HBox(6);
            cardTop.setAlignment(Pos.CENTER_LEFT);
            Label nameLbl = new Label("🌐 " + fis.name());
            nameLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);
            Label lockLbl = new Label("🔒");
            lockLbl.setStyle("-fx-font-size: 10px;");
            cardTop.getChildren().addAll(nameLbl, lockLbl);

            Label descLbl = new Label(fis.description());
            descLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            descLbl.setWrapText(true);

            card.getChildren().addAll(cardTop, descLbl);
            fixedGrid.add(card, col, row);
            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
        }
        fixedSection.getChildren().addAll(fixedHeader, fixedGrid);

        // Reactivity for internet toggle disabling fixed sources visually
        Runnable updateFixedOpacity = () -> {
            boolean active = internetToggle.isSelected();
            fixedSection.setOpacity(active ? 1.0 : 0.4);
            fixedSection.setDisable(!active);
        };
        updateFixedOpacity.run();
        internetToggle.selectedProperty().addListener((obs, oldV, newV) -> {
            ImageSourcesConfigManager.setInternetSearchEnabled(newV);
            updateFixedOpacity.run();
        });

        // 4. Local Folders Section (Max 10)
        VBox foldersSection = new VBox(10);
        HBox foldersHeader = new HBox(12);
        foldersHeader.setAlignment(Pos.CENTER_LEFT);

        Label foldersTitle = new Label(I18n.get("settings.ai.sources.local_folders_title"));
        foldersTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #34d399;");

        Label foldersCountBadge = new Label();
        foldersCountBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #a7f3d0; -fx-background-color: #064e3b; -fx-padding: 2 8; -fx-background-radius: 8;");

        Button addFolderBtn = new Button(I18n.get("settings.ai.sources.add_folder_btn"));
        addFolderBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox.setHgrow(foldersTitle, Priority.ALWAYS);
        foldersHeader.getChildren().addAll(foldersTitle, foldersCountBadge, addFolderBtn);

        VBox foldersList = new VBox(6);

        Runnable refreshFoldersList = () -> {
            foldersList.getChildren().clear();
            List<String> folders = ImageSourcesConfigManager.getLocalFolders();
            foldersCountBadge.setText(folders.size() + " / " + ImageSourcesConfigManager.MAX_LOCAL_FOLDERS);
            addFolderBtn.setDisable(folders.size() >= ImageSourcesConfigManager.MAX_LOCAL_FOLDERS);

            if (folders.isEmpty()) {
                Label emptyLbl = new Label(I18n.get("settings.ai.sources.empty_folders"));
                emptyLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
                foldersList.getChildren().add(emptyLbl);
            } else {
                for (String folder : folders) {
                    HBox rowCard = new HBox(10);
                    rowCard.setAlignment(Pos.CENTER_LEFT);
                    rowCard.setPadding(new Insets(8, 12, 8, 12));
                    rowCard.setStyle("-fx-background-color: #141929; -fx-border-color: #1e293b; -fx-border-radius: 8px; -fx-background-radius: 8px;");

                    Label pathLbl = new Label("📁 " + folder);
                    pathLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #f1f5f9;");
                    HBox.setHgrow(pathLbl, Priority.ALWAYS);

                    Button delBtn = new Button("🗑️");
                    delBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;");
                    delBtn.setOnAction(e -> {
                        ImageSourcesConfigManager.removeLocalFolder(folder);
                        foldersList.getChildren().remove(rowCard);
                        List<String> updated = ImageSourcesConfigManager.getLocalFolders();
                        foldersCountBadge.setText(updated.size() + " / " + ImageSourcesConfigManager.MAX_LOCAL_FOLDERS);
                        addFolderBtn.setDisable(updated.size() >= ImageSourcesConfigManager.MAX_LOCAL_FOLDERS);
                    });

                    rowCard.getChildren().addAll(pathLbl, delBtn);
                    foldersList.getChildren().add(rowCard);
                }
            }
        };

        addFolderBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(I18n.get("settings.ai.sources.folder_chooser_title"));
            File dir = chooser.showDialog(dialog);
            if (dir != null && dir.exists() && dir.isDirectory()) {
                ImageSourcesConfigManager.addLocalFolder(dir.getAbsolutePath());
                refreshFoldersList.run();
            }
        });

        refreshFoldersList.run();
        foldersSection.getChildren().addAll(foldersHeader, foldersList);

        // 5. School Web Repositories Section (Max 2)
        VBox urlsSection = new VBox(10);
        HBox urlsHeader = new HBox(12);
        urlsHeader.setAlignment(Pos.CENTER_LEFT);

        Label urlsTitle = new Label(I18n.get("settings.ai.sources.school_urls_title"));
        urlsTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #fbbf24;");

        Label urlsCountBadge = new Label();
        urlsCountBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #fef08a; -fx-background-color: #713f12; -fx-padding: 2 8; -fx-background-radius: 8;");

        HBox.setHgrow(urlsTitle, Priority.ALWAYS);
        urlsHeader.getChildren().addAll(urlsTitle, urlsCountBadge);

        HBox addUrlBox = new HBox(8);
        addUrlBox.setAlignment(Pos.CENTER_LEFT);
        TextField urlField = new TextField();
        urlField.setPromptText(I18n.get("settings.ai.sources.url_input_placeholder"));
        urlField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b; -fx-border-color: #334155; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7 12; -fx-font-size: 11px;");
        HBox.setHgrow(urlField, Priority.ALWAYS);

        Button addUrlBtn = new Button(I18n.get("settings.ai.sources.add_school_url_btn"));
        addUrlBtn.setStyle("-fx-background-color: #d97706; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 8; -fx-cursor: hand;");

        addUrlBox.getChildren().addAll(urlField, addUrlBtn);

        VBox urlsList = new VBox(6);

        Runnable refreshUrlsList = () -> {
            urlsList.getChildren().clear();
            List<String> urls = ImageSourcesConfigManager.getSchoolUrls();
            urlsCountBadge.setText(urls.size() + " / " + ImageSourcesConfigManager.MAX_SCHOOL_URLS);
            boolean limitReached = urls.size() >= ImageSourcesConfigManager.MAX_SCHOOL_URLS;
            addUrlBtn.setDisable(limitReached);
            urlField.setDisable(limitReached);

            if (urls.isEmpty()) {
                Label emptyLbl = new Label(I18n.get("settings.ai.sources.empty_urls"));
                emptyLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
                urlsList.getChildren().add(emptyLbl);
            } else {
                for (String url : urls) {
                    HBox rowCard = new HBox(10);
                    rowCard.setAlignment(Pos.CENTER_LEFT);
                    rowCard.setPadding(new Insets(8, 12, 8, 12));
                    rowCard.setStyle("-fx-background-color: #141929; -fx-border-color: #1e293b; -fx-border-radius: 8px; -fx-background-radius: 8px;");

                    Label urlLbl = new Label("🔗 " + url);
                    urlLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #f1f5f9;");
                    HBox.setHgrow(urlLbl, Priority.ALWAYS);

                    Button delBtn = new Button("🗑️");
                    delBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;");
                    delBtn.setOnAction(e -> {
                        ImageSourcesConfigManager.removeSchoolUrl(url);
                        urlsList.getChildren().remove(rowCard);
                        List<String> updated = ImageSourcesConfigManager.getSchoolUrls();
                        urlsCountBadge.setText(updated.size() + " / " + ImageSourcesConfigManager.MAX_SCHOOL_URLS);
                        addUrlBtn.setDisable(updated.size() >= ImageSourcesConfigManager.MAX_SCHOOL_URLS);
                        urlField.setDisable(updated.size() >= ImageSourcesConfigManager.MAX_SCHOOL_URLS);
                    });

                    rowCard.getChildren().addAll(urlLbl, delBtn);
                    urlsList.getChildren().add(rowCard);
                }
            }
        };

        addUrlBtn.setOnAction(e -> {
            String text = urlField.getText();
            if (text != null && !text.isBlank()) {
                ImageSourcesConfigManager.addSchoolUrl(text);
                urlField.clear();
                refreshUrlsList.run();
            }
        });
        urlField.setOnAction(e -> addUrlBtn.fire());

        refreshUrlsList.run();
        urlsSection.getChildren().addAll(urlsHeader, addUrlBox, urlsList);

        rootBox.getChildren().addAll(
            titleBox,
            toggleCard,
            strictnessCard,
            fixedSection,
            new Separator(),
            foldersSection,
            new Separator(),
            urlsSection
        );

        tab.setContent(scrollPane);
        return tab;
    }

    private static Tab createRagDocumentsTab(Stage parentStage, Stage dialog) {
        Tab tab = new Tab(I18n.get("theme.tab.rag_documents"));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox rootBox = new VBox(18);
        rootBox.setPadding(new Insets(16));
        rootBox.setStyle("-fx-background-color: transparent;");
        scrollPane.setContent(rootBox);

        // Header Title
        VBox titleBox = new VBox(4);
        Label titleLbl = new Label(I18n.get("theme.rag.title"));
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #f8fafc;");
        Label subLbl = new Label(I18n.get("theme.rag.subtitle"));
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        subLbl.setWrapText(true);
        titleBox.getChildren().addAll(titleLbl, subLbl);

        // Section Box
        VBox ragSection = new VBox(12);
        ragSection.setPadding(new Insets(16));
        ragSection.setStyle("-fx-background-color: #0f172a; -fx-border-color: #1e293b; -fx-border-radius: 12px; -fx-background-radius: 12px;");

        HBox ragHeader = new HBox(12);
        ragHeader.setAlignment(Pos.CENTER_LEFT);

        Label secTitle = new Label("DOCUMENTOS PDF CARGADOS");
        secTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        Label ragCountBadge = new Label();
        ragCountBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-background-color: #0c4a6e; -fx-padding: 2 8; -fx-background-radius: 8;");

        HBox.setHgrow(secTitle, Priority.ALWAYS);

        Button addPdfBtn = new Button(I18n.get("theme.rag.add_btn"));
        addPdfBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 8; -fx-cursor: hand;");

        ragHeader.getChildren().addAll(secTitle, ragCountBadge, addPdfBtn);

        VBox docList = new VBox(8);

        Runnable refreshDocList = () -> {
            docList.getChildren().clear();
            List<String> docs = RagDocumentsConfigManager.getRagDocuments();
            ragCountBadge.setText(docs.size() + " / " + RagDocumentsConfigManager.MAX_RAG_DOCUMENTS);
            boolean limitReached = docs.size() >= RagDocumentsConfigManager.MAX_RAG_DOCUMENTS;
            addPdfBtn.setDisable(limitReached);

            if (docs.isEmpty()) {
                Label emptyLbl = new Label(I18n.get("theme.rag.empty"));
                emptyLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 12 0;");
                docList.getChildren().add(emptyLbl);
            } else {
                for (String docPath : docs) {
                    File docFile = new File(docPath);
                    HBox rowCard = new HBox(12);
                    rowCard.setAlignment(Pos.CENTER_LEFT);
                    rowCard.setPadding(new Insets(10, 14, 10, 14));
                    rowCard.setStyle("-fx-background-color: #141929; -fx-border-color: #1e293b; -fx-border-radius: 8px; -fx-background-radius: 8px;");

                    Label iconLbl = new Label("📄");
                    iconLbl.setStyle("-fx-font-size: 16px;");

                    VBox infoBox = new VBox(2);
                    HBox.setHgrow(infoBox, Priority.ALWAYS);

                    String fileName = docFile.getName();
                    Label nameLbl = new Label(fileName);
                    nameLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

                    String sizeStr = docFile.exists() ? formatFileSize(docFile.length()) : "Archivo no encontrado";
                    Label pathLbl = new Label(docPath + " (" + sizeStr + ")");
                    pathLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

                    infoBox.getChildren().addAll(nameLbl, pathLbl);

                    Button delBtn = new Button("🗑️");
                    delBtn.setTooltip(new Tooltip(I18n.get("theme.rag.delete_tooltip")));
                    delBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
                    delBtn.setOnAction(e -> {
                        RagDocumentsConfigManager.removeRagDocument(docPath);
                        docList.getChildren().remove(rowCard);
                        List<String> updated = RagDocumentsConfigManager.getRagDocuments();
                        ragCountBadge.setText(updated.size() + " / " + RagDocumentsConfigManager.MAX_RAG_DOCUMENTS);
                        addPdfBtn.setDisable(updated.size() >= RagDocumentsConfigManager.MAX_RAG_DOCUMENTS);
                        if (updated.isEmpty()) {
                            Label emptyLbl = new Label(I18n.get("theme.rag.empty"));
                            emptyLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 12 0;");
                            docList.getChildren().add(emptyLbl);
                        }
                    });

                    rowCard.getChildren().addAll(iconLbl, infoBox, delBtn);
                    docList.getChildren().add(rowCard);
                }
            }
        };

        addPdfBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(I18n.get("theme.tab.rag_documents"));
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("theme.rag.filter"), "*.pdf", "*.PDF")
            );
            File selected = chooser.showOpenDialog(dialog);
            if (selected != null && selected.exists() && selected.isFile()) {
                String path = selected.getAbsolutePath();
                if (path.toLowerCase().endsWith(".pdf")) {
                    RagDocumentsConfigManager.addRagDocument(path);
                    refreshDocList.run();
                }
            }
        });

        refreshDocList.run();
        ragSection.getChildren().addAll(ragHeader, docList);

        rootBox.getChildren().addAll(titleBox, ragSection);
        tab.setContent(scrollPane);
        return tab;
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
