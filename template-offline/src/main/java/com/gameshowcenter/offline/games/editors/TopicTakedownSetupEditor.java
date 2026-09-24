package com.gameshowcenter.offline.games.editors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.util.FileChooserHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TopicTakedownSetupEditor implements IGameSetupEditor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Spinner<Integer> categoriesSpinner;
    private Spinner<Integer> questionsSpinner;
    private CheckBox battleRoyaleCheckBox;

    private VBox categoriesContainer;
    private final List<CategoryData> categoryDataList = new ArrayList<>();
    private ThemeManager.Palette currentPalette;

    public static class QuestionData {
        public TextField questionField;
        public TextField answerField;
        public TextField imageField;
        public TextField audioField;

        public QuestionData(String question, String answer, String image, String audio) {
            this.questionField = new TextField(question != null ? question : "");
            this.answerField = new TextField(answer != null ? answer : "");
            this.imageField = new TextField(image != null ? image : "");
            this.audioField = new TextField(audio != null ? audio : "");
        }
    }

    public static class CategoryData {
        public TextField categoryNameField;
        public List<QuestionData> questions = new ArrayList<>();

        public CategoryData(String name) {
            this.categoryNameField = new TextField(name != null ? name : "");
        }
    }

    @Override
    public Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, ThemeManager.Palette palette) {
        this.currentPalette = palette;
        VBox root = new VBox(14);
        root.setPadding(new Insets(4));
        String textColor = palette != null ? ThemeManager.getContrastTextColor(palette.bgCard) : ThemeManager.getTextPrimaryHex();

        // Battle Royale Toggle
        boolean curBR = currentSetup != null && currentSetup.has("battleRoyale") && currentSetup.get("battleRoyale").asBoolean();
        battleRoyaleCheckBox = new CheckBox(I18n.get("game.editor.battleroyale.check"));
        battleRoyaleCheckBox.setSelected(curBR);
        battleRoyaleCheckBox.setStyle(String.format("-fx-font-weight: 900; -fx-text-fill: #f59e0b; -fx-font-size: 13px; -fx-cursor: hand;"));

        Label brHint = new Label(I18n.get("game.editor.battleroyale.hint"));
        brHint.setWrapText(true);
        brHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");

        VBox brCard = new VBox(4, battleRoyaleCheckBox, brHint);
        brCard.setStyle("-fx-background-color: rgba(245, 158, 11, 0.08); -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px 12px;");
        root.getChildren().add(brCard);

        // 1. Initial dimensions from JSON or fallback
        int curCats = 3;
        int curQPerCat = 4;

        if (currentSetup != null) {
            if (currentSetup.has("numCategories")) curCats = currentSetup.get("numCategories").asInt(3);
            else if (currentSetup.has("num_categories")) curCats = currentSetup.get("num_categories").asInt(3);

            if (currentSetup.has("questionsPerCategory")) curQPerCat = currentSetup.get("questionsPerCategory").asInt(4);
            else if (currentSetup.has("questions_per_category")) curQPerCat = currentSetup.get("questions_per_category").asInt(4);
        }

        // Clamp to offline limits: max 6 categories, max 8 questions per category
        curCats = Math.max(1, Math.min(6, curCats));
        curQPerCat = Math.max(1, Math.min(8, curQPerCat));

        // 2. Spinners Bar
        HBox configBar = new HBox(20);
        configBar.setAlignment(Pos.CENTER_LEFT);
        configBar.getStyleClass().add("sponsor-card");
        configBar.setPadding(new Insets(12, 16, 12, 16));

        // Categories Spinner (1..6)
        HBox catsBox = new HBox(8);
        catsBox.setAlignment(Pos.CENTER_LEFT);
        Label catsLabel = new Label(I18n.get("game.editor.topic.categories_max"));
        catsLabel.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));
        categoriesSpinner = new Spinner<>(1, 6, curCats, 1);
        categoriesSpinner.setEditable(false);
        categoriesSpinner.setPrefWidth(80);
        catsBox.getChildren().addAll(catsLabel, categoriesSpinner);

        // Questions per Category Spinner (1..8)
        HBox qBox = new HBox(8);
        qBox.setAlignment(Pos.CENTER_LEFT);
        Label qLabel = new Label(I18n.get("game.editor.topic.questions_per_cat"));
        qLabel.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));
        questionsSpinner = new Spinner<>(1, 8, curQPerCat, 1);
        questionsSpinner.setEditable(false);
        questionsSpinner.setPrefWidth(80);
        qBox.getChildren().addAll(qLabel, questionsSpinner);

        configBar.getChildren().addAll(catsBox, qBox);
        root.getChildren().add(configBar);

        // 3. Load initial data from JSON
        categoryDataList.clear();
        JsonNode catsNode = (currentSetup != null && currentSetup.has("categories")) ? currentSetup.get("categories") : null;

        if (catsNode != null && catsNode.isArray() && catsNode.size() > 0) {
            for (int c = 0; c < catsNode.size(); c++) {
                JsonNode cNode = catsNode.get(c);
                String catName = cNode.has("categoryName") ? cNode.get("categoryName").asText() :
                        (cNode.has("category_name") ? cNode.get("category_name").asText() : "Category " + (c + 1));
                CategoryData catData = new CategoryData(catName);

                JsonNode qArray = cNode.has("questions") ? cNode.get("questions") : null;
                if (qArray != null && qArray.isArray()) {
                    for (JsonNode qNode : qArray) {
                        String q = (qNode.has("question") && !qNode.get("question").isNull()) ? qNode.get("question").asText() : "";
                        String a = (qNode.has("answer") && !qNode.get("answer").isNull()) ? qNode.get("answer").asText() : "";
                        String img = (qNode.has("imageUrl") && !qNode.get("imageUrl").isNull()) ? qNode.get("imageUrl").asText() :
                                ((qNode.has("image_url") && !qNode.get("image_url").isNull()) ? qNode.get("image_url").asText() : "");
                        String aud = (qNode.has("audioUrl") && !qNode.get("audioUrl").isNull()) ? qNode.get("audioUrl").asText() :
                                ((qNode.has("audio_url") && !qNode.get("audio_url").isNull()) ? qNode.get("audio_url").asText() : "");
                        catData.questions.add(new QuestionData(q, a, img, aud));
                    }
                }
                categoryDataList.add(catData);
            }
        }

        // 4. Categories Container & ScrollPane
        categoriesContainer = new VBox(16);
        rebuildCategoriesUI();

        // Listeners for spinners to maintain rectangular layout
        categoriesSpinner.valueProperty().addListener((obs, oldVal, newVal) -> rebuildCategoriesUI());
        questionsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> rebuildCategoriesUI());

        ScrollPane scrollPane = new ScrollPane(categoriesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(380);
        scrollPane.setMaxHeight(440);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        root.getChildren().add(scrollPane);
        return root;
    }

    private void rebuildCategoriesUI() {
        if (categoriesSpinner == null || questionsSpinner == null || categoriesContainer == null) return;

        int targetCats = categoriesSpinner.getValue();
        int targetQPerCat = questionsSpinner.getValue();
        String textColor = currentPalette != null ? ThemeManager.getContrastTextColor(currentPalette.bgCard) : ThemeManager.getTextPrimaryHex();

        // Ensure categoryDataList size matches targetCats
        while (categoryDataList.size() < targetCats) {
            categoryDataList.add(new CategoryData("Category #" + (categoryDataList.size() + 1)));
        }

        categoriesContainer.getChildren().clear();

        for (int c = 0; c < targetCats; c++) {
            CategoryData catData = categoryDataList.get(c);

            // Ensure questions count matches targetQPerCat
            while (catData.questions.size() < targetQPerCat) {
                catData.questions.add(new QuestionData("", "", "", ""));
            }

            VBox catCard = new VBox(10);
            catCard.getStyleClass().add("sponsor-card");
            catCard.setPadding(new Insets(12));

            // Category Name Header
            HBox catHeader = new HBox(10);
            catHeader.setAlignment(Pos.CENTER_LEFT);

            Label catHeaderLabel = new Label(String.format(I18n.get("game.editor.topic.category_label"), (c + 1)) + ":");
            catHeaderLabel.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-font-size: 13px;", textColor));

            catData.categoryNameField.setPromptText(I18n.get("game.editor.topic.category_placeholder"));
            HBox.setHgrow(catData.categoryNameField, Priority.ALWAYS);

            catHeader.getChildren().addAll(catHeaderLabel, catData.categoryNameField);
            catCard.getChildren().add(catHeader);

            // Questions list for this category
            VBox qList = new VBox(8);
            for (int q = 0; q < targetQPerCat; q++) {
                QuestionData qData = catData.questions.get(q);

                VBox qBox = new VBox(6);
                qBox.setStyle("-fx-background-color: rgba(0,0,0,0.18); -fx-padding: 8; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 8;");

                // Question Prompt and Answer Row
                HBox mainRow = new HBox(8);
                mainRow.setAlignment(Pos.CENTER_LEFT);

                Label qNumLabel = new Label("Q" + (q + 1) + ":");
                qNumLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #F59E0B; -fx-font-size: 12px; -fx-min-width: 28px;");

                qData.questionField.setPromptText(I18n.get("game.editor.topic.prompt_placeholder"));
                HBox.setHgrow(qData.questionField, Priority.ALWAYS);

                Label aLabel = new Label(I18n.get("game.editor.topic.ans_short"));
                aLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10B981; -fx-font-size: 12px;");

                qData.answerField.setPromptText(I18n.get("game.editor.topic.ans_placeholder"));
                qData.answerField.setPrefWidth(160);

                mainRow.getChildren().addAll(qNumLabel, qData.questionField, aLabel, qData.answerField);
                qBox.getChildren().add(mainRow);

                // Media row: Image (optional) & MP3 Audio (optional)
                HBox mediaRow = new HBox(8);
                mediaRow.setAlignment(Pos.CENTER_LEFT);

                // Image Field & Browse Button
                Label imgIcon = new Label("🖼");
                qData.imageField.setPromptText(I18n.get("game.editor.topic.image_placeholder"));
                HBox.setHgrow(qData.imageField, Priority.ALWAYS);

                Button browseImgBtn = new Button(I18n.get("game.editor.topic.browse"));
                browseImgBtn.getStyleClass().add("btn-small");
                browseImgBtn.setOnAction(e -> {
                    FileChooser chooser = FileChooserHelper.createChooser("Select Question Image");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp"));
                    File file = FileChooserHelper.showOpenDialog(chooser, browseImgBtn.getScene().getWindow());
                    if (file != null) {
                        qData.imageField.setText(file.getAbsolutePath());
                    }
                });

                // Audio Field & Browse Button
                Label audIcon = new Label("🎵");
                qData.audioField.setPromptText(I18n.get("game.editor.topic.audio_placeholder"));
                HBox.setHgrow(qData.audioField, Priority.ALWAYS);

                Button browseAudBtn = new Button(I18n.get("game.editor.topic.browse"));
                browseAudBtn.getStyleClass().add("btn-small");
                browseAudBtn.setOnAction(e -> {
                    FileChooser chooser = FileChooserHelper.createChooser("Select Question Audio MP3");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files (*.mp3, *.wav, *.m4a)", "*.mp3", "*.wav", "*.m4a", "*.aac", "*.ogg"));
                    File file = FileChooserHelper.showOpenDialog(chooser, browseAudBtn.getScene().getWindow());
                    if (file != null) {
                        qData.audioField.setText(file.getAbsolutePath());
                    }
                });

                mediaRow.getChildren().addAll(imgIcon, qData.imageField, browseImgBtn, audIcon, qData.audioField, browseAudBtn);
                qBox.getChildren().add(mediaRow);

                qList.getChildren().add(qBox);
            }

            catCard.getChildren().add(qList);
            categoriesContainer.getChildren().add(catCard);
        }
    }

    @Override
    public String validateSetup(List<Competitor> profiles) {
        return validateSetup(profiles, battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());
    }

    @Override
    public String validateSetup(List<Competitor> profiles, boolean battleRoyale) {
        if (categoriesSpinner == null || questionsSpinner == null) return null;
        int targetCats = categoriesSpinner.getValue();
        int targetQPerCat = questionsSpinner.getValue();

        boolean isBr = battleRoyale || (battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());
        if (isBr) {
            if (targetCats < 1) {
                return "Topic Takedown: At least 1 category is required.";
            }
        } else {
            if (targetCats < 1 || targetCats > 6) {
                return "Topic Takedown: Number of categories must be between 1 and 6.";
            }
            if (targetQPerCat < 1 || targetQPerCat > 8) {
                return "Topic Takedown: Questions per category must be between 1 and 8.";
            }
        }

        for (int c = 0; c < targetCats; c++) {
            CategoryData catData = categoryDataList.get(c);
            String catName = catData.categoryNameField.getText().trim();
            if (catName.isEmpty()) {
                return String.format("Topic Takedown: Category #%d must have a name.", c + 1);
            }

            for (int q = 0; q < targetQPerCat; q++) {
                QuestionData qData = catData.questions.get(q);
                String qText = qData.questionField.getText().trim();
                String aText = qData.answerField.getText().trim();

                if (qText.isEmpty() || aText.isEmpty()) {
                    return String.format("Topic Takedown: Question #%d in category '%s' must have both Question and Answer text.", q + 1, catName);
                }
            }
        }
        return null;
    }

    @Override
    public String validateSetupData(JsonNode setupData, List<Competitor> profiles) {
        boolean br = setupData != null && setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean();
        return validateSetupData(setupData, profiles, br);
    }

    @Override
    public String validateSetupData(JsonNode setupData, List<Competitor> profiles, boolean battleRoyale) {
        if (setupData == null) return null;
        boolean isBr = battleRoyale || (setupData.has("battleRoyale") && setupData.get("battleRoyale").asBoolean());
        int numCats = setupData.has("numCategories") ? setupData.get("numCategories").asInt(0) :
                (setupData.has("num_categories") ? setupData.get("num_categories").asInt(0) : 0);
        int qPerCat = setupData.has("questionsPerCategory") ? setupData.get("questionsPerCategory").asInt(0) :
                (setupData.has("questions_per_category") ? setupData.get("questions_per_category").asInt(0) : 0);

        if (isBr) {
            if (numCats < 1) {
                return "Topic Takedown setup invalid: At least 1 category is required.";
            }
        } else {
            if (numCats < 1 || numCats > 6) {
                return "Topic Takedown setup invalid: Categories must be between 1 and 6.";
            }
            if (qPerCat < 1 || qPerCat > 8) {
                return "Topic Takedown setup invalid: Questions per category must be between 1 and 8.";
            }
        }

        JsonNode cats = setupData.has("categories") ? setupData.get("categories") : null;
        if (cats == null || !cats.isArray() || cats.size() < (isBr ? 1 : numCats)) {
            return "Topic Takedown setup invalid: Incomplete categories.";
        }

        return null;
    }

    @Override
    public JsonNode getUpdatedSetup() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("game", "Topic_Takedown");
        root.put("battleRoyale", battleRoyaleCheckBox != null && battleRoyaleCheckBox.isSelected());

        int targetCats = categoriesSpinner.getValue();
        int targetQPerCat = questionsSpinner.getValue();

        root.put("numCategories", targetCats);
        root.put("num_categories", targetCats);
        root.put("questionsPerCategory", targetQPerCat);
        root.put("questions_per_category", targetQPerCat);

        ArrayNode catArr = root.putArray("categories");

        for (int c = 0; c < targetCats; c++) {
            CategoryData catData = categoryDataList.get(c);
            ObjectNode catObj = objectMapper.createObjectNode();
            String name = catData.categoryNameField.getText().trim();
            if (name.isEmpty()) name = "Category #" + (c + 1);

            catObj.put("categoryName", name);
            catObj.put("category_name", name);

            ArrayNode qArr = catObj.putArray("questions");
            for (int q = 0; q < targetQPerCat; q++) {
                QuestionData qData = catData.questions.get(q);
                ObjectNode qObj = objectMapper.createObjectNode();
                qObj.put("difficulty", q + 1);
                qObj.put("question", qData.questionField.getText().trim());
                qObj.put("answer", qData.answerField.getText().trim());
                qObj.put("imageUrl", qData.imageField.getText().trim());
                qObj.put("audioUrl", qData.audioField.getText().trim());
                qArr.add(qObj);
            }

            catArr.add(catObj);
        }

        return root;
    }
}
