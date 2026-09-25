package com.gameshowcenter.offline;

import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.GameDescriptor;
import com.gameshowcenter.offline.model.MatchConfig;
import com.gameshowcenter.offline.service.GameScannerService;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.views.*;
import com.gameshowcenter.offline.util.SvgEmoji;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.List;

public class MainApp extends Application {

    private final GameScannerService scannerService = new GameScannerService();
    private List<GameDescriptor> availableGames;

    private BorderPane root;
    private StackPane centerContainer;
    private Stage primaryStage;
    private MatchConfig currentMatchConfig;

    private HomeFxView homeView;
    private SettingsFxView settingsView;
    private ArenaStageFxView arenaStageView;
    private WinnerStageFxView winnerStageView;

    private ComboBox<I18n.LanguageOption> langCombo;
    private Button themeBtn;

    private static String cachedBgPath = null;
    private static Image cachedBgImage = null;

    public void setHeaderControlsLocked(boolean locked) {
        I18n.setSwitchingLocked(locked);
        if (langCombo != null) {
            langCombo.setDisable(locked);
            langCombo.setTooltip(locked ? new Tooltip(I18n.get("app.header.locked_in_match")) : null);
        }
        if (themeBtn != null) {
            themeBtn.setDisable(locked);
            themeBtn.setTooltip(locked ? new Tooltip(I18n.get("app.header.locked_in_match")) : null);
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        ThemeManager.loadSavedThemePreference();
        primaryStage.setTitle(I18n.get("app.title"));

        // Set Window & Taskbar Icon
        File iconFile = new File("assets/logo/logo.png");
        if (!iconFile.exists()) iconFile = new File("template-offline/assets/logo/logo.png");
        if (iconFile.exists()) {
            try {
                primaryStage.getIcons().add(new Image(iconFile.toURI().toString()));
            } catch (Exception ignored) {}
        }

        // Register i18n listener to refresh views dynamically when language changes
        I18n.addListener(this::refreshLanguageAndTheme);

        // Allow fluid resizing on any screen, laptop or monitor
        primaryStage.setMinWidth(750);
        primaryStage.setMinHeight(520);

        primaryStage.setOnCloseRequest(e -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        // Scan /games/ folder for offline minigames
        File gamesDir = new File("games");
        if (!gamesDir.exists()) {
            gamesDir = new File("template-offline/games");
        }
        availableGames = scannerService.scanGamesFolder(gamesDir);
        System.out.println("Discovered " + availableGames.size() + " offline games in /games/ folder.");

        // Root BorderPane Layout
        root = new BorderPane();

        // 1. TOP HEADER (With Logo, Language, Theme Customizer & Fullscreen)
        root.setTop(createHeader());

        // 2. BOTTOM FOOTER
        root.setBottom(createFooter());

        // 3. CENTER FLUID CONTAINER (100% responsive without artificial zoom)
        centerContainer = new StackPane();
        centerContainer.setStyle("-fx-padding: 12px;");
        centerContainer.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(centerContainer, Priority.ALWAYS);
        HBox.setHgrow(centerContainer, Priority.ALWAYS);

        root.setCenter(centerContainer);

        // Initialize Navigation Views (License check)
        if (com.gameshowcenter.offline.security.LicenseManager.isActivated()) {
            showHomeView();
        } else {
            showLicenseActivationView();
        }

        // Scene & Stylesheet
        Scene scene = new Scene(root, 1280, 800);
        scene.setFill(Color.web(ThemeManager.getAppBgHex()));
        URL cssResource = getClass().getResource("/styles/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        // F11: Fullscreen toggle
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
                event.consume();
            }
        });

        applyRootBackground();

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showLicenseActivationView() {
        LicenseActivationFxView licenseView = new LicenseActivationFxView(this);
        centerContainer.getChildren().setAll(licenseView);
        applyRootBackground();
    }

    public void onLicenseActivated() {
        showHomeView();
    }

    public void applyRootBackground() {
        String bgHex = ThemeManager.getAppBgHex();
        Color bgColor = Color.web(bgHex);

        if (primaryStage != null && primaryStage.getScene() != null) {
            primaryStage.getScene().setFill(bgColor);
        }

        BackgroundFill solidFill = new BackgroundFill(
                bgColor,
                CornerRadii.EMPTY,
                Insets.EMPTY
        );

        String bgPath = ThemeManager.getCustomBackgroundImagePath();
        File imgFile = null;
        if (bgPath != null && !bgPath.isBlank()) {
            imgFile = new File(bgPath);
            if (!imgFile.exists()) {
                imgFile = new File("template-offline/" + bgPath);
            }
        }

        if (imgFile != null && imgFile.exists()) {
            try {
                if (cachedBgImage == null || !imgFile.getAbsolutePath().equals(cachedBgPath)) {
                    Image loaded = new Image(imgFile.toURI().toString());
                    if (!loaded.isError()) {
                        cachedBgImage = loaded;
                        cachedBgPath = imgFile.getAbsolutePath();
                    } else {
                        cachedBgImage = null;
                        cachedBgPath = null;
                    }
                }

                if (cachedBgImage != null && !cachedBgImage.isError()) {
                    BackgroundImage bgImage = new BackgroundImage(
                            cachedBgImage,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundPosition.CENTER,
                            new BackgroundSize(100, 100, true, true, true, true));
                    root.setBackground(new Background(new BackgroundFill[]{solidFill}, new BackgroundImage[]{bgImage}));
                } else {
                    root.setBackground(new Background(solidFill));
                }
            } catch (Exception ignored) {
                root.setBackground(new Background(solidFill));
            }
        } else {
            cachedBgImage = null;
            cachedBgPath = null;
            root.setBackground(new Background(solidFill));
        }

        root.setStyle(String.format("-fx-background-color: %s;", bgHex));

        ThemeManager.applyTextScale(root, ThemeManager.getFontScale());
    }

    public void refreshHeaderOnly() {
        applyRootBackground();
        root.setTop(createHeader());
        root.setBottom(createFooter());
        ThemeManager.applyTextScale(root, ThemeManager.getFontScale());
    }

    public void refreshTheme() {
        refreshHeaderOnly();
        if (centerContainer != null && !centerContainer.getChildren().isEmpty()) {
            javafx.scene.Node current = centerContainer.getChildren().get(0);
            if (current instanceof HomeFxView) {
                showHomeView();
            } else if (current instanceof SettingsFxView) {
                ((SettingsFxView) current).refreshTextsAndStyles();
            } else if (current instanceof ArenaStageFxView) {
                showArenaStageView();
            } else if (current instanceof WinnerStageFxView) {
                showWinnerStageView();
            }
        }
        ThemeManager.applyTextScale(root, ThemeManager.getFontScale());
    }

    public void refreshLanguageAndTheme() {
        if (primaryStage != null) {
            primaryStage.setTitle(I18n.get("app.title"));
        }
        refreshTheme();
    }

    private HBox createHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(String.format(
                "-fx-background-color: %s; -fx-padding: 10px 20px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 0 0 1px 0;",
                ThemeManager.getMainBoxHex()));

        // Official Logo Image
        ImageView logoView = null;
        File logoFile = new File("assets/logo/logo.png");
        if (!logoFile.exists())
            logoFile = new File("template-offline/assets/logo/logo.png");

        if (logoFile.exists()) {
            try {
                Image logoImg = new Image(logoFile.toURI().toString());
                logoView = new ImageView(logoImg);
                logoView.setFitHeight(38);
                logoView.setPreserveRatio(true);
            } catch (Exception ignored) {
            }
        }

        Text logoText = new Text(I18n.get("app.logo.text"));
        logoText.setStyle(String.format("-fx-font-size: 20px; -fx-font-weight: 900; -fx-fill: %s;",
                ThemeManager.getTextOnMainBoxPrimaryHex()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Language Selector ComboBox
        langCombo = new ComboBox<>();
        langCombo.getItems().addAll(I18n.SUPPORTED_LANGUAGES);
        for (I18n.LanguageOption opt : I18n.SUPPORTED_LANGUAGES) {
            if (opt.code.equalsIgnoreCase(I18n.getLanguage())) {
                langCombo.setValue(opt);
                break;
            }
        }
        langCombo.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        langCombo.setOnAction(e -> {
            I18n.LanguageOption selected = langCombo.getValue();
            if (selected != null) {
                I18n.setLanguage(selected.code);
            }
        });

        // Fullscreen Toggle Button
        Button fullScreenBtn = new Button(I18n.get("app.header.fullscreen"));
        SvgEmoji.setGraphic(fullScreenBtn, "monitor", 14);
        fullScreenBtn.setTooltip(new Tooltip(I18n.get("app.header.fullscreen.tooltip")));
        fullScreenBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 7px 14px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 8px;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        fullScreenBtn.setOnAction(e -> {
            if (primaryStage != null) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
            }
        });

        // Theme Customizer Button
        themeBtn = new Button(I18n.get("app.header.theme"));
        SvgEmoji.setGraphic(themeBtn, "settings", 14);
        themeBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 7px 14px; -fx-background-radius: 8px; -fx-cursor: hand;",
                ThemeManager.getButtonHex(), ThemeManager.getTextOnButtonPrimaryHex()));
        themeBtn.setOnAction(e -> ThemeCustomizerDialog.show(primaryStage, this::refreshTheme));

        // Studio Branding Badge
        HBox badgeBox = new HBox(4);
        badgeBox.setAlignment(Pos.CENTER);
        badgeBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-padding: 5px 10px; -fx-background-radius: 10px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 10px;",
                ThemeManager.getCardHex()));

        Label byText = new Label(I18n.get("app.header.by") + " ");
        byText.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 10px;",
                ThemeManager.getTextOnCardSecondaryHex()));
        Label studioText = new Label("YUYI STUDIO");
        studioText.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: 900; -fx-font-size: 10px;",
                ThemeManager.getAccentHex()));
        badgeBox.getChildren().addAll(byText, studioText);

        if (logoView != null) {
            header.getChildren().addAll(logoView, logoText, spacer, langCombo, fullScreenBtn, themeBtn, badgeBox);
        } else {
            Label fallbackIcon = new Label();
            SvgEmoji.setGraphic(fallbackIcon, "target", 24);
            header.getChildren().addAll(fallbackIcon, logoText, spacer, langCombo, fullScreenBtn, themeBtn, badgeBox);
        }

        if (I18n.isSwitchingLocked()) {
            setHeaderControlsLocked(true);
        }

        return header;
    }

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.getStyleClass().add("app-footer");
        footer.setStyle("-fx-padding: 8px 16px;");

        Label footerText = new Label(I18n.get("app.footer.copyright"));
        footerText.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s;", ThemeManager.getTextOnAppBgSecondaryHex()));
        footerText.getStyleClass().add("app-footer-text");

        footer.getChildren().add(footerText);
        return footer;
    }

    public void showHomeView() {
        setHeaderControlsLocked(false);
        com.gameshowcenter.offline.sound.SoundManager.getInstance().stopAll();
        homeView = new HomeFxView(this, () -> showSettingsView());
        centerContainer.getChildren().setAll(homeView);
        applyRootBackground();
    }

    public void showSettingsView() {
        setHeaderControlsLocked(false);
        com.gameshowcenter.offline.sound.SoundManager.getInstance().stopAll();
        settingsView = new SettingsFxView(availableGames, new SettingsFxView.SettingsListener() {
            @Override
            public void onStartMatchConfigured(MatchConfig config) {
                currentMatchConfig = config;
                showArenaStageView(); // Direct to Arena Stage
            }

            @Override
            public void onBackToHome() {
                showHomeView();
            }
        });
        centerContainer.getChildren().setAll(settingsView);
        applyRootBackground();
    }

    public void showArenaStageView() {
        setHeaderControlsLocked(true);
        com.gameshowcenter.offline.sound.SoundManager.getInstance().stopAll();
        arenaStageView = new ArenaStageFxView(currentMatchConfig, new ArenaStageFxView.ArenaStageListener() {
            @Override
            public void onEndMatch(MatchConfig config) {
                showWinnerStageView();
            }

            @Override
            public void onOpenSettings() {
                showSettingsView();
            }
        });
        centerContainer.getChildren().setAll(arenaStageView);
        applyRootBackground();
        ThemeManager.applyTextScale(arenaStageView, ThemeManager.getFontScale());
    }

    public void showWinnerStageView() {
        setHeaderControlsLocked(false);
        com.gameshowcenter.offline.sound.SoundManager.getInstance().stopAll();
        winnerStageView = new WinnerStageFxView(currentMatchConfig, new WinnerStageFxView.WinnerStageListener() {
            @Override
            public void onRestartMatch() {
                showSettingsView();
            }

            @Override
            public void onReturnHome() {
                showHomeView();
            }
        });
        centerContainer.getChildren().setAll(winnerStageView);
        applyRootBackground();
    }

    @Override
    public void stop() throws Exception {
        com.gameshowcenter.offline.sound.SoundManager.getInstance().shutdown();
        super.stop();
        javafx.application.Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 GameShowCenter/1.0");
        launch(args);
    }
}
