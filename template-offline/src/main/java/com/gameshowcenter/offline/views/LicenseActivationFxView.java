package com.gameshowcenter.offline.views;

import com.gameshowcenter.offline.MainApp;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.security.HardwareIdUtil;
import com.gameshowcenter.offline.security.LicenseManager;
import com.gameshowcenter.offline.util.SvgEmoji;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class LicenseActivationFxView extends VBox {

    private final MainApp mainApp;
    private final Label statusTitleLabel;
    private final Label statusMessageLabel;
    private final VBox statusCard;
    private final TextField keyInput;

    public LicenseActivationFxView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setPadding(new Insets(30, 20, 30, 20));

        // Card Container
        VBox card = new VBox(18);
        card.setMaxWidth(620);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: #1e293b; " +
            "-fx-background-radius: 18px; " +
            "-fx-border-color: #334155; " +
            "-fx-border-radius: 18px; " +
            "-fx-border-width: 1.5px; " +
            "-fx-padding: 32px 36px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 25, 0, 0, 12);"
        );

        // Header Title
        Node lockIcon = SvgEmoji.create("lock", 46);

        Label title = new Label(I18n.get("license.title"));
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #f8fafc; -fx-letter-spacing: 1px;");

        Label subtitle = new Label(I18n.get("license.subtitle"));
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");

        // HWID Display Box
        VBox hwidBox = new VBox(6);
        hwidBox.setAlignment(Pos.CENTER_LEFT);
        hwidBox.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 10px; -fx-padding: 12px 14px; -fx-border-color: #475569; -fx-border-radius: 10px;");

        Label hwidLabel = new Label(I18n.get("license.hwid_label"));
        hwidLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        String currentHwid = HardwareIdUtil.getHardwareId();
        TextField hwidField = new TextField(currentHwid);
        hwidField.setEditable(false);
        hwidField.setStyle("-fx-background-color: transparent; -fx-text-fill: #f1f5f9; -fx-font-family: monospace; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button copyBtn = new Button(I18n.get("license.copy_hwid"));
        copyBtn.setStyle(
            "-fx-background-color: #334155; -fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6px 12px;"
        );
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(currentHwid);
            clipboard.setContent(content);
            copyBtn.setText(I18n.get("license.copied"));
        });

        HBox hwidRow = new HBox(10, hwidField, copyBtn);
        HBox.setHgrow(hwidField, Priority.ALWAYS);
        hwidRow.setAlignment(Pos.CENTER_LEFT);
        hwidBox.getChildren().addAll(hwidLabel, hwidRow);

        // 1. PRIMARY: ENCRYPTED FILE ACTIVATION ZONE (Drag and drop + Button)
        VBox fileDropBox = new VBox(10);
        fileDropBox.setAlignment(Pos.CENTER);
        fileDropBox.setStyle(
            "-fx-background-color: rgba(59, 130, 246, 0.08); " +
            "-fx-border-color: #3b82f6; -fx-border-width: 1.5px; -fx-border-style: dashed; " +
            "-fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 20px 16px;"
        );

        Node fileIcon = SvgEmoji.create("document", 32);
        Label fileDropTitle = new Label("Cargar Archivo de Licencia Encriptado");
        fileDropTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        Label fileDropHint = new Label("Arrastra tu archivo (.lic / .gsc) aquí o haz clic para buscarlo.");
        fileDropHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        Button selectFileBtn = new Button("📂 Seleccionar Archivo de Licencia (.lic)");
        selectFileBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb); " +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 9px 20px; " +
            "-fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(59,130,246,0.3), 8, 0, 0, 2);"
        );
        selectFileBtn.setOnAction(e -> handleSelectLicenseFile());

        fileDropBox.getChildren().addAll(fileIcon, fileDropTitle, fileDropHint, selectFileBtn);

        // Support Drag & Drop on the fileDropBox
        fileDropBox.setOnDragOver(event -> {
            if (event.getGestureSource() != fileDropBox && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        fileDropBox.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File dropped = db.getFiles().get(0);
                handleFileActivation(dropped);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // 2. STATUS CARD (Alert Box for Conflict, Expiration or Success)
        statusCard = new VBox(6);
        statusCard.setAlignment(Pos.CENTER_LEFT);
        statusCard.setPadding(new Insets(12, 14, 12, 14));
        statusCard.setStyle("-fx-background-radius: 10px;");
        statusCard.setVisible(false);
        statusCard.setManaged(false);

        statusTitleLabel = new Label("");
        statusTitleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 900;");

        statusMessageLabel = new Label("");
        statusMessageLabel.setWrapText(true);
        statusMessageLabel.setStyle("-fx-font-size: 12px;");

        statusCard.getChildren().addAll(statusTitleLabel, statusMessageLabel);

        // 3. SECONDARY: DEVELOPER / MANUAL KEY ACCORDION (Subtle)
        TitledPane devPane = new TitledPane();
        devPane.setText("Opción avanzada: Clave manual o Master Key");
        devPane.setExpanded(false);
        devPane.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        VBox keySection = new VBox(8);
        keySection.setPadding(new Insets(10));
        keySection.setAlignment(Pos.CENTER_LEFT);

        keyInput = new TextField();
        keyInput.setPromptText(I18n.get("license.key_placeholder"));
        keyInput.setStyle(
            "-fx-background-color: #0f172a; -fx-text-fill: #ffffff; -fx-font-size: 13px; " +
            "-fx-prompt-text-fill: #64748b; -fx-border-color: #475569; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px;"
        );

        Button activateKeyBtn = new Button(I18n.get("license.activate_btn"));
        activateKeyBtn.setMaxWidth(Double.MAX_VALUE);
        activateKeyBtn.setStyle(
            "-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8px; " +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        );
        activateKeyBtn.setOnAction(e -> handleKeyActivation());

        keySection.getChildren().addAll(keyInput, activateKeyBtn);
        devPane.setContent(keySection);

        // Footer info
        Label footerHint = new Label(I18n.get("license.footer_hint"));
        footerHint.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #64748b; -fx-text-alignment: center;");

        card.getChildren().addAll(lockIcon, title, subtitle, hwidBox, fileDropBox, statusCard, devPane, footerHint);
        this.getChildren().add(card);
    }

    private void handleSelectLicenseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar Archivo de Licencia Encriptado (.lic)");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Archivos de Licencia (*.lic, *.gsc)", "*.lic", "*.gsc"),
            new FileChooser.ExtensionFilter("Todos los archivos (*.*)", "*.*")
        );

        File initialDir = new File(System.getProperty("user.dir"));
        if (initialDir.exists()) chooser.setInitialDirectory(initialDir);

        File selected = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            handleFileActivation(selected);
        }
    }

    private void handleFileActivation(File file) {
        LicenseManager.ActivationResult result = LicenseManager.activateWithFile(file);
        renderActivationResult(result);
    }

    private void handleKeyActivation() {
        String key = keyInput.getText();
        if (key == null || key.isBlank()) {
            showStatus("Entrada requerida", I18n.get("license.status.enter_key"), "#ef4444", "#450a0a", "#dc2626");
            return;
        }

        boolean success = LicenseManager.activateProduct(key);
        if (success) {
            showStatus("¡Activación Exitosa!", "La aplicación ha sido desbloqueada correctamente.", "#22c55e", "#052e16", "#16a34a");
            mainApp.onLicenseActivated();
        } else {
            showStatus("Clave Inválida", I18n.get("license.status.invalid"), "#ef4444", "#450a0a", "#dc2626");
        }
    }

    private void renderActivationResult(LicenseManager.ActivationResult result) {
        if (result.isSuccess()) {
            showStatus("✅ ¡ACTIVACIÓN EXITOSA!", result.getMessage(), "#22c55e", "#052e16", "#16a34a");
            mainApp.onLicenseActivated();
            return;
        }

        switch (result.getStatus()) {
            case HARDWARE_CONFLICT:
                showStatus("⚠️ CONFLICTO DE HARDWARE", result.getMessage(), "#f87171", "#450a0a", "#dc2626");
                break;
            case ACTIVATION_WINDOW_EXPIRED:
                showStatus("⏳ PERÍODO DE ACTIVACIÓN VENCIDO", result.getMessage(), "#fbbf24", "#451a03", "#d97706");
                break;
            case ANNUAL_EXPIRED:
                showStatus("📅 LICENCIA ANUAL EXPIRADA", result.getMessage(), "#f87171", "#450a0a", "#dc2626");
                break;
            default:
                showStatus("❌ ERROR DE ACTIVACIÓN", result.getMessage(), "#ef4444", "#450a0a", "#dc2626");
                break;
        }
    }

    private void showStatus(String title, String message, String textHex, String bgHex, String borderHex) {
        statusCard.setVisible(true);
        statusCard.setManaged(true);
        statusCard.setStyle(String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 12px 14px;",
            bgHex, borderHex
        ));
        statusTitleLabel.setText(title);
        statusTitleLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: 900;", textHex));

        statusMessageLabel.setText(message);
        statusMessageLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 12px;", textHex));
    }
}
