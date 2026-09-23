package com.gameshowcenter.offline.games.editors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class TimeLineSetupEditor implements IGameSetupEditor {

    public static class EventItem {
        public String id;
        public TextField titleField;
        public TextField yearField;
        public TextField descField;

        public EventItem(String id, String title, int year, String desc) {
            this.id = id;
            this.titleField = new TextField(title != null ? title : "");
            this.yearField = new TextField(String.valueOf(year));
            this.descField = new TextField(desc != null ? desc : "");
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<EventItem> eventItems = new ArrayList<>();
    private VBox eventsContainer;

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(10));
        String textColor = palette != null ? ThemeManager.getContrastTextColor(palette.bgCard) : ThemeManager.getTextPrimaryHex();

        // 1. Info Label explaining manual host scoring and no rounds
        Label descLabel = new Label("Juego educativo sin rondas predefinidas: termina cuando todos los hitos se colocan en la línea de tiempo. El host asigna los puntos manualmente desde la Arena.");
        descLabel.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s; -fx-wrap-text: true;", ThemeManager.getTextOnCardSecondaryHex()));
        descLabel.setWrapText(true);
        root.getChildren().add(descLabel);

        // 2. Events List Section Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label info = new Label("Hitos Históricos en la Línea de Tiempo:");
        info.setStyle(String.format("-fx-font-weight: 900; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));

        Button addBtn = new Button("➕ Agregar Hito");
        addBtn.getStyleClass().add("btn-accent-emerald");
        addBtn.setOnAction(e -> addEventRow("Nuevo Hito", 2000, "Descripción del evento histórico"));

        header.getChildren().addAll(info, addBtn);
        root.getChildren().add(header);

        // 3. Scrollable list of events
        eventsContainer = new VBox(8);
        eventItems.clear();

        if (currentSetup != null && currentSetup.has("events") && currentSetup.get("events").isArray()) {
            for (JsonNode ev : currentSetup.get("events")) {
                String id = ev.has("id") ? ev.get("id").asText() : ("ev_" + (eventItems.size() + 1));
                String title = ev.has("title") ? ev.get("title").asText() : "Hito";
                int year = ev.has("year") ? ev.get("year").asInt() : 1900;
                String desc = ev.has("description") ? ev.get("description").asText() : "";
                addEventRow(id, title, year, desc);
            }
        }

        if (eventItems.isEmpty()) {
            addEventRow("Invención de la Rueda", -3500, "Primeros vestigios en Mesopotamia");
            addEventRow("Imprenta de Gutenberg", 1440, "Revolución de los libros impresos");
            addEventRow("Apolo 11: Llegada a la Luna", 1969, "Primer alunizaje de la historia");
        }

        ScrollPane scroll = new ScrollPane(eventsContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(340);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 4px;");

        root.getChildren().add(scroll);
        return root;
    }

    private void addEventRow(String title, int year, String desc) {
        addEventRow("ev_" + (eventItems.size() + 1), title, year, desc);
    }

    private void addEventRow(String id, String title, int year, String desc) {
        EventItem item = new EventItem(id, title, year, desc);
        eventItems.add(item);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 8, 6, 8));
        row.setStyle("-fx-background-color: #1a2238; -fx-border-color: #2e3856; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        item.titleField.setPromptText("Título del hito");
        item.titleField.setPrefWidth(180);

        item.yearField.setPromptText("Año (ej. -500 o 1969)");
        item.yearField.setPrefWidth(90);

        item.descField.setPromptText("Descripción o pista breve");
        HBox.setHgrow(item.descField, Priority.ALWAYS);

        Button delBtn = new Button("✕");
        delBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            if (eventItems.size() <= 3) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Se requieren al menos 3 eventos para la línea de tiempo.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            eventItems.remove(item);
            eventsContainer.getChildren().remove(row);
        });

        row.getChildren().addAll(item.titleField, item.yearField, item.descField, delBtn);
        eventsContainer.getChildren().add(row);
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "TimeLine");

        ArrayNode eventsArr = root.putArray("events");
        for (int i = 0; i < eventItems.size(); i++) {
            EventItem item = eventItems.get(i);
            String title = item.titleField.getText().trim();
            if (title.isEmpty()) continue;

            int year = 0;
            try {
                year = Integer.parseInt(item.yearField.getText().trim());
            } catch (Exception ignored) {}

            String desc = item.descField.getText().trim();

            ObjectNode evNode = objectMapper.createObjectNode();
            evNode.put("id", item.id != null ? item.id : ("ev_" + (i + 1)));
            evNode.put("title", title);
            evNode.put("year", year);
            evNode.put("description", desc);
            eventsArr.add(evNode);
        }

        return root;
    }

    @Override
    public String validateSetupData(JsonNode setupData, List<Competitor> profiles) {
        if (setupData == null) return "La configuración de TimeLine está vacía.";

        JsonNode eventsNode = setupData.has("events") ? setupData.get("events") : null;
        if (eventsNode == null || !eventsNode.isArray() || eventsNode.size() < 3) {
            return "TimeLine requiere al menos 3 eventos históricos para poder jugar.";
        }

        for (JsonNode ev : eventsNode) {
            if (!ev.has("title") || ev.get("title").asText().trim().isEmpty()) {
                return "Todos los eventos deben tener un título válido.";
            }
            if (!ev.has("year") || !ev.get("year").isIntegralNumber()) {
                return "Todos los eventos deben tener un año numérico entero.";
            }
        }

        return null;
    }
}
