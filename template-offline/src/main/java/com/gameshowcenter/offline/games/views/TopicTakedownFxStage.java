package com.gameshowcenter.offline.games.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.*;
import com.gameshowcenter.offline.util.SvgEmoji;

public class TopicTakedownFxStage extends VBox {

    public static class QuestionItem {
        public final int categoryIndex;
        public final int questionIndex;
        public final String categoryName;
        public final String question;
        public final String answer;
        public final String imageUrl;
        public final String audioUrl;

        public QuestionItem(int categoryIndex, int questionIndex, String categoryName, String question, String answer, String imageUrl, String audioUrl) {
            this.categoryIndex = categoryIndex;
            this.questionIndex = questionIndex;
            this.categoryName = categoryName;
            this.question = question;
            this.answer = answer;
            this.imageUrl = imageUrl;
            this.audioUrl = audioUrl;
        }
    }

    public static class CategoryModel {
        public final String name;
        public final List<QuestionItem> questions = new ArrayList<>();

        public CategoryModel(String name) {
            this.name = name;
        }
    }

    private final List<Competitor> profiles;
    private final JsonNode setupData;

    private final List<CategoryModel> categories = new ArrayList<>();
    private final Set<String> revealedQuestions = new HashSet<>(); // "c_q"

    private int numCategories = 3;
    private int questionsPerCategory = 4;
    private int totalQuestions = 12;

    // View Switching
    private StackPane rootContainer;
    private VBox boardView;
    private VBox questionDetailView;

    // Board UI
    private Label progressLabel;
    private HBox boardGrid;
    private final Map<String, Button> questionButtons = new HashMap<>();
    private VBox completedBanner;

    // Active Question State & UI
    private QuestionItem activeQuestion = null;
    private boolean isAnswerRevealed = false;
    private MediaPlayer activeMediaPlayer = null;

    private Label activeBadgeLabel;
    private Label questionPromptLabel;
    private VBox answerBox;
    private Label answerStatusLabel;
    private Label answerValueLabel;
    private Button revealAnswerBtn;
    private Button backToBoardBtn;

    private VBox imageContainer;
    private ImageView imageView;
    private VBox audioContainer;
    private Label audioStatusLabel;
    private Button audioPlayPauseBtn;

    public TopicTakedownFxStage(List<Competitor> profiles, JsonNode setupData) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        this.setupData = setupData;

        setSpacing(14);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(12, 16, 16, 16));

        parseSetup();
        buildUI();
    }

    private void parseSetup() {
        categories.clear();

        if (setupData != null) {
            if (setupData.has("numCategories")) numCategories = setupData.get("numCategories").asInt(3);
            else if (setupData.has("num_categories")) numCategories = setupData.get("num_categories").asInt(3);

            if (setupData.has("questionsPerCategory")) questionsPerCategory = setupData.get("questionsPerCategory").asInt(4);
            else if (setupData.has("questions_per_category")) questionsPerCategory = setupData.get("questions_per_category").asInt(4);
        }

        numCategories = Math.max(1, Math.min(6, numCategories));
        questionsPerCategory = Math.max(1, Math.min(8, questionsPerCategory));

        JsonNode catsNode = (setupData != null && setupData.has("categories")) ? setupData.get("categories") : null;

        if (catsNode != null && catsNode.isArray() && catsNode.size() > 0) {
            int catsCount = Math.min(numCategories, catsNode.size());
            for (int c = 0; c < catsCount; c++) {
                JsonNode cNode = catsNode.get(c);
                String catName = cNode.has("categoryName") ? cNode.get("categoryName").asText() :
                        (cNode.has("category_name") ? cNode.get("category_name").asText() : "Category " + (c + 1));
                CategoryModel cat = new CategoryModel(catName);

                JsonNode qArr = cNode.has("questions") ? cNode.get("questions") : null;
                for (int q = 0; q < questionsPerCategory; q++) {
                    String qText = "Question " + (q + 1);
                    String aText = "Answer " + (q + 1);
                    String img = "";
                    String aud = "";

                    if (qArr != null && qArr.isArray() && q < qArr.size()) {
                        JsonNode qNode = qArr.get(q);
                        qText = (qNode.has("question") && !qNode.get("question").isNull()) ? qNode.get("question").asText() : qText;
                        aText = (qNode.has("answer") && !qNode.get("answer").isNull()) ? qNode.get("answer").asText() : aText;
                        img = (qNode.has("imageUrl") && !qNode.get("imageUrl").isNull()) ? qNode.get("imageUrl").asText() :
                                ((qNode.has("image_url") && !qNode.get("image_url").isNull()) ? qNode.get("image_url").asText() : "");
                        aud = (qNode.has("audioUrl") && !qNode.get("audioUrl").isNull()) ? qNode.get("audioUrl").asText() :
                                ((qNode.has("audio_url") && !qNode.get("audio_url").isNull()) ? qNode.get("audio_url").asText() : "");
                    }
                    cat.questions.add(new QuestionItem(c, q, catName, qText, aText, img, aud));
                }
                categories.add(cat);
            }
        }

        // Fallback if not configured
        if (categories.isEmpty()) {
            for (int c = 0; c < numCategories; c++) {
                CategoryModel cat = new CategoryModel("Topic #" + (c + 1));
                for (int q = 0; q < questionsPerCategory; q++) {
                    cat.questions.add(new QuestionItem(c, q, cat.name, "Sample Question " + (q + 1) + " for " + cat.name, "Sample Answer " + (q + 1), "", ""));
                }
                categories.add(cat);
            }
        }

        totalQuestions = categories.size() * questionsPerCategory;
    }

    private void buildUI() {
        rootContainer = new StackPane();
        VBox.setVgrow(rootContainer, Priority.ALWAYS);

        buildBoardView();
        buildQuestionDetailView();

        rootContainer.getChildren().addAll(boardView, questionDetailView);
        questionDetailView.setVisible(false);
        questionDetailView.setManaged(false);

        getChildren().add(rootContainer);
    }

    private void buildBoardView() {
        boardView = new VBox(14);
        boardView.setAlignment(Pos.TOP_CENTER);
        boardView.setMaxWidth(1100);

        // Header Bar
        HBox headerBar = new HBox(16);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(10, 16, 10, 16));
        headerBar.getStyleClass().add("sponsor-card");

        Label iconLabel = new Label();
        SvgEmoji.setGraphic(iconLabel, "target", 24);

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label("Topic Takedown");
        titleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: white;");
        Label subtitleLabel = new Label(numCategories + " Categories × " + questionsPerCategory + " Questions");
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        progressLabel = new Label(String.format(I18n.get("game.topic.questions_revealed"), 0, totalQuestions));
        progressLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #F59E0B; -fx-background-color: rgba(0,0,0,0.3); -fx-padding: 6 12; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 8;");

        Button restartBtn = new Button(I18n.get("game.common.restart_round"));
        restartBtn.getStyleClass().add("btn-secondary");
        restartBtn.setOnAction(e -> restartRound());

        headerBar.getChildren().addAll(iconLabel, titleBox, spacer, progressLabel, restartBtn);
        boardView.getChildren().add(headerBar);

        // Completion Banner (Hidden by default)
        completedBanner = new VBox(8);
        completedBanner.setAlignment(Pos.CENTER);
        completedBanner.setPadding(new Insets(14));
        completedBanner.setStyle("-fx-background-color: rgba(180, 83, 9, 0.2); -fx-border-color: #F59E0B; -fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16;");
        completedBanner.setVisible(false);
        completedBanner.setManaged(false);

        Label trophyIcon = new Label();
        SvgEmoji.setGraphic(trophyIcon, "trophy", 28);
        Label compTitle = new Label(I18n.get("game.topic.board_completed"));
        compTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 18px; -fx-text-fill: #FDE68A;");
        Label compSubtitle = new Label(I18n.get("game.topic.all_cleared"));
        compSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #E2E8F0;");
        Button newRoundBtn = new Button(I18n.get("game.topic.play_new_round"));
        newRoundBtn.getStyleClass().add("btn-accent-emerald");
        newRoundBtn.setOnAction(e -> restartRound());

        completedBanner.getChildren().addAll(trophyIcon, compTitle, compSubtitle, newRoundBtn);
        boardView.getChildren().add(completedBanner);

        // Columns Grid
        boardGrid = new HBox(12);
        boardGrid.setAlignment(Pos.TOP_CENTER);
        questionButtons.clear();

        double fontScale = Math.max(0.70, ThemeManager.getFontScale());
        double baseFontSize = 13.0 * fontScale;
        javafx.scene.text.Font catFont = javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, baseFontSize);

        // Determine max category header height based on text and font scale
        double maxCatHeaderH = 72.0 * fontScale;
        for (CategoryModel cat : categories) {
            javafx.scene.text.Text t = new javafx.scene.text.Text(cat.name != null ? cat.name : "");
            t.setFont(catFont);
            t.setWrappingWidth(140.0 * fontScale);
            double h = t.getLayoutBounds().getHeight() + (36.0 * fontScale);
            if (h > maxCatHeaderH) maxCatHeaderH = h;
        }

        // Determine uniform question button height across all rows and columns
        double maxQBtnH = Math.max(68.0, 62.0 * fontScale);
        int catCount = Math.max(1, categories.size());

        for (int c = 0; c < categories.size(); c++) {
            CategoryModel cat = categories.get(c);
            VBox colBox = new VBox(10);
            colBox.setAlignment(Pos.TOP_CENTER);
            colBox.setPadding(new Insets(10));
            colBox.getStyleClass().add("sponsor-card");
            HBox.setHgrow(colBox, Priority.ALWAYS);

            // Equal column width across all columns
            colBox.minWidthProperty().bind(boardGrid.widthProperty().subtract(12 * (catCount - 1) + 24).divide(catCount));
            colBox.prefWidthProperty().bind(boardGrid.widthProperty().subtract(12 * (catCount - 1) + 24).divide(catCount));
            colBox.maxWidthProperty().bind(boardGrid.widthProperty().subtract(12 * (catCount - 1) + 24).divide(catCount));

            // Category Header Card (Row 0: Uniform exact max height across all columns)
            VBox headerCard = new VBox();
            headerCard.setAlignment(Pos.CENTER);
            headerCard.setPadding(new Insets(12, 8, 12, 8));
            headerCard.setMinHeight(maxCatHeaderH);
            headerCard.setPrefHeight(maxCatHeaderH);
            headerCard.setMaxHeight(maxCatHeaderH);
            headerCard.setMaxWidth(Double.MAX_VALUE);
            headerCard.setStyle("-fx-background-color: linear-gradient(to bottom right, #312E81, #4C1D95); -fx-background-radius: 12; -fx-border-color: rgba(165,180,252,0.3); -fx-border-radius: 12;");

            Label catNameLabel = new Label(cat.name);
            catNameLabel.setWrapText(true);
            catNameLabel.setAlignment(Pos.CENTER);
            catNameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            catNameLabel.setEllipsisString("");
            catNameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: #EEF2FF; -fx-text-alignment: center;");
            headerCard.getChildren().add(catNameLabel);
            colBox.getChildren().add(headerCard);

            // Questions Buttons (Rows 1..N: Uniform exact max height and width across all boxes)
            for (int q = 0; q < questionsPerCategory; q++) {
                final int catIdx = c;
                final int qIdx = q;
                final QuestionItem item = cat.questions.get(q);

                Button qBtn = new Button();
                qBtn.setMaxWidth(Double.MAX_VALUE);
                qBtn.setMinHeight(maxQBtnH);
                qBtn.setPrefHeight(maxQBtnH);
                qBtn.setMaxHeight(maxQBtnH);
                qBtn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                qBtn.setEllipsisString("");
                qBtn.setFocusTraversable(false);
                VBox.setVgrow(qBtn, Priority.ALWAYS);

                // Visible question number
                VBox btnContent = new VBox(2);
                btnContent.setAlignment(Pos.CENTER);
                Label subTxt = new Label(I18n.get("game.common.question"));
                subTxt.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: #94A3B8;");
                subTxt.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                subTxt.setEllipsisString("");
                Label numTxt = new Label(String.valueOf(q + 1));
                numTxt.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #F59E0B;");
                numTxt.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                numTxt.setEllipsisString("");
                btnContent.getChildren().addAll(subTxt, numTxt);

                qBtn.setGraphic(btnContent);
                qBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #1E293B, #0F172A); -fx-border-color: rgba(99,102,241,0.4); -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10; -fx-cursor: hand;");

                qBtn.setOnAction(e -> openQuestion(item));

                String key = catIdx + "_" + qIdx;
                questionButtons.put(key, qBtn);
                colBox.getChildren().add(qBtn);
            }

            boardGrid.getChildren().add(colBox);
        }

        ScrollPane scroll = new ScrollPane(boardGrid);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        boardView.getChildren().add(scroll);
    }

    private void buildQuestionDetailView() {
        questionDetailView = new VBox(14);
        questionDetailView.setAlignment(Pos.CENTER);
        questionDetailView.setMaxWidth(680);
        questionDetailView.setPadding(new Insets(20));
        questionDetailView.getStyleClass().add("sponsor-card");
        questionDetailView.setStyle(questionDetailView.getStyle() + "; -fx-border-color: rgba(245, 158, 11, 0.4); -fx-border-width: 2; -fx-border-radius: 20; -fx-background-radius: 20;");

        // Top Category Badge
        activeBadgeLabel = new Label(I18n.get("game.topic.card_category"));
        activeBadgeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #F59E0B; -fx-background-color: rgba(245,158,11,0.15); -fx-padding: 4 14; -fx-background-radius: 12; -fx-border-color: rgba(245,158,11,0.3); -fx-border-radius: 12;");

        // Image View Container (Item B of prompt: Imagen si hubiese)
        imageContainer = new VBox(6);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPadding(new Insets(6));
        imageContainer.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 12;");
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(420);
        imageView.setFitHeight(200);
        imageContainer.getChildren().add(imageView);

        // Audio Player Container (Item B of prompt: Audio si hubiese)
        audioContainer = new VBox(8);
        audioContainer.setAlignment(Pos.CENTER);
        audioContainer.setPadding(new Insets(10));
        audioContainer.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 12;");

        HBox audioControls = new HBox(12);
        audioControls.setAlignment(Pos.CENTER);
        Label audIcon = new Label();
        SvgEmoji.setGraphic(audIcon, "musical-notes", 18);
        audioStatusLabel = new Label(I18n.get("game.topic.audio_attached"));
        audioStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #C084FC;");
        audioPlayPauseBtn = new Button(I18n.get("game.topic.play_audio"));
        audioPlayPauseBtn.getStyleClass().add("btn-small");
        audioPlayPauseBtn.setOnAction(e -> toggleAudio());

        audioControls.getChildren().addAll(audIcon, audioStatusLabel, audioPlayPauseBtn);
        audioContainer.getChildren().add(audioControls);

        // Question Prompt (Item B of prompt: Pregunta)
        VBox promptBox = new VBox(4);
        promptBox.setAlignment(Pos.CENTER);
        Label promptHeader = new Label(I18n.get("game.topic.prompt_label"));
        promptHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #818CF8;");
        questionPromptLabel = new Label("Question text");
        questionPromptLabel.setWrapText(true);
        questionPromptLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        questionPromptLabel.setEllipsisString("");
        questionPromptLabel.setAlignment(Pos.CENTER);
        questionPromptLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: white; -fx-text-alignment: center;");
        promptBox.getChildren().addAll(promptHeader, questionPromptLabel);

        // Hidden / Revealed Answer Container (Item B of prompt: Respuesta Oculta)
        answerBox = new VBox(6);
        answerBox.setAlignment(Pos.CENTER);
        answerBox.setPadding(new Insets(12, 16, 12, 16));
        answerBox.setMaxWidth(460);
        answerBox.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 12;");

        Label ansHeader = new Label(I18n.get("game.common.correct_answer"));
        ansHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #F59E0B;");

        answerStatusLabel = new Label(I18n.get("game.common.answer_hidden"));
        answerStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        answerValueLabel = new Label("");
        answerValueLabel.setWrapText(true);
        answerValueLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        answerValueLabel.setEllipsisString("");
        answerValueLabel.setAlignment(Pos.CENTER);
        answerValueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #34D399; -fx-text-alignment: center;");
        answerValueLabel.setVisible(false);
        answerValueLabel.setManaged(false);

        answerBox.getChildren().addAll(ansHeader, answerStatusLabel, answerValueLabel);

        // Action Buttons: Revelar Respuesta & Volver (Item B of prompt)
        HBox actionsBox = new HBox(14);
        actionsBox.setAlignment(Pos.CENTER);
        actionsBox.setPadding(new Insets(8, 0, 0, 0));

        revealAnswerBtn = new Button(I18n.get("game.common.reveal_answer"));
        SvgEmoji.setGraphic(revealAnswerBtn, "eye", 16);
        revealAnswerBtn.getStyleClass().add("btn-accent-amber");
        revealAnswerBtn.setMinWidth(180);
        revealAnswerBtn.setMinHeight(44);
        revealAnswerBtn.setPrefHeight(Region.USE_COMPUTED_SIZE);
        revealAnswerBtn.setOnAction(e -> revealAnswer());

        backToBoardBtn = new Button(I18n.get("game.topic.btn_back"));
        SvgEmoji.setGraphic(backToBoardBtn, "back", 16);
        backToBoardBtn.getStyleClass().add("btn-secondary");
        backToBoardBtn.setMinWidth(140);
        backToBoardBtn.setMinHeight(44);
        backToBoardBtn.setPrefHeight(Region.USE_COMPUTED_SIZE);
        backToBoardBtn.setOnAction(e -> backToBoard());

        actionsBox.getChildren().addAll(revealAnswerBtn, backToBoardBtn);

        questionDetailView.getChildren().addAll(
                activeBadgeLabel,
                imageContainer,
                audioContainer,
                promptBox,
                answerBox,
                actionsBox
        );
    }

    private void openQuestion(QuestionItem item) {
        String key = item.categoryIndex + "_" + item.questionIndex;
        if (revealedQuestions.contains(key)) {
            return; // Item C: Si ya fue revelada, no se puede abrir de nuevo
        }

        stopAudio();

        activeQuestion = item;
        isAnswerRevealed = false;

        // Badge
        activeBadgeLabel.setText(item.categoryName.toUpperCase() + " • PREGUNTA #" + (item.questionIndex + 1));

        // Image handling
        if (item.imageUrl != null && !item.imageUrl.trim().isEmpty()) {
            try {
                String path = item.imageUrl.trim();
                Image img;
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    img = new Image(path, true);
                } else {
                    File file = new File(path);
                    img = new Image(file.toURI().toString(), true);
                }
                imageView.setImage(img);
                imageContainer.setVisible(true);
                imageContainer.setManaged(true);
            } catch (Exception ex) {
                imageContainer.setVisible(false);
                imageContainer.setManaged(false);
            }
        } else {
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
        }

        // Audio handling
        if (item.audioUrl != null && !item.audioUrl.trim().isEmpty()) {
            try {
                String path = item.audioUrl.trim();
                String uri;
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    uri = path;
                } else {
                    File file = new File(path);
                    uri = file.toURI().toString();
                }
                Media media = new Media(uri);
                activeMediaPlayer = new MediaPlayer(media);
                activeMediaPlayer.setOnEndOfMedia(() -> {
                    audioPlayPauseBtn.setText(I18n.get("game.topic.play_audio"));
                    audioStatusLabel.setText(I18n.get("game.topic.audio_finished"));
                });

                audioStatusLabel.setText(I18n.get("game.topic.audio_ready"));
                audioPlayPauseBtn.setText(I18n.get("game.topic.play_audio"));
                audioContainer.setVisible(true);
                audioContainer.setManaged(true);
            } catch (Exception ex) {
                audioContainer.setVisible(false);
                audioContainer.setManaged(false);
            }
        } else {
            audioContainer.setVisible(false);
            audioContainer.setManaged(false);
        }

        // Question Prompt
        questionPromptLabel.setText("\"" + item.question + "\"");

        // Hidden Answer
        answerStatusLabel.setVisible(true);
        answerStatusLabel.setManaged(true);
        answerValueLabel.setVisible(false);
        answerValueLabel.setManaged(false);
        answerValueLabel.setText("");

        revealAnswerBtn.setVisible(true);
        revealAnswerBtn.setManaged(true);

        // Switch to Question View
        boardView.setVisible(false);
        boardView.setManaged(false);

        questionDetailView.setVisible(true);
        questionDetailView.setManaged(true);
    }

    private void revealAnswer() {
        if (activeQuestion == null) return;

        isAnswerRevealed = true;
        answerValueLabel.setText(activeQuestion.answer);
        SvgEmoji.setGraphic(answerValueLabel, "lightbulb", 14);

        answerStatusLabel.setVisible(false);
        answerStatusLabel.setManaged(false);

        answerValueLabel.setVisible(true);
        answerValueLabel.setManaged(true);

        revealAnswerBtn.setVisible(false);
        revealAnswerBtn.setManaged(false);

        // Item C: Bloquear la opción para esta ronda
        String key = activeQuestion.categoryIndex + "_" + activeQuestion.questionIndex;
        revealedQuestions.add(key);

        updateBoardButtonState(key, activeQuestion.questionIndex + 1);
        updateProgress();
    }

    private void updateBoardButtonState(String key, int questionNumber) {
        Button btn = questionButtons.get(key);
        if (btn != null) {
            btn.setDisable(true);
            VBox content = new VBox(2);
            content.setAlignment(Pos.CENTER);
            Label check = new Label(I18n.get("game.topic.card_revealed"));
            check.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: #475569;");
            Label num = new Label(String.valueOf(questionNumber));
            num.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #64748B; -fx-strikethrough: true;");
            content.getChildren().addAll(check, num);

            btn.setGraphic(content);
            btn.setStyle("-fx-background-color: rgba(15,23,42,0.5); -fx-border-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-border-radius: 10; -fx-opacity: 0.6;");
        }
    }

    private void updateProgress() {
        int revealed = revealedQuestions.size();
        progressLabel.setText(String.format(I18n.get("game.topic.questions_revealed"), revealed, totalQuestions));

        if (revealed >= totalQuestions && totalQuestions > 0) {
            completedBanner.setVisible(true);
            completedBanner.setManaged(true);
        }
    }

    private void backToBoard() {
        stopAudio();
        activeQuestion = null;

        questionDetailView.setVisible(false);
        questionDetailView.setManaged(false);

        boardView.setVisible(true);
        boardView.setManaged(true);
    }

    private void toggleAudio() {
        if (activeMediaPlayer == null) return;
        MediaPlayer.Status status = activeMediaPlayer.getStatus();
        if (status == MediaPlayer.Status.PLAYING) {
            activeMediaPlayer.pause();
            audioPlayPauseBtn.setText(I18n.get("game.topic.play_audio"));
            audioStatusLabel.setText(I18n.get("game.topic.audio_paused"));
        } else {
            activeMediaPlayer.play();
            audioPlayPauseBtn.setText(I18n.get("game.topic.pause_audio"));
            audioStatusLabel.setText(I18n.get("game.topic.playing_audio"));
        }
    }

    private void stopAudio() {
        if (activeMediaPlayer != null) {
            try {
                activeMediaPlayer.stop();
                activeMediaPlayer.dispose();
            } catch (Exception ignored) {}
            activeMediaPlayer = null;
        }
    }

    private void restartRound() {
        stopAudio();
        revealedQuestions.clear();
        activeQuestion = null;

        completedBanner.setVisible(false);
        completedBanner.setManaged(false);

        // Reset all board buttons
        for (int c = 0; c < categories.size(); c++) {
            CategoryModel cat = categories.get(c);
            for (int q = 0; q < questionsPerCategory; q++) {
                String key = c + "_" + q;
                Button btn = questionButtons.get(key);
                if (btn != null) {
                    btn.setDisable(false);
                    VBox btnContent = new VBox(2);
                    btnContent.setAlignment(Pos.CENTER);
                    Label subTxt = new Label(I18n.get("game.common.question"));
                    subTxt.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: #94A3B8;");
                    Label numTxt = new Label(String.valueOf(q + 1));
                    numTxt.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #F59E0B;");
                    btnContent.getChildren().addAll(subTxt, numTxt);

                    btn.setGraphic(btnContent);
                    btn.setStyle("-fx-background-color: linear-gradient(to bottom, #1E293B, #0F172A); -fx-border-color: rgba(99,102,241,0.4); -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10; -fx-cursor: hand;");
                }
            }
        }

        updateProgress();

        questionDetailView.setVisible(false);
        questionDetailView.setManaged(false);

        boardView.setVisible(true);
        boardView.setManaged(true);
    }
}
