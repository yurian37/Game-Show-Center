package com.gameshowcenter.offline.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.games.GameEditorRegistry;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.model.GameDescriptor;
import com.gameshowcenter.offline.model.MatchConfig;
import com.gameshowcenter.offline.service.AIService;
import com.gameshowcenter.offline.theme.ImageSourcesConfigManager;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.util.FileChooserHelper;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.util.Duration;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFxView extends ScrollPane {

    public interface SettingsListener {
        void onStartMatchConfigured(MatchConfig config);

        void onBackToHome();
    }

    // Dedicate almost all CPU cores to the validation pool, reserving at least 1 core for the JavaFX UI / loading animation thread
    private static final int THREAD_POOL_SIZE = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    private static final ExecutorService VALIDATION_EXECUTOR = Executors.newFixedThreadPool(
            THREAD_POOL_SIZE,
            r -> {
                Thread t = new Thread(r, "GameValidationWorker");
                t.setDaemon(true);
                return t;
            }
    );

    private record GameValidationError(String gameName, String errorMessage) {}
    private record ValidationSaveResult(String error, JsonNode updatedSetup) {}

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<GameDescriptor> availableGames;
    private final List<GameDescriptor> selectedGames = new ArrayList<>();
    private final SettingsListener listener;
    private final Runnable i18nListener = this::refreshTextsAndStyles;

    private String selectedGameMode = "1vs1";
    private int teamPlayerCount = 2;
    private int ffaPlayerCount = 4;

    private final VBox competitorsContainer = new VBox(10);
    private final List<CompetitorInputRow> competitorRows = new ArrayList<>();
    private final VBox gameCardsContainer = new VBox(12);
    private final List<String> scorePresets = new ArrayList<>(Arrays.asList("+10", "+20", "+50", "-10", "-20"));
    private FlowPane scoreChipsPane;
    private TextField newPresetInput;
    private Button addPresetBtn;
    private Button resetPresetsBtn;
    private Label scorePresetsSubtitle;
    private final VBox contentBox;

    private Label titleLabel;
    private Label subLabel;
    private VBox modeBox;
    private Label modeTitle;
    private Button btn1v1;
    private Button btnTeam;
    private Button btnFfa;
    private HBox teamCountBox;
    private Label teamLabel;
    private ComboBox<Integer> teamCombo;
    private HBox ffaCountBox;
    private Label ffaLabel;
    private ComboBox<Integer> ffaCombo;

    private boolean battleRoyale = false;
    private VBox battleRoyaleCard;
    private Label battleRoyaleTitle;
    private Label battleRoyaleBadge;
    private Label battleRoyaleHelpLabel;
    private Button battleRoyaleToggleBtn;
    private VBox competitorsBox;
    private Label competitorsTitle;
    private VBox scorePresetsBox;
    private Label scorePresetsTitle;

    private VBox gamesBox;
    private Label gamesTitle;
    private Button importSetupBtn;
    private Button exportSetupBtn;
    private Button resetSetupBtn;
    private Button backBtn;
    private Button launchBtn;
    private StackPane centerWrap;
    private Label modeHelpLabel;
    private Label competitorsSubtitle;
    private Label scoreHelpLabel;
    private Label gamesSubtitle;

    private static class CompetitorInputRow {
        String id;
        TextField nameInput;
        String avatarPath = null;
        ImageView avatarPreview;
        HBox box;
        Button chooseAvatarBtn;
    }

    public SettingsFxView(List<GameDescriptor> games, SettingsListener listener) {
        this.availableGames = games != null ? games : new ArrayList<>();
        this.listener = listener;

        resetGameSelections();
        I18n.addListener(i18nListener);

        setFitToWidth(true);
        setFitToHeight(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        contentBox = new VBox(20);
        contentBox.setPadding(new Insets(24));
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.prefWidthProperty().bind(widthProperty().subtract(48));
        contentBox.maxWidthProperty().bind(widthProperty().subtract(48));
        contentBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 20px; -fx-background-radius: 20px;",
                ThemeManager.getMainBoxHex()));

        VBox titleBox = new VBox(6);
        titleBox.setAlignment(Pos.CENTER);
        titleLabel = new Label(I18n.get("settings.title"));
        titleLabel.setStyle(String.format("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getTextOnMainBoxPrimaryHex()));

        subLabel = new Label(I18n.get("settings.subtitle"));
        subLabel.setStyle(String.format("-fx-font-size: 13px; -fx-text-fill: %s; -fx-wrap-text: true; -fx-text-alignment: center;",
                ThemeManager.getTextOnMainBoxSecondaryHex()));

        HBox setupActionsBar = new HBox(10);
        setupActionsBar.setAlignment(Pos.CENTER);
        setupActionsBar.setPadding(new Insets(4, 0, 8, 0));

        importSetupBtn = new Button(I18n.get("settings.btn.import_setup"));
        importSetupBtn.setTooltip(new Tooltip(I18n.get("settings.help.import_btn_tooltip")));
        importSetupBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #a855f7; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: #7e22ce; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 16px; -fx-cursor: hand;",
                ThemeManager.getButtonHex()));
        importSetupBtn.setOnAction(e -> handleImportGeneralSetupJson());

        exportSetupBtn = new Button(I18n.get("settings.btn.export_setup"));
        exportSetupBtn.setTooltip(new Tooltip(I18n.get("settings.help.export_btn_tooltip")));
        exportSetupBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: #0284c7; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 16px; -fx-cursor: hand;",
                ThemeManager.getButtonHex()));
        exportSetupBtn.setOnAction(e -> handleExportGeneralSetupJson());

        resetSetupBtn = new Button(I18n.get("settings.btn.reset_setup"));
        resetSetupBtn.setTooltip(new Tooltip(I18n.get("settings.help.reset_btn_tooltip")));
        resetSetupBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 16px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        resetSetupBtn.setOnAction(e -> handleResetSetup());

        setupActionsBar.getChildren().addAll(importSetupBtn, exportSetupBtn, resetSetupBtn);
        titleBox.getChildren().addAll(titleLabel, subLabel, setupActionsBar);

        modeBox = new VBox(10);
        modeBox.setPadding(new Insets(16));
        modeBox.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.98));
        modeBox.setMaxWidth(Double.MAX_VALUE);
        modeBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;",
                ThemeManager.getCardHex()));

        modeTitle = new Label(I18n.get("settings.mode.title"));
        modeTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;",
                ThemeManager.getAccentHex()));

        modeHelpLabel = new Label(I18n.get("settings.mode.help"));
        modeHelpLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-wrap-text: true;");

        HBox modePills = new HBox(12);
        modePills.setAlignment(Pos.CENTER_LEFT);
        modePills.prefWidthProperty().bind(modeBox.widthProperty().multiply(0.98));

        btn1v1 = createPillButton(I18n.get("settings.mode.1v1"), "1vs1");
        btn1v1.setTooltip(new Tooltip(I18n.get("settings.mode.1v1.hint")));
        btnTeam = createPillButton(I18n.get("settings.mode.teams"), "team");
        btnTeam.setTooltip(new Tooltip(I18n.get("settings.mode.teams.hint")));
        btnFfa = createPillButton(I18n.get("settings.mode.ffa"), "FREE_FOR_ALL");
        btnFfa.setTooltip(new Tooltip(I18n.get("settings.mode.ffa.hint")));

        HBox.setHgrow(btn1v1, Priority.ALWAYS);
        HBox.setHgrow(btnTeam, Priority.ALWAYS);
        HBox.setHgrow(btnFfa, Priority.ALWAYS);
        btn1v1.setMaxWidth(Double.MAX_VALUE);
        btnTeam.setMaxWidth(Double.MAX_VALUE);
        btnFfa.setMaxWidth(Double.MAX_VALUE);

        teamCountBox = new HBox(10);
        teamCountBox.setAlignment(Pos.CENTER_LEFT);
        teamLabel = new Label(I18n.get("settings.mode.teams") + ":");
        teamLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 12px;",
                ThemeManager.getTextOnCardSecondaryHex()));

        teamCombo = new ComboBox<>();
        teamCombo.getItems().addAll(2, 3, 4, 5, 6, 7, 8);
        teamCombo.setValue(teamPlayerCount);
        teamCombo.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s;", ThemeManager.getButtonHex(),
                ThemeManager.getTextOnButtonPrimaryHex()));
        teamCombo.setOnAction(e -> {
            if (teamCombo.getValue() != null) {
                teamPlayerCount = teamCombo.getValue();
                renderCompetitorInputs();
            }
        });
        teamCountBox.getChildren().addAll(teamLabel, teamCombo);
        teamCountBox.setVisible(false);

        ffaCountBox = new HBox(10);
        ffaCountBox.setAlignment(Pos.CENTER_LEFT);
        ffaLabel = new Label(I18n.get("settings.mode.ffa.count"));
        ffaLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 12px;",
                ThemeManager.getTextOnCardSecondaryHex()));

        ffaCombo = new ComboBox<>();
        for (int i = 3; i <= 30; i++) {
            ffaCombo.getItems().add(i);
        }
        ffaCombo.setValue(ffaPlayerCount);
        ffaCombo.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s;", ThemeManager.getButtonHex(),
                ThemeManager.getTextOnButtonPrimaryHex()));
        ffaCombo.setOnAction(e -> {
            if (ffaCombo.getValue() != null) {
                ffaPlayerCount = ffaCombo.getValue();
                renderCompetitorInputs();
            }
        });

        ffaCountBox.getChildren().addAll(ffaLabel, ffaCombo);
        ffaCountBox.setVisible(false);

        updatePillSelection(btn1v1, btnTeam, btnFfa, "1vs1");

        btn1v1.setOnAction(e -> {
            selectedGameMode = "1vs1";
            teamCountBox.setVisible(false);
            ffaCountBox.setVisible(false);
            updatePillSelection(btn1v1, btnTeam, btnFfa, "1vs1");
            resetGameSelections();
            renderCompetitorInputs();
            renderGameCards();
        });
        btnTeam.setOnAction(e -> {
            selectedGameMode = "team";
            teamCountBox.setVisible(true);
            ffaCountBox.setVisible(false);
            updatePillSelection(btn1v1, btnTeam, btnFfa, "team");
            resetGameSelections();
            renderCompetitorInputs();
            renderGameCards();
        });
        btnFfa.setOnAction(e -> {
            selectedGameMode = "FREE_FOR_ALL";
            teamCountBox.setVisible(false);
            ffaCountBox.setVisible(true);
            updatePillSelection(btn1v1, btnTeam, btnFfa, "FREE_FOR_ALL");
            resetGameSelections();
            renderCompetitorInputs();
            renderGameCards();
        });

        modePills.getChildren().addAll(btn1v1, btnTeam, btnFfa);

        modeBox.getChildren().addAll(modeTitle, modeHelpLabel, modePills, teamCountBox, ffaCountBox);

        competitorsBox = new VBox(12);
        competitorsBox.setPadding(new Insets(16));
        competitorsBox.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.98));
        competitorsBox.setMaxWidth(Double.MAX_VALUE);
        competitorsBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;",
                ThemeManager.getCardHex()));

        competitorsTitle = new Label(I18n.get("settings.players.title"));
        competitorsTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;",
                ThemeManager.getAccentHex()));

        competitorsSubtitle = new Label(I18n.get("settings.players.help"));
        competitorsSubtitle.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s; -fx-wrap-text: true;",
                ThemeManager.getTextOnCardSecondaryHex()));

        renderCompetitorInputs();
        competitorsBox.getChildren().addAll(competitorsTitle, competitorsSubtitle, competitorsContainer);

        scorePresetsBox = new VBox(12);
        scorePresetsBox.setPadding(new Insets(16));
        scorePresetsBox.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.98));
        scorePresetsBox.setMaxWidth(Double.MAX_VALUE);
        scorePresetsBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;",
                ThemeManager.getCardHex()));

        HBox scoreHeaderBox = new HBox(8);
        scoreHeaderBox.setAlignment(Pos.CENTER_LEFT);

        scorePresetsTitle = new Label(I18n.get("settings.score.title"));
        scorePresetsTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;",
                ThemeManager.getAccentHex()));

        Region scoreSpacer = new Region();
        HBox.setHgrow(scoreSpacer, Priority.ALWAYS);

        resetPresetsBtn = new Button("🔄 " + I18n.get("settings.score.reset_btn"));
        resetPresetsBtn.setFocusTraversable(false);
        resetPresetsBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 8px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        resetPresetsBtn.setOnAction(e -> {
            e.consume();
            scorePresets.clear();
            scorePresets.addAll(Arrays.asList("+10", "+20", "+50", "-10", "-20"));
            renderScorePresetChips();
        });

        scoreHeaderBox.getChildren().addAll(scorePresetsTitle, scoreSpacer, resetPresetsBtn);

        scorePresetsSubtitle = new Label(I18n.get("settings.score.subtitle"));
        scorePresetsSubtitle.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s;",
                ThemeManager.getTextOnCardSecondaryHex()));

        scoreHelpLabel = new Label(I18n.get("settings.score.help"));
        scoreHelpLabel.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s; -fx-wrap-text: true;",
                ThemeManager.getTextOnCardSecondaryHex()));

        scoreChipsPane = new FlowPane(8, 8);
        scoreChipsPane.setAlignment(Pos.CENTER_LEFT);
        renderScorePresetChips();

        HBox addPresetRow = new HBox(10);
        addPresetRow.setAlignment(Pos.CENTER_LEFT);

        newPresetInput = new TextField();
        newPresetInput.setPromptText(I18n.get("settings.score.input_placeholder"));
        newPresetInput.setPrefWidth(160);
        newPresetInput.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 8px; -fx-padding: 8px 12px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));

        newPresetInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^[+-]?\\d{0,6}$")) {
                return change;
            }
            return null;
        }));

        addPresetBtn = new Button("➕ " + I18n.get("settings.score.add_btn"));
        addPresetBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));

        Runnable handleAddPreset = () -> {
            String text = newPresetInput.getText().trim();
            if (text.isEmpty() || text.equals("+") || text.equals("-")) return;
            try {
                int val = Integer.parseInt(text.replace("+", ""));
                if (val == 0) {
                    showScoreOverlay(I18n.get("settings.modal.score_invalid_title"), I18n.get("settings.modal.score_invalid_desc"), null);
                    return;
                }
                String formatted = (val > 0 ? "+" : "") + val;
                if (scorePresets.contains(formatted)) {
                    showScoreOverlay(I18n.get("settings.modal.score_duplicate_title"), String.format(I18n.get("settings.modal.score_duplicate_desc"), formatted), formatted);
                    return;
                }
                scorePresets.add(formatted);
                newPresetInput.clear();
                renderScorePresetChips();
            } catch (Exception ex) {
                showScoreOverlay(I18n.get("settings.modal.score_invalid_title"), I18n.get("settings.modal.score_invalid_desc"), null);
            }
        };

        newPresetInput.setOnAction(e -> handleAddPreset.run());
        addPresetBtn.setOnAction(e -> handleAddPreset.run());

        HBox quickAddBox = new HBox(6);
        quickAddBox.setAlignment(Pos.CENTER_LEFT);
        String[] quickValues = new String[]{ "+5", "+25", "+100", "-5", "-25", "-100" };
        for (String qv : quickValues) {
            Button qb = new Button(qv);
            qb.setFocusTraversable(false);
            boolean isNeg = qv.startsWith("-");
            qb.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 6px;",
                    ThemeManager.getButtonHex(), isNeg ? "#f43f5e" : "#10b981"));
            qb.setOnAction(e -> {
                e.consume();
                if (scorePresets.contains(qv)) {
                    showScoreOverlay(I18n.get("settings.modal.score_duplicate_title"), String.format(I18n.get("settings.modal.score_duplicate_desc"), qv), qv);
                } else {
                    scorePresets.add(qv);
                    renderScorePresetChips();
                }
            });
            quickAddBox.getChildren().add(qb);
        }

        addPresetRow.getChildren().addAll(newPresetInput, addPresetBtn, quickAddBox);

        scorePresetsBox.getChildren().addAll(scoreHeaderBox, scorePresetsSubtitle, scoreHelpLabel, scoreChipsPane, addPresetRow);

        gamesBox = new VBox(12);
        gamesBox.setPadding(new Insets(16));
        gamesBox.prefWidthProperty().bind(contentBox.widthProperty().multiply(0.98));
        gamesBox.setMaxWidth(Double.MAX_VALUE);
        gamesBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;",
                ThemeManager.getCardHex()));

        gamesTitle = new Label(I18n.get("settings.games.title"));
        gamesTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;",
                ThemeManager.getAccentHex()));

        gamesSubtitle = new Label(I18n.get("settings.games.help"));
        gamesSubtitle.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s; -fx-wrap-text: true;",
                ThemeManager.getTextOnCardSecondaryHex()));

        centerWrap = new StackPane(contentBox);
        centerWrap.setAlignment(Pos.TOP_CENTER);
        centerWrap.setPadding(new Insets(16));

        backBtn = new Button(I18n.get("settings.btn.main_menu"));
        backBtn.setPrefHeight(40);
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        backBtn.setEllipsisString("");
        backBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: 800; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8px 16px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        backBtn.setOnAction(e -> {
            saveCurrentSetupToCache();
            if (listener != null)
                listener.onBackToHome();
        });

        launchBtn = new Button(I18n.get("settings.btn.start_game"));
        launchBtn.setPrefHeight(44);
        launchBtn.setMaxWidth(Double.MAX_VALUE);
        launchBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        launchBtn.setEllipsisString("");
        launchBtn.setStyle(String.format(
                "-fx-background-color: linear-gradient(to right, %s, derive(%s, 20%%)); -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: 900; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 10px 16px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 0, 2);",
                ThemeManager.getAccentHex(), ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        launchBtn.setOnAction(e -> {
            if (selectedGames.isEmpty()) {
                StackPane overlay = createNoGamesSelectedOverlay(centerWrap);
                if (!centerWrap.getChildren().contains(overlay)) {
                    centerWrap.getChildren().add(overlay);
                }
                return;
            }

            StackPane loadingOverlay = createLoadingOverlay(I18n.get("settings.loading.validating_match"));
            if (!centerWrap.getChildren().contains(loadingOverlay)) {
                centerWrap.getChildren().add(loadingOverlay);
            }

            List<Competitor> activeProfiles = getCompetitorProfiles();
            List<GameDescriptor> gamesToValidate = new ArrayList<>(selectedGames);

            long startTime = System.currentTimeMillis();
            List<CompletableFuture<GameValidationError>> futures = gamesToValidate.stream()
                .map(game -> CompletableFuture.supplyAsync(() -> {
                    IGameSetupEditor editor = GameEditorRegistry.getEditor(game);
                    JsonNode setupData = game.getSetupData();
                    if (setupData == null) {
                        File setupFile = new File("games/" + game.getName() + "/setup.json");
                        if (!setupFile.exists())
                            setupFile = new File("template-offline/games/" + game.getName() + "/setup.json");
                        if (setupFile.exists()) {
                            try {
                                setupData = objectMapper.readTree(setupFile);
                                game.setSetupData(setupData);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    boolean isBr = setupData != null && setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean();
                    String error = editor.validateSetupData(setupData, activeProfiles, isBr);
                    return error != null ? new GameValidationError(game.getName(), error) : null;
                }, VALIDATION_EXECUTOR))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(v -> {
                    for (CompletableFuture<GameValidationError> f : futures) {
                        GameValidationError err = f.join();
                        if (err != null) return err;
                    }
                    return null;
                }, VALIDATION_EXECUTOR)
                .thenAcceptAsync(validationError -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long remaining = Math.max(0, 850 - elapsed);
                    if (remaining > 0) {
                        try {
                            Thread.sleep(remaining);
                        } catch (InterruptedException ignored) {
                        }
                    }

                    Platform.runLater(() -> {
                        centerWrap.getChildren().remove(loadingOverlay);

                        if (validationError != null) {
                            StackPane errorOverlay = createValidationErrorOverlay(centerWrap, validationError.gameName(), validationError.errorMessage());
                            if (!centerWrap.getChildren().contains(errorOverlay)) {
                                centerWrap.getChildren().add(errorOverlay);
                            }
                            return;
                        }

                        if (listener != null) {
                            MatchConfig config = new MatchConfig();
                            config.setGameMode(selectedGameMode);
                            config.setProfiles(activeProfiles);

                            config.setScorePresets(new ArrayList<>(scorePresets.isEmpty() ? Arrays.asList("+10", "+20", "+50", "-10", "-20") : scorePresets));

                            List<GameDescriptor> orderedGames = new ArrayList<>(selectedGames);
                            orderedGames.sort(Comparator.comparingInt(GameDescriptor::getPlayOrder));
                            config.setSelectedGames(orderedGames);

                            boolean anyBR = orderedGames.stream().anyMatch(g -> {
                                JsonNode sd = g.getSetupData();
                                return sd != null && sd.has("battleRoyale") && sd.get("battleRoyale").asBoolean();
                            });
                            config.setBattleRoyale(anyBR);

                            saveCurrentSetupToCache();
                            listener.onStartMatchConfigured(config);
                        }
                    });
                }, VALIDATION_EXECUTOR)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        centerWrap.getChildren().remove(loadingOverlay);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Game Show Center");
                        alert.setHeaderText("Error de Validación");
                        alert.setContentText(ex.getMessage() != null ? ex.getMessage() : ex.toString());
                        alert.showAndWait();
                    });
                    return null;
                });
        });

        renderGameCards();
        gamesBox.getChildren().addAll(gamesTitle, gamesSubtitle, gameCardsContainer);

        contentBox.getChildren().addAll(titleBox, modeBox, competitorsBox, scorePresetsBox, gamesBox);

        setContent(centerWrap);

        // Auto-restore last used match setup if available
        loadCachedSetupIfExists();
    }

    private StackPane createNoGamesSelectedOverlay(StackPane rootPane) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.82);");
        overlay.setAlignment(Pos.CENTER);

        // Compact Square Dimensions (350x350)
        VBox modalCard = new VBox(16);
        modalCard.setPrefSize(350, 350);
        modalCard.setMaxSize(350, 350);
        modalCard.setAlignment(Pos.CENTER);
        modalCard.setPadding(new Insets(24));
        modalCard.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
                ThemeManager.getCardHex(),
                ThemeManager.getAccentHex()));

        // Sadly Piece Icon Box (Loading from assets/icons/sadly-piece.png)
        File sadlyIconFile = new File("assets/icons/sadly-piece.png");
        if (!sadlyIconFile.exists())
            sadlyIconFile = new File("template-offline/assets/icons/sadly-piece.png");

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(70, 70);
        iconCircle.setMaxSize(70, 70);
        iconCircle.setStyle(String.format(
                "-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: %s; -fx-border-radius: 18px; -fx-background-radius: 18px;",
                ThemeManager.getAccentHex()));

        if (sadlyIconFile.exists()) {
            try {
                ImageView imgView = new ImageView(new Image(sadlyIconFile.toURI().toString()));
                imgView.setFitWidth(56);
                imgView.setFitHeight(56);
                imgView.setPreserveRatio(true);
                iconCircle.getChildren().add(imgView);
            } catch (Exception e) {
                Label fallBackIcon = new Label("😢");
                fallBackIcon.setStyle("-fx-font-size: 32px;");
                iconCircle.getChildren().add(fallBackIcon);
            }
        } else {
            Label fallBackIcon = new Label("😢");
            fallBackIcon.setStyle("-fx-font-size: 32px;");
            iconCircle.getChildren().add(fallBackIcon);
        }

        VBox textGroup = new VBox(6);
        textGroup.setAlignment(Pos.CENTER);

        Label titleL = new Label(I18n.get("settings.modal.no_games_title"));
        titleL.setStyle(String.format("-fx-font-size: 17px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));

        Label descL = new Label(I18n.get("settings.modal.no_games_desc"));
        descL.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s; -fx-text-alignment: center;",
                ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));
        descL.setWrapText(true);

        textGroup.getChildren().addAll(titleL, descL);

        Button gotItBtn = new Button(I18n.get("settings.modal.got_it"));
        gotItBtn.setMaxWidth(Double.MAX_VALUE);
        gotItBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 10px 24px; -fx-background-radius: 10px; -fx-cursor: hand;",
                ThemeManager.getAccentHex()));

        File confirmIconFile = new File("assets/icons/button_confirm.png");
        if (!confirmIconFile.exists())
            confirmIconFile = new File("template-offline/assets/icons/button_confirm.png");

        if (confirmIconFile.exists()) {
            try {
                ImageView confirmImg = new ImageView(new Image(confirmIconFile.toURI().toString()));
                confirmImg.setFitWidth(18);
                confirmImg.setFitHeight(18);
                confirmImg.setPreserveRatio(true);
                gotItBtn.setGraphic(confirmImg);
            } catch (Exception ignored) {
            }
        }
        gotItBtn.setOnAction(ev -> rootPane.getChildren().remove(overlay));

        modalCard.getChildren().addAll(iconCircle, textGroup, gotItBtn);
        overlay.getChildren().add(modalCard);

        return overlay;
    }

    private StackPane createValidationErrorOverlay(StackPane rootPane, String gameName, String errorMessage) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.82);");
        overlay.setAlignment(Pos.CENTER);

        // Compact Square Dimensions (360x360)
        VBox modalCard = new VBox(16);
        modalCard.setPrefSize(360, 360);
        modalCard.setMaxSize(360, 360);
        modalCard.setAlignment(Pos.CENTER);
        modalCard.setPadding(new Insets(24));
        modalCard.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: #f43f5e; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
                ThemeManager.getCardHex()));

        // Sadly Piece Icon Box
        File sadlyIconFile = new File("assets/icons/sadly-piece.png");
        if (!sadlyIconFile.exists())
            sadlyIconFile = new File("template-offline/assets/icons/sadly-piece.png");

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(70, 70);
        iconCircle.setMaxSize(70, 70);
        iconCircle.setStyle(
                "-fx-background-color: rgba(244, 63, 94, 0.12); -fx-border-color: rgba(244, 63, 94, 0.4); -fx-border-radius: 18px; -fx-background-radius: 18px;");

        if (sadlyIconFile.exists()) {
            try {
                ImageView imgView = new ImageView(new Image(sadlyIconFile.toURI().toString()));
                imgView.setFitWidth(54);
                imgView.setFitHeight(54);
                imgView.setPreserveRatio(true);
                iconCircle.getChildren().add(imgView);
            } catch (Exception e) {
                Label fallBackIcon = new Label("⚠️");
                fallBackIcon.setStyle("-fx-font-size: 32px;");
                iconCircle.getChildren().add(fallBackIcon);
            }
        } else {
            Label fallBackIcon = new Label("⚠️");
            fallBackIcon.setStyle("-fx-font-size: 32px;");
            iconCircle.getChildren().add(fallBackIcon);
        }

        VBox textGroup = new VBox(6);
        textGroup.setAlignment(Pos.CENTER);

        Label titleL = new Label(String.format(I18n.get("settings.games.setup_error_prefix"), gameName));
        titleL.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #f43f5e;");

        Label descL = new Label(errorMessage);
        descL.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1; -fx-text-alignment: center;");
        descL.setWrapText(true);

        textGroup.getChildren().addAll(titleL, descL);

        Button fixBtn = new Button(I18n.get("settings.btn.fix_setup"));
        fixBtn.setMaxWidth(Double.MAX_VALUE);
        fixBtn.setStyle(
                "-fx-background-color: #f43f5e; -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 10px 24px; -fx-background-radius: 10px; -fx-cursor: hand;");

        File confirmIconFile = new File("assets/icons/button_confirm.png");
        if (!confirmIconFile.exists())
            confirmIconFile = new File("template-offline/assets/icons/button_confirm.png");

        if (confirmIconFile.exists()) {
            try {
                ImageView confirmImg = new ImageView(new Image(confirmIconFile.toURI().toString()));
                confirmImg.setFitWidth(18);
                confirmImg.setFitHeight(18);
                confirmImg.setPreserveRatio(true);
                fixBtn.setGraphic(confirmImg);
            } catch (Exception ignored) {
            }
        }
        fixBtn.setOnAction(ev -> rootPane.getChildren().remove(overlay));

        modalCard.getChildren().addAll(iconCircle, textGroup, fixBtn);
        overlay.getChildren().add(modalCard);

        return overlay;
    }

    private void openGameInstructionsModal(GameDescriptor game) {
        if (game == null) return;
        StackPane overlay = createInstructionsOverlay(centerWrap, game);
        if (!centerWrap.getChildren().contains(overlay)) {
            centerWrap.getChildren().add(overlay);
        }
    }

    private StackPane createInstructionsOverlay(StackPane rootPane, GameDescriptor activeGame) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                rootPane.getChildren().remove(overlay);
            }
        });

        VBox modalCard = new VBox(16);
        modalCard.setPrefSize(560, 500);
        modalCard.setMaxSize(560, 520);
        modalCard.setAlignment(Pos.TOP_CENTER);
        modalCard.setPadding(new Insets(24));
        modalCard.setStyle(String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
            ThemeManager.getCardHex(),
            ThemeManager.getAccentHex()
        ));

        String gameName = activeGame.getName();
        String rulesWord = I18n.get("arena.btn.rules").replace("📖", "").trim();
        Label titleLabel = new Label("📖 " + gameName.toUpperCase() + " - " + rulesWord);
        titleLabel.setStyle(String.format("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: %s;", ThemeManager.getTextOnCardPrimaryHex()));

        HBox langRow = new HBox(8);
        langRow.setAlignment(Pos.CENTER);

        final String[] selectedLang = { I18n.getLanguage() };

        Label fallbackNotice = new Label("");
        fallbackNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.15); -fx-padding: 4px 10px; -fx-background-radius: 6px;");
        fallbackNotice.setVisible(false);
        fallbackNotice.setManaged(false);

        VBox contentList = new VBox(10);
        contentList.setPadding(new Insets(8));

        Label descLabel = new Label("");
        descLabel.setStyle(String.format("-fx-font-size: 13px; -fx-text-fill: %s; -fx-font-style: italic;", ThemeManager.getTextOnCardSecondaryHex()));
        descLabel.setWrapText(true);

        ScrollPane scrollContent = new ScrollPane();
        scrollContent.setContent(contentList);
        scrollContent.setFitToWidth(true);
        scrollContent.setPrefHeight(260);
        scrollContent.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 12px;");

        List<Button> langButtons = new ArrayList<>();
        Runnable refreshView = () -> {
            String current = selectedLang[0];
            descLabel.setText(activeGame.getDescription(current));

            String rawInstructions = activeGame.getInstructions(current);
            boolean hasNativeTranslation = false;
            if (activeGame.getTranslations().containsKey(current.toLowerCase())) {
                GameDescriptor.Translation tr = activeGame.getTranslations().get(current.toLowerCase());
                if (tr != null && tr.instructions != null && !tr.instructions.trim().isEmpty()) {
                    hasNativeTranslation = true;
                }
            }

            boolean isFallback = !current.equalsIgnoreCase("en") && !hasNativeTranslation;
            if (isFallback) {
                fallbackNotice.setText("ℹ️ " + (current.equals("es") ? "Instrucciones en inglés (idioma no traducido)" : "Showing English instructions (fallback)"));
                fallbackNotice.setVisible(true);
                fallbackNotice.setManaged(true);
            } else {
                fallbackNotice.setVisible(false);
                fallbackNotice.setManaged(false);
            }

            contentList.getChildren().clear();
            if (rawInstructions == null || rawInstructions.trim().isEmpty()) {
                Label emptyLbl = new Label(current.equals("es") ? "No hay instrucciones disponibles." : "No instructions available.");
                emptyLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
                contentList.getChildren().add(emptyLbl);
            } else {
                String[] lines = rawInstructions.split("\n");
                int stepNum = 1;
                for (String line : lines) {
                    String clean = line.trim();
                    if (clean.isEmpty()) continue;
                    clean = clean.replaceFirst("^\\d+[\\.\\-]\\s*", "");

                    HBox itemRow = new HBox(10);
                    itemRow.setAlignment(Pos.CENTER_LEFT);
                    itemRow.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-padding: 8px 12px; -fx-background-radius: 8px;");

                    Label badge = new Label(String.valueOf(stepNum++));
                    badge.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: 900; -fx-min-width: 22px; -fx-min-height: 22px; -fx-alignment: center; -fx-background-radius: 11px;",
                        ThemeManager.getAccentHex(),
                        ThemeManager.getTextOnAccentPrimaryHex()
                    ));

                    Label stepLabel = new Label(clean);
                    stepLabel.setStyle(String.format("-fx-font-size: 13px; -fx-text-fill: %s;", ThemeManager.getTextOnCardPrimaryHex()));
                    stepLabel.setWrapText(true);
                    HBox.setHgrow(stepLabel, Priority.ALWAYS);

                    itemRow.getChildren().addAll(badge, stepLabel);
                    contentList.getChildren().add(itemRow);
                }
            }

            for (Button btn : langButtons) {
                String code = (String) btn.getUserData();
                if (code.equalsIgnoreCase(current)) {
                    btn.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 8px; -fx-cursor: hand;",
                        ThemeManager.getAccentHex(),
                        ThemeManager.getTextOnAccentPrimaryHex()
                    ));
                } else {
                    btn.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6px 14px; -fx-background-radius: 8px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 8px; -fx-cursor: hand;",
                        ThemeManager.getButtonHex(),
                        ThemeManager.getTextOnButtonPrimaryHex()
                    ));
                }
            }
        };

        for (I18n.LanguageOption opt : I18n.SUPPORTED_LANGUAGES) {
            Button lBtn = new Button(opt.flag + " " + opt.code.toUpperCase());
            lBtn.setUserData(opt.code);
            lBtn.setOnAction(e -> {
                selectedLang[0] = opt.code;
                refreshView.run();
            });
            langButtons.add(lBtn);
            langRow.getChildren().add(lBtn);
        }

        refreshView.run();

        Button closeBtn = new Button(I18n.get("common.btn.cancel"));
        closeBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8px 24px; -fx-background-radius: 10px; -fx-cursor: hand;", ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        closeBtn.setOnAction(e -> rootPane.getChildren().remove(overlay));

        modalCard.getChildren().addAll(titleLabel, langRow, fallbackNotice, descLabel, scrollContent, closeBtn);
        overlay.getChildren().add(modalCard);

        return overlay;
    }

    private StackPane createLoadingOverlay(String message) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(6, 9, 18, 0.85);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setMouseTransparent(false);
        overlay.setOnMouseClicked(Event::consume);
        overlay.setOnKeyPressed(Event::consume);

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setPadding(new Insets(26, 32, 26, 32));
        card.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(99, 102, 241, 0.5); -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 24, 0, 0, 8);",
                ThemeManager.getCardHex()));

        Node gifNode = createLoadingGifNode(105);

        Label msgLabel = new Label(message);
        msgLabel.setStyle(String.format(
                "-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-text-alignment: center;",
                ThemeManager.getTextOnCardPrimaryHex()));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(360);

        Label waitLabel = new Label(I18n.get("settings.loading.please_wait"));
        waitLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-text-alignment: center;");
        waitLabel.setWrapText(true);

        card.getChildren().addAll(gifNode, msgLabel, waitLabel);
        overlay.getChildren().add(card);
        return overlay;
    }

    public static class AILoadingOverlay {
        private final StackPane overlay;
        private final ProgressBar progressBar;
        private final Label countLabel;
        private final Label stepLabel;

        public AILoadingOverlay(StackPane overlay, ProgressBar progressBar, Label countLabel, Label stepLabel) {
            this.overlay = overlay;
            this.progressBar = progressBar;
            this.countLabel = countLabel;
            this.stepLabel = stepLabel;
        }

        public StackPane getOverlay() {
            return overlay;
        }

        public void updateProgress(int current, int total, double percent, String step) {
            int pctInt = (int) Math.round(percent * 100.0);
            if (pctInt > 100) pctInt = 100;
            if (pctInt < 0) pctInt = 0;

            String countStr = String.format(I18n.get("settings.ai.progress_count"), current, total, pctInt);
            countLabel.setText(countStr);
            progressBar.setProgress(Math.max(0.0, Math.min(1.0, percent)));
            if (step != null && !step.isBlank()) {
                stepLabel.setText(step);
            }
        }
    }

    private AILoadingOverlay createAILoadingOverlay(int totalCount) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(6, 9, 18, 0.88);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setMouseTransparent(false);
        overlay.setOnMouseClicked(Event::consume);
        overlay.setOnKeyPressed(Event::consume);

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(460);
        card.setPrefWidth(460);
        card.setPadding(new Insets(26, 32, 26, 32));
        card.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: rgba(168, 85, 247, 0.6); -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 24, 0, 0, 8);",
                ThemeManager.getCardHex()));

        Node gifNode = createLoadingGifNode(90);

        Label titleLabel = new Label("✨ " + I18n.get("settings.ai.loading_message"));
        titleLabel.setStyle(String.format(
                "-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-text-alignment: center;",
                ThemeManager.getTextOnCardPrimaryHex()));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(400);

        String initCount = String.format(I18n.get("settings.ai.progress_count"), 0, totalCount, 0);
        Label countLabel = new Label(initCount);
        countLabel.setStyle("-fx-background-color: rgba(139, 92, 246, 0.22); -fx-border-color: rgba(192, 132, 252, 0.4); -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 5px 16px; -fx-font-size: 13.5px; -fx-font-weight: 900; -fx-text-fill: #f472b6;");

        ProgressBar progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(390);
        progressBar.setMaxWidth(390);
        progressBar.setPrefHeight(12);
        progressBar.setStyle("-fx-accent: #ec4899; -fx-background-radius: 6px;");

        Label stepLabel = new Label(I18n.get("settings.loading.please_wait"));
        stepLabel.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-text-alignment: center;");
        stepLabel.setWrapText(true);
        stepLabel.setMaxWidth(400);

        card.getChildren().addAll(gifNode, titleLabel, countLabel, progressBar, stepLabel);
        overlay.getChildren().add(card);

        return new AILoadingOverlay(overlay, progressBar, countLabel, stepLabel);
    }

    private Node createLoadingGifNode(double size) {
        File gifFile = new File("assets/main_uses/loading.gif");
        if (!gifFile.exists()) {
            gifFile = new File("template-offline/assets/main_uses/loading.gif");
        }
        if (gifFile.exists()) {
            try {
                Image img = new Image(gifFile.toURI().toString());
                ImageView iv = new ImageView(img);
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                iv.setPreserveRatio(true);
                return iv;
            } catch (Exception ignored) {
            }
        }
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(size, size);
        return pi;
    }

    private StackPane createScorePresetAlertOverlay(StackPane rootPane, String title, String message, String highlightValue) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.82);");
        overlay.setAlignment(Pos.CENTER);

        // Compact Square Dimensions (360x360)
        VBox modalCard = new VBox(16);
        modalCard.setPrefSize(360, 360);
        modalCard.setMaxSize(360, 360);
        modalCard.setAlignment(Pos.CENTER);
        modalCard.setPadding(new Insets(24));
        modalCard.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: #f59e0b; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 24, 0, 0, 8);",
                ThemeManager.getCardHex()));

        // Warning Icon Box
        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(72, 72);
        iconCircle.setMaxSize(72, 72);
        iconCircle.setStyle(
                "-fx-background-color: rgba(245, 158, 11, 0.12); -fx-border-color: rgba(245, 158, 11, 0.5); -fx-border-width: 1.5px; -fx-border-radius: 18px; -fx-background-radius: 18px;");

        File sadlyIconFile = new File("assets/icons/sadly-piece.png");
        if (!sadlyIconFile.exists())
            sadlyIconFile = new File("template-offline/assets/icons/sadly-piece.png");

        if (sadlyIconFile.exists()) {
            try {
                ImageView imgView = new ImageView(new Image(sadlyIconFile.toURI().toString()));
                imgView.setFitWidth(54);
                imgView.setFitHeight(54);
                imgView.setPreserveRatio(true);
                iconCircle.getChildren().add(imgView);
            } catch (Exception e) {
                Label fallBackIcon = new Label("⚠️");
                fallBackIcon.setStyle("-fx-font-size: 32px;");
                iconCircle.getChildren().add(fallBackIcon);
            }
        } else {
            Label fallBackIcon = new Label("⚠️");
            fallBackIcon.setStyle("-fx-font-size: 32px;");
            iconCircle.getChildren().add(fallBackIcon);
        }

        VBox textGroup = new VBox(8);
        textGroup.setAlignment(Pos.CENTER);

        Label titleL = new Label(title);
        titleL.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #f59e0b;");

        Label descL = new Label(message);
        descL.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s; -fx-text-alignment: center;",
                ThemeManager.toHex(ThemeManager.getCurrentPalette().textSecondary)));
        descL.setWrapText(true);

        if (highlightValue != null && !highlightValue.isEmpty()) {
            Label chipBadge = new Label(highlightValue);
            chipBadge.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 15px; -fx-font-weight: 900; -fx-padding: 4px 16px; -fx-background-radius: 10px; -fx-border-color: #f59e0b; -fx-border-radius: 10px;",
                    ThemeManager.getButtonHex(),
                    highlightValue.startsWith("-") ? "#f43f5e" : "#10b981"));
            textGroup.getChildren().addAll(titleL, chipBadge, descL);
        } else {
            textGroup.getChildren().addAll(titleL, descL);
        }

        Button gotItBtn = new Button(I18n.get("settings.modal.got_it"));
        gotItBtn.setMaxWidth(Double.MAX_VALUE);
        gotItBtn.setStyle(
                "-fx-background-color: #f59e0b; -fx-text-fill: #0f172a; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 10px 24px; -fx-background-radius: 10px; -fx-cursor: hand;");

        File confirmIconFile = new File("assets/icons/button_confirm.png");
        if (!confirmIconFile.exists())
            confirmIconFile = new File("template-offline/assets/icons/button_confirm.png");

        if (confirmIconFile.exists()) {
            try {
                ImageView confirmImg = new ImageView(new Image(confirmIconFile.toURI().toString()));
                confirmImg.setFitWidth(18);
                confirmImg.setFitHeight(18);
                confirmImg.setPreserveRatio(true);
                gotItBtn.setGraphic(confirmImg);
            } catch (Exception ignored) {
            }
        }
        gotItBtn.setOnAction(ev -> rootPane.getChildren().remove(overlay));

        modalCard.getChildren().addAll(iconCircle, textGroup, gotItBtn);
        overlay.getChildren().add(modalCard);

        return overlay;
    }

    private void showScoreOverlay(String title, String message, String highlightValue) {
        if (centerWrap == null) return;
        StackPane overlay = createScorePresetAlertOverlay(centerWrap, title, message, highlightValue);
        if (!centerWrap.getChildren().contains(overlay)) {
            centerWrap.getChildren().add(overlay);
        }
    }

    private void resetGameSelections() {
        selectedGames.clear();
        for (GameDescriptor game : availableGames) {
            game.setPlayOrder(0);
        }
    }

    private Button createPillButton(String text, String modeKey) {
        Button btn = new Button(text);
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-padding: 8px 18px; -fx-background-radius: 12px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        return btn;
    }

    private void updatePillSelection(Button b1, Button b2, Button b3, String activeMode) {
        String selectedStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-padding: 8px 18px; -fx-background-radius: 12px;",
                ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex());
        String normalStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-padding: 8px 18px; -fx-background-radius: 12px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex());
        b1.setStyle(activeMode.equals("1vs1") ? selectedStyle : normalStyle);
        b2.setStyle(activeMode.equals("team") ? selectedStyle : normalStyle);
        b3.setStyle(activeMode.equals("FREE_FOR_ALL") ? selectedStyle : normalStyle);
    }

    private List<Competitor> getCompetitorProfiles() {
        List<Competitor> profiles = new ArrayList<>();
        for (CompetitorInputRow row : competitorRows) {
            String name = row.nameInput.getText().trim();
            if (name.isEmpty())
                name = row.id;

            Competitor c = new Competitor();
            c.setId(row.id);
            c.setName(name);
            c.setAvatarPath(row.avatarPath);
            profiles.add(c);
        }
        return profiles;
    }

    private void renderCompetitorInputs() {
        competitorsContainer.getChildren().clear();
        competitorRows.clear();

        List<String> targetIds = new ArrayList<>();

        if ("1vs1".equals(selectedGameMode)) {
            targetIds.add("Player #1");
            targetIds.add("Player #2");
        } else if ("team".equals(selectedGameMode)) {
            for (int i = 1; i <= teamPlayerCount; i++) {
                targetIds.add("Team #" + i);
            }
        } else {
            for (int i = 1; i <= ffaPlayerCount; i++) {
                targetIds.add("Player #" + i);
            }
        }

        for (String id : targetIds) {
            CompetitorInputRow row = new CompetitorInputRow();
            row.id = id;

            HBox box = new HBox(12);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(10, 14, 10, 14));
            box.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 12px; -fx-background-radius: 12px;",
                    ThemeManager.getCardHex()));

            Label label = new Label(id + ":");
            label.setPrefWidth(75);
            label.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 12px;",
                    ThemeManager.getTextOnCardPrimaryHex()));

            row.nameInput = new TextField();
            row.nameInput.setPromptText(I18n.get("settings.players.enter_name"));
            row.nameInput.setText("team".equals(selectedGameMode) ? (I18n.get("settings.players.default_team_prefix") + id.replaceAll("\\D+", "")) : id);
            row.nameInput.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 8px; -fx-padding: 6px 10px;",
                    ThemeManager.getAppBgHex(), ThemeManager.getTextOnAppBgPrimaryHex()));

            row.avatarPreview = new ImageView();
            row.avatarPreview.setFitWidth(32);
            row.avatarPreview.setFitHeight(32);
            row.avatarPreview.setPreserveRatio(true);

            row.chooseAvatarBtn = new Button(I18n.get("settings.players.choose_avatar"));
            row.chooseAvatarBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6px 10px; -fx-font-size: 11px; -fx-cursor: hand;",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));

            row.chooseAvatarBtn.setOnAction(e -> {
                FileChooser chooser = FileChooserHelper.createChooser(I18n.get("settings.players.avatar_chooser_title"));
                chooser.getExtensionFilters()
                        .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
                File file = FileChooserHelper.showOpenDialog(chooser, getScene() != null ? getScene().getWindow() : null);
                if (file != null) {
                    row.avatarPath = file.getAbsolutePath();
                    try {
                        Image img = new Image(file.toURI().toString());
                        row.avatarPreview.setImage(img);
                    } catch (Exception ignored) {
                    }
                }
            });

            HBox.setHgrow(row.nameInput, Priority.ALWAYS);
            box.getChildren().addAll(label, row.nameInput, row.avatarPreview, row.chooseAvatarBtn);

            row.box = box;
            competitorRows.add(row);
        }

        if (competitorRows.size() == 2) {
            GridPane compGrid = new GridPane();
            compGrid.setHgap(12);
            compGrid.setVgap(12);

            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(50);
            c1.setHgrow(Priority.ALWAYS);

            ColumnConstraints c2 = new ColumnConstraints();
            c2.setPercentWidth(50);
            c2.setHgrow(Priority.ALWAYS);

            compGrid.getColumnConstraints().addAll(c1, c2);

            compGrid.add(competitorRows.get(0).box, 0, 0);
            compGrid.add(competitorRows.get(1).box, 1, 0);
            competitorsContainer.getChildren().add(compGrid);
        } else if (competitorRows.size() > 2) {
            GridPane compGrid = new GridPane();
            compGrid.setHgap(12);
            compGrid.setVgap(10);

            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(50);
            c1.setHgrow(Priority.ALWAYS);

            ColumnConstraints c2 = new ColumnConstraints();
            c2.setPercentWidth(50);
            c2.setHgrow(Priority.ALWAYS);

            compGrid.getColumnConstraints().addAll(c1, c2);

            for (int i = 0; i < competitorRows.size(); i++) {
                compGrid.add(competitorRows.get(i).box, i % 2, i / 2);
            }
            competitorsContainer.getChildren().add(compGrid);
        } else {
            for (CompetitorInputRow row : competitorRows) {
                competitorsContainer.getChildren().add(row.box);
            }
        }
    }

    private void renderGameCards() {
        double currentVValue = getVvalue();
        gameCardsContainer.getChildren().clear();

        if (availableGames.isEmpty()) {
            Label emptyLabel = new Label(I18n.get("settings.games.none_found"));
            emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            gameCardsContainer.getChildren().add(emptyLabel);
            return;
        }

        HBox puzzleDeckBox = new HBox(16);
        puzzleDeckBox.setAlignment(Pos.CENTER);

        VBox leftCol = new VBox(12);
        leftCol.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        VBox rightCol = new VBox(12);
        rightCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // 1. CENTRAL CONTROL SPINE HUB WITH LED LIGHTS
        VBox spineHub = new VBox(14);
        spineHub.setPrefWidth(220);
        spineHub.setMaxWidth(220);
        spineHub.setAlignment(Pos.CENTER);
        spineHub.setPadding(new Insets(16));
        spineHub.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 16, 0, 0, 6);",
                ThemeManager.getCardHex(),
                ThemeManager.getAccentHex()));

            VBox spineHeader = new VBox(2);
        spineHeader.setAlignment(Pos.CENTER);
        Label spineTag = new Label(I18n.get("settings.games.spine_tag"));
        spineTag.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getAccentHex()));
        Label spineTitle = new Label(I18n.get("settings.games.spine_title"));
        spineTitle.setStyle(String.format("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getTextOnCardPrimaryHex()));
        spineHeader.getChildren().addAll(spineTag, spineTitle);

        // Central LED Status Light 1: System Readiness
        HBox led1Box = new HBox(8);
        led1Box.setAlignment(Pos.CENTER_LEFT);
        led1Box.setPadding(new Insets(8, 10, 8, 10));
        led1Box.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.4); -fx-background-radius: 10px; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 10px;");

        Label led1Title = new Label(I18n.get("settings.games.system"));
        led1Title.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: %s;",
                ThemeManager.getTextOnCardSecondaryHex()));

        Label led1Dot = new Label("●");
        boolean hasGames = !selectedGames.isEmpty();
        led1Dot.setStyle(hasGames
                ? "-fx-font-size: 14px; -fx-text-fill: #10b981;"
                : "-fx-font-size: 14px; -fx-text-fill: #f59e0b;");
        Label led1Text = new Label(hasGames ? I18n.get("settings.games.arena_ready") : I18n.get("settings.games.select_games"));
        led1Text.setStyle(hasGames
                ? "-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #34d399;"
                : "-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #fbbf24;");
        led1Box.getChildren().addAll(led1Title, led1Dot, led1Text);

        // Central LED Status Light 2: Interlocked Count
        HBox led2Box = new HBox(8);
        led2Box.setAlignment(Pos.CENTER_LEFT);
        led2Box.setPadding(new Insets(8, 10, 8, 10));
        led2Box.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.4); -fx-background-radius: 10px; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 10px;");

        Label led2Title = new Label(I18n.get("settings.games.connected"));
        led2Title.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: %s;",
                ThemeManager.getTextOnCardSecondaryHex()));

        Label led2Val = new Label(String.format("%d / %d", selectedGames.size(), availableGames.size()));
        led2Val.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.getAccentHex()));
        led2Box.getChildren().addAll(led2Title, led2Val);

        // Spine Footer Badge
        Label spineFooter = new Label(hasGames ? I18n.get("settings.games.all_active") : I18n.get("settings.games.awaiting"));
        spineFooter.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        VBox spineActionsBox = new VBox(8);
        spineActionsBox.setAlignment(Pos.CENTER);
        spineActionsBox.setMaxWidth(Double.MAX_VALUE);
        spineActionsBox.setPadding(new Insets(6, 0, 0, 0));
        if (launchBtn != null && backBtn != null) {
            launchBtn.setMaxWidth(Double.MAX_VALUE);
            backBtn.setMaxWidth(Double.MAX_VALUE);
            spineActionsBox.getChildren().addAll(launchBtn, backBtn);
        }

        spineHub.getChildren().addAll(spineHeader, led1Box, led2Box, spineFooter, spineActionsBox);

        // 2. POPULATE LEFT & RIGHT COLUMNS WITH INTERLOCKING PUZZLE CARDS
        for (int i = 0; i < availableGames.size(); i++) {
            GameDescriptor game = availableGames.get(i);
            boolean isLeft = (i % 2 == 0);
            boolean modeSupported = game.isModeSupported(selectedGameMode);

            if (!modeSupported) {
                selectedGames.remove(game);
                game.setPlayOrder(0);
            }
            boolean isSelected = modeSupported && selectedGames.contains(game);

            StackPane cardWrap = new StackPane();

            HBox card = new HBox(10);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(12, 14, 12, 14));

            String baseStyle = String.format(
                    "-fx-background-color: %s; -fx-border-radius: 14px; -fx-background-radius: 14px;",
                    ThemeManager.getCardHex());
            if (!modeSupported) {
                card.setStyle(baseStyle + " -fx-border-color: rgba(255, 255, 255, 0.05); -fx-opacity: 0.4;");
            } else if (isSelected) {
                card.setStyle(baseStyle + String.format(
                        " -fx-border-color: %s; -fx-border-width: 2px; -fx-effect: dropshadow(three-pass-box, rgba(99,102,241,0.4), 14, 0, 0, 4);",
                        ThemeManager.getAccentHex()));
            } else {
                card.setStyle(baseStyle + " -fx-border-color: rgba(255, 255, 255, 0.1);");
            }

            if (modeSupported) {
                card.setCursor(Cursor.HAND);
                card.setOnMouseClicked(e -> {
                    if (selectedGames.contains(game)) {
                        selectedGames.remove(game);
                        game.setPlayOrder(0);
                        for (int j = 0; j < selectedGames.size(); j++) {
                            selectedGames.get(j).setPlayOrder(j + 1);
                        }
                        renderGameCards();
                    } else {
                        StackPane loadingOverlay = createLoadingOverlay(I18n.get("settings.loading.validating_game"));
                        if (!centerWrap.getChildren().contains(loadingOverlay)) {
                            centerWrap.getChildren().add(loadingOverlay);
                        }

                        List<Competitor> activeProfiles = getCompetitorProfiles();
                        long startTime = System.currentTimeMillis();

                        CompletableFuture.supplyAsync(() -> {
                            IGameSetupEditor editor = GameEditorRegistry.getEditor(game);
                            JsonNode setupData = game.getSetupData();
                            if (setupData == null) {
                                File setupFile = new File("games/" + game.getName() + "/setup.json");
                                if (!setupFile.exists())
                                    setupFile = new File("template-offline/games/" + game.getName() + "/setup.json");
                                if (setupFile.exists()) {
                                    try {
                                        setupData = objectMapper.readTree(setupFile);
                                        game.setSetupData(setupData);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            boolean isBr = setupData != null && setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean();
                            String error = editor.validateSetupData(setupData, activeProfiles, isBr);

                            long elapsed = System.currentTimeMillis() - startTime;
                            long remaining = Math.max(0, 750 - elapsed);
                            if (remaining > 0) {
                                try {
                                    Thread.sleep(remaining);
                                } catch (InterruptedException ignored) {
                                }
                            }
                            return error;
                        }, VALIDATION_EXECUTOR).thenAcceptAsync(error -> {
                            Platform.runLater(() -> {
                                centerWrap.getChildren().remove(loadingOverlay);

                                if (error != null) {
                                    StackPane errorOverlay = createValidationErrorOverlay(centerWrap, game.getName(), error);
                                    if (!centerWrap.getChildren().contains(errorOverlay)) {
                                        centerWrap.getChildren().add(errorOverlay);
                                    }
                                    return;
                                }

                                selectedGames.add(game);
                                game.setPlayOrder(selectedGames.size());
                                renderGameCards();
                            });
                        }, VALIDATION_EXECUTOR);
                    }
                });
            }

            VBox infoBox = new VBox(2);
            HBox titleRow = new HBox(6);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(game.getName() != null ? game.getName() : "Minigame");
            nameLabel.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;",
                    ThemeManager.getTextOnCardPrimaryHex()));
            titleRow.getChildren().add(nameLabel);

            JsonNode gSetup = game.getSetupData();
            if (gSetup == null) {
                File setupFile = new File("games/" + game.getName() + "/setup.json");
                if (!setupFile.exists()) setupFile = new File("template-offline/games/" + game.getName() + "/setup.json");
                if (setupFile.exists()) {
                    try {
                        gSetup = objectMapper.readTree(setupFile);
                        game.setSetupData(gSetup);
                    } catch (Exception ignored) {}
                }
            }
            if (gSetup != null && gSetup.has("battleRoyale") && gSetup.get("battleRoyale").asBoolean()) {
                Label brBadge = new Label("⚔️ BR");
                brBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.25); -fx-text-fill: #fbbf24; -fx-font-size: 9px; -fx-font-weight: 900; -fx-padding: 2px 6px; -fx-background-radius: 6px; -fx-border-color: #f59e0b; -fx-border-radius: 6px;");
                titleRow.getChildren().add(brBadge);
            }

            Label statusModeLabel = new Label(
                    modeSupported ? "Author: " + (game.getAuthor() != null ? game.getAuthor() : "YuyiStudio")
                            : I18n.get("settings.games.not_supported"));
            statusModeLabel.setStyle(modeSupported
                    ? String.format("-fx-text-fill: %s; -fx-font-size: 10px; -fx-font-weight: bold;",
                            ThemeManager.getAccentHex())
                    : "-fx-text-fill: #f43f5e; -fx-font-size: 10px; -fx-font-weight: bold;");

            infoBox.getChildren().addAll(titleRow, statusModeLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            HBox actionButtonsBox = new HBox(8);
            actionButtonsBox.setAlignment(Pos.CENTER_RIGHT);

            Button instructionsBtn = new Button(I18n.get("arena.btn.rules"));
            instructionsBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 6px 10px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 6, 0, 0, 2);",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
            instructionsBtn.setOnAction(e -> {
                e.consume();
                openGameInstructionsModal(game);
            });
            instructionsBtn.setOnMouseClicked(Event::consume);

            Button configBtn = new Button(I18n.get("settings.games.setup_btn"));
            configBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.25); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 6px 10px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 6, 0, 0, 2);",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
            configBtn.setOnAction(e -> {
                e.consume();
                openGameSetupModal(game);
            });
            configBtn.setOnMouseClicked(Event::consume);

            actionButtonsBox.getChildren().addAll(instructionsBtn, configBtn);
            card.getChildren().addAll(infoBox, actionButtonsBox);
            cardWrap.getChildren().add(card);

            // INTERLOCKING CONNECTOR BADGE AT JOINT
            if (isSelected && game.getPlayOrder() > 0) {
                Label jointBadge = new Label("#" + game.getPlayOrder());
                jointBadge.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-border-color: #ffffff; -fx-border-width: 1.5px; -fx-border-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 2);",
                        ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));

                StackPane.setAlignment(jointBadge, isLeft ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                cardWrap.getChildren().add(jointBadge);
            }

            if (isLeft) {
                leftCol.getChildren().add(cardWrap);
            } else {
                rightCol.getChildren().add(cardWrap);
            }
        }

        puzzleDeckBox.getChildren().addAll(leftCol, spineHub, rightCol);
        gameCardsContainer.getChildren().add(puzzleDeckBox);
        setVvalue(currentVValue);
    }

    private void openGameSetupModal(GameDescriptor game) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(I18n.get("settings.modal.game_settings_prefix", game.getName()));
        dialog.setMinWidth(680);

        VBox rootBox = new VBox(16);
        rootBox.setPadding(new Insets(20));
        rootBox.setStyle(String.format("-fx-background-color: %s;", ThemeManager.getMainBoxHex()));

        Label title = new Label(I18n.get("settings.modal.game_settings_title", game.getName().toUpperCase()));
        title.setStyle(String.format("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: %s;",
                ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));

        List<Competitor> activeProfiles = getCompetitorProfiles();
        IGameSetupEditor editor = GameEditorRegistry.getEditor(game);
        Node editorPanel = editor.createEditorPanel(game.getSetupData(), activeProfiles,
                ThemeManager.getCurrentPalette());

        HBox actionBox = new HBox(12);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button exportBtn = new Button(I18n.get("settings.btn.export_json"));
        exportBtn.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-border-color: #0284c7; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> {
            JsonNode setupNode = editor.getUpdatedSetup();
            FileChooser chooser = FileChooserHelper.createChooser("Export " + game.getName() + " Setup JSON");
            chooser.setInitialFileName("setup-" + game.getName() + ".json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
            File file = FileChooserHelper.showSaveDialog(chooser, dialog);
            if (file != null) {
                try {
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, setupNode);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        Button importBtn = new Button(I18n.get("settings.btn.import_json"));
        importBtn.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: #a855f7; -fx-font-weight: bold; -fx-border-color: #7e22ce; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-cursor: hand;");
        importBtn.setOnAction(e -> {
            FileChooser chooser = FileChooserHelper.createChooser("Import " + game.getName() + " Setup JSON");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
            File file = FileChooserHelper.showOpenDialog(chooser, dialog);
            if (file != null) {
                try {
                    JsonNode importedNode = objectMapper.readTree(file);
                    game.setSetupData(importedNode);
                    File targetGameSetupFile = new File("games/" + game.getName() + "/setup.json");
                    if (!targetGameSetupFile.getParentFile().exists()) {
                        targetGameSetupFile = new File("template-offline/games/" + game.getName() + "/setup.json");
                    }
                    if (targetGameSetupFile.getParentFile().exists()) {
                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetGameSetupFile, importedNode);
                    }
                    dialog.close();
                    openGameSetupModal(game);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        Button saveBtn = new Button("💾 " + I18n.get("settings.modal.save_changes"));
        saveBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 10px; -fx-cursor: hand;",
                ThemeManager.getAccentHex()));

        StackPane modalRoot = new StackPane(rootBox);

        if (isGameSupportedByAI(game.getName())) {
            Button aiBtn = new Button("✨ " + I18n.get("settings.btn.ai"));
            boolean hasRam = AIService.hasMinimumMemory();
            boolean hasNet = AIService.hasInternetConnection();
            boolean aiEnabled = hasRam && hasNet;

            if (!aiEnabled) {
                aiBtn.setDisable(true);
                String reason = AIService.getAIDisabledReason();
                Tooltip tooltip = new Tooltip(reason != null ? reason : "IA no disponible");
                tooltip.setStyle("-fx-font-size: 11px;");
                Tooltip.install(aiBtn, tooltip);
                aiBtn.setStyle("-fx-background-color: rgba(71, 85, 105, 0.4); -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-border-color: rgba(148, 163, 184, 0.2); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-cursor: not-allowed;");
            } else {
                aiBtn.setStyle("-fx-background-color: linear-gradient(to right, #8b5cf6, #ec4899); -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-background-radius: 8px; -fx-padding: 8px 16px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(236,72,153,0.35), 8, 0, 0, 2);");
                aiBtn.setOnAction(e -> openAIAssistantDialog(game, dialog, modalRoot, editor));
            }
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            actionBox.getChildren().addAll(aiBtn, spacer);
        }

        saveBtn.setOnAction(e -> {
            StackPane loadingOverlay = createLoadingOverlay(I18n.get("settings.loading.validating_game"));
            if (!modalRoot.getChildren().contains(loadingOverlay)) {
                modalRoot.getChildren().add(loadingOverlay);
            }

            JsonNode updated = editor.getUpdatedSetup();
            long startTime = System.currentTimeMillis();

            CompletableFuture.supplyAsync(() -> {
                boolean isBr = updated != null && updated.has("battleRoyale") && updated.get("battleRoyale").asBoolean();
                String error = editor.validateSetupData(updated, activeProfiles, isBr);
                if (error == null) {
                    try {
                        File targetFile = new File("games/" + game.getName() + "/setup.json");
                        if (!targetFile.getParentFile().exists()) {
                            targetFile = new File("template-offline/games/" + game.getName() + "/setup.json");
                        }
                        if (targetFile.getParentFile().exists()) {
                            objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, updated);
                        }
                        // Purge any orphan AI images no longer used in setup (Requirement 4)
                        AIService.purgeOrphanImages(game.getName(), updated);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                long elapsed = System.currentTimeMillis() - startTime;
                long remaining = Math.max(0, 850 - elapsed);
                if (remaining > 0) {
                    try {
                        Thread.sleep(remaining);
                    } catch (InterruptedException ignored) {
                    }
                }

                return new ValidationSaveResult(error, updated);
            }, VALIDATION_EXECUTOR).thenAcceptAsync(result -> {
                Platform.runLater(() -> {
                    modalRoot.getChildren().remove(loadingOverlay);

                    if (result.error() != null) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, result.error(), ButtonType.OK);
                        alert.setTitle(I18n.get("settings.error.validation_title"));
                        alert.setHeaderText(I18n.get("settings.error.validation_header"));
                        alert.showAndWait();
                        return;
                    }

                    game.setSetupData(result.updatedSetup());
                    renderGameCards();
                    dialog.close();
                });
            }, VALIDATION_EXECUTOR);
        });

        Button cancelBtn = new Button(I18n.get("common.btn.cancel"));
        cancelBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #cbd5e1; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 10px; -fx-cursor: hand;",
                ThemeManager.getButtonHex()));
        cancelBtn.setOnAction(e -> dialog.close());

        actionBox.getChildren().addAll(importBtn, exportBtn, cancelBtn, saveBtn);

        rootBox.getChildren().addAll(title, editorPanel, actionBox);
        modalRoot.setStyle(String.format("-fx-background-color: %s;", ThemeManager.getMainBoxHex()));
        Scene scene = new Scene(modalRoot);
        scene.setFill(Color.web(ThemeManager.getMainBoxHex()));
        dialog.setScene(scene);
        dialog.show();
    }

    private boolean isGameSupportedByAI(String gameName) {
        if (gameName == null || gameName.isBlank()) return false;
        // Dynamically detect if game folder contains ai_prompt.json (Requirement 1)
        File f1 = new File("template-offline/games/" + gameName + "/ai_prompt.json");
        if (f1.exists()) return true;
        File f2 = new File("games/" + gameName + "/ai_prompt.json");
        if (f2.exists()) return true;
        return false;
    }

    private void openAIAssistantDialog(GameDescriptor game, Stage parentDialog, StackPane setupModalRoot, IGameSetupEditor editor) {
        Stage aiStage = new Stage();
        aiStage.initModality(Modality.APPLICATION_MODAL);
        aiStage.initOwner(parentDialog);
        aiStage.setTitle("Game Show Center - " + String.format(I18n.get("settings.ai.modal_title"), game.getName()));

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(24));
        layout.setPrefWidth(480);
        layout.setStyle(String.format("-fx-background-color: %s; -fx-border-color: rgba(139, 92, 246, 0.4); -fx-border-width: 1.5px; -fx-border-radius: 12px; -fx-background-radius: 12px;",
                ThemeManager.getMainBoxHex()));

        // Title
        Label headerTitle = new Label("✨ " + String.format(I18n.get("settings.ai.modal_title"), game.getName()));
        headerTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 900; -fx-text-fill: #c084fc;");

        Label headerSubtitle = new Label(I18n.get("settings.ai.modal_subtitle"));
        headerSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        VBox headerBox = new VBox(4, headerTitle, headerSubtitle);

        // 1. INPUT 1: MODO (Agregar o Sobreescribir)
        VBox modeBox = new VBox(8);
        Label modeLabel = new Label(I18n.get("settings.ai.mode_title"));
        modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0; -fx-font-size: 13px;");

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton addRadio = new RadioButton(I18n.get("settings.ai.mode_add"));
        addRadio.setToggleGroup(modeGroup);
        addRadio.setSelected(true);
        addRadio.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold; -fx-cursor: hand;");
        Tooltip.install(addRadio, new Tooltip(I18n.get("settings.ai.mode_add_desc")));

        RadioButton overwriteRadio = new RadioButton(I18n.get("settings.ai.mode_overwrite"));
        overwriteRadio.setToggleGroup(modeGroup);
        overwriteRadio.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-cursor: hand;");
        Tooltip.install(overwriteRadio, new Tooltip(I18n.get("settings.ai.mode_overwrite_desc")));
        // Read current live setup data dynamically from editor if open, otherwise fallback to game.getSetupData()
        JsonNode currentLiveSetup = editor != null ? editor.getUpdatedSetup() : game.getSetupData();
        List<String> existingItems = AIService.extractExistingItems(game.getName(), currentLiveSetup);

        VBox metadataPreviewBox = new VBox(6);
        metadataPreviewBox.setPadding(new Insets(8, 12, 8, 12));
        metadataPreviewBox.setStyle("-fx-background-color: #0f172a; -fx-border-color: rgba(52, 211, 153, 0.3); -fx-border-radius: 8px; -fx-background-radius: 8px;");

        HBox metaHeader = new HBox(8);
        metaHeader.setAlignment(Pos.CENTER_LEFT);
        Label metaTitle = new Label("📋 " + I18n.get("settings.ai.existing_metadata_title"));
        metaTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #34d399;");
        Label metaBadge = new Label(existingItems.size() + " " + (existingItems.size() == 1 ? "item" : "items"));
        metaBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #a7f3d0; -fx-background-color: #064e3b; -fx-padding: 1 6; -fx-background-radius: 6;");
        HBox.setHgrow(metaTitle, Priority.ALWAYS);
        metaHeader.getChildren().addAll(metaTitle, metaBadge);
        metadataPreviewBox.getChildren().add(metaHeader);

        if (existingItems.isEmpty()) {
            Label emptyLbl = new Label(I18n.get("settings.ai.existing_metadata_empty"));
            emptyLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            metadataPreviewBox.getChildren().add(emptyLbl);
        } else {
            FlowPane chipsPane = new FlowPane(6, 6);
            int displayCount = Math.min(existingItems.size(), 12);
            for (int i = 0; i < displayCount; i++) {
                String it = existingItems.get(i);
                Label chip = new Label(it.length() > 28 ? it.substring(0, 25) + "..." : it);
                chip.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 6;");
                chipsPane.getChildren().add(chip);
            }
            if (existingItems.size() > 12) {
                Label moreLbl = new Label("+" + (existingItems.size() - 12) + "...");
                moreLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-style: italic;");
                chipsPane.getChildren().add(moreLbl);
            }
            metadataPreviewBox.getChildren().add(chipsPane);
        }

        modeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            boolean isAdd = addRadio.isSelected();
            metadataPreviewBox.setVisible(isAdd);
            metadataPreviewBox.setManaged(isAdd);
        });

        HBox modeRadios = new HBox(16, addRadio, overwriteRadio);
        modeBox.getChildren().addAll(modeLabel, modeRadios, metadataPreviewBox);

        // 2. INPUT 2: NÚMERO DE BÚSQUEDA
        VBox countBox = new VBox(8);
        String gameNorm = game.getName().toLowerCase().replace(" ", "_");
        boolean isGeo = gameNorm.contains("geolocation") || gameNorm.contains("geo");
        boolean isTopic = gameNorm.contains("topic");

        int curImagesPerRound = 3;
        if (isGeo && currentLiveSetup != null) {
            if (currentLiveSetup.has("images_per_round")) {
                curImagesPerRound = currentLiveSetup.get("images_per_round").asInt(3);
            } else if (currentLiveSetup.has("imagesPerRound")) {
                curImagesPerRound = currentLiveSetup.get("imagesPerRound").asInt(3);
            }
        }

        Label countLabel = new Label(isGeo 
                ? String.format(I18n.get("settings.ai.search_count_geo"), curImagesPerRound)
                : I18n.get("settings.ai.search_count"));
        countLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0; -fx-font-size: 13px;");

        int maxCount = isTopic ? 6 : 20;
        int defCount = isTopic ? 3 : (isGeo ? 2 : 5);
        Spinner<Integer> countSpinner = new Spinner<>(1, maxCount, defCount, 1);
        countSpinner.setEditable(true);
        countSpinner.setPrefWidth(90);

        HBox countRow = new HBox(12, countLabel, countSpinner);
        countRow.setAlignment(Pos.CENTER_LEFT);
        countBox.getChildren().add(countRow);

        // 3. INPUT 3: PROMPT
        VBox promptBox = new VBox(8);
        Label promptLabel = new Label(I18n.get("settings.ai.prompt_label"));
        promptLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0; -fx-font-size: 13px;");

        TextArea promptArea = new TextArea();
        promptArea.setPromptText(I18n.get("settings.ai.prompt_placeholder"));
        promptArea.setWrapText(true);
        promptArea.setPrefRowCount(3);
        promptArea.setStyle("-fx-control-inner-background: #0f172a; -fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8px; -fx-background-radius: 8px;");

        promptBox.getChildren().addAll(promptLabel, promptArea);

        // BUTTONS
        HBox buttonsBox = new HBox(12);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button(I18n.get("common.btn.cancel"));
        cancelBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 8px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> aiStage.close());

        Button generateBtn = new Button("✨ " + I18n.get("settings.ai.btn_generate"));
        generateBtn.setStyle("-fx-background-color: linear-gradient(to right, #8b5cf6, #ec4899); -fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-padding: 9px 20px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(236,72,153,0.4), 8, 0, 0, 2);");

        final int finalImagesPerRound = curImagesPerRound;
        generateBtn.setOnAction(e -> {
            String prompt = promptArea.getText().trim();
            if (prompt.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(aiStage);
                alert.setTitle("Game Show Center");
                alert.setHeaderText(null);
                alert.setContentText(I18n.get("settings.ai.error_empty_prompt"));
                alert.showAndWait();
                return;
            }

            // Prohibited themes client check (instant feedback)
            String pLower = prompt.toLowerCase();
            if (pLower.matches(".*\\b(sexo|sexual|sexuales|pornografia|pornografía|porno|erotico|erótico|desnudo|desnudos|sex|porn|nude|droga|drogas|cocaina|cocaína|heroina|heroína|marihuana|drugs|arma|armas|pistola|pistolas|rifle|rifles|bomba|bombas|explosivo|explosivos|gun|guns|weapon|weapons).*")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(aiStage);
                alert.setTitle("Game Show Center - Moderación");
                alert.setHeaderText("🚫 Tema No Permitido");
                alert.setContentText(I18n.get("settings.ai.error_prohibited_theme"));
                alert.showAndWait();
                return;
            }

            String mode = addRadio.isSelected() ? "add" : "overwrite";
            int count = countSpinner.getValue();

            // Fatal Error check: Game requires images but no image sources are configured
            boolean requiresImages = AIService.isGameRequiringImages(game.getName());
            if (requiresImages && !ImageSourcesConfigManager.hasAnyActiveSource()) {
                Alert fatalAlert = new Alert(Alert.AlertType.ERROR);
                fatalAlert.initOwner(aiStage);
                fatalAlert.setTitle(I18n.get("settings.ai.sources.fatal_title"));
                fatalAlert.setHeaderText("🚫 " + I18n.get("settings.ai.sources.fatal_header"));
                fatalAlert.setContentText(I18n.get("settings.ai.sources.fatal_desc"));
                fatalAlert.showAndWait();
                return;
            }

            aiStage.close();

            // Show full AI progress loading overlay on setup modal root blocking any requests
            AILoadingOverlay aiLoading = createAILoadingOverlay(count);
            if (!setupModalRoot.getChildren().contains(aiLoading.getOverlay())) {
                setupModalRoot.getChildren().add(aiLoading.getOverlay());
            }

            AIService.generateGameSetupAsync(
                    game.getName(),
                    prompt,
                    count,
                    finalImagesPerRound,
                    mode,
                    ImageSourcesConfigManager.getActiveSourcesForWorker(),
                    currentLiveSetup,
                    (current, total, percent, step) -> {
                        Platform.runLater(() -> aiLoading.updateProgress(current, total, percent, step));
                    }
            ).thenAccept(result -> {
                Platform.runLater(() -> {
                    setupModalRoot.getChildren().remove(aiLoading.getOverlay());

                    if (!result.isSuccess()) {
                        Alert errAlert = new Alert(Alert.AlertType.ERROR);
                        errAlert.initOwner(parentDialog);
                        errAlert.setTitle("Game Show Center - Error IA");
                        errAlert.setHeaderText("PROHIBITED_THEME".equals(result.getErrorCode()) ? "🚫 Tema Prohibido" : "Error en Generación de IA");
                        errAlert.setContentText(result.getErrorMessage());
                        errAlert.showAndWait();
                    } else {
                        // Update game setup data
                        game.setSetupData(result.getMergedSetup());

                        // Reopen setup modal so editor refreshes with new items
                        parentDialog.close();
                        openGameSetupModal(game);

                        Alert succAlert = new Alert(Alert.AlertType.INFORMATION);
                        succAlert.initOwner(parentDialog);
                        succAlert.setTitle("Game Show Center - IA");
                        succAlert.setHeaderText(I18n.get("settings.ai.success_title"));
                        succAlert.setContentText(I18n.get("settings.ai.success_msg"));
                        succAlert.showAndWait();
                    }
                });
            });
        });

        buttonsBox.getChildren().addAll(cancelBtn, generateBtn);

        layout.getChildren().addAll(headerBox, new Separator(), modeBox, countBox, promptBox, new Separator(), buttonsBox);

        Scene scene = new Scene(layout);
        scene.setFill(Color.web(ThemeManager.getMainBoxHex()));
        aiStage.setScene(scene);
        aiStage.showAndWait();
    }


    private void handleImportGeneralSetupJson() {
        FileChooser chooser = FileChooserHelper.createChooser(I18n.get("settings.btn.import_setup"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        File file = FileChooserHelper.showOpenDialog(chooser, getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;

        try {
            JsonNode root = objectMapper.readTree(file);
            boolean ok = applySetupJsonNode(root);
            if (ok) {
                I18n.notifyListeners();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18n.get("settings.import.success_title"));
                alert.setHeaderText(null);
                alert.setContentText(I18n.get("settings.import.success_desc"));
                alert.showAndWait();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18n.get("settings.import.error_title"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.get("settings.import.error_desc") + "\n\n" + ex.getMessage());
            alert.showAndWait();
        }
    }

    private void handleExportGeneralSetupJson() {
        FileChooser chooser = FileChooserHelper.createChooser(I18n.get("settings.btn.export_setup"));
        chooser.setInitialFileName("setup.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        File file = FileChooserHelper.showSaveDialog(chooser, getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;

        try {
            ObjectNode root = buildSetupJsonObject();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ObjectNode buildSetupJsonObject() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("gameMode", selectedGameMode);
        root.put("mode", selectedGameMode);
        root.put("ffaPlayerCount", ffaPlayerCount);
        root.put("battleRoyale", battleRoyale);

        // Score presets
        ArrayNode presetsArray = objectMapper.createArrayNode();
        for (String part : scorePresets) {
            presetsArray.add(part);
        }
        root.set("scorePresets", presetsArray);

        // AI Image Sources
        ArrayNode sourcesArray = objectMapper.createArrayNode();
        for (String src : ImageSourcesConfigManager.getActiveSourcesForWorker()) {
            sourcesArray.add(src);
        }
        root.set("aiImageSources", sourcesArray);

        // Profiles (Offline format)
        List<Competitor> currentComps = getCompetitorProfiles();
        ArrayNode profilesArray = objectMapper.createArrayNode();
        for (Competitor c : currentComps) {
            ObjectNode p = objectMapper.createObjectNode();
            p.put("id", c.getId());
            p.put("name", c.getName());
            if (c.getAvatarPath() != null) p.put("avatarPath", c.getAvatarPath());
            profilesArray.add(p);
        }
        root.set("profiles", profilesArray);

        // initialPlayers (Online React format compatibility)
        ObjectNode initialPlayersNode = objectMapper.createObjectNode();
        if ("1vs1".equals(selectedGameMode)) {
            Competitor c1 = currentComps.size() > 0 ? currentComps.get(0) : new Competitor("Player #1", "Player #1", null);
            Competitor c2 = currentComps.size() > 1 ? currentComps.get(1) : new Competitor("Player #2", "Player #2", null);
            ObjectNode p1 = objectMapper.createObjectNode();
            p1.put("name", c1.getName());
            if (c1.getAvatarPath() != null) p1.put("avatar", c1.getAvatarPath());
            ObjectNode p2 = objectMapper.createObjectNode();
            p2.put("name", c2.getName());
            if (c2.getAvatarPath() != null) p2.put("avatar", c2.getAvatarPath());
            initialPlayersNode.set("player1", p1);
            initialPlayersNode.set("player2", p2);
        } else if ("team".equals(selectedGameMode)) {
            ArrayNode teamsNode = objectMapper.createArrayNode();
            Competitor t1 = currentComps.size() > 0 ? currentComps.get(0) : new Competitor("Team #1", "Team #1", null);
            Competitor t2 = currentComps.size() > 1 ? currentComps.get(1) : new Competitor("Team #2", "Team #2", null);
            ObjectNode team1 = objectMapper.createObjectNode();
            team1.put("name", t1.getName());
            if (t1.getAvatarPath() != null) team1.put("avatar", t1.getAvatarPath());
            team1.set("members", objectMapper.createArrayNode());
            ObjectNode team2 = objectMapper.createObjectNode();
            team2.put("name", t2.getName());
            if (t2.getAvatarPath() != null) team2.put("avatar", t2.getAvatarPath());
            team2.set("members", objectMapper.createArrayNode());
            teamsNode.add(team1);
            teamsNode.add(team2);
            initialPlayersNode.set("teams", teamsNode);
        } else if ("FREE_FOR_ALL".equals(selectedGameMode)) {
            initialPlayersNode.put("ffaPlayerCount", ffaPlayerCount);
            ArrayNode ffaArray = objectMapper.createArrayNode();
            for (Competitor c : currentComps) {
                ObjectNode p = objectMapper.createObjectNode();
                p.put("name", c.getName());
                if (c.getAvatarPath() != null) p.put("avatar", c.getAvatarPath());
                ffaArray.add(p);
            }
            initialPlayersNode.set("ffaPlayers", ffaArray);
        }
        root.set("initialPlayers", initialPlayersNode);

        // Selected Games
        ArrayNode gamesArray = objectMapper.createArrayNode();
        for (GameDescriptor g : selectedGames) {
            ObjectNode gNode = objectMapper.createObjectNode();
            gNode.put("name", g.getName());
            gNode.put("playOrder", g.getPlayOrder());
            if (g.getSetupData() != null) {
                gNode.set("setup", g.getSetupData());
                gNode.set("setupData", g.getSetupData());
            }
            gamesArray.add(gNode);
        }
        root.set("selectedGames", gamesArray);
        root.set("games", gamesArray);

        // Theme details
        ThemeManager.Palette pal = ThemeManager.getCurrentPalette();
        if (pal != null) {
            root.put("theme", pal.name);
            ObjectNode colorsNode = objectMapper.createObjectNode();
            colorsNode.put("bgApp", ThemeManager.getAppBgHex());
            colorsNode.put("bgMainBox", ThemeManager.getMainBoxHex());
            colorsNode.put("bgCard", ThemeManager.getCardHex());
            colorsNode.put("accent", ThemeManager.getAccentHex());
            root.set("colors", colorsNode);
        }
        return root;
    }

    public boolean applySetupJsonNode(JsonNode root) {
        return applySetupJsonNode(root, true);
    }

    public boolean applySetupJsonNode(JsonNode root, boolean importTheme) {
        if (root == null) return false;
        try {
            // 1. GAME MODE
            if (root.has("gameMode")) {
                String mode = root.get("gameMode").asText();
                if ("1vs1".equalsIgnoreCase(mode) || "1v1".equalsIgnoreCase(mode)) {
                    this.selectedGameMode = "1vs1";
                } else if ("team".equalsIgnoreCase(mode) || "teams".equalsIgnoreCase(mode)) {
                    this.selectedGameMode = "team";
                } else if ("FREE_FOR_ALL".equalsIgnoreCase(mode) || "ffa".equalsIgnoreCase(mode) || "free_for_all".equalsIgnoreCase(mode)) {
                    this.selectedGameMode = "FREE_FOR_ALL";
                }
            } else if (root.has("mode")) {
                String mode = root.get("mode").asText();
                if ("1vs1".equalsIgnoreCase(mode) || "1v1".equalsIgnoreCase(mode)) {
                    this.selectedGameMode = "1vs1";
                } else if ("team".equalsIgnoreCase(mode) || "teams".equalsIgnoreCase(mode)) {
                    this.selectedGameMode = "team";
                } else if ("FREE_FOR_ALL".equalsIgnoreCase(mode) || "ffa".equalsIgnoreCase(mode) || "free_for_all".equalsIgnoreCase(mode)) {
                    this.selectedGameMode = "FREE_FOR_ALL";
                }
            }

            // 2. FFA PLAYER COUNT
            if (root.has("ffaPlayerCount")) {
                this.ffaPlayerCount = root.get("ffaPlayerCount").asInt(4);
            } else if (root.has("initialPlayers") && root.get("initialPlayers").has("ffaPlayerCount")) {
                this.ffaPlayerCount = root.get("initialPlayers").get("ffaPlayerCount").asInt(4);
            }

            // 2.1 BATTLE ROYALE MODE
            if (root.has("battleRoyale")) {
                this.battleRoyale = root.get("battleRoyale").asBoolean(false);
            } else {
                this.battleRoyale = false;
            }
            updateBattleRoyaleUI();

            // 3. SCORE PRESETS
            if (root.has("scorePresets")) {
                JsonNode sp = root.get("scorePresets");
                List<String> list = new ArrayList<>();
                if (sp.isArray()) {
                    for (JsonNode elem : sp) {
                        String raw;
                        if (elem.isObject() && elem.has("value")) {
                            raw = elem.get("value").asText();
                        } else {
                            raw = elem.asText();
                        }
                        raw = raw.trim();
                        try {
                            int val = Integer.parseInt(raw.replace("+", ""));
                            if (val != 0) {
                                list.add((val > 0 ? "+" : "") + val);
                            }
                        } catch (Exception ignored) {}
                    }
                } else if (sp.isTextual()) {
                    for (String part : sp.asText().split(",")) {
                        String raw = part.trim();
                        try {
                            int val = Integer.parseInt(raw.replace("+", ""));
                            if (val != 0) {
                                list.add((val > 0 ? "+" : "") + val);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (!list.isEmpty()) {
                    scorePresets.clear();
                    scorePresets.addAll(list);
                    renderScorePresetChips();
                }
            }

            // AI Image Sources
            if (root.has("aiImageSources") && root.get("aiImageSources").isArray()) {
                for (JsonNode sn : root.get("aiImageSources")) {
                    if (sn.isTextual()) {
                        String val = sn.asText().trim();
                        if (val.startsWith("folder:") || (val.length() >= 3 && val.charAt(1) == ':')) {
                            String p = val.startsWith("folder:") ? val.substring(7) : val;
                            ImageSourcesConfigManager.addFolder(p);
                        } else if (val.startsWith("http://") || val.startsWith("https://")) {
                            ImageSourcesConfigManager.addSchoolUrl(val);
                        }
                    }
                }
            }

            // 4. THEME / TEMPLATE COLOR CUSTOMIZATION (Only when explicitly requested by file import)
            if (importTheme) {
                if (root.has("theme") && root.get("theme").isTextual()) {
                    String themeName = root.get("theme").asText();
                    ThemeManager.setPalette(themeName);
                }
                if (root.has("palette") && root.get("palette").isTextual()) {
                    String themeName = root.get("palette").asText();
                    ThemeManager.setPalette(themeName);
                }
                if (root.has("colors") || root.has("customTheme")) {
                    JsonNode colNode = root.has("colors") ? root.get("colors") : root.get("customTheme");
                    try {
                        Color bgApp = colNode.has("bgApp") ? Color.web(colNode.get("bgApp").asText()) : null;
                        Color bgMainBox = colNode.has("bgMainBox") ? Color.web(colNode.get("bgMainBox").asText()) : null;
                        Color bgCard = colNode.has("bgCard") ? Color.web(colNode.get("bgCard").asText()) : null;
                        Color accent = colNode.has("accent") ? Color.web(colNode.get("accent").asText()) : null;
                        ThemeManager.saveCustomPalette("Imported Setup Theme", bgApp, bgMainBox, bgCard, accent);
                    } catch (Exception ignored) {}
                }
            }

            // 5. PLAYERS & AVATARS
            List<Competitor> importedProfiles = new ArrayList<>();
            if (root.has("profiles") && root.get("profiles").isArray()) {
                for (JsonNode pNode : root.get("profiles")) {
                    String id = pNode.has("id") ? pNode.get("id").asText() : "";
                    String name = pNode.has("name") ? pNode.get("name").asText() : "";
                    String avatar = pNode.has("avatarPath") ? pNode.get("avatarPath").asText() : (pNode.has("avatar") ? pNode.get("avatar").asText() : null);
                    importedProfiles.add(new Competitor(id, name, avatar));
                }
            } else if (root.has("initialPlayers") && root.get("initialPlayers").isObject()) {
                JsonNode ip = root.get("initialPlayers");
                if ("1vs1".equals(selectedGameMode)) {
                    String p1 = "Player #1";
                    String a1 = null;
                    if (ip.has("player1")) {
                        JsonNode p1Node = ip.get("player1");
                        if (p1Node.isObject()) {
                            if (p1Node.has("name")) p1 = p1Node.get("name").asText();
                            if (p1Node.has("avatar")) a1 = p1Node.get("avatar").asText();
                        } else {
                            p1 = p1Node.asText();
                        }
                    }
                    String p2 = "Player #2";
                    String a2 = null;
                    if (ip.has("player2")) {
                        JsonNode p2Node = ip.get("player2");
                        if (p2Node.isObject()) {
                            if (p2Node.has("name")) p2 = p2Node.get("name").asText();
                            if (p2Node.has("avatar")) a2 = p2Node.get("avatar").asText();
                        } else {
                            p2 = p2Node.asText();
                        }
                    }
                    importedProfiles.add(new Competitor("Player #1", p1, a1));
                    importedProfiles.add(new Competitor("Player #2", p2, a2));
                } else if ("team".equals(selectedGameMode)) {
                    if (ip.has("teams") && ip.get("teams").isArray() && ip.get("teams").size() >= 2) {
                        JsonNode t1Node = ip.get("teams").get(0);
                        JsonNode t2Node = ip.get("teams").get(1);
                        String t1 = t1Node.has("name") ? t1Node.get("name").asText() : "Team #1";
                        String a1 = t1Node.has("avatar") ? t1Node.get("avatar").asText() : null;
                        String t2 = t2Node.has("name") ? t2Node.get("name").asText() : "Team #2";
                        String a2 = t2Node.has("avatar") ? t2Node.get("avatar").asText() : null;
                        importedProfiles.add(new Competitor("Team #1", t1, a1));
                        importedProfiles.add(new Competitor("Team #2", t2, a2));
                    } else {
                        String t1 = ip.has("team1") ? ip.get("team1").asText() : "Team #1";
                        String t2 = ip.has("team2") ? ip.get("team2").asText() : "Team #2";
                        importedProfiles.add(new Competitor("Team #1", t1, null));
                        importedProfiles.add(new Competitor("Team #2", t2, null));
                    }
                } else if ("FREE_FOR_ALL".equals(selectedGameMode)) {
                    if (ip.has("ffaPlayers") && ip.get("ffaPlayers").isArray()) {
                        int idx = 1;
                        for (JsonNode ffaP : ip.get("ffaPlayers")) {
                            String pName = ffaP.isObject() && ffaP.has("name") ? ffaP.get("name").asText() : ffaP.asText();
                            String pAvatar = ffaP.isObject() && ffaP.has("avatar") ? ffaP.get("avatar").asText() : null;
                            importedProfiles.add(new Competitor("Player #" + idx, pName, pAvatar));
                            idx++;
                        }
                    }
                }
            }

            // 6. SELECTED GAMES & GAME SETUP
            JsonNode gamesNode = null;
            if (root.has("selectedGames") && root.get("selectedGames").isArray()) {
                gamesNode = root.get("selectedGames");
            } else if (root.has("games") && root.get("games").isArray()) {
                gamesNode = root.get("games");
            }

            if (gamesNode != null) {
                resetGameSelections();
                int order = 1;
                for (JsonNode gNode : gamesNode) {
                    String gName = gNode.has("name") ? gNode.get("name").asText() : (gNode.isTextual() ? gNode.asText() : "");
                    String normGName = gName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    for (GameDescriptor available : availableGames) {
                        String normAvail = available.getName().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                        if (normAvail.equals(normGName) || available.getName().equalsIgnoreCase(gName.trim())) {
                            if (!available.isAvailableInMode(selectedGameMode)) {
                                continue;
                            }
                            if (!selectedGames.contains(available)) {
                                selectedGames.add(available);
                                available.setPlayOrder(order++);
                                if (gNode.has("setup")) {
                                    available.setSetupData(gNode.get("setup"));
                                } else if (gNode.has("setupData")) {
                                    available.setSetupData(gNode.get("setupData"));
                                }
                            }
                            break;
                        }
                    }
                }
            }

            // 7. RE-RENDER AND REFRESH TEMPLATE & CONTROLS
            refreshViewAfterImport(importedProfiles);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void saveCurrentSetupToCache() {
        try {
            ObjectNode root = buildSetupJsonObject();
            File target = new File("last_match_setup.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target, root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadCachedSetupIfExists() {
        try {
            File file = new File("last_match_setup.json");
            if (!file.exists()) {
                file = new File("template-offline/last_match_setup.json");
            }
            if (file.exists()) {
                JsonNode root = objectMapper.readTree(file);
                applySetupJsonNode(root, false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleResetSetup() {
        this.selectedGameMode = "1vs1";
        this.ffaPlayerCount = 4;
        this.battleRoyale = false;
        updateBattleRoyaleUI();
        resetGameSelections();
        scorePresets.clear();
        scorePresets.addAll(Arrays.asList("+10", "+20", "+50", "-10", "-20"));
        renderScorePresetChips();
        try {
            File f1 = new File("last_match_setup.json");
            if (f1.exists()) f1.delete();
            File f2 = new File("template-offline/last_match_setup.json");
            if (f2.exists()) f2.delete();
        } catch (Exception ignored) {}
        refreshViewAfterImport(null);
        I18n.notifyListeners();
    }

    private void refreshViewAfterImport(List<Competitor> importedProfiles) {
        // 1. Update Mode Pills
        updatePillSelection(btn1v1, btnTeam, btnFfa, selectedGameMode);
        if (ffaCountBox != null) {
            ffaCountBox.setVisible("FREE_FOR_ALL".equals(selectedGameMode));
        }
        if (ffaCombo != null) {
            ffaCombo.setValue(ffaPlayerCount);
        }

        // 2. Render Competitors Inputs with Imported Data
        renderCompetitorInputs();
        if (importedProfiles != null && !importedProfiles.isEmpty()) {
            for (int i = 0; i < competitorRows.size(); i++) {
                CompetitorInputRow row = competitorRows.get(i);
                if (i < importedProfiles.size()) {
                    Competitor imp = importedProfiles.get(i);
                    if (imp.getName() != null && !imp.getName().isEmpty()) {
                        row.nameInput.setText(imp.getName());
                    }
                    if (imp.getAvatarPath() != null && !imp.getAvatarPath().isEmpty()) {
                        row.avatarPath = imp.getAvatarPath();
                        try {
                            File f = new File(imp.getAvatarPath());
                            if (!f.exists()) f = new File("template-offline/" + imp.getAvatarPath());
                            if (f.exists()) {
                                row.avatarPreview.setImage(new Image(f.toURI().toString()));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        // 3. Render Game Cards
        renderGameCards();

        // 4. Update Styles and Colors according to current ThemeManager
        refreshStyles();
    }

    public boolean isBattleRoyale() {
        return battleRoyale;
    }

    public void setBattleRoyale(boolean battleRoyale) {
        this.battleRoyale = battleRoyale;
        updateBattleRoyaleUI();
    }

    private void updateBattleRoyaleUI() {
        if (battleRoyaleCard == null) return;

        if (battleRoyale) {
            battleRoyaleCard.setStyle(
                    "-fx-background-color: rgba(245, 158, 11, 0.08); -fx-border-color: rgba(245, 158, 11, 0.5); -fx-border-width: 1.5px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 12px 14px; -fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.2), 10, 0, 0, 2);");
            if (battleRoyaleBadge != null) {
                battleRoyaleBadge.setText(I18n.get("settings.mode.battleroyale.badge_on"));
                battleRoyaleBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.25); -fx-text-fill: #fbbf24; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 3px 8px; -fx-background-radius: 6px; -fx-border-color: #f59e0b; -fx-border-radius: 6px;");
            }
            if (battleRoyaleToggleBtn != null) {
                battleRoyaleToggleBtn.setText("🔥 " + I18n.get("settings.mode.battleroyale.btn_on"));
                battleRoyaleToggleBtn.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #e11d48); -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: 900; -fx-padding: 6px 14px; -fx-background-radius: 10px; -fx-border-color: #fbbf24; -fx-border-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.4), 8, 0, 0, 2);");
            }
            if (gamesSubtitle != null) {
                gamesSubtitle.setText(I18n.get("settings.games.help_battleroyale"));
            }
        } else {
            battleRoyaleCard.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 12px 14px;");
            if (battleRoyaleBadge != null) {
                battleRoyaleBadge.setText(I18n.get("settings.mode.battleroyale.badge_off"));
                battleRoyaleBadge.setStyle("-fx-background-color: rgba(148, 163, 184, 0.15); -fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 3px 8px; -fx-background-radius: 6px; -fx-border-color: rgba(148, 163, 184, 0.3); -fx-border-radius: 6px;");
            }
            if (battleRoyaleToggleBtn != null) {
                battleRoyaleToggleBtn.setText("⚪ " + I18n.get("settings.mode.battleroyale.btn_off"));
                battleRoyaleToggleBtn.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6px 14px; -fx-background-radius: 10px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 10px; -fx-cursor: hand;",
                        ThemeManager.getButtonHex()));
            }
            if (gamesSubtitle != null) {
                gamesSubtitle.setText(I18n.get("settings.games.help"));
            }
        }
    }

    public void refreshStyles() {
        ThemeManager.updateDynamicTextColors();

        if (contentBox != null) {
            contentBox.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 20px; -fx-background-radius: 20px;",
                    ThemeManager.getMainBoxHex()));
        }
        if (titleLabel != null) {
            titleLabel.setStyle(String.format("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: %s;",
                    ThemeManager.getTextOnMainBoxPrimaryHex()));
        }
        if (subLabel != null) {
            subLabel.setStyle(String.format("-fx-font-size: 13px; -fx-text-fill: %s; -fx-wrap-text: true; -fx-text-alignment: center;",
                    ThemeManager.getTextOnMainBoxSecondaryHex()));
        }
        if (modeBox != null) {
            modeBox.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;",
                    ThemeManager.getCardHex()));
        }
        if (modeTitle != null) {
            modeTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;",
                    ThemeManager.getAccentHex()));
        }
        updateBattleRoyaleUI();
        if (competitorsBox != null) {
            competitorsBox.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;",
                    ThemeManager.getCardHex()));
        }
        if (competitorsTitle != null) {
            competitorsTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;",
                    ThemeManager.getAccentHex()));
        }
        if (scorePresetsBox != null) {
            scorePresetsBox.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;",
                    ThemeManager.getCardHex()));
        }
        if (scorePresetsTitle != null) {
            scorePresetsTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;",
                    ThemeManager.getAccentHex()));
        }
        if (scorePresetsSubtitle != null) {
            scorePresetsSubtitle.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s;",
                    ThemeManager.getTextOnCardSecondaryHex()));
        }
        if (resetPresetsBtn != null) {
            resetPresetsBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 8px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 8px; -fx-cursor: hand;",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        }
        if (newPresetInput != null) {
            newPresetInput.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 8px; -fx-padding: 8px 12px;",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        }
        if (addPresetBtn != null) {
            addPresetBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 8px; -fx-cursor: hand;",
                    ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        }
        renderScorePresetChips();

        if (gamesBox != null) {
            gamesBox.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;",
                    ThemeManager.getCardHex()));
        }
        if (gamesTitle != null) {
            gamesTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;",
                    ThemeManager.getAccentHex()));
        }
        if (backBtn != null) {
            backBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: 800; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8px 16px; -fx-cursor: hand;",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        }
        if (launchBtn != null) {
            launchBtn.setStyle(String.format(
                    "-fx-background-color: linear-gradient(to right, %s, derive(%s, 20%%)); -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: 900; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 10px 16px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 0, 2);",
                    ThemeManager.getAccentHex(), ThemeManager.getAccentHex(), ThemeManager.getTextOnAccentPrimaryHex()));
        }
        if (importSetupBtn != null) {
            importSetupBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: %s; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 16px; -fx-cursor: hand;",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex(), ThemeManager.getAccentHex()));
        }
        if (exportSetupBtn != null) {
            exportSetupBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: %s; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 16px; -fx-cursor: hand;",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex(), ThemeManager.getAccentHex()));
        }
        if (resetSetupBtn != null) {
            resetSetupBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 16px; -fx-cursor: hand;",
                    ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        }
        if (btn1v1 != null && btnTeam != null && btnFfa != null) {
            updatePillSelection(btn1v1, btnTeam, btnFfa, selectedGameMode);
        }

        // Re-render inputs and game cards so buttons & borders update to new palette
        List<Competitor> currentCompData = getCompetitorProfiles();
        renderCompetitorInputs();
        if (currentCompData != null && !currentCompData.isEmpty()) {
            for (int i = 0; i < competitorRows.size(); i++) {
                if (i < currentCompData.size()) {
                    Competitor c = currentCompData.get(i);
                    CompetitorInputRow row = competitorRows.get(i);
                    if (c.getName() != null && !c.getName().isEmpty()) {
                        row.nameInput.setText(c.getName());
                    }
                    if (c.getAvatarPath() != null && !c.getAvatarPath().isEmpty()) {
                        row.avatarPath = c.getAvatarPath();
                        try {
                            File f = new File(c.getAvatarPath());
                            if (!f.exists()) f = new File("template-offline/" + c.getAvatarPath());
                            if (f.exists()) {
                                row.avatarPreview.setImage(new Image(f.toURI().toString()));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        renderGameCards();
    }

    public void cleanup() {
        I18n.removeListener(i18nListener);
    }

    public void refreshTextsAndStyles() {
        refreshTexts();
        refreshStyles();
    }

    public void refreshTexts() {
        if (titleLabel != null) titleLabel.setText(I18n.get("settings.title"));
        if (subLabel != null) subLabel.setText(I18n.get("settings.subtitle"));
        if (importSetupBtn != null) {
            importSetupBtn.setText(I18n.get("settings.btn.import_setup"));
            importSetupBtn.setTooltip(new Tooltip(I18n.get("settings.help.import_btn_tooltip")));
        }
        if (exportSetupBtn != null) {
            exportSetupBtn.setText(I18n.get("settings.btn.export_setup"));
            exportSetupBtn.setTooltip(new Tooltip(I18n.get("settings.help.export_btn_tooltip")));
        }
        if (resetSetupBtn != null) {
            resetSetupBtn.setText(I18n.get("settings.btn.reset_setup"));
            resetSetupBtn.setTooltip(new Tooltip(I18n.get("settings.help.reset_btn_tooltip")));
        }

        if (modeTitle != null) modeTitle.setText(I18n.get("settings.mode.title"));
        if (modeHelpLabel != null) modeHelpLabel.setText(I18n.get("settings.mode.help"));
        if (btn1v1 != null) {
            btn1v1.setText(I18n.get("settings.mode.1v1"));
            btn1v1.setTooltip(new Tooltip(I18n.get("settings.mode.1v1.hint")));
        }
        if (btnTeam != null) {
            btnTeam.setText(I18n.get("settings.mode.teams"));
            btnTeam.setTooltip(new Tooltip(I18n.get("settings.mode.teams.hint")));
        }
        if (btnFfa != null) {
            btnFfa.setText(I18n.get("settings.mode.ffa"));
            btnFfa.setTooltip(new Tooltip(I18n.get("settings.mode.ffa.hint")));
        }
        if (ffaLabel != null) ffaLabel.setText(I18n.get("settings.mode.ffa.count"));

        if (battleRoyaleTitle != null) battleRoyaleTitle.setText(I18n.get("settings.mode.battleroyale.title"));
        if (battleRoyaleHelpLabel != null) battleRoyaleHelpLabel.setText(I18n.get("settings.mode.battleroyale.desc"));
        updateBattleRoyaleUI();

        if (competitorsTitle != null) competitorsTitle.setText(I18n.get("settings.players.title"));
        if (competitorsSubtitle != null) competitorsSubtitle.setText(I18n.get("settings.players.help"));
        for (CompetitorInputRow row : competitorRows) {
            if (row.nameInput != null) {
                row.nameInput.setPromptText(I18n.get("settings.players.enter_name"));
            }
            if (row.chooseAvatarBtn != null) {
                row.chooseAvatarBtn.setText(I18n.get("settings.players.choose_avatar"));
            }
        }

        if (scorePresetsTitle != null) scorePresetsTitle.setText(I18n.get("settings.score.title"));
        if (resetPresetsBtn != null) resetPresetsBtn.setText("🔄 " + I18n.get("settings.score.reset_btn"));
        if (scorePresetsSubtitle != null) scorePresetsSubtitle.setText(I18n.get("settings.score.subtitle"));
        if (scoreHelpLabel != null) scoreHelpLabel.setText(I18n.get("settings.score.help"));

        if (newPresetInput != null) newPresetInput.setPromptText(I18n.get("settings.score.input_placeholder"));
        if (addPresetBtn != null) addPresetBtn.setText("➕ " + I18n.get("settings.score.add_btn"));

        if (gamesTitle != null) gamesTitle.setText(I18n.get("settings.games.title"));
        if (gamesSubtitle != null) gamesSubtitle.setText(I18n.get("settings.games.help"));
        if (backBtn != null) backBtn.setText(I18n.get("settings.btn.main_menu"));
        if (launchBtn != null) launchBtn.setText(I18n.get("settings.btn.start_game"));
    }

    private void renderScorePresetChips() {
        if (scoreChipsPane == null) return;
        double currentV = getVvalue();
        scoreChipsPane.getChildren().clear();

        for (String preset : new ArrayList<>(scorePresets)) {
            boolean isNegative = preset.startsWith("-");
            HBox chip = new HBox(6);
            chip.setAlignment(Pos.CENTER);
            chip.setPadding(new Insets(5, 10, 5, 12));

            String chipBg = ThemeManager.getButtonHex();
            String chipTextColor = isNegative ? "#f43f5e" : (ThemeManager.getTextOnButtonPrimaryHex());
            String borderColor = isNegative ? "rgba(244, 63, 94, 0.4)" : "rgba(255, 255, 255, 0.15)";

            chip.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 12px; -fx-background-radius: 12px;",
                    chipBg, borderColor));

            Label valLabel = new Label(preset);
            valLabel.setStyle(String.format(
                    "-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: %s;",
                    chipTextColor));

            Button delBtn = new Button("✕");
            delBtn.setFocusTraversable(false);
            delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 900; -fx-padding: 0 2; -fx-cursor: hand;");
            delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: 900; -fx-padding: 0 2; -fx-cursor: hand;"));
            delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 900; -fx-padding: 0 2; -fx-cursor: hand;"));
            delBtn.setOnAction(e -> {
                e.consume();
                scorePresets.remove(preset);
                renderScorePresetChips();
            });

            chip.getChildren().addAll(valLabel, delBtn);
            scoreChipsPane.getChildren().add(chip);
        }

        javafx.application.Platform.runLater(() -> setVvalue(currentV));
    }
}
