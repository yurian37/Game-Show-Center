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
import javafx.scene.layout.*;

public class LicenseActivationFxView extends VBox {

    private final MainApp mainApp;
    private final Label statusLabel;
    private final TextField keyInput;

    public LicenseActivationFxView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setPadding(new Insets(40));

        // Card Container
        VBox card = new VBox(20);
        card.setMaxWidth(550);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: #1e293b; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: #334155; " +
            "-fx-border-radius: 16px; " +
            "-fx-border-width: 1.5px; " +
            "-fx-padding: 36px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 10);"
        );

        // Header Title
        Node lockIcon = SvgEmoji.create("lock", 48);

        Label title = new Label(I18n.get("license.title"));
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label subtitle = new Label(I18n.get("license.subtitle"));
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");

        // HWID Display Box
        VBox hwidBox = new VBox(8);
        hwidBox.setAlignment(Pos.CENTER_LEFT);
        hwidBox.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 10px; -fx-padding: 14px; -fx-border-color: #475569; -fx-border-radius: 10px;");

        Label hwidLabel = new Label(I18n.get("license.hwid_label"));
        hwidLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        String currentHwid = HardwareIdUtil.getHardwareId();
        TextField hwidField = new TextField(currentHwid);
        hwidField.setEditable(false);
        hwidField.setStyle("-fx-background-color: transparent; -fx-text-fill: #f1f5f9; -fx-font-family: monospace; -fx-font-size: 15px; -fx-font-weight: bold;");

        Button copyBtn = new Button(I18n.get("license.copy_hwid"));
        copyBtn.setStyle(
            "-fx-background-color: #334155; -fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6px 12px;"
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

        // License Key Entry Section
        VBox keySection = new VBox(8);
        keySection.setAlignment(Pos.CENTER_LEFT);

        Label inputLabel = new Label(I18n.get("license.key_label"));
        inputLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        keyInput = new TextField();
        keyInput.setPromptText(I18n.get("license.key_placeholder"));
        keyInput.setStyle(
            "-fx-background-color: #0f172a; -fx-text-fill: #ffffff; -fx-font-size: 14px; " +
            "-fx-prompt-text-fill: #64748b; -fx-border-color: #3b82f6; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10px;"
        );

        keySection.getChildren().addAll(inputLabel, keyInput);

        // Status Label
        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // Action Buttons
        Button activateBtn = new Button(I18n.get("license.activate_btn"));
        activateBtn.setMaxWidth(Double.MAX_VALUE);
        activateBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb); " +
            "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 12px; " +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        );
        activateBtn.setOnAction(e -> handleActivation());

        // Developer info
        Label footerHint = new Label(I18n.get("license.footer_hint"));
        footerHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(lockIcon, title, subtitle, hwidBox, keySection, statusLabel, activateBtn, footerHint);
        this.getChildren().add(card);
    }

    private void handleActivation() {
        String key = keyInput.getText();
        if (key == null || key.isBlank()) {
            statusLabel.setText(I18n.get("license.status.enter_key"));
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        boolean success = LicenseManager.activateProduct(key);
        if (success) {
            statusLabel.setText(I18n.get("license.status.success"));
            statusLabel.setStyle("-fx-text-fill: #22c55e;");
            mainApp.onLicenseActivated();
        } else {
            statusLabel.setText(I18n.get("license.status.invalid"));
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
        }
    }
}
