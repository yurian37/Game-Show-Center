package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class TicTacToeFxStage extends VBox {

    private static final int[][] WINNING_COMBOS = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // Rows
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // Columns
        {0, 4, 8}, {2, 4, 6}             // Diagonals
    };

    private final List<Competitor> profiles;
    private final JsonNode setupData;
    private final Consumer<Competitor> onWinnerSelected;

    private final String[] board = new String[9];
    private final Button[] gridButtons = new Button[9];

    private String currentTurn = "blue";
    private String startingTeam = "blue";
    private String winner = null; // null, "blue", "red", "draw"
    private int[] winningLine = null;
    private int roundNumber = 1;

    private Image blueTeamImg;
    private Image redTeamImg;

    private Label roundBadgeLabel;
    private Label statusLabel;
    private GridPane gridPane;

    private VBox resultBanner;
    private Label resultIcon;
    private Label resultTitle;
    private Label resultMsg;

    private Button resetBtn;

    public TicTacToeFxStage(List<Competitor> profiles, JsonNode setupData, Consumer<Competitor> onWinnerSelected) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        this.setupData = setupData;
        this.onWinnerSelected = onWinnerSelected;

        setSpacing(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(16));

        loadTeamImages();

        // 1. ROUND & TURN BADGES
        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);

        roundBadgeLabel = new Label();
        roundBadgeLabel.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 3px 12px; -fx-background-radius: 10px;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary), ThemeManager.getButtonHex()));

        statusLabel = new Label();
        statusLabel.setStyle(String.format("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 6px 18px; -fx-background-radius: 14px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 14px;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary), ThemeManager.getCardHex()));

        headerBox.getChildren().addAll(roundBadgeLabel, statusLabel);

        // 2. 3x3 GRID OF SQUARE BUTTONS (HOLDERS)
        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setStyle(String.format("-fx-background-color: %s; -fx-padding: 16px; -fx-background-radius: 20px; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 20px;", ThemeManager.getCardHex()));

        for (int i = 0; i < 9; i++) {
            final int index = i;
            Button btn = new Button();
            btn.setPrefSize(100, 100);
            btn.setStyle(String.format("-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-width: 2px; -fx-background-radius: 14px; -fx-border-radius: 14px; -fx-cursor: hand;", ThemeManager.getButtonHex()));
            btn.setOnAction(e -> handleCellClick(index));

            gridButtons[i] = btn;
            int row = i / 3;
            int col = i % 3;
            gridPane.add(btn, col, row);
        }

        // 3. RESULT BANNER (WINNER OR DRAW)
        resultBanner = new VBox(8);
        resultBanner.setAlignment(Pos.CENTER);
        resultBanner.setPadding(new Insets(14, 20, 14, 20));
        resultBanner.setMaxWidth(480);
        resultBanner.setVisible(false);
        resultBanner.setManaged(false);

        resultIcon = new Label("👑");
        resultIcon.setStyle("-fx-font-size: 32px;");

        resultTitle = new Label(I18n.get("game.tictactoe.victory"));
        resultTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #ffffff;");

        resultMsg = new Label("");
        resultMsg.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s; -fx-wrap-text: true; -fx-alignment: center;", ThemeManager.toHex(ThemeManager.getCurrentPalette().textPrimary)));

        resultBanner.getChildren().addAll(resultIcon, resultTitle, resultMsg);

        // 4. ALWAYS AVAILABLE RESET / NEW ROUND BUTTON
        resetBtn = new Button(I18n.get("game.tictactoe.start_new_round"));
        resetBtn.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 10px 24px; -fx-background-color: %s; -fx-text-fill: #ffffff; -fx-background-radius: 12px; -fx-cursor: hand;", ThemeManager.getAccentHex()));
        resetBtn.setOnAction(e -> {
            roundNumber++;
            startNewRound();
        });

        getChildren().addAll(headerBox, gridPane, resultBanner, resetBtn);

        // INITIALIZE FIRST ROUND
        startNewRound();
    }

    private void loadTeamImages() {
        try {
            File blueFile = new File("assets/games/tictactoe/blueteam.png");
            if (!blueFile.exists()) blueFile = new File("template-offline/assets/games/tictactoe/blueteam.png");
            if (blueFile.exists()) {
                blueTeamImg = new Image(blueFile.toURI().toString());
            }

            File redFile = new File("assets/games/tictactoe/redteam.png");
            if (!redFile.exists()) redFile = new File("template-offline/assets/games/tictactoe/redteam.png");
            if (redFile.exists()) {
                redTeamImg = new Image(redFile.toURI().toString());
            }
        } catch (Exception ignored) {}
    }

    private String determineStartingTeam() {
        String pref = "random";
        if (setupData != null) {
            if (setupData.has("starting_team")) pref = setupData.get("starting_team").asText();
            else if (setupData.has("startingTeam")) pref = setupData.get("startingTeam").asText();
        }

        if ("blue".equalsIgnoreCase(pref)) return "blue";
        if ("red".equalsIgnoreCase(pref)) return "red";
        return new Random().nextBoolean() ? "blue" : "red";
    }

    private void startNewRound() {
        startingTeam = determineStartingTeam();
        currentTurn = startingTeam;
        winner = null;
        winningLine = null;
        Arrays.fill(board, null);

        for (int i = 0; i < 9; i++) {
            gridButtons[i].setText("");
            gridButtons[i].setGraphic(null);
            gridButtons[i].setDisable(false);
            gridButtons[i].setStyle(String.format("-fx-background-color: %s; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-width: 2px; -fx-background-radius: 14px; -fx-border-radius: 14px; -fx-cursor: hand;", ThemeManager.getButtonHex()));
        }

        resultBanner.setVisible(false);
        resultBanner.setManaged(false);

        updateStatusDisplay();
    }

    private void updateStatusDisplay() {
        roundBadgeLabel.setText(String.format("Round %d • Starting Team: %s", roundNumber, startingTeam.toUpperCase()));

        Competitor blueP = (!profiles.isEmpty()) ? profiles.get(0) : new Competitor("p1", "Blue Team (P1)", null);
        Competitor redP = (profiles.size() > 1) ? profiles.get(1) : new Competitor("p2", "Red Team (P2)", null);

        if (winner == null) {
            boolean isBlue = "blue".equalsIgnoreCase(currentTurn);
            String name = isBlue ? blueP.getName() : redP.getName();
            String symbol = isBlue ? "Blue X" : "Red O";
            String colorStyle = isBlue ? "-fx-text-fill: #38bdf8;" : "-fx-text-fill: #f43f5e;";
            statusLabel.setText(String.format("Current Turn: %s (%s)", name, symbol));
            statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; " + colorStyle + " -fx-background-color: #121624; -fx-padding: 6px 18px; -fx-background-radius: 14px; -fx-border-color: #29334d; -fx-border-radius: 14px;");
        } else {
            statusLabel.setText(I18n.get("game.tictactoe.match_status") + " " + ("draw".equalsIgnoreCase(winner) ? "🤝 " + I18n.get("game.tictactoe.tie_draw") : "👑 " + I18n.get("game.tictactoe.victory")));
            statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #fcd34d; -fx-background-color: #121624; -fx-padding: 6px 18px; -fx-background-radius: 14px; -fx-border-color: #29334d; -fx-border-radius: 14px;");
        }
    }

    private void handleCellClick(int index) {
        if (board[index] != null || winner != null) return;

        board[index] = currentTurn;
        Button btn = gridButtons[index];
        btn.setDisable(true);

        Image imgToUse = "blue".equalsIgnoreCase(currentTurn) ? blueTeamImg : redTeamImg;
        if (imgToUse != null) {
            ImageView iv = new ImageView(imgToUse);
            iv.setFitWidth(64);
            iv.setFitHeight(64);
            iv.setPreserveRatio(true);
            btn.setGraphic(iv);
        } else {
            btn.setText("blue".equalsIgnoreCase(currentTurn) ? "X" : "O");
            btn.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: " + ("blue".equalsIgnoreCase(currentTurn) ? "#38bdf8" : "#f43f5e"));
        }

        // Check Win Condition
        String foundWinner = null;
        int[] foundLine = null;

        for (int[] combo : WINNING_COMBOS) {
            int a = combo[0], b = combo[1], c = combo[2];
            if (board[a] != null && board[a].equals(board[b]) && board[a].equals(board[c])) {
                foundWinner = board[a];
                foundLine = combo;
                break;
            }
        }

        if (foundWinner != null) {
            winner = foundWinner;
            winningLine = foundLine;

            // Highlight Winning Combo
            for (int winIdx : winningLine) {
                gridButtons[winIdx].setStyle("-fx-background-color: rgba(16, 185, 129, 0.25); -fx-border-color: #10b981; -fx-border-width: 3px; -fx-background-radius: 14px; -fx-border-radius: 14px;");
            }

            // Disable all remaining buttons
            for (int i = 0; i < 9; i++) {
                gridButtons[i].setDisable(true);
            }

            showWinBanner(foundWinner);
            updateStatusDisplay();

            // Notify winner
            Competitor blueP = (!profiles.isEmpty()) ? profiles.get(0) : new Competitor("p1", "Blue Team (P1)", null);
            Competitor redP = (profiles.size() > 1) ? profiles.get(1) : new Competitor("p2", "Red Team (P2)", null);
            Competitor winnerComp = "blue".equalsIgnoreCase(foundWinner) ? blueP : redP;
            if (onWinnerSelected != null) {
                onWinnerSelected.accept(winnerComp);
            }
            return;
        }

        // Check Draw Condition
        boolean isFull = true;
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                isFull = false;
                break;
            }
        }

        if (isFull) {
            winner = "draw";
            showDrawBanner();
            updateStatusDisplay();
            return;
        }

        // Switch Turn
        currentTurn = "blue".equalsIgnoreCase(currentTurn) ? "red" : "blue";
        updateStatusDisplay();
    }

    private void showWinBanner(String winningTeam) {
        boolean isBlue = "blue".equalsIgnoreCase(winningTeam);
        Competitor blueP = (!profiles.isEmpty()) ? profiles.get(0) : new Competitor("p1", "Blue Team (P1)", null);
        Competitor redP = (profiles.size() > 1) ? profiles.get(1) : new Competitor("p2", "Red Team (P2)", null);
        String name = isBlue ? blueP.getName() : redP.getName();

        resultIcon.setText("👑");
        resultTitle.setText(String.format("VICTORY FOR %s TEAM!", winningTeam.toUpperCase()));
        resultTitle.setStyle(isBlue ? "-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #38bdf8;" : "-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #f43f5e;");
        resultMsg.setText(String.format("%s connected 3 symbols in a row!", name));

        resultBanner.setStyle(isBlue ?
            "-fx-background-color: rgba(56, 189, 248, 0.15); -fx-border-color: #38bdf8; -fx-border-width: 2px; -fx-background-radius: 20px; -fx-border-radius: 20px;" :
            "-fx-background-color: rgba(244, 63, 94, 0.15); -fx-border-color: #f43f5e; -fx-border-width: 2px; -fx-background-radius: 20px; -fx-border-radius: 20px;");

        resultBanner.setVisible(true);
        resultBanner.setManaged(true);
    }

    private void showDrawBanner() {
        resultIcon.setText("🤝");
        resultTitle.setText(I18n.get("game.tictactoe.tie_draw"));
        resultTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #e2e8f0;");
        resultMsg.setText(I18n.get("game.tictactoe.tie_desc"));

        resultBanner.setStyle("-fx-background-color: rgba(100, 116, 139, 0.2); -fx-border-color: #64748b; -fx-border-width: 2px; -fx-background-radius: 20px; -fx-border-radius: 20px;");

        resultBanner.setVisible(true);
        resultBanner.setManaged(true);
    }
}
