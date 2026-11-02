package com.gameshowcenter.offline.views;

import com.gameshowcenter.offline.service.MegaCoreManagerService;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.util.SvgEmoji;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Diálogo escalable y dinámico para la carga y gestión de paquetes MegaCore en la Cáscara Offline.
 * Diseñado sin elementos rígidos predefinidos para permitir la carga continua de futuros paquetes
 * (MegaCore 1, 2, 3, 4, 5...), presentando explícitamente los nombres de los juegos al cargar cada archivo.
 */
public class MegaCoreManagerDialog {

    public static void show(Stage ownerStage, File gamesDir, Runnable onPacksUpdated) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (ownerStage != null) {
            dialog.initOwner(ownerStage);
        }
        dialog.setTitle("Gestor de Paquetes MegaCore — Game Show Center v1.1");
        dialog.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24, 28, 24, 28));
        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(600);
        root.setMaxWidth(600);
        root.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 16px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 8);",
                ThemeManager.getMainBoxHex()));

        // 1. Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Node packIcon = SvgEmoji.create("package", 32);
        VBox titleBox = new VBox(2);
        Label titleLabel = new Label("GESTOR DE PAQUETES MEGACORE");
        titleLabel.setStyle(String.format("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getTextOnMainBoxPrimaryHex()));

        Label subtitleLabel = new Label("Carga módulos modulares de actividades y minijuegos (.zip) para la cáscara");
        subtitleLabel.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s;",
                ThemeManager.getTextOnMainBoxSecondaryHex()));
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        header.getChildren().addAll(packIcon, titleBox);

        // 2. Action Button Area
        VBox actionArea = new VBox(10);
        actionArea.setAlignment(Pos.CENTER);

        Button loadPackageBtn = new Button("📥 Cargar Paquete MegaCore (.zip, .megacore)");
        loadPackageBtn.setMaxWidth(Double.MAX_VALUE);
        loadPackageBtn.setStyle(String.format(
                "-fx-background-color: linear-gradient(to right, %s, derive(%s, 20%%)); -fx-text-fill: %s; -fx-font-size: 14px; -fx-font-weight: 900; -fx-padding: 12px 24px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.35), 8, 0, 0, 2);",
                ThemeManager.getAccentHex(), ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()
        ));

        // 3. Dynamic Feedback & Loaded Package Result Box
        VBox resultCard = new VBox(10);
        resultCard.setPadding(new Insets(14, 16, 14, 16));
        resultCard.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 12px; -fx-border-color: #22c55e; -fx-border-width: 1.5px; -fx-border-radius: 12px;",
                ThemeManager.getCardHex()));
        resultCard.setVisible(false);
        resultCard.setManaged(false);

        Label statusNoticeLabel = new Label("");
        statusNoticeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        statusNoticeLabel.setVisible(false);
        statusNoticeLabel.setManaged(false);

        // 4. Installed Activities Section Header
        HBox installedHeader = new HBox(10);
        installedHeader.setAlignment(Pos.CENTER_LEFT);
        Label installedTitle = new Label("ACTIVIDADES DISPONIBLES EN LA CÁSCARA");
        installedTitle.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getTextOnMainBoxPrimaryHex()));
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label installedBadge = new Label("Total: 0");
        installedBadge.setStyle("-fx-background-color: rgba(99, 102, 241, 0.2); -fx-text-fill: #a5b4fc; -fx-font-size: 11px; -fx-font-weight: 900; -fx-padding: 3px 8px; -fx-background-radius: 6px;");
        installedHeader.getChildren().addAll(installedTitle, spacer1, installedBadge);

        // Scrollable List of Installed Activities
        VBox activitiesList = new VBox(6);
        activitiesList.setAlignment(Pos.TOP_CENTER);

        ScrollPane activitiesScroll = new ScrollPane(activitiesList);
        activitiesScroll.setFitToWidth(true);
        activitiesScroll.setPrefHeight(200);
        activitiesScroll.setMaxHeight(230);
        activitiesScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        // Helper to refresh installed activities list
        Runnable refreshInstalledList = () -> {
            activitiesList.getChildren().clear();
            List<MegaCoreManagerService.InstalledActivity> activities =
                    MegaCoreManagerService.getInstance().getInstalledActivities(gamesDir);

            installedBadge.setText("Total: " + activities.size());

            if (activities.isEmpty()) {
                VBox emptyCard = new VBox(6);
                emptyCard.setAlignment(Pos.CENTER);
                emptyCard.setPadding(new Insets(24));
                emptyCard.setStyle(String.format(
                        "-fx-background-color: %s; -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 12px;",
                        ThemeManager.getCardHex()));

                Label emptyTitle = new Label("📦 No hay actividades instaladas todavía");
                emptyTitle.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: %s;",
                        ThemeManager.getTextOnCardPrimaryHex()));

                Label emptySub = new Label("Usa el botón de arriba para cargar cualquier paquete MegaCore (.zip).\nLa cáscara incorporará de inmediato todos los juegos y sus assets.");
                emptySub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                emptySub.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s;",
                        ThemeManager.getTextOnCardSecondaryHex()));

                emptyCard.getChildren().addAll(emptyTitle, emptySub);
                activitiesList.getChildren().add(emptyCard);
            } else {
                for (MegaCoreManagerService.InstalledActivity act : activities) {
                    HBox itemRow = new HBox(10);
                    itemRow.setAlignment(Pos.CENTER_LEFT);
                    itemRow.setPadding(new Insets(8, 12, 8, 12));
                    itemRow.setStyle(String.format(
                            "-fx-background-color: %s; -fx-background-radius: 8px; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 8px;",
                            ThemeManager.getCardHex()));

                    Node iconNode = SvgEmoji.create("controller", 18);

                    VBox nameBox = new VBox(1);
                    Label nameLbl = new Label(act.getName());
                    nameLbl.setStyle(String.format("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: %s;",
                            ThemeManager.getTextOnCardPrimaryHex()));

                    Label folderLbl = new Label("Carpeta: games/" + act.getFolderName());
                    folderLbl.setStyle(String.format("-fx-font-size: 10px; -fx-text-fill: %s;",
                            ThemeManager.getTextOnCardSecondaryHex()));
                    nameBox.getChildren().addAll(nameLbl, folderLbl);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label readyBadge = new Label("LISTO");
                    readyBadge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.15); -fx-text-fill: #22c55e; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 2px 7px; -fx-background-radius: 4px;");

                    itemRow.getChildren().addAll(iconNode, nameBox, spacer, readyBadge);
                    activitiesList.getChildren().add(itemRow);
                }
            }
        };

        refreshInstalledList.run();

        // Load Package Action
        loadPackageBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleccionar Paquete de Juegos MegaCore");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Paquetes MegaCore (*.zip, *.megacore)", "*.zip", "*.megacore"),
                    new FileChooser.ExtensionFilter("Todos los archivos (*.*)", "*.*")
            );

            File initialDir = MegaCoreManagerService.getInstance().resolveMegacoresDir();
            if (initialDir != null && initialDir.exists()) {
                chooser.setInitialDirectory(initialDir);
            }

            File selected = chooser.showOpenDialog(dialog);
            if (selected != null) {
                loadPackageBtn.setDisable(true);
                statusNoticeLabel.setVisible(true);
                statusNoticeLabel.setManaged(true);
                statusNoticeLabel.setText("⏳ Descomprimiendo paquete e instalando juegos y recursos vinculados...");
                statusNoticeLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: bold;");

                CompletableFuture.supplyAsync(() -> MegaCoreManagerService.getInstance().installPackage(selected, gamesDir))
                        .thenAccept(res -> Platform.runLater(() -> {
                            loadPackageBtn.setDisable(false);
                            statusNoticeLabel.setVisible(false);
                            statusNoticeLabel.setManaged(false);

                            if (res.isSuccess()) {
                                resultCard.getChildren().clear();
                                resultCard.setVisible(true);
                                resultCard.setManaged(true);

                                // Header of Result Card
                                HBox resHeader = new HBox(8);
                                resHeader.setAlignment(Pos.CENTER_LEFT);

                                Label packTitle = new Label("📦 " + res.getTitle().toUpperCase());
                                packTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #22c55e;");

                                Region sp = new Region();
                                HBox.setHgrow(sp, Priority.ALWAYS);

                                Label successBadge = new Label("INSTALADO CON ÉXITO");
                                successBadge.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 3px 8px; -fx-background-radius: 6px;");

                                resHeader.getChildren().addAll(packTitle, sp, successBadge);

                                Label introLbl = new Label("Se han incorporado las siguientes actividades a tu sistema:");
                                introLbl.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: %s;",
                                        ThemeManager.getTextOnCardSecondaryHex()));

                                // FlowPane displaying the names of the games
                                FlowPane gamesFlow = new FlowPane(8, 8);
                                gamesFlow.setAlignment(Pos.CENTER_LEFT);
                                for (String gameName : res.getInstalledGames()) {
                                    HBox pill = new HBox(6);
                                    pill.setAlignment(Pos.CENTER);
                                    pill.setPadding(new Insets(4, 10, 4, 10));
                                    pill.setStyle("-fx-background-color: rgba(34, 197, 94, 0.15); -fx-background-radius: 8px; -fx-border-color: rgba(34, 197, 94, 0.4); -fx-border-radius: 8px;");

                                    Node pillIcon = SvgEmoji.create("controller", 12);
                                    Label gameLbl = new Label(gameName);
                                    gameLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #4ade80;");

                                    pill.getChildren().addAll(pillIcon, gameLbl);
                                    gamesFlow.getChildren().add(pill);
                                }

                                resultCard.getChildren().addAll(resHeader, introLbl, gamesFlow);

                                if (res.getAssetsExtracted() > 0) {
                                    Label assetsNote = new Label(String.format("🎨 %d recursos visuales y sonoros vinculados han sido integrados en assets/.", res.getAssetsExtracted()));
                                    assetsNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
                                    resultCard.getChildren().add(assetsNote);
                                }

                                refreshInstalledList.run();
                                if (onPacksUpdated != null) {
                                    onPacksUpdated.run();
                                }
                            } else {
                                statusNoticeLabel.setVisible(true);
                                statusNoticeLabel.setManaged(true);
                                statusNoticeLabel.setText("❌ " + res.getMessage());
                                statusNoticeLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
                            }
                        }));
            }
        });

        // 5. Close Button
        Button closeBtn = new Button("Cerrar");
        closeBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8px 24px; -fx-background-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()
        ));
        closeBtn.setOnAction(e -> dialog.close());

        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.getChildren().add(closeBtn);

        actionArea.getChildren().addAll(loadPackageBtn, statusNoticeLabel, resultCard);

        root.getChildren().addAll(header, actionArea, installedHeader, activitiesScroll, bottomBar);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }
}
