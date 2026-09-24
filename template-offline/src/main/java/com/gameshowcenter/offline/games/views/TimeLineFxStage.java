package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;

public class TimeLineFxStage extends VBox {

    public static class Milestone {
        public String id;
        public String title;
        public int year;
        public String description;

        public Milestone(String id, String title, int year, String description) {
            this.id = id;
            this.title = title;
            this.year = year;
            this.description = description;
        }

        public String formatYear() {
            if (year < 0) {
                return Math.abs(year) + " a.C.";
            }
            return year + " d.C.";
        }
    }

    private final List<Competitor> profiles;
    private final JsonNode setupData;
    private final Consumer<Competitor> winnerListener;

    private int currentPlayerIndex = 0;

    private final List<Milestone> allEvents = new ArrayList<>();
    private final List<Milestone> timeline = new ArrayList<>();
    private final List<Milestone> deck = new ArrayList<>();

    private Milestone currentMystery = null;
    private String lastPlacedId = null;
    private boolean isTurnAnswered = false;
    private boolean isMatchCompleted = false;

    // UI elements
    private Label remainingBadge;
    private VBox mysteryCardBox;
    private Label mysteryYear;
    private Label mysteryTitle;
    private Label mysteryDesc;
    private Label feedbackLabel;
    private Button nextTurnBtn;
    private HBox timelineAxisBox;
    private ScrollPane timelineScroll;
    private VBox winnerBox;

    public TimeLineFxStage(List<Competitor> profiles, JsonNode setupData) {
        this(profiles, setupData, null);
    }

    private boolean isBattleRoyale = false;

    public TimeLineFxStage(List<Competitor> profiles, JsonNode setupData, Consumer<Competitor> winnerListener) {
        this.profiles = (profiles != null && !profiles.isEmpty())
                ? profiles
                : Arrays.asList(new Competitor("1", "Jugador 1", null), new Competitor("2", "Jugador 2", null));
        this.setupData = setupData;
        this.winnerListener = winnerListener;

        initData();
        buildUI();
        startMatch();
    }

    private void initData() {
        if (setupData != null && setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean()) {
            this.isBattleRoyale = true;
        }

        if (setupData != null && setupData.has("events") && setupData.get("events").isArray()) {
            for (JsonNode ev : setupData.get("events")) {
                String id = ev.has("id") ? ev.get("id").asText() : ("ev_" + (allEvents.size() + 1));
                String title = ev.has("title") ? ev.get("title").asText() : "Hito";
                int year = ev.has("year") ? ev.get("year").asInt() : 1900;
                String desc = ev.has("description") ? ev.get("description").asText() : "";
                allEvents.add(new Milestone(id, title, year, desc));
            }
        }

        boolean needFallback = isBattleRoyale ? allEvents.isEmpty() : (allEvents.size() < 3);
        if (needFallback) {
            allEvents.clear();
            allEvents.add(new Milestone("ev_1", "Invención de la Rueda", -3500, "Primeros vestigios en Mesopotamia"));
            allEvents.add(new Milestone("ev_2", "Imprenta de Gutenberg", 1440, "Revolución de los libros impresos"));
            allEvents.add(new Milestone("ev_3", "Apolo 11: Llegada a la Luna", 1969, "Primer alunizaje de la historia"));
            allEvents.add(new Milestone("ev_4", "Descubrimiento de América", 1492, "Llegada de Colón al Nuevo Mundo"));
            allEvents.add(new Milestone("ev_5", "Penicilina de Fleming", 1928, "Inicio de la era de antibióticos"));
            allEvents.add(new Milestone("ev_6", "World Wide Web", 1989, "Propuesta de Tim Berners-Lee en el CERN"));
        }
    }

    private void buildUI() {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: transparent;");

        // 1. Top Header Bar
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setStyle("-fx-background-color: #131726; -fx-border-color: #2e3856; -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-max-width: 960px;");

        Label gameTitle = new Label(isBattleRoyale ? "⏳ TIMELINE [⚔️ BATTLE ROYALE]" : "⏳ TIMELINE");
        gameTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #f59e0b; -fx-letter-spacing: 1px;");

        remainingBadge = new Label(isBattleRoyale ? "⚔️ BR: Hitos pendientes" : "Hitos pendientes");
        remainingBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1; -fx-background-color: #1e293b; -fx-padding: 4px 10px; -fx-background-radius: 8px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(gameTitle, spacer, remainingBadge);
        getChildren().add(topBar);

        // 2. Mystery Card Section
        mysteryCardBox = new VBox(10);
        mysteryCardBox.setAlignment(Pos.CENTER);
        mysteryCardBox.setPadding(new Insets(18, 24, 18, 24));
        mysteryCardBox.setMinHeight(Region.USE_COMPUTED_SIZE);
        mysteryCardBox.setPrefWidth(640);
        mysteryCardBox.setMaxWidth(720);
        mysteryCardBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e2544, #121626); -fx-border-color: #f59e0b; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.25), 14, 0, 0, 4);");

        HBox mysteryHeader = new HBox(12);
        mysteryHeader.setAlignment(Pos.CENTER);

        Label promptTag = new Label("Hito a ubicar:");
        promptTag.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        promptTag.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        promptTag.setEllipsisString("");

        Region mSpacer = new Region();
        HBox.setHgrow(mSpacer, Priority.ALWAYS);

        mysteryYear = new Label("??? AÑO OCULTO");
        mysteryYear.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #f59e0b; -fx-background-color: #0b0e17; -fx-padding: 4px 12px; -fx-border-color: rgba(245, 158, 11, 0.5); -fx-border-radius: 8px; -fx-background-radius: 8px;");
        mysteryYear.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        mysteryYear.setEllipsisString("");

        mysteryHeader.getChildren().addAll(promptTag, mSpacer, mysteryYear);

        mysteryTitle = new Label("Título del Evento Histórico");
        mysteryTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white; -fx-text-alignment: center;");
        mysteryTitle.setAlignment(Pos.CENTER);
        mysteryTitle.setMaxWidth(Double.MAX_VALUE);
        mysteryTitle.setWrapText(true);
        mysteryTitle.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        mysteryTitle.setEllipsisString("");

        mysteryDesc = new Label("Descripción y pistas sobre cuándo ocurrió este hito...");
        mysteryDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
        mysteryDesc.setAlignment(Pos.CENTER);
        mysteryDesc.setMaxWidth(Double.MAX_VALUE);
        mysteryDesc.setWrapText(true);
        mysteryDesc.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        mysteryDesc.setEllipsisString("");

        feedbackLabel = new Label("");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setAlignment(Pos.CENTER);
        feedbackLabel.setMaxWidth(Double.MAX_VALUE);
        feedbackLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        feedbackLabel.setEllipsisString("");
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        mysteryCardBox.getChildren().addAll(mysteryHeader, mysteryTitle, mysteryDesc, feedbackLabel);
        getChildren().add(mysteryCardBox);

        // Next Turn Button
        nextTurnBtn = new Button("Continuar ➔");
        nextTurnBtn.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #d97706); -fx-text-fill: #0f172a; -fx-font-weight: 900; -fx-font-size: 13px; -fx-padding: 8px 24px; -fx-background-radius: 12px; -fx-cursor: hand;");
        nextTurnBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        nextTurnBtn.setEllipsisString("");
        nextTurnBtn.setVisible(false);
        nextTurnBtn.setManaged(false);
        nextTurnBtn.setOnAction(e -> advanceTurn());
        getChildren().add(nextTurnBtn);

        // 3. Prompt label
        Label prompt = new Label("Haz clic en una ranura [ + ] para ubicar este acontecimiento en la historia:");
        prompt.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1; -fx-background-color: rgba(15, 23, 42, 0.6); -fx-padding: 4px 16px; -fx-background-radius: 20px;");
        prompt.setWrapText(true);
        prompt.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        prompt.setEllipsisString("");
        getChildren().add(prompt);

        // 4. Horizontal Timeline Axis
        timelineAxisBox = new HBox(8);
        timelineAxisBox.setAlignment(Pos.CENTER_LEFT);
        timelineAxisBox.setPadding(new Insets(12, 16, 20, 16));

        timelineScroll = new ScrollPane(timelineAxisBox);
        timelineScroll.setFitToHeight(true);
        timelineScroll.setPrefHeight(250);
        timelineScroll.prefWidthProperty().bind(widthProperty().subtract(32));
        timelineScroll.setMaxWidth(Double.MAX_VALUE);
        timelineScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 4px;");

        timelineScroll.setOnScroll(event -> {
            if (event.getDeltaY() != 0) {
                double delta = event.getDeltaY();
                timelineScroll.setHvalue(timelineScroll.getHvalue() - delta / 300.0);
                event.consume();
            }
        });

        getChildren().add(timelineScroll);

        // 5. Completion Summary Box (hidden initially)
        winnerBox = new VBox(12);
        winnerBox.setAlignment(Pos.CENTER);
        winnerBox.setPadding(new Insets(24));
        winnerBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e2544, #121626); -fx-border-color: #f59e0b; -fx-border-width: 2px; -fx-border-radius: 24px; -fx-background-radius: 24px; -fx-max-width: 500px;");
        winnerBox.setVisible(false);
        winnerBox.setManaged(false);
        getChildren().add(winnerBox);
    }

    private void startMatch() {
        timeline.clear();
        deck.clear();
        currentPlayerIndex = 0;
        lastPlacedId = null;
        isTurnAnswered = false;
        isMatchCompleted = false;

        List<Milestone> shuffled = new ArrayList<>(allEvents);
        Collections.shuffle(shuffled);

        if (isBattleRoyale) {
            if (shuffled.size() == 1) {
                timeline.add(new Milestone("anchor_0", "Año 0 (Inicio de la Era Común)", 0, "Hito base de referencia histórica"));
                deck.addAll(shuffled);
            } else {
                // First milestone anchors the timeline, the remaining N-1 appear as cards to be positioned
                timeline.add(shuffled.remove(0));
                deck.addAll(shuffled);
            }
        } else {
            if (shuffled.size() >= 2) {
                timeline.add(shuffled.remove(0));
                timeline.add(shuffled.remove(0));
                timeline.sort(Comparator.comparingInt(a -> a.year));
            }
            deck.addAll(shuffled);
        }

        if (!deck.isEmpty()) {
            currentMystery = deck.remove(0);
        } else {
            currentMystery = null;
        }

        renderTurn();
        renderTimeline();
    }

    private void renderTurn() {
        int pending = deck.size() + (currentMystery != null ? 1 : 0);
        remainingBadge.setText(isBattleRoyale 
                ? String.format("⚔️ BR: %d hitos restantes", pending)
                : String.format("%d hitos pendientes", pending));


        if (currentMystery != null) {
            mysteryYear.setText("??? AÑO OCULTO");
            mysteryYear.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #f59e0b; -fx-background-color: #0b0e17; -fx-padding: 4px 12px; -fx-border-color: rgba(245, 158, 11, 0.5); -fx-border-radius: 8px; -fx-background-radius: 8px;");
            mysteryTitle.setText(currentMystery.title);
            mysteryDesc.setText(currentMystery.description);
        }

        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        nextTurnBtn.setVisible(false);
        nextTurnBtn.setManaged(false);
        isTurnAnswered = false;
    }

    private static final double CARD_WIDTH = 185.0;

    private double getScaledCardWidth() {
        return Math.max(CARD_WIDTH, CARD_WIDTH * ThemeManager.getFontScale());
    }

    private double computeUniformCardHeight() {
        double scale = ThemeManager.getFontScale();
        double currentCardWidth = getScaledCardWidth();
        double contentWidth = currentCardWidth - 24; // 12px padding each side
        double maxTextHeight = 0;

        javafx.scene.text.Font titleFont = javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12 * scale);
        javafx.scene.text.Font descFont = javafx.scene.text.Font.font("System", 11 * scale);

        List<Milestone> poolToCheck = new ArrayList<>(allEvents);
        poolToCheck.addAll(timeline);

        for (Milestone m : poolToCheck) {
            javafx.scene.text.Text titleText = new javafx.scene.text.Text(m.title != null ? m.title : "");
            titleText.setFont(titleFont);
            titleText.setWrappingWidth(contentWidth);
            double titleH = titleText.getLayoutBounds().getHeight();

            javafx.scene.text.Text descText = new javafx.scene.text.Text(m.description != null ? m.description : "");
            descText.setFont(descFont);
            descText.setWrappingWidth(contentWidth);
            double descH = descText.getLayoutBounds().getHeight();

            double sumH = titleH + descH;
            if (sumH > maxTextHeight) {
                maxTextHeight = sumH;
            }
        }

        // Top tag row (~28px) + padding (24px) + spacing (16px) + text + extra margin (20px * scale)
        double needed = (28 * scale) + 24 + 16 + maxTextHeight + (20 * scale);
        return Math.max(175.0 * scale, Math.ceil(needed));
    }

    private void renderTimeline() {
        timelineAxisBox.getChildren().clear();

        double uniformHeight = computeUniformCardHeight();
        timelineScroll.setPrefHeight(uniformHeight + 65);
        timelineScroll.setMinHeight(uniformHeight + 45);

        // Slot 0: Before first event
        timelineAxisBox.getChildren().add(createSlotButton(0, "Antes de todo", uniformHeight));

        for (int i = 0; i < timeline.size(); i++) {
            Milestone m = timeline.get(i);
            timelineAxisBox.getChildren().add(createMilestoneCard(m, uniformHeight));

            String slotText = (i == timeline.size() - 1) ? "Después de todo" : "Aquí";
            timelineAxisBox.getChildren().add(createSlotButton(i + 1, slotText, uniformHeight));
        }
    }

    private VBox createMilestoneCard(Milestone m, double height) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(12));
        double cardW = getScaledCardWidth();
        card.setPrefSize(cardW, height);
        card.setMinSize(cardW, height);
        card.setMaxSize(cardW, height);

        boolean isNewlyPlaced = m.id != null && m.id.equals(lastPlacedId);

        if (isNewlyPlaced) {
            card.setStyle("-fx-background-color: linear-gradient(to bottom right, #2a2d48, #1a1e36); -fx-border-color: #f59e0b; -fx-border-width: 2.5px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(245, 158, 11, 0.8), 16, 0.3, 0, 0);");
            
            // Pop Animation
            ScaleTransition st = new ScaleTransition(Duration.millis(350), card);
            st.setFromX(0.92);
            st.setFromY(0.92);
            st.setToX(1.05);
            st.setToY(1.05);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        } else {
            card.setStyle("-fx-background-color: #1a2238; -fx-border-color: #2e3856; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 0, 2);");
        }

        HBox topRow = new HBox(4);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label tag = new Label(isNewlyPlaced ? "✨ Recién Colocado" : "Hito");
        tag.setStyle(isNewlyPlaced
            ? "-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: #0f172a; -fx-background-color: #f59e0b; -fx-padding: 2px 6px; -fx-background-radius: 4px;"
            : "-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-background-color: rgba(255, 255, 255, 0.08); -fx-padding: 2px 6px; -fx-background-radius: 4px;");
        tag.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        tag.setEllipsisString("");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label yr = new Label(m.formatYear());
        yr.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.15); -fx-padding: 2px 6px; -fx-border-color: rgba(245, 158, 11, 0.4); -fx-border-radius: 4px; -fx-background-radius: 4px;");
        yr.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        yr.setEllipsisString("");

        topRow.getChildren().addAll(tag, sp, yr);

        Label title = new Label(m.title);
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        title.setWrapText(true);
        title.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        title.setEllipsisString("");

        Label desc = new Label(m.description);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        desc.setWrapText(true);
        desc.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        desc.setEllipsisString("");
        VBox.setVgrow(desc, Priority.ALWAYS);

        card.getChildren().addAll(topRow, title, desc);
        return card;
    }

    private Button createSlotButton(int slotIndex, String labelText, double height) {
        Button btn = new Button("+ \n" + labelText);
        double scale = ThemeManager.getFontScale();
        double slotWidth = Math.max(92.0, 92.0 * scale);
        double btnHeight = Math.max(140.0 * scale, height - 12);
        btn.setPrefSize(slotWidth, btnHeight);
        btn.setMinSize(slotWidth, btnHeight);
        btn.setMaxSize(slotWidth, btnHeight);
        btn.setStyle("-fx-background-color: rgba(245, 158, 11, 0.06); -fx-border-color: rgba(245, 158, 11, 0.4); -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-text-fill: #f59e0b; -fx-font-weight: 900; -fx-font-size: 11px; -fx-cursor: hand; -fx-text-alignment: center;");
        btn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        btn.setEllipsisString("");

        btn.setOnAction(e -> handleSlotChoice(slotIndex));
        return btn;
    }

    private void handleSlotChoice(int slotIndex) {
        if (isTurnAnswered || currentMystery == null || isMatchCompleted) return;
        isTurnAnswered = true;

        boolean correct = false;
        if (slotIndex == 0) {
            correct = currentMystery.year <= timeline.get(0).year;
        } else if (slotIndex == timeline.size()) {
            correct = currentMystery.year >= timeline.get(timeline.size() - 1).year;
        } else {
            int prevYr = timeline.get(slotIndex - 1).year;
            int nextYr = timeline.get(slotIndex).year;
            correct = currentMystery.year >= prevYr && currentMystery.year <= nextYr;
        }

        mysteryYear.setText(currentMystery.formatYear());
        mysteryYear.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0f172a; -fx-background-color: #f59e0b; -fx-padding: 4px 14px; -fx-background-radius: 8px;");

        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);

        if (correct) {
            feedbackLabel.setText(String.format("¡CORRECTO! %s ocurrió en %s. Asigna los puntos desde la Arena.", currentMystery.title, currentMystery.formatYear()));
            feedbackLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #34d399; -fx-background-color: rgba(16, 185, 129, 0.15); -fx-border-color: rgba(16, 185, 129, 0.4); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6px 14px;");
        } else {
            feedbackLabel.setText(String.format("¡FALLO! %s ocurrió en %s.", currentMystery.title, currentMystery.formatYear()));
            feedbackLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #f87171; -fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: rgba(239, 68, 68, 0.4); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6px 14px;");
        }

        // Insert mystery milestone into timeline and mark as newly placed
        lastPlacedId = currentMystery.id;
        timeline.add(currentMystery);
        timeline.sort(Comparator.comparingInt(a -> a.year));
        renderTimeline();

        nextTurnBtn.setVisible(true);
        nextTurnBtn.setManaged(true);
    }

    private void advanceTurn() {
        if (deck.isEmpty()) {
            finishMatch();
            return;
        }

        currentPlayerIndex = (currentPlayerIndex + 1) % profiles.size();
        currentMystery = deck.remove(0);
        renderTurn();
    }

    private void finishMatch() {
        isMatchCompleted = true;
        mysteryCardBox.setVisible(false);
        mysteryCardBox.setManaged(false);
        nextTurnBtn.setVisible(false);
        nextTurnBtn.setManaged(false);

        winnerBox.getChildren().clear();
        winnerBox.setVisible(true);
        winnerBox.setManaged(true);

        Label trophy = new Label("📜");
        trophy.setStyle("-fx-font-size: 44px;");

        Label congrats = new Label("¡LÍNEA DE TIEMPO COMPLETADA!");
        congrats.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: white;");

        Label champ = new Label(isBattleRoyale
                ? "⚔️ ¡BATTLE ROYALE COMPLETADO! Todos los elementos fueron presentados en la arena."
                : "Todos los hitos han sido colocados en orden cronológico.");
        champ.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.15); -fx-padding: 6px 16px; -fx-border-color: rgba(245, 158, 11, 0.4); -fx-border-radius: 10px; -fx-background-radius: 10px;");

        Button restartBtn = new Button("Jugar Otra Vez ↺");
        restartBtn.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #d97706); -fx-text-fill: #0f172a; -fx-font-weight: 900; -fx-font-size: 13px; -fx-padding: 8px 20px; -fx-background-radius: 10px; -fx-cursor: hand;");
        restartBtn.setOnAction(e -> {
            mysteryCardBox.setVisible(true);
            mysteryCardBox.setManaged(true);
            winnerBox.setVisible(false);
            winnerBox.setManaged(false);
            startMatch();
        });

        winnerBox.getChildren().addAll(trophy, congrats, champ, restartBtn);

        if (winnerListener != null && !profiles.isEmpty()) {
            winnerListener.accept(profiles.get(0));
        }
    }
}
