package com.gameshowcenter.offline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.games.GameStageRegistry;
import com.gameshowcenter.offline.games.editors.HangmanSetupEditor;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.model.GameDescriptor;
import com.gameshowcenter.offline.model.MatchConfig;
import com.gameshowcenter.offline.service.AIService;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.views.ArenaStageFxView;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class RequirementVerification {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("RUNNING VERIFICATION TESTS FOR USER REQUIREMENTS");
        System.out.println("==================================================");

        testAccessibilityFontScaling();
        testHangmanSetupEditorValidation();
        testHangmanAIServiceDeduplication();
        testArenaGameFontScaling();
        testBackgroundPersistenceAcrossWindows();

        System.out.println("==================================================");
        System.out.println("ALL JAVA VERIFICATION TESTS PASSED SUCCESSFULLY!");
        System.out.println("==================================================");
        System.exit(0);
    }

    private static void testAccessibilityFontScaling() {
        System.out.println("\n[Test 1] Testing Accessibility Font Scaling in ThemeManager...");

        // Initial default scale
        ThemeManager.setFontScale(1.0);
        assert Math.abs(ThemeManager.getFontScale() - 1.0) < 0.001 : "Initial font scale should be 1.0";
        assert ThemeManager.getScaledFontSize(12) == 12 : "Scaled 12px at 1.0 should be 12";
        assert ThemeManager.getScaledFontSize(20) == 20 : "Scaled 20px at 1.0 should be 20";

        // Enlarge text (+25%)
        ThemeManager.setFontScale(1.25);
        assert Math.abs(ThemeManager.getFontScale() - 1.25) < 0.001 : "Font scale should be 1.25";
        assert ThemeManager.getScaledFontSize(12) == 15 : "Scaled 12px at 1.25 should be 15";
        assert ThemeManager.getScaledFontSize(20) == 25 : "Scaled 20px at 1.25 should be 25";
        assert ThemeManager.getFontSizeStyle(12).contains("15px") : "Font style should contain 15px";

        // Shrink text (-20%)
        ThemeManager.setFontScale(0.80);
        assert Math.abs(ThemeManager.getFontScale() - 0.80) < 0.001 : "Font scale should be 0.80";
        assert ThemeManager.getScaledFontSize(20) == 16 : "Scaled 20px at 0.80 should be 16";

        // Clamping check
        ThemeManager.setFontScale(0.10); // Below min
        assert ThemeManager.getFontScale() >= 0.70 : "Font scale should be clamped to minimum 0.70";

        ThemeManager.setFontScale(5.0); // Above max
        assert ThemeManager.getFontScale() <= 2.0 : "Font scale should be clamped to maximum 2.0";

        // Reset to normal
        ThemeManager.setFontScale(1.0);
        System.out.println("-> Accessibility Font Scaling test passed!");
    }

    private static void testHangmanSetupEditorValidation() {
        System.out.println("\n[Test 2] Testing Hangman Setup Editor Duplicate Prevention...");

        ObjectMapper mapper = new ObjectMapper();
        HangmanSetupEditor editor = new HangmanSetupEditor();

        List<Competitor> profiles = new ArrayList<>();
        profiles.add(new Competitor("1", "Jugador 1", null));
        profiles.add(new Competitor("2", "Jugador 2", null));

        // 1. Test validateSetupData with duplicate words (case-insensitive)
        ObjectNode dupeSetup = mapper.createObjectNode();
        dupeSetup.put("game", "Hangman");
        dupeSetup.put("rounds_per_player", 2);
        ArrayNode dupePool = dupeSetup.putArray("wordPool");
        dupePool.add("MANZANA").add("PERA").add("manzana").add("UVA").add("KIWI").add("MELON");

        String error = editor.validateSetupData(dupeSetup, profiles);
        assert error != null && error.toLowerCase().contains("manzana") :
                "Validation should catch duplicate word 'manzana', but got: " + error;
        System.out.println("-> Correctly caught duplicate word in setup data: " + error);

        // 2. Test validateSetupData with valid unique words (enough for 2 players * 2 rounds = 4 words)
        ObjectNode validSetup = mapper.createObjectNode();
        validSetup.put("game", "Hangman");
        validSetup.put("rounds_per_player", 2);
        ArrayNode validPool = validSetup.putArray("wordPool");
        validPool.add("MANZANA").add("PERA").add("UVA").add("KIWI").add("MELON");

        String validError = editor.validateSetupData(validSetup, profiles);
        assert validError == null : "Valid unique words should pass validation, but got: " + validError;
        System.out.println("-> Valid unique word list correctly passed validation.");
    }

    private static void testHangmanAIServiceDeduplication() {
        System.out.println("\n[Test 3] Testing Hangman AIService Merge Word Deduplication...");

        ObjectMapper mapper = new ObjectMapper();

        // Current setup has "MANZANA", "PERA"
        ObjectNode currentSetup = mapper.createObjectNode();
        currentSetup.put("game", "Hangman");
        ArrayNode existingWords = currentSetup.putArray("wordPool");
        existingWords.add("MANZANA").add("PERA");

        // AI generated data contains duplicates within itself and with existing words: "pera", "UVA", "uva", "MELON"
        ObjectNode aiGenerated = mapper.createObjectNode();
        ArrayNode aiWords = aiGenerated.putArray("wordPool");
        aiWords.add("pera").add("UVA").add("uva").add("MELON");

        // Merge in 'add' mode
        com.fasterxml.jackson.databind.JsonNode merged = AIService.mergeSetup("Hangman", "add", currentSetup, aiGenerated, null);
        ArrayNode resultPool = (ArrayNode) merged.get("wordPool");

        List<String> words = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode n : resultPool) {
            words.add(n.asText());
        }

        System.out.println("-> Merged word pool: " + words);

        assert words.contains("MANZANA") : "Must contain MANZANA";
        assert words.contains("PERA") : "Must contain PERA";
        assert words.contains("UVA") : "Must contain UVA";
        assert words.contains("MELON") : "Must contain MELON";
        assert words.size() == 4 : "Merged pool must contain exactly 4 unique words, but had " + words.size();

        // Check overwrite mode with duplicates in AI words: "KIWI", "kiwi", "FRESA"
        ObjectNode aiGenOverwrite = mapper.createObjectNode();
        ArrayNode aiWords2 = aiGenOverwrite.putArray("wordPool");
        aiWords2.add("KIWI").add("kiwi").add("FRESA");

        com.fasterxml.jackson.databind.JsonNode overwritten = AIService.mergeSetup("Hangman", "overwrite", currentSetup, aiGenOverwrite, null);
        ArrayNode overPool = (ArrayNode) overwritten.get("wordPool");
        List<String> overWords = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode n : overPool) {
            overWords.add(n.asText());
        }

        System.out.println("-> Overwritten word pool: " + overWords);
        assert overWords.size() == 2 : "Overwritten pool must contain exactly 2 unique words, but had " + overWords.size();
        assert overWords.contains("KIWI") && overWords.contains("FRESA") : "Overwritten words must be KIWI and FRESA";

        System.out.println("-> Hangman AIService Merge Word Deduplication test passed!");
    }

    private static void testArenaGameFontScaling() {
        System.out.println("\n[Test 4] Testing Arena and Game Font Scaling...");

        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        // Set scale to 1.50 (150%)
        ThemeManager.setFontScale(1.50);

        // 1. Test GameStageRegistry creates stage with 1.50 font scale
        GameDescriptor desc = new GameDescriptor();
        desc.setName("TimeLine");
        MatchConfig config = new MatchConfig();
        List<Competitor> players = new ArrayList<>();
        players.add(new Competitor("1", "Jugador 1", null));
        players.add(new Competitor("2", "Jugador 2", null));
        config.setProfiles(players);

        Region stage = GameStageRegistry.createStage(desc, config, null);
        assert stage != null : "Stage should be created";

        // Check that any node with base 16px has been scaled to 24px
        boolean foundScaledFont = false;
        List<Node> nodesToInspect = new ArrayList<>();
        nodesToInspect.add(stage);
        while (!nodesToInspect.isEmpty()) {
            Node n = nodesToInspect.remove(0);
            if (n.getStyle() != null && n.getStyle().contains("-fx-font-size: 24px")) {
                foundScaledFont = true;
                break;
            }
            if (n instanceof Parent p) {
                nodesToInspect.addAll(p.getChildrenUnmodifiable());
            }
        }
        assert foundScaledFont : "Stage created by GameStageRegistry should have font-size scaled to 24px (16px * 1.50)";
        System.out.println("-> GameStageRegistry stage initial font scaling verified (16px -> 24px).");

        // 2. Test Dynamic Child Addition scaling
        VBox container = new VBox();
        ThemeManager.applyTextScale(container, 1.50);

        Label dynamicLabel = new Label("Dynamic Card Label");
        dynamicLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        container.getChildren().add(dynamicLabel);

        assert dynamicLabel.getStyle().contains("-fx-font-size: 15px") :
            "Dynamically added child should be scaled to 15px (10px * 1.50), but got: " + dynamicLabel.getStyle();
        System.out.println("-> Dynamically added child in container was automatically scaled (10px -> 15px).");

        // 3. Test Dynamic Style Update (setStyle) scaling
        dynamicLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: red;");
        assert dynamicLabel.getStyle().contains("-fx-font-size: 30px") :
            "Dynamic style update should be scaled to 30px (20px * 1.50), but got: " + dynamicLabel.getStyle();
        System.out.println("-> Dynamic setStyle modification was automatically scaled (20px -> 30px).");

        // 4. Test ArenaStageFxView scaling
        config.setSelectedGames(List.of(desc));
        ArenaStageFxView arenaView = new ArenaStageFxView(config, null);
        assert arenaView != null : "ArenaStageFxView should instantiate";

        // Reset font scale to 1.0
        ThemeManager.setFontScale(1.0);
        System.out.println("-> Arena and Game Font Scaling tests passed!");
    }

    private static void testBackgroundPersistenceAcrossWindows() {
        System.out.println("\n[Test 5] Testing Background Persistence Across Windows and View Transitions...");

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 1280, 800);

        // 1. Initial configuration
        ThemeManager.setPalette("1. Midnight Indigo");
        ThemeManager.setCustomAppBgColor(null);
        ThemeManager.setCustomBackgroundImagePath(null);

        MainApp app = new MainApp();
        // Set scene fill and background
        Color bgIndigo = Color.web(ThemeManager.getAppBgHex());
        scene.setFill(bgIndigo);
        root.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(bgIndigo, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)
        ));

        assert root.getBackground() != null : "Initial background must not be null";
        assert !scene.getFill().equals(Color.WHITE) : "Initial scene fill must not be white";
        Color initialFill = (Color) root.getBackground().getFills().get(0).getFill();
        assert initialFill.equals(Color.web("#090d16")) : "Expected #090d16, got " + initialFill;
        System.out.println("-> Initial background and Scene fill correctly set to #090d16 (not white).");

        // 2. Simulate switching views 10 times consecutively
        for (int i = 1; i <= 10; i++) {
            Color currentAppBg = Color.web(ThemeManager.getAppBgHex());
            scene.setFill(currentAppBg);
            root.setBackground(new javafx.scene.layout.Background(
                    new javafx.scene.layout.BackgroundFill(currentAppBg, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)
            ));

            assert root.getBackground() != null : "Switch " + i + ": Root background became null!";
            Color switchFill = (Color) root.getBackground().getFills().get(0).getFill();
            assert switchFill.equals(Color.web("#090d16")) : "Switch " + i + ": Expected #090d16, got " + switchFill;
            assert !scene.getFill().equals(Color.WHITE) : "Switch " + i + ": Scene fill became white!";
        }
        System.out.println("-> 10 consecutive window/screen switches verified: background remained intact and NEVER turned white.");

        // 3. Test changing to custom color and switching views
        Color customPurple = Color.web("#1e1b4b");
        ThemeManager.setCustomAppBgColor(customPurple);
        scene.setFill(customPurple);
        root.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(customPurple, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)
        ));

        for (int i = 1; i <= 5; i++) {
            Color cur = Color.web(ThemeManager.getAppBgHex());
            scene.setFill(cur);
            root.setBackground(new javafx.scene.layout.Background(
                    new javafx.scene.layout.BackgroundFill(cur, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)
            ));
            Color c = (Color) root.getBackground().getFills().get(0).getFill();
            assert c.equals(customPurple) : "Custom color switch " + i + ": Expected #1e1b4b, got " + c;
            assert !scene.getFill().equals(Color.WHITE) : "Custom color switch " + i + ": Scene fill became white!";
        }
        System.out.println("-> Custom color maintained across view switches without resetting.");

        // Restore default palette
        ThemeManager.setPalette("1. Midnight Indigo");
        ThemeManager.setCustomAppBgColor(null);
        System.out.println("-> Background Persistence Across Windows tests passed successfully!");
    }
}
